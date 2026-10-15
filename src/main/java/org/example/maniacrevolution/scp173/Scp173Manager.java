package org.example.maniacrevolution.scp173;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.downed.DownedCapability;
import org.example.maniacrevolution.downed.DownedState;
import org.example.maniacrevolution.game.GameManager;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.Scp173HoldPacket;
import org.example.maniacrevolution.util.PlayerModeUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** All state and world queries run on the server thread. No anchors or velocity cancellation. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public final class Scp173Manager {
    private static final Map<UUID, Hold> HOLDS = new HashMap<>();
    private Scp173Manager() {}

    private static String team(ServerPlayer p) { return p.getTeam() == null ? null : p.getTeam().getName(); }

    public static boolean active(ServerPlayer p) {
        return Scp173Rules.active(p.isAlive(), PlayerModeUtil.isSurvivalOrAdventure(p), GameManager.getPhaseValue(),
                PlayerDataManager.get(p).getManiacClassId(), team(p));
    }

    public static boolean eligibleObserver(ServerPlayer p) {
        var downed = DownedCapability.get(p);
        return Scp173Rules.observer(p.isAlive(), PlayerModeUtil.isSurvivalOrAdventure(p),
                downed != null && downed.getState() == DownedState.DOWNED, team(p));
    }

    /** Refresh before accepting input/attacks, so downing or turning away doesn't wait for a cached tick. */
    public static boolean held(ServerPlayer p) {
        Hold old = HOLDS.get(p.getUUID());
        if (old != null && (old.player != p || old.level != p.level())) {
            HOLDS.remove(p.getUUID());
            old = null;
        }
        boolean visible = active(p) && p.serverLevel().players().stream()
                .anyMatch(other -> other != p && eligibleObserver(other) && !Scp173GameplayManager.blinking(other) && Scp173Vision.sees(other, p));
        if (visible) {
            if (old == null) {
                old = new Hold(p);
                HOLDS.put(p.getUUID(), old);
                send(p, true, old.yaw, old.pitch);
            }
            lockRotation(p, old);
        } else if (old != null) {
            HOLDS.remove(p.getUUID());
            send(p, false, p.getYRot(), p.getXRot());
        }
        return visible;
    }

    private static void lockRotation(ServerPlayer p, Hold hold) {
        p.setYRot(hold.yaw); p.setXRot(hold.pitch);
        p.setYHeadRot(hold.yaw); p.setYBodyRot(hold.yaw);
        p.yRotO = hold.yaw; p.xRotO = hold.pitch;
        p.yHeadRotO = hold.yaw; p.yBodyRotO = hold.yaw;
    }

    private static void send(ServerPlayer p, boolean held, float yaw, float pitch) {
        ModNetworking.sendToPlayer(new Scp173HoldPacket(p.getUUID(), p.level().dimension().location(), held,
                yaw, pitch, p.position(), p.getDeltaMovement()), p);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tick(TickEvent.PlayerTickEvent e) {
        if (!(e.player instanceof ServerPlayer p)) return;
        if (e.phase == TickEvent.Phase.START) {
            if (!held(p)) return;
            var hold = HOLDS.get(p.getUUID());
            hold.before = p.position();
            p.setPlayerInput(0, 0, false, false);
        } else {
            var hold = HOLDS.get(p.getUUID());
            if (hold == null) return;
            if (hold.player != p || hold.level != p.level()) { held(p); return; }
            boolean stillHeld = held(p);
            if (stillHeld) lockRotation(p, hold);
            // ServerGamePacketListenerImpl normally restores firstGood after doTick(). Keep
            // vanilla travel(Vec3.ZERO), collision, gravity and ALL external impulses instead.
            p.connection.resetPosition();
            if (hold.before != null) {
                var delta = p.position().subtract(hold.before);
                p.doCheckFallDamage(delta.x, delta.y, delta.z, p.onGround());
                p.serverLevel().getChunkSource().move(p);
                hold.before = null;
            }
            // Preserve the final physical step even when it takes the statue out of sight.
            if (stillHeld) send(p, true, hold.yaw, hold.pitch);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void jump(net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent e) {
        Scp173MovementGate.packetJump(e.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attack(AttackEntityEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && held(p)) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) Scp173MovementGate.install(p.connection);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent e) { HOLDS.remove(e.getEntity().getUUID()); }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent e) { HOLDS.clear(); }

    private static final class Hold {
        final ServerPlayer player;
        final Object level;
        final float yaw, pitch;
        net.minecraft.world.phys.Vec3 before;
        Hold(ServerPlayer p) { player = p; level = p.level(); yaw = p.getYRot(); pitch = p.getXRot(); }
    }
}
