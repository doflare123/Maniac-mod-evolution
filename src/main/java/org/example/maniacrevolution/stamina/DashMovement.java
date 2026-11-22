package org.example.maniacrevolution.stamina;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.event.PenaltySlotManager;
import org.example.maniacrevolution.helsing.HelsingManager;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.DashStartedPacket;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public final class DashMovement {
    public static final int COST = 2 * Stamina.CELL;
    public static final int DODGE_TICKS = 4;
    public static final int MOVE_TICKS = 6;
    private static final int BOOST_TICKS = 40;
    private static final Set<RelativeMovement> KEEP_LOOK = Set.of(RelativeMovement.X_ROT, RelativeMovement.Y_ROT);
    private static final double[] STEP_DISTANCE = {1.25, 1.15, 0.95, 0.75, 0.55, 0.35};
    private static final AttributeModifier BOOST = new AttributeModifier(
            UUID.fromString("123a08f4-8113-4c77-9dc4-b0567687e472"), "Dash recovery speed",
            0.05, AttributeModifier.Operation.MULTIPLY_TOTAL);
    private static final Map<ServerPlayer, Dash> ACTIVE = new HashMap<>();

    private DashMovement() {}

    private static final class Dash {
        final Vec3 direction;
        final long startTick;
        long lastMoveTick = Long.MIN_VALUE;
        long boostUntil;
        int step;

        Dash(Vec3 direction, long startTick) {
            this.direction = direction;
            this.startTick = startTick;
        }
    }

    private static boolean canMove(ServerPlayer player) {
        return StaminaMovement.usesStamina(player) && player.connection != null
                && !PenaltySlotManager.isInPenaltySlot(player) && !HelsingManager.isFrozen(player)
                && !player.hasEffect(ModEffects.STUN.get());
    }

    public static boolean canStart(ServerPlayer player) {
        return canMove(player) && player.onGround() && !isMoving(player)
                && clearDistance(player, Vec3.directionFromRotation(0, player.getYRot()), 0.125) > 0;
    }

    public static boolean isMoving(ServerPlayer player) {
        Dash dash = ACTIVE.get(player);
        return dash != null && dash.step < MOVE_TICKS;
    }

    public static void start(ServerPlayer player) {
        if (!canStart(player) || !StaminaManager.spend(player, COST)) return;
        clear(player);
        long now = player.level().getGameTime();
        Dash dash = new Dash(Vec3.directionFromRotation(0, player.getYRot()), now);
        ACTIVE.put(player, dash);
        player.setSprinting(false);
        StaminaManager.updateSprintSpeed(player);
        ModNetworking.sendToTrackingPlayer(new DashStartedPacket(player.getId()), player);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.55f, 1.35f);
        advance(player, dash, now);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ACTIVE.isEmpty()) return;
        // The connection restores the player's previous position after PlayerTickEvent.
        // Move after that restore so each step starts from the preceding authoritative position.
        for (ServerPlayer player : List.copyOf(ACTIVE.keySet())) {
            Dash dash = ACTIVE.get(player);
            long now = player.level().getGameTime();
            if (!canMove(player) || dash.step == MOVE_TICKS && now >= dash.boostUntil) {
                clear(player);
            } else if (dash.step < MOVE_TICKS) {
                advance(player, dash, now);
            }
        }
    }

    private static void advance(ServerPlayer player, Dash dash, long now) {
        if (dash.lastMoveTick == now) return;
        dash.lastMoveTick = now;
        double wanted = STEP_DISTANCE[dash.step++];
        double distance = clearDistance(player, dash.direction, wanted);
        if (distance > 0) {
            Vec3 step = dash.direction.scale(distance);
            player.setDeltaMovement(0, player.getDeltaMovement().y, 0);
            player.connection.teleport(player.getX() + step.x, player.getY(), player.getZ() + step.z,
                    player.getYRot(), player.getXRot(), KEEP_LOOK);
            player.setOnGround(!player.level().noCollision(player, player.getBoundingBox().move(0, -0.04, 0)));
            if (now - dash.startTick < DODGE_TICKS) {
                player.serverLevel().sendParticles(ParticleTypes.CLOUD, player.getX() - step.x * 0.5,
                        player.getY() + 0.14, player.getZ() - step.z * 0.5, 4, 0.19, 0.08, 0.19, 0.015);
            }
        }
        if (distance < wanted || dash.step == MOVE_TICKS) {
            dash.step = MOVE_TICKS;
            dash.boostUntil = now + BOOST_TICKS;
            var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null && !speed.hasModifier(BOOST)) speed.addTransientModifier(BOOST);
        }
    }

    private static double clearDistance(ServerPlayer player, Vec3 direction, double wanted) {
        var level = player.level();
        var box = player.getBoundingBox();
        int parts = (int) Math.ceil(wanted / 0.125);
        Vec3 piece = direction.scale(wanted / parts);
        for (int i = 0; i < parts; i++) {
            Vec3 legal = Entity.collideBoundingBox(player, piece, box, level,
                    level.getEntityCollisions(player, box.expandTowards(piece)));
            if (legal.distanceToSqr(piece) > 1.0e-10 || !level.noCollision(player, box.move(piece))) {
                return wanted * i / parts;
            }
            box = box.move(piece);
        }
        return wanted;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void dodgeAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Dash dash = ACTIVE.get(player);
        if (dash == null || !canMove(player) || player.level().getGameTime() - dash.startTick >= DODGE_TICKS) return;
        var source = event.getSource();
        Entity direct = source.getDirectEntity();
        boolean directAttack = direct instanceof LivingEntity && direct == source.getEntity()
                && !source.is(DamageTypeTags.IS_EXPLOSION) && !source.is(DamageTypeTags.NO_IMPACT);
        if (direct == null || source.getEntity() == player || (!directAttack && !source.is(DamageTypeTags.IS_PROJECTILE))) return;
        event.setCanceled(true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.PLAYERS, 0.8f, 1.4f);
    }

    public static void clear(ServerPlayer player) {
        if (ACTIVE.remove(player) != null) {
            var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) speed.removeModifier(BOOST);
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) clear(player);
    }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) clear(player);
    }

    @SubscribeEvent
    public static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) clear(player);
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent event) {
        ACTIVE.clear();
    }
}
