package org.example.maniacrevolution.stamina.client;

import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.api.layered.modifier.SpeedModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.downed.DownedCapability;
import org.example.maniacrevolution.stamina.DashMovement;
import org.example.maniacrevolution.stamina.StaminaMovement;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PlayerLocomotionAnimations {
    private static final ResourceLocation LAYER = new ResourceLocation(Maniacrev.MODID, "locomotion");
    static final int DASH_POSE_TICKS = 11;

    private PlayerLocomotionAnimations() {}

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER, 10, Locomotion::new);
            MinecraftForge.EVENT_BUS.addListener(PlayerLocomotionAnimations::updatePlayer);
        });
    }

    private static void updatePlayer(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof AbstractClientPlayer player
                && PlayerAnimationAccess.getPlayerAssociatedData(player).get(LAYER) instanceof Locomotion layer) {
            layer.updateMotion();
        }
    }

    public static void dashStarted(int entityId) {
        var level = Minecraft.getInstance().level;
        if (level != null && level.getEntity(entityId) instanceof AbstractClientPlayer player) {
            var data = PlayerAnimationAccess.getPlayerAssociatedData(player).get(LAYER);
            if (data instanceof Locomotion layer) {
                layer.dashStart = player.tickCount;
                layer.startDashPose();
            }
        }
    }

    public static boolean isDashing(AbstractClientPlayer player) {
        return dashAge(player) < DashMovement.MOVE_TICKS;
    }

    public static float dodgeProgress(AbstractClientPlayer player, float partialTick) {
        return Mth.clamp(1 - (dashAge(player) + partialTick) / DashMovement.DODGE_TICKS, 0, 1);
    }

    static float dashAge(AbstractClientPlayer player) {
        if (DownedCapability.isDowned(player)) return Float.POSITIVE_INFINITY;
        if (PlayerAnimationAccess.getPlayerAssociatedData(player).get(LAYER) instanceof Locomotion layer) {
            return player.tickCount - layer.dashStart;
        }
        return Float.POSITIVE_INFINITY;
    }

    private enum Motion {
        VANILLA(null), WALK("walking"), RUN("running"), SNEAK_IDLE("sneak_idle"), SNEAK_WALK("sneak_walk"),
        JUMP("jump_first"), FALL("falling"), LAND("landing"), DASH("dash");

        final ResourceLocation clip;

        Motion(String name) {
            clip = name == null ? null : new ResourceLocation(Maniacrev.MODID, name);
        }
    }

    private static final class Locomotion extends ModifierLayer<KeyframeAnimationPlayer> {
        private final AbstractClientPlayer player;
        private final SpeedModifier speed = new SpeedModifier(1.0f);
        private Motion motion = Motion.VANILLA;
        private KeyframeAnimation clip;
        private Vec3 previousPosition;
        private int airTicks;
        private double highestY;
        private long dashStart = Long.MIN_VALUE / 2;

        Locomotion(AbstractClientPlayer player) {
            this.player = player;
            addModifierLast(speed);
        }

        void updateMotion() {
            Vec3 position = player.position();
            Vec3 movement = previousPosition == null ? Vec3.ZERO : position.subtract(previousPosition);
            previousPosition = position;
            // A teleport is neither a stride nor a jump.
            if (player.tickCount - dashStart < DASH_POSE_TICKS) {
                airTicks = 0;
                if (motion != Motion.DASH) startDashPose();
            } else if (!StaminaMovement.usesStamina(player) || movement.lengthSqr() > 4.0) {
                airTicks = 0;
                select(Motion.VANILLA, 1.0f);
            } else {
                select(motionFor(movement, position.y), paceFor(movement.horizontalDistance()));
            }
        }

        private void startDashPose() {
            motion = Motion.DASH;
            clip = PlayerAnimationRegistry.getAnimation(Motion.DASH.clip);
            speed.speed = 1.0f;
            // Remove outgoing locomotion fades, leaving the speed modifier at index 0.
            while (size() > 1) removeModifier(1);
            setAnimation(clip == null ? null : new KeyframeAnimationPlayer(clip));
        }

        private Motion motionFor(Vec3 movement, double y) {
            if (!player.onGround()) {
                if (airTicks++ == 0) highestY = y;
                highestY = Math.max(highestY, y);
                if (movement.y > 0.015 || airTicks <= 2 && motion == Motion.JUMP) return Motion.JUMP;
                return airTicks > 2 ? Motion.FALL : Motion.VANILLA;
            }
            boolean justLanded = airTicks >= 3 && highestY - y > 0.15;
            airTicks = 0;
            if (justLanded || motion == Motion.LAND && getAnimation() != null && getAnimation().isActive()) {
                return Motion.LAND;
            }

            boolean moving = movement.horizontalDistanceSqr() > 0.0004;
            if (player.isCrouching()) return moving ? Motion.SNEAK_WALK : Motion.SNEAK_IDLE;
            if (!moving) return Motion.VANILLA;
            return player.isSprinting() ? Motion.RUN : Motion.WALK;
        }

        private float paceFor(double horizontalSpeed) {
            double normalSpeed = player.isCrouching() ? 0.065 : player.isSprinting() ? 0.28 : 0.21;
            return Mth.clamp((float) (horizontalSpeed / normalSpeed), 0.6f, 1.65f);
        }

        private void select(Motion next, float pace) {
            KeyframeAnimation nextClip = next.clip == null ? null : PlayerAnimationRegistry.getAnimation(next.clip);
            speed.speed = switch (next) {
                case WALK, RUN, SNEAK_WALK -> Mth.lerp(0.35f, speed.speed, pace);
                default -> 1.0f;
            };
            if (next == motion && nextClip == clip) return;
            motion = next;
            clip = nextClip;
            replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(3, Ease.INOUTSINE),
                    clip == null ? null : new KeyframeAnimationPlayer(clip));
        }

        @Override
        public Vec3f get3DTransform(String part, TransformType type, float tickDelta, Vec3f original) {
            boolean rolling = motion == Motion.DASH && player.tickCount - dashStart < DASH_POSE_TICKS;
            if (rolling && part.equals("body")) {
                // playerAnimator rotates the whole model around its 0.7-block body pivot.
                float angle = DashPresentation.rollAngle(player, tickDelta);
                if (type == TransformType.ROTATION) return new Vec3f(-angle * Mth.DEG_TO_RAD, 0, 0);
                if (type == TransformType.POSITION) return new Vec3f(0, DashPresentation.rollLift(angle), 0);
            }
            // PlayerAnimator positions the head and torso independently. Apply the clip's head
            // position alongside the torso position, and preserve the look outside the roll.
            if (!rolling && ((part.equals("head") && type == TransformType.ROTATION) || (player.swinging || player.isUsingItem())
                    && (part.equals("leftArm") || part.equals("rightArm")))) return original;
            return super.get3DTransform(part, type, tickDelta, original);
        }
    }
}
