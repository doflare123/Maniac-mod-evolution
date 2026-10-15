package org.example.maniacrevolution.techies.client;

import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class TechiesVisual implements GeoAnimatable {
    public final String stem;
    public String clip="";
    public double seconds;
    public float weight=1, red=1,green=1,blue=1,alpha=1;
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    public TechiesVisual(String stem) { this.stem=stem; }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override public double getTick(Object context) { return TechiesClient.time(); }
}
