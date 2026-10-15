package org.example.maniacrevolution.warden;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.game.GameManager;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.WardenScentPacket;
import java.util.*;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public final class WardenSniffManager {
    private record Source(ServerPlayer player, ResourceLocation dimension, int classId, WardenScentTrail trail) {}
    private record Listener(ServerPlayer player, ResourceLocation dimension, WardenSniffCycle cycle) {}
    private static final Map<UUID, Source> SOURCES = new HashMap<>();
    private static final Map<UUID, Listener> LISTENERS = new HashMap<>();
    private WardenSniffManager() {}
    private static boolean mode(ServerPlayer player) {
        return player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL || player.gameMode.getGameModeForPlayer() == GameType.ADVENTURE;
    }
    private static String team(ServerPlayer player) { return player.getTeam() == null ? null : player.getTeam().getName(); }
    private static boolean active(ServerPlayer player) {
        return WardenMatchRules.active(player.isAlive(), mode(player), GameManager.getPhaseValue(),
                PlayerDataManager.get(player).getManiacClassId(), team(player));
    }
    private static boolean source(ServerPlayer player) {
        return "survivors".equalsIgnoreCase(team(player)) && WardenMatchRules.participant(player.isAlive(), mode(player), GameManager.getPhaseValue(), team(player));
    }
    private static ResourceLocation dimension(ServerPlayer player) { return player.level().dimension().location(); }
    public static void activate(ServerPlayer player) {
        if (!active(player)) return;
        var listener = LISTENERS.get(player.getUUID());
        if (listener == null || listener.player != player || !listener.dimension.equals(dimension(player))) {
            listener = new Listener(player, dimension(player), new WardenSniffCycle()); LISTENERS.put(player.getUUID(), listener);
        }
        if (listener.cycle.activate(player.level().getGameTime())) {
            send(listener, player.level().getGameTime());
            WardenAnimationSync.sniff(player);
            player.serverLevel().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.WARDEN_SNIFF,
                    net.minecraft.sounds.SoundSource.PLAYERS, 5.0F, 1.0F);
        }
    }
    private static void send(Listener listener, long now) {
        var cycle = listener.cycle;
        var points = cycle.duration(now) == 0 ? List.<WardenScentPoint>of() : SOURCES.values().stream()
                .filter(s -> s.dimension.equals(listener.dimension)).flatMap(s -> s.trail.nearby(listener.player.position(), now).stream())
                .sorted(Comparator.comparingDouble((WardenScentPoint p) -> p.position().distanceToSqr(listener.player.position()))
                        .thenComparing(Comparator.comparingLong(WardenScentPoint::tick).reversed()))
                .limit(WardenScentPacket.MAX_POINTS).toList();
        ModNetworking.sendToPlayer(new WardenScentPacket(listener.dimension, now, cycle.preparation(now), cycle.duration(now), cycle.cooldown(now), points), listener.player);
    }
    private static void clear(Listener listener) {
        ModNetworking.sendToPlayer(new WardenScentPacket(dimension(listener.player), listener.player.level().getGameTime(), 0, 0, 0, List.of()), listener.player);
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var server = ServerLifecycleHooks.getCurrentServer(); if (server == null) return;
        if (GameManager.getPhaseValue() < 1 || GameManager.getPhaseValue() > 3) {
            LISTENERS.values().forEach(WardenSniffManager::clear); LISTENERS.clear(); SOURCES.clear(); return;
        }
        SOURCES.entrySet().removeIf(e -> {
            var player = server.getPlayerList().getPlayer(e.getKey());
            return player == null || !source(player) || player != e.getValue().player || !dimension(player).equals(e.getValue().dimension)
                    || PlayerDataManager.get(player).getSurvivorClassId() != e.getValue().classId;
        });
        for (var player : server.getPlayerList().getPlayers()) if (source(player)) {
            var trail = SOURCES.computeIfAbsent(player.getUUID(), id -> new Source(player, dimension(player),
                    PlayerDataManager.get(player).getSurvivorClassId(), new WardenScentTrail()));
            trail.trail.sample(player.position(), player.level().getGameTime(), player.isShiftKeyDown());
        }
        LISTENERS.entrySet().removeIf(e -> {
            var player = server.getPlayerList().getPlayer(e.getKey()); var listener = e.getValue();
            boolean invalid = player == null || !active(player) || player != listener.player || !dimension(player).equals(listener.dimension);
            if (invalid && player != null) clear(listener);
            return invalid;
        });
        for (var listener : LISTENERS.values()) {
            long now = listener.player.level().getGameTime();
            // Exact transition packets, plus bounded refresh of changing trails/HUD.
            if (now % 4 == 0 || listener.cycle.preparation(now) == 0 && listener.cycle.duration(now) == WardenSniffCycle.DURATION
                    || listener.cycle.cooldown(now) == WardenSniffCycle.COOLDOWN - WardenSniffCycle.PREPARATION - WardenSniffCycle.DURATION
                    || listener.cycle.cooldown(now) == 0) send(listener, now);
        }
        LISTENERS.entrySet().removeIf(e -> e.getValue().cycle.cooldown(e.getValue().player.level().getGameTime()) == 0);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { SOURCES.remove(event.getEntity().getUUID()); LISTENERS.remove(event.getEntity().getUUID()); }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { SOURCES.clear(); LISTENERS.clear(); }
}
