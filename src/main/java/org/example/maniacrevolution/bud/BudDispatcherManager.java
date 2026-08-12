package org.example.maniacrevolution.bud;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.data.PlayerData;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.game.GameManager;
import org.example.maniacrevolution.hack.ComputerBlockEntity;
import org.example.maniacrevolution.hack.HackConfig;
import org.example.maniacrevolution.hack.HackManager;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.BudDispatcherPacket;
import org.example.maniacrevolution.perk.PerkPhase;
import org.example.maniacrevolution.perk.perks.maniac.BudDispatcherPerk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

/** Серверное состояние соответствия «логический компьютер → цветок». */
public final class BudDispatcherManager {
    private static final Map<Integer, Integer> FLOWER_BY_COMPUTER = new LinkedHashMap<>();
    private static final Map<Integer, Integer> STAGE_BY_COMPUTER = new HashMap<>();
    private static final List<Integer> SHUFFLED_FLOWERS = new ArrayList<>();
    private static final Map<UUID, ServerPlayer> SYNCHRONIZED_PLAYERS = new HashMap<>();

    private static boolean initialized;
    private static int nextFlowerIndex;
    private static int lastMatchStartTick = Integer.MIN_VALUE;

    private BudDispatcherManager() {
    }

    /** Вызывается всеми владельцами в один серверный тик, но перемешивает цветы лишь один раз. */
    public static synchronized void startMatch(MinecraftServer server) {
        if (server == null) return;
        int serverTick = server.getTickCount();
        if (initialized && lastMatchStartTick == serverTick) {
            return;
        }

        initialize(server, serverTick);
        syncAllEligiblePlayers(server);
    }

    /**
     * Вызывается самим ComputerBlockEntity. Сеть используется только при переходе
     * через одну из шести визуальных стадий, а не на каждом проценте прогресса.
     */
    public static synchronized void onComputerProgressChanged(ComputerBlockEntity computer,
                                                               float normalizedProgress) {
        if (computer == null || computer.getLevel() == null || computer.getLevel().isClientSide()) return;
        if (!initialized || computer.getLevel().getServer() == null) return;

        int computerId = computer.getComputerId();
        boolean assignedNow = !FLOWER_BY_COMPUTER.containsKey(computerId);
        int flowerIndex = ensureAssignment(computerId);
        if (flowerIndex < 0) return;

        int stage = BudDispatcherPerk.getWiltStage(normalizedProgress);
        Integer previousStage = STAGE_BY_COMPUTER.put(computerId, stage);
        if (!assignedNow && previousStage != null && previousStage == stage) return;

        BudDispatcherPacket packet = BudDispatcherPacket.update(computerId, flowerIndex, stage);
        for (ServerPlayer player : computer.getLevel().getServer().getPlayerList().getPlayers()) {
            if (canReceiveState(player)) {
                ModNetworking.sendToPlayer(packet, player);
            }
        }
    }

    public static synchronized void syncFullState(ServerPlayer player, boolean openScreen) {
        if (player == null || player.server == null) return;
        ensureInitialized(player.server);
        refreshKnownStages();
        ModNetworking.sendToPlayer(BudDispatcherPacket.full(snapshotEntries(), openScreen), player);
        SYNCHRONIZED_PLAYERS.put(player.getUUID(), player);
    }

    /** Один лёгкий lookup за тик; сеть используется лишь для нового объекта игрока после входа. */
    public static synchronized void ensurePlayerSynchronized(ServerPlayer player) {
        if (player == null || !canReceiveState(player)) return;
        if (SYNCHRONIZED_PLAYERS.get(player.getUUID()) != player) {
            syncFullState(player, false);
        }
    }

    private static void ensureInitialized(MinecraftServer server) {
        if (!initialized) {
            initialize(server, server.getTickCount());
        }
    }

    private static void initialize(MinecraftServer server, int serverTick) {
        FLOWER_BY_COMPUTER.clear();
        STAGE_BY_COMPUTER.clear();
        SHUFFLED_FLOWERS.clear();
        SYNCHRONIZED_PLAYERS.clear();
        nextFlowerIndex = 0;

        for (int flower = 0; flower < BudDispatcherPerk.FLOWER_VARIANT_COUNT; flower++) {
            SHUFFLED_FLOWERS.add(flower);
        }
        Collections.shuffle(SHUFFLED_FLOWERS);

        TreeSet<Integer> computerIds = collectLoadedComputerIds(server);
        for (int computerId : computerIds) {
            if (FLOWER_BY_COMPUTER.size() >= BudDispatcherPerk.COMPUTER_COUNT) break;
            ensureAssignment(computerId);
        }

        initialized = true;
        lastMatchStartTick = serverTick;
        refreshKnownStages();
        Maniacrev.LOGGER.info("[BudDispatcher] Assigned {} unique flowers for this match",
                FLOWER_BY_COMPUTER.size());
    }

    private static TreeSet<Integer> collectLoadedComputerIds(MinecraftServer server) {
        TreeSet<Integer> ids = new TreeSet<>();
        for (BlockPos pos : ComputerBlockEntity.getTrackedPositionsSnapshot()) {
            for (ServerLevel level : server.getAllLevels()) {
                if (level.getBlockEntity(pos) instanceof ComputerBlockEntity computer) {
                    ids.add(computer.getComputerId());
                }
            }
        }
        return ids;
    }

    private static int ensureAssignment(int computerId) {
        Integer existing = FLOWER_BY_COMPUTER.get(computerId);
        if (existing != null) return existing;
        if (nextFlowerIndex >= SHUFFLED_FLOWERS.size()
                || FLOWER_BY_COMPUTER.size() >= BudDispatcherPerk.COMPUTER_COUNT) {
            Maniacrev.LOGGER.warn("[BudDispatcher] Cannot assign a unique flower to computer {}: limit is {}",
                    computerId, BudDispatcherPerk.COMPUTER_COUNT);
            return -1;
        }

        int flowerIndex = SHUFFLED_FLOWERS.get(nextFlowerIndex++);
        FLOWER_BY_COMPUTER.put(computerId, flowerIndex);
        STAGE_BY_COMPUTER.put(computerId, currentStage(computerId));
        return flowerIndex;
    }

    private static void refreshKnownStages() {
        for (int computerId : FLOWER_BY_COMPUTER.keySet()) {
            STAGE_BY_COMPUTER.put(computerId, currentStage(computerId));
        }
    }

    private static int currentStage(int computerId) {
        float required = Math.max(0.0001F, HackConfig.HACK_POINTS_REQUIRED);
        float normalized = HackManager.get().getLiveProgress(computerId) / required;
        if (HackManager.get().isHacked(computerId)) {
            normalized = 1.0F;
        }
        return BudDispatcherPerk.getWiltStage(normalized);
    }

    private static List<BudDispatcherPacket.Entry> snapshotEntries() {
        List<BudDispatcherPacket.Entry> entries = new ArrayList<>();
        FLOWER_BY_COMPUTER.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> entries.add(new BudDispatcherPacket.Entry(
                        entry.getKey(),
                        entry.getValue(),
                        STAGE_BY_COMPUTER.getOrDefault(entry.getKey(), 0)
                )));
        return entries;
    }

    private static void syncAllEligiblePlayers(MinecraftServer server) {
        refreshKnownStages();
        BudDispatcherPacket packet = BudDispatcherPacket.full(snapshotEntries(), false);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (canReceiveState(player)) {
                ModNetworking.sendToPlayer(packet, player);
                SYNCHRONIZED_PLAYERS.put(player.getUUID(), player);
            }
        }
    }

    private static boolean canReceiveState(ServerPlayer player) {
        PerkPhase phase = GameManager.getCurrentPhase();
        if (phase != PerkPhase.HUNT && phase != PerkPhase.MIDGAME) return false;
        PlayerData data = PlayerDataManager.get(player);
        return data != null && data.getPerkInstance(BudDispatcherPerk.ID) != null;
    }
}
