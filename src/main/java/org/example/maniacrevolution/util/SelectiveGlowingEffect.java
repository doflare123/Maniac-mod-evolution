package org.example.maniacrevolution.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.SelectiveGlowPacket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Keeps viewer-specific glowing entirely outside vanilla entity metadata.
 * Packets are sent only when a viewer starts or stops seeing a target glow.
 */
@Mod.EventBusSubscriber
public final class SelectiveGlowingEffect {
    private static final Map<UUID, TargetGlow> TARGETS = new HashMap<>();

    private SelectiveGlowingEffect() {
    }

    public static void addGlowing(Entity target, ServerPlayer viewer, int durationTicks) {
        if (target == null || viewer == null || target.level().isClientSide() || durationTicks <= 0) {
            return;
        }

        TargetGlow state = TARGETS.get(target.getUUID());
        if (state == null || state.target != target) {
            if (state != null) {
                disableForAll(state);
            }
            state = new TargetGlow(target);
            TARGETS.put(target.getUUID(), state);
        }

        long expiresAt = viewer.server.getTickCount() + (long) durationTicks;
        Long oldExpiry = state.viewers.put(viewer.getUUID(), expiresAt);
        if (oldExpiry == null) {
            send(target, viewer, true);
        }
    }

    public static void addGlowing(Entity target, List<ServerPlayer> viewers, int durationTicks) {
        for (ServerPlayer viewer : viewers) {
            addGlowing(target, viewer, durationTicks);
        }
    }

    public static void addGlowingMultiple(List<? extends Entity> targets, ServerPlayer viewer, int durationTicks) {
        for (Entity target : targets) {
            addGlowing(target, viewer, durationTicks);
        }
    }

    public static void removeGlowing(Entity target, ServerPlayer viewer) {
        if (target == null || viewer == null) {
            return;
        }

        TargetGlow state = TARGETS.get(target.getUUID());
        if (state == null || state.viewers.remove(viewer.getUUID()) == null) {
            return;
        }

        send(state.target, viewer, false);
        if (state.viewers.isEmpty()) {
            TARGETS.remove(target.getUUID());
        }
    }

    public static void removeAllGlowing(Entity target) {
        if (target == null || target.level().isClientSide()) {
            return;
        }

        TargetGlow state = TARGETS.remove(target.getUUID());
        if (state != null) {
            disableForAll(state);
        }
    }

    public static boolean isGlowing(Entity target, ServerPlayer viewer) {
        if (target == null || viewer == null) {
            return false;
        }

        TargetGlow state = TARGETS.get(target.getUUID());
        Long expiresAt = state == null ? null : state.viewers.get(viewer.getUUID());
        return expiresAt != null && viewer.server.getTickCount() < expiresAt;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || TARGETS.isEmpty()) {
            return;
        }

        long currentTick = event.getServer().getTickCount();
        Iterator<Map.Entry<UUID, TargetGlow>> targets = TARGETS.entrySet().iterator();
        while (targets.hasNext()) {
            TargetGlow state = targets.next().getValue();
            if (state.target.isRemoved()) {
                disableForAll(state);
                targets.remove();
                continue;
            }

            Iterator<Map.Entry<UUID, Long>> viewers = state.viewers.entrySet().iterator();
            while (viewers.hasNext()) {
                Map.Entry<UUID, Long> viewerEntry = viewers.next();
                ServerPlayer viewer = event.getServer().getPlayerList().getPlayer(viewerEntry.getKey());
                if (viewer == null) {
                    viewers.remove();
                } else if (currentTick >= viewerEntry.getValue()) {
                    send(state.target, viewer, false);
                    viewers.remove();
                }
            }

            if (state.viewers.isEmpty()) {
                targets.remove();
            }
        }
    }

    public static void onPlayerLogout(ServerPlayer player) {
        removeAllGlowing(player);

        Iterator<Map.Entry<UUID, TargetGlow>> targets = TARGETS.entrySet().iterator();
        while (targets.hasNext()) {
            TargetGlow state = targets.next().getValue();
            state.viewers.remove(player.getUUID());
            if (state.viewers.isEmpty()) {
                targets.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        TARGETS.clear();
    }

    private static void disableForAll(TargetGlow state) {
        if (state.target.getServer() == null) {
            return;
        }

        for (UUID viewerId : new ArrayList<>(state.viewers.keySet())) {
            ServerPlayer viewer = state.target.getServer().getPlayerList().getPlayer(viewerId);
            if (viewer != null) {
                send(state.target, viewer, false);
            }
        }
    }

    private static void send(Entity target, ServerPlayer viewer, boolean enabled) {
        ModNetworking.sendToPlayer(new SelectiveGlowPacket(target, enabled), viewer);
    }

    private static final class TargetGlow {
        private final Entity target;
        private final Map<UUID, Long> viewers = new HashMap<>();

        private TargetGlow(Entity target) {
            this.target = target;
        }
    }
}
