package org.example.maniacrevolution.warden;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.WardenAnimationPacket;
import java.util.*;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public final class WardenAnimationSync {
    private static final class Session {
        final ServerPlayer player; final Object dimension;
        WardenAnimationTimeline.Action action = WardenAnimationTimeline.Action.NONE;
        long begin = -1, sniff = -1; int charge;
        Session(ServerPlayer player) { this.player = player; dimension = player.level().dimension(); }
        WardenAnimationTimeline.State state() { return new WardenAnimationTimeline.State(action, begin, charge, sniff, player.level().getGameTime()); }
    }
    private static final Map<UUID, Session> STATES = new HashMap<>();
    private WardenAnimationSync() {}
    private static Session session(ServerPlayer player) {
        var s = STATES.get(player.getUUID());
        if (s == null || s.player != player || !s.dimension.equals(player.level().dimension())) {
            s = new Session(player); STATES.put(player.getUUID(), s);
        }
        return s;
    }
    private static WardenAnimationPacket packet(Session s) { return new WardenAnimationPacket(s.player.level().dimension().location(), s.player.getUUID(), s.state()); }
    private static void send(Session s) { ModNetworking.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> s.player), packet(s)); }
    public static void combat(ServerPlayer player, WardenAnimationTimeline.Action action) {
        if (!WardenCombatManager.active(player)) return;
        var s = session(player); long now = player.level().getGameTime();
        if (action == WardenAnimationTimeline.Action.RECOVER) {
            if (s.action != WardenAnimationTimeline.Action.CHARGE) return;
            s.charge = (int) Math.min(20, Math.max(0, now - s.begin));
        } else s.charge = 0;
        s.action = action; s.begin = action == WardenAnimationTimeline.Action.NONE ? -1 : now; send(s);
    }
    public static void sniff(ServerPlayer player) { var s = session(player); s.sniff = player.level().getGameTime(); send(s); }
    private static void clear(Session s) { s.action = WardenAnimationTimeline.Action.NONE; s.begin = s.sniff = -1; s.charge = 0; send(s); }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        var server = ServerLifecycleHooks.getCurrentServer(); if (server == null) return;
        var iterator = STATES.values().iterator();
        while (iterator.hasNext()) {
            var s = iterator.next(); var current = server.getPlayerList().getPlayer(s.player.getUUID());
            if (current != s.player || !WardenCombatManager.active(s.player) || !s.dimension.equals(s.player.level().dimension())) {
                if (current != null) clear(s); iterator.remove(); continue;
            }
            long now = s.player.level().getGameTime(); var frame = s.state().frame(now);
            if (frame.action() == WardenAnimationTimeline.Action.NONE) { s.action = frame.action(); s.begin = -1; s.charge = 0; }
            if (frame.sniff() < 0) s.sniff = -1;
            if (s.action == WardenAnimationTimeline.Action.NONE && s.sniff < 0) { clear(s); iterator.remove(); }
            else if (now % 20 == 0) send(s);
        }
    }
    @SubscribeEvent public static void tracking(PlayerEvent.StartTracking e) {
        if (e.getEntity() instanceof ServerPlayer receiver && e.getTarget() instanceof ServerPlayer player) {
            var s = STATES.get(player.getUUID());
            if (s != null && s.player == player && s.dimension.equals(player.level().dimension()) && WardenCombatManager.active(player))
                ModNetworking.sendToPlayer(packet(s), receiver);
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        var s = STATES.remove(e.getEntity().getUUID()); if (s != null) clear(s);
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent e) { STATES.clear(); }
}
