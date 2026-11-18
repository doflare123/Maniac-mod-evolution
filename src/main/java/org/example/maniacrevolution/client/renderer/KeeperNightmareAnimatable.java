package org.example.maniacrevolution.client.renderer;

import java.util.Map;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.animatable.GeoReplacedEntity;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animatable.model.CoreGeoModel;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.state.BoneSnapshot;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class KeeperNightmareAnimatable implements GeoReplacedEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("Walk");
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("swim");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Override
    public EntityType<?> getReplacingEntityType() {
        return EntityType.PLAYER;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new KeeperMovementController(this));
    }

    private static PlayState selectAnimation(AnimationState<KeeperNightmareAnimatable> state) {
        RawAnimation animation = state.isMoving() ? WALK : IDLE;
        if (state.getData(DataTickets.ENTITY) instanceof Player player) {
            if (player.isSwimming() || player.getPose() == Pose.SWIMMING
                    || (player.isInWater() && player.getDeltaMovement().horizontalDistanceSqr() > 0.0004D)) {
                animation = SWIM;
            } else {
                animation = player.walkAnimation.speed(state.getPartialTick()) > 0.03F ? WALK : IDLE;
            }
        }
        state.getController().transitionLength(animation == WALK ? 3 : 7);
        return state.setAndContinue(animation);
    }

    private static final class KeeperMovementController extends AnimationController<KeeperNightmareAnimatable> {
        // Vanilla accumulates about four walk-position units per block. The foot travel in this clip is 1.3 blocks per cycle.
        private static final double WALK_UNITS_PER_CYCLE = 5.2;
        private double walkPosition;
        private double walkStart;

        private KeeperMovementController(KeeperNightmareAnimatable keeper) {
            super(keeper, "keeper_form", 7, KeeperNightmareAnimatable::selectAnimation);
        }

        @Override
        public void process(CoreGeoModel<KeeperNightmareAnimatable> model, AnimationState<KeeperNightmareAnimatable> state,
                            Map<String, CoreGeoBone> bones, Map<String, BoneSnapshot> snapshots, double tick,
                            boolean crashIfBoneMissing) {
            walkPosition = state.getData(DataTickets.ENTITY) instanceof Player player
                    ? player.walkAnimation.position(state.getPartialTick()) : Double.NaN;
            super.process(model, state, bones, snapshots, tick, crashIfBoneMissing);
        }

        @Override
        protected double adjustTick(double tick) {
            boolean resetting = shouldResetTick;
            double elapsed = super.adjustTick(tick);
            if (animationState != State.RUNNING || currentRawAnimation != WALK || currentAnimation == null
                    || !Double.isFinite(walkPosition)) {
                return elapsed;
            }

            // Keep transitions on GeckoLib's clock. Start at the pose it just blended into.
            if (resetting) {
                walkStart = walkPosition;
            }
            double phase = Math.max(0, walkPosition - walkStart) / WALK_UNITS_PER_CYCLE;
            return (phase % 1.0) * currentAnimation.animation().length();
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object object) {
        return object instanceof Entity entity ? entity.tickCount : GeoReplacedEntity.super.getTick(object);
    }
}
