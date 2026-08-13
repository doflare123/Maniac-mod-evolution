package org.example.maniacrevolution.client.renderer;

import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class DeathEmbraceAnimatable implements GeoAnimatable {
    private static final RawAnimation EMBRACE = RawAnimation.begin().thenPlayAndHold("death_embrace");
    private static final RawAnimation RELEASE = RawAnimation.begin().thenPlay("release");
    private static final double EMBRACE_TICKS = 85.0D;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final long startedAtNanos = System.nanoTime();

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "death_embrace", 0, state ->
                state.setAndContinue(getTick(this) < EMBRACE_TICKS ? EMBRACE : RELEASE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object object) {
        return (System.nanoTime() - startedAtNanos) / 50_000_000.0D;
    }
}
