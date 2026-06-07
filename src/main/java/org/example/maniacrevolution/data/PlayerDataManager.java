package org.example.maniacrevolution.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.character.CharacterType;
import org.example.maniacrevolution.game.GameManager;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.SyncPlayerClassPacket;
import org.example.maniacrevolution.network.packets.SyncPlayerDataPacket;
import org.example.maniacrevolution.perk.PerkPhase;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {
    private static final Map<UUID, PlayerData> PLAYER_DATA = new ConcurrentHashMap<>();
    private static MinecraftServer server;

    // ФИКС: Счётчик для периодической синхронизации кулдаунов
    private static int syncTickCounter = 0;
    private static final int SYNC_INTERVAL = 10; // Каждые 10 тиков (0.5 сек)

    public static PlayerData getOrCreate(UUID uuid) {
        return PLAYER_DATA.computeIfAbsent(uuid, PlayerData::new);
    }

    public static PlayerData get(ServerPlayer player) {
        return getOrCreate(player.getUUID());
    }

    public static void syncToClient(ServerPlayer player) {
        PlayerData data = get(player);
        ModNetworking.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncPlayerDataPacket(data)
        );
    }

    public static void syncClassToClient(ServerPlayer player) {
        PlayerData data = get(player);
        ModNetworking.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncPlayerClassPacket(CharacterType.SURVIVOR, data.getSurvivorClassId())
        );
        ModNetworking.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncPlayerClassPacket(CharacterType.MANIAC, data.getManiacClassId())
        );
    }

    public static void setSelectedClass(ServerPlayer player, CharacterType type, int classId) {
        get(player).setSelectedClass(type, classId);
        syncClassToClient(player);
    }

    public static void clearSelectedClasses(ServerPlayer player) {
        get(player).clearSelectedClasses();
        syncClassToClient(player);
    }

    public static boolean isSelectedClass(ServerPlayer player, CharacterType type, int classId) {
        return get(player).isSelectedClass(type, classId);
    }

    public static void syncToAll() {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncToClient(player);
            syncClassToClient(player);
        }
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerData data = getOrCreate(player.getUUID());
            migrateClassFromScoreboardIfMissing(player, data);
            syncToClient(player);
            syncClassToClient(player);
        }
    }

    private static void migrateClassFromScoreboardIfMissing(ServerPlayer player, PlayerData data) {
        if (data.getSurvivorClassId() != -1 || data.getManiacClassId() != -1) {
            return;
        }

        Scoreboard scoreboard = player.getServer().getScoreboard();
        String teamName = player.getTeam() == null ? "" : player.getTeam().getName();
        if ("maniac".equalsIgnoreCase(teamName)) {
            migrateClassTypeFromScoreboard(player, data, scoreboard, CharacterType.MANIAC);
        } else if ("survivors".equalsIgnoreCase(teamName)) {
            migrateClassTypeFromScoreboard(player, data, scoreboard, CharacterType.SURVIVOR);
        } else {
            migrateClassTypeFromScoreboard(player, data, scoreboard, CharacterType.SURVIVOR);
            if (data.getSurvivorClassId() == -1) {
                migrateClassTypeFromScoreboard(player, data, scoreboard, CharacterType.MANIAC);
            }
        }
    }

    private static void migrateClassTypeFromScoreboard(ServerPlayer player, PlayerData data,
                                                       Scoreboard scoreboard, CharacterType type) {
        Objective objective = scoreboard.getObjective(type.getScoreboardName());
        if (objective == null || !scoreboard.hasPlayerScore(player.getScoreboardName(), objective)) {
            return;
        }

        int score = scoreboard.getOrCreatePlayerScore(player.getScoreboardName(), objective).getScore();
        if (score > 0) {
            data.setSelectedClass(type, score);
        }
    }

    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerData data = PLAYER_DATA.get(player.getUUID());
            if (data != null) {
                data.clearPerks(player);
            }
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (server == null) return;

        int gamePhase = GameManager.getPhaseValue();

        // ФИКС: Увеличиваем счётчик и проверяем нужна ли синхронизация
        syncTickCounter++;
        boolean shouldSync = syncTickCounter >= SYNC_INTERVAL;
        if (shouldSync) {
            syncTickCounter = 0;
        }

        PerkPhase currentPhase = PerkPhase.fromScoreboardValue(gamePhase);

        // Тикаем перки всех игроков
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerData data = PLAYER_DATA.get(player.getUUID());
            if (data != null) {
                // Тикаем перки только если игра идёт
                if (currentPhase != null && gamePhase != 0) {
                    data.tick(player, currentPhase);
                }

                // ФИКС: Периодически синхронизируем кулдауны с клиентом
                if (shouldSync && !data.getSelectedPerks().isEmpty()) {
                    syncToClient(player);
                }
            }
        }
    }

    public static void load(MinecraftServer srv) {
        server = srv;
        PLAYER_DATA.clear();

        File file = getDataFile(srv);
        if (file.exists()) {
            try {
                CompoundTag root = NbtIo.readCompressed(file);
                CompoundTag playersTag = root.getCompound("players");

                for (String uuidStr : playersTag.getAllKeys()) {
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        CompoundTag playerTag = playersTag.getCompound(uuidStr);
                        PLAYER_DATA.put(uuid, PlayerData.load(uuid, playerTag));
                    } catch (IllegalArgumentException e) {
                        Maniacrev.LOGGER.warn("Invalid UUID: {}", uuidStr);
                    }
                }
            } catch (Exception e) {
                Maniacrev.LOGGER.error("Failed to load player data", e);
            }
        }
    }

    public static void save(MinecraftServer srv) {
        File file = getDataFile(srv);
        try {
            CompoundTag root = new CompoundTag();
            CompoundTag playersTag = new CompoundTag();

            for (var entry : PLAYER_DATA.entrySet()) {
                playersTag.put(entry.getKey().toString(), entry.getValue().save());
            }

            root.put("players", playersTag);
            NbtIo.writeCompressed(root, file);
        } catch (Exception e) {
            Maniacrev.LOGGER.error("Failed to save player data", e);
        }
    }

    private static File getDataFile(MinecraftServer srv) {
        File worldDir = srv.getWorldPath(LevelResource.ROOT).toFile();
        File modDir = new File(worldDir, "maniacrev");
        if (!modDir.exists()) modDir.mkdirs();
        return new File(modDir, "playerdata.dat");
    }
}
