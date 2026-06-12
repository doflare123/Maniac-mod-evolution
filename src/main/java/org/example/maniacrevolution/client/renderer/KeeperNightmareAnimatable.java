package org.example.maniacrevolution.client.renderer;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.animatable.GeoReplacedEntity;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
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
        controllers.add(new AnimationController<>(this, "keeper_form", 5, state -> {
            Entity entity = state.getData(DataTickets.ENTITY);
            boolean moving = state.isMoving();
            if (entity instanceof Player player) {
                moving = player.walkAnimation.speed() > 0.03F;
                boolean swimming = player.isSwimming()
                        || player.getPose() == Pose.SWIMMING
                        || (player.isInWater() && player.getDeltaMovement().horizontalDistanceSqr() > 0.0004D);
                if (swimming) {
                    state.setControllerSpeed(1.0F);
                    return state.setAndContinue(SWIM);
                }
            }
            state.setControllerSpeed(moving ? 1.65F : 1.0F);
            return state.setAndContinue(moving ? WALK : IDLE);
        }));
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
