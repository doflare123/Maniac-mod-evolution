package org.example.maniacrevolution.cloak;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.downed.DownedCapability;
import org.example.maniacrevolution.downed.DownedState;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.entity.ModEntities;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.perk.PerkTeam;

import java.util.*;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public final class CloakManager {
    public static final double THROW_RANGE = 16;
    private static final Map<UUID, Session> OWNERS = new HashMap<>();
    private static final Map<UUID, Session> CAPTIVES = new HashMap<>();

    private static final class Session {
        final ServerPlayer owner;
        final CloakEntity cloak;
        ServerPlayer target;
        CloakFlappyGame game;
        Vec3 anchor;
        float facing;
        boolean previousShift, hovering, originalGravity;
        Session(ServerPlayer owner, CloakEntity cloak) { this.owner = owner; this.cloak = cloak; }
    }

    public static boolean equipped(Player player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.DOCTOR_STRANGE_CLOAK.get());
    }
    public static boolean restrained(Player player) { return CAPTIVES.containsKey(player.getUUID()); }
    public static boolean isManaged(CloakEntity cloak) {
        return OWNERS.values().stream().anyMatch(s -> s.cloak == cloak);
    }
    private static boolean usable(ServerPlayer player) {
        var downed = DownedCapability.get(player);
        return player.isAlive() && !player.hasDisconnected() && !player.isCreative() && !player.isSpectator()
                && (downed == null || downed.getState() != DownedState.DOWNED)
                && !player.isPassenger() && !player.isSleeping() && !restrained(player)
                && !player.hasEffect(ModEffects.STUN.get());
    }
    public static boolean canThrow(ServerPlayer player) {
        Session session = OWNERS.get(player.getUUID());
        return equipped(player) && usable(player) && session != null && session.cloak.stage() == CloakStage.IDLE;
    }
    public static boolean throwCloak(ServerPlayer player) {
        if (!canThrow(player)) return false;
        Vec3 eye = player.getEyePosition(), look = player.getLookAngle();
        ServerPlayer target = player.serverLevel().players().stream()
                .filter(p -> p != player && usable(p) && PerkTeam.fromPlayer(p) == PerkTeam.MANIAC)
                .filter(p -> p.distanceToSqr(player) <= THROW_RANGE * THROW_RANGE && player.hasLineOfSight(p))
                .filter(p -> p.getBoundingBox().inflate(0.35).clip(eye, eye.add(look.scale(THROW_RANGE))).isPresent())
                .min(Comparator.comparingDouble(p -> p.distanceToSqr(player))).orElse(null);
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.maniacrev.cloak.no_target"), true);
            return false;
        }
        Session session = OWNERS.get(player.getUUID());
        session.target = target;
        session.cloak.target(target);
        session.cloak.stage(CloakStage.DETACH);
        return true;
    }

    public static void flap(ServerPlayer player, int cloakId) {
        Session session = CAPTIVES.get(player.getUUID());
        if (session != null && session.cloak.getId() == cloakId) session.game.flap();
    }

    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (Session s : new ArrayList<>(OWNERS.values())) {
            if (!equipped(s.owner) || !s.owner.isAlive() || s.owner.hasDisconnected()
                    || s.cloak.isRemoved() || s.cloak.level() != s.owner.level()) {
                remove(s);
                continue;
            }
            update(s);
        }
    }

    @SubscribeEvent public static void equip(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || !player.isAlive() || !equipped(player) || OWNERS.containsKey(player.getUUID())) return;
        CloakEntity cloak = new CloakEntity(ModEntities.DOCTOR_STRANGE_CLOAK.get(), player.level());
        cloak.owner(player);
        follow(cloak, player.position(), player.yBodyRot);
        Session session = new Session(player, cloak);
        OWNERS.put(player.getUUID(), session);
        player.serverLevel().addFreshEntity(cloak);
    }

    private static void update(Session s) {
        CloakEntity cloak = s.cloak;
        boolean shift = s.owner.isShiftKeyDown();
        boolean pressed = shift && !s.previousShift;
        s.previousShift = shift;
        if (cloak.stage().hovering() && (!shift || !usable(s.owner) || s.owner.isInWaterOrBubble() || s.owner.isInLava())) {
            stopHover(s);
            cloak.stage(CloakStage.LAND);
        }
        if (cloak.stage() == CloakStage.IDLE && pressed && usable(s.owner)
                && !s.owner.isInWaterOrBubble() && !s.owner.isInLava()) {
            s.hovering = true;
            s.originalGravity = s.owner.isNoGravity();
            s.owner.setNoGravity(true);
            s.owner.stopFallFlying();
            cloak.hoverHeight(s.owner.getY() + 0.5);
            cloak.stage(CloakStage.DEPLOY);
        }
        if (s.hovering) {
            hover(s.owner, cloak.hoverHeight());
            if (s.owner.getY() > cloak.hoverHeight()) {
                s.owner.connection.teleport(s.owner.getX(), cloak.hoverHeight(), s.owner.getZ(), s.owner.getYRot(), s.owner.getXRot());
            }
        }

        if (s.target != null && (s.target.hasDisconnected() || !s.target.isAlive()
                || s.target.level() != s.owner.level() || s.target.isSpectator() || s.target.isCreative())) {
            release(s, true);
        }
        if (cloak.stage().binding()) {
            if (!restrained(s.target)) { release(s, false); return; }
            var downed = DownedCapability.get(s.target);
            if (downed != null && downed.getState() == DownedState.DOWNED) { release(s, true); return; }
            hold(s);
            s.game.tick();
            if (s.game.finished()) { release(s, false); return; }
            sendGame(s);
        }

        switch (cloak.stage()) {
            case IDLE, HOVER -> follow(cloak, s.owner.position(), s.owner.yBodyRot);
            case DEPLOY -> attachedTransition(s, CloakStage.HOVER);
            case LAND, REATTACH -> attachedTransition(s, CloakStage.IDLE);
            case DETACH -> attachedTransition(s, CloakStage.OUTBOUND);
            case OUTBOUND -> {
                if (s.target == null || !usable(s.target) || s.owner.distanceToSqr(s.target) > 32 * 32
                        || !s.owner.hasLineOfSight(s.target) || cloak.ageInStage() > 60) {
                    release(s, true);
                } else if (fly(cloak, s.target.position())) {
                    s.anchor = s.target.position();
                    s.facing = s.target.yBodyRot;
                    s.game = new CloakFlappyGame();
                    CAPTIVES.put(s.target.getUUID(), s);
                    s.target.stopUsingItem();
                    s.target.stopFallFlying();
                    cloak.stage(CloakStage.CAPTURE);
                    hold(s);
                    sendGame(s);
                }
            }
            case CAPTURE, FALL, BOUND -> {
                follow(cloak, s.anchor, s.facing);
                if (cloak.stage() == CloakStage.CAPTURE && cloak.ageInStage() >= 15) cloak.stage(CloakStage.FALL);
                else if (cloak.stage() == CloakStage.FALL && cloak.ageInStage() >= 21) cloak.stage(CloakStage.BOUND);
            }
            case RELEASE -> { if (cloak.ageInStage() >= 14) cloak.stage(CloakStage.LAUNCH); }
            case LAUNCH -> { if (cloak.ageInStage() >= 12) cloak.stage(CloakStage.RETURN); }
            case RETURN -> {
                if (fly(cloak, s.owner.position()) || cloak.ageInStage() > 80) {
                    cloak.target(null);
                    s.target = null;
                    cloak.stage(CloakStage.REATTACH);
                }
            }
        }
    }

    public static void hover(Player player, double height) {
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.x, Math.max(-0.15, Math.min(0.1, height - player.getY())), velocity.z);
        player.fallDistance = 0;
        player.hurtMarked = true;
    }
    private static void stopHover(Session s) {
        if (!s.hovering) return;
        s.hovering = false;
        s.owner.setNoGravity(s.originalGravity);
        s.owner.setDeltaMovement(s.owner.getDeltaMovement().multiply(1, 0, 1));
        s.owner.fallDistance = 0;
        s.owner.hurtMarked = true;
    }
    private static void hold(Session s) {
        s.target.setDeltaMovement(Vec3.ZERO);
        s.target.fallDistance = 0;
        s.target.hurtMarked = true;
        if (s.target.position().distanceToSqr(s.anchor) > 0.0001) {
            s.target.connection.teleport(s.anchor.x, s.anchor.y, s.anchor.z, s.target.getYRot(), s.target.getXRot());
        }
    }
    private static void attachedTransition(Session s, CloakStage next) {
        follow(s.cloak, s.owner.position(), s.owner.yBodyRot);
        if (s.cloak.ageInStage() >= s.cloak.stage().ticks) s.cloak.stage(next);
    }
    private static void follow(CloakEntity cloak, Vec3 pos, float yaw) {
        cloak.setPos(pos);
        cloak.setYRot(yaw);
    }
    private static boolean fly(CloakEntity cloak, Vec3 destination) {
        Vec3 delta = destination.subtract(cloak.position());
        if (delta.lengthSqr() <= 0.8 * 0.8) { cloak.setPos(destination); return true; }
        cloak.setPos(cloak.position().add(delta.normalize().scale(0.8)));
        cloak.setYRot((float)Math.toDegrees(Math.atan2(-delta.x, delta.z)));
        return false;
    }
    private static void sendGame(Session s) {
        ModNetworking.sendToPlayer(new CloakGamePacket(s.cloak.getId(), s.game.remaining(), s.game.bird(),
                s.game.runTicks(), s.game.score()), s.target);
    }
    private static void release(Session s, boolean immediate) {
        if (s.target != null && CAPTIVES.remove(s.target.getUUID(), s)) {
            ModNetworking.sendToPlayer(new CloakGamePacket(s.cloak.getId(), 0, 0, 0, 0), s.target);
        }
        s.game = null;
        s.cloak.stage(immediate ? CloakStage.RETURN : CloakStage.RELEASE);
        if (immediate) { s.cloak.target(null); s.target = null; }
    }
    private static void remove(Session s) {
        stopHover(s);
        release(s, true);
        OWNERS.remove(s.owner.getUUID());
        s.cloak.discard();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST) public static void damaged(LivingHurtEvent event) {
        if (event.getAmount() > 0 && event.getEntity() instanceof ServerPlayer player) {
            Session s = CAPTIVES.get(player.getUUID());
            if (s != null) release(s, false);
        }
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void attack(AttackEntityEvent event) {
        if (restrained(event.getEntity())) event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void interact(PlayerInteractEvent event) {
        if (event.isCancelable() && restrained(event.getEntity())) event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void use(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof Player player && restrained(player)) event.setCanceled(true);
    }
    @SubscribeEvent public static void breaking(BlockEvent.BreakEvent event) {
        if (restrained(event.getPlayer())) event.setCanceled(true);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { cleanup(event.getEntity()); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { cleanup(event.getEntity()); }
    private static void cleanup(Player player) {
        Session owner = OWNERS.get(player.getUUID());
        if (owner != null) remove(owner);
        Session captive = CAPTIVES.get(player.getUUID());
        if (captive != null) release(captive, true);
    }
    @SubscribeEvent public static void stop(ServerStoppingEvent event) {
        for (Session s : new ArrayList<>(OWNERS.values())) remove(s);
        CAPTIVES.clear();
    }
}
