package org.example.maniacrevolution.warden;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.downed.*;
import org.example.maniacrevolution.event.PenaltySlotManager;
import org.example.maniacrevolution.game.GameManager;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.*;
import java.util.*;

/** Server-owned attacks, charge timing and swept projectile collision. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public final class WardenCombatManager {
    private record Session(ServerPlayer player, WardenCombatCycle cycle, Object dimension) {}
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final List<Wave> WAVES = new ArrayList<>();
    private static ServerPlayer authorized;
    private WardenCombatManager() {}
    private static boolean mode(ServerPlayer p) { var m = p.gameMode.getGameModeForPlayer(); return m == GameType.SURVIVAL || m == GameType.ADVENTURE; }
    private static String team(ServerPlayer p) { return p.getTeam() == null ? null : p.getTeam().getName(); }
    public static boolean active(ServerPlayer p) {
        return WardenMatchRules.active(p.isAlive(), mode(p), GameManager.getPhaseValue(), PlayerDataManager.get(p).getManiacClassId(), team(p));
    }
    private static boolean allowed(ServerPlayer p) {
        var d = DownedCapability.get(p);
        return active(p) && (d == null || d.getState() != DownedState.DOWNED) && !PenaltySlotManager.isInPenaltySlot(p);
    }
    private static boolean hands(ServerPlayer p) { return p.getMainHandItem().isEmpty() && p.getOffhandItem().isEmpty() && p.containerMenu == p.inventoryMenu; }
    private static boolean target(LivingEntity e) {
        return e.isAlive() && (!(e instanceof ServerPlayer p) || mode(p) && "survivors".equalsIgnoreCase(team(p)));
    }
    private static Session session(ServerPlayer p) {
        var s = SESSIONS.get(p.getUUID());
        if (s == null || s.player != p || !s.dimension.equals(p.level().dimension())) {
            s = new Session(p, new WardenCombatCycle(), p.level().dimension()); SESSIONS.put(p.getUUID(), s);
        }
        return s;
    }
    private static void send(Session s, int token) {
        long now = s.player.level().getGameTime(); var c = s.cycle;
        ModNetworking.sendToPlayer(new WardenCombatStatePacket(s.player.level().dimension().location(), now, token,
                c.charging(), c.charge(now), c.meleeCooldown(now), c.waveCooldown(now)), s.player);
    }
    private static boolean interaction(ServerPlayer p) {
        var start = p.getEyePosition(); var end = start.add(p.getLookAngle().scale(5));
        var hit = p.level().clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, p));
        net.minecraft.world.entity.Entity nearest = null; double distance = start.distanceToSqr(hit.getLocation());
        for (var e : p.level().getEntities(p, new AABB(start, end).inflate(1), entity -> entity.isPickable())) {
            var box = e.getBoundingBox(); var contact = box.contains(start) ? Optional.of(start) : box.clip(start, hit.getLocation());
            if (contact.isPresent() && start.distanceToSqr(contact.get()) <= distance) { nearest = e; distance = start.distanceToSqr(contact.get()); }
        }
        if (nearest != null) return WardenInteractionPolicy.interactive(nearest);
        return hit.getType() != HitResult.Type.MISS && WardenInteractionPolicy.interactive(p.level().getBlockState(hit.getBlockPos()), p.level(), hit.getBlockPos());
    }
    public static void input(ServerPlayer p, WardenCombatInputPacket packet) {
        if (!allowed(p) || packet.token() < 0) return;
        var s = session(p); var c = s.cycle; long now = p.level().getGameTime();
        boolean wasCharging = c.charging();
        switch (packet.action()) {
            case MELEE -> { if (p.containerMenu == p.inventoryMenu && c.melee(now)) melee(p); }
            case START -> { if (hands(p) && !interaction(p) && c.start(now, packet.token())) WardenAnimationSync.combat(p, WardenAnimationTimeline.Action.CHARGE); }
            case CANCEL -> c.cancel(packet.token());
            case KEEP_ALIVE -> { if (hands(p)) c.keepAlive(now, packet.token()); }
            case RELEASE -> {
                if (!hands(p) || WAVES.size() >= 256) c.cancel(packet.token());
                var shot = c.release(now, packet.token());
                if (shot != null && WAVES.size() < 256) {
                    WAVES.add(new Wave(p, shot));
                    WardenAnimationSync.combat(p, WardenAnimationTimeline.Action.RELEASE);
                    p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1 + shot.strength() * 2, 1);
                }
            }
        }
        if (wasCharging && !c.charging() && c.waveCooldown(now) == 0) WardenAnimationSync.combat(p, WardenAnimationTimeline.Action.RECOVER);
        send(s, c.charging() ? c.token() : packet.token());
    }
    private static Vec3 wall(ServerPlayer p, Vec3 a, Vec3 b) {
        return p.level().clip(new ClipContext(a, b, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p)).getLocation();
    }
    private static void melee(ServerPlayer p) {
        Vec3 start = p.getEyePosition(), end = wall(p, start, start.add(p.getLookAngle().scale(WardenCombatRules.MELEE_RANGE)));
        LivingEntity nearest = null; double distance = start.distanceToSqr(end);
        for (var e : p.level().getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(0.2), e -> e != p && e.isAlive() && e.isPickable())) {
            var hit = WardenCombatGeometry.meleeContact(e.getBoundingBox(), start, end, e.getPickRadius());
            if (hit.isPresent() && start.distanceToSqr(hit.get()) <= distance
                    && WardenCombatGeometry.unobstructed(hit.get(), wall(p, start, hit.get()))) {
                distance = start.distanceToSqr(hit.get()); nearest = e;
            }
        }
        p.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        WardenAnimationSync.combat(p, WardenAnimationTimeline.Action.ATTACK);
        WardenNoiseManager.combatNoise(p, p.position(), 0.6F, 16);
        boolean landed = false;
        if (nearest != null && target(nearest)) {
            authorized = p;
            try {
                if (!MinecraftForge.EVENT_BUS.post(new AttackEntityEvent(p, nearest))) {
                    landed = nearest.hurt(p.damageSources().playerAttack(p), WardenCombatRules.MELEE_DAMAGE);
                    if (landed) p.setLastHurtMob(nearest);
                }
            } finally { authorized = null; }
        }
        p.serverLevel().playSound(null, p.blockPosition(), landed ? SoundEvents.WARDEN_ATTACK_IMPACT : SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, landed ? 1.2F : 0.8F, landed ? 0.9F : 0.65F);
        ModNetworking.sendToPlayer(new WardenMeleeFeedbackPacket(p.level().dimension().location(), p.level().getGameTime(), landed), p);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void vanillaAttack(AttackEntityEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && active(p) && p != authorized) e.setCanceled(true);
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        var server = ServerLifecycleHooks.getCurrentServer(); if (server == null) return;
        SESSIONS.entrySet().removeIf(entry -> {
            var s = entry.getValue(); var p = server.getPlayerList().getPlayer(entry.getKey());
            boolean invalid = p != s.player || !allowed(s.player) || !s.dimension.equals(s.player.level().dimension());
            if (invalid && s.cycle.charging()) WardenAnimationSync.combat(s.player, WardenAnimationTimeline.Action.RECOVER);
            if (invalid && p != null) ModNetworking.sendToPlayer(new WardenCombatStatePacket(p.level().dimension().location(), p.level().getGameTime(), s.cycle.token(), false, 0, 0, 0), p);
            return invalid;
        });
        for (var s : SESSIONS.values()) {
            long now = s.player.level().getGameTime(); boolean was = s.cycle.charging();
            if (!hands(s.player)) s.cycle.cancel(s.cycle.token());
            s.cycle.expire(now);
            if (was && !s.cycle.charging()) WardenAnimationSync.combat(s.player, WardenAnimationTimeline.Action.RECOVER);
            if (s.cycle.charging() && s.cycle.charge(now) == WardenCombatRules.MIN_CHARGE)
                s.player.serverLevel().playSound(null, s.player.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 1, 1);
            if (was != s.cycle.charging() || now % 4 == 0 && (was || s.cycle.meleeCooldown(now) > 0 || s.cycle.waveCooldown(now) > 0)) send(s, s.cycle.token());
        }
        WAVES.removeIf(w -> server.getPlayerList().getPlayer(w.owner.getUUID()) != w.owner || !allowed(w.owner)
                || w.owner.serverLevel() != w.level || !w.step());
    }
    private static final class Wave {
        final ServerPlayer owner; final net.minecraft.server.level.ServerLevel level;
        final WardenCombatRules.Shot shot; final Vec3 direction; final Set<UUID> hits = new HashSet<>();
        Vec3 position; double travelled;
        Wave(ServerPlayer p, WardenCombatRules.Shot shot) { owner = p; level = p.serverLevel(); this.shot = shot; direction = p.getLookAngle().normalize(); position = p.getEyePosition(); }
        boolean step() {
            var next = position.add(direction.scale(Math.min(WardenCombatRules.WAVE_SPEED, shot.range() - travelled)));
            if (!level.hasChunkAt(BlockPos.containing(next))) return false;
            var stop = wall(owner, position, next); boolean blocked = stop.distanceToSqr(next) > 1.0e-8;
            for (var e : level.getEntitiesOfClass(LivingEntity.class, new AABB(position, stop).inflate(WardenCombatRules.WAVE_RADIUS), e -> e != owner && target(e))) {
                var contact = WardenCombatGeometry.contact(e.getBoundingBox(), position, stop, WardenCombatRules.WAVE_RADIUS);
                if (contact.isPresent() && WardenCombatGeometry.unobstructed(contact.get(), wall(owner, position, contact.get())) && hits.add(e.getUUID()))
                    e.hurt(owner.damageSources().sonicBoom(owner), shot.damage());
            }
            travelled += position.distanceTo(stop); position = stop;
            level.sendParticles(ParticleTypes.SONIC_BOOM, position.x, position.y, position.z, 1, 0, 0, 0, 0);
            if (level.getGameTime() % 2 == 0) WardenNoiseManager.waveNoise(owner, position, shot.strength(), 16 + shot.strength() * 48);
            for (var listener : level.players()) if (active(listener) && listener.position().distanceToSqr(position) < 64 * 64)
                ModNetworking.sendToPlayer(new WardenWavePacket(level.dimension().location(), position, direction, level.getGameTime(), shot.strength()), listener);
            return !blocked && travelled < shot.range() - 1.0e-6;
        }
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent e) { SESSIONS.clear(); WAVES.clear(); authorized = null; }
}
