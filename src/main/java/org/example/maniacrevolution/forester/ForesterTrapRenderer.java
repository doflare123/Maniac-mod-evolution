package org.example.maniacrevolution.forester;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import org.example.maniacrevolution.Maniacrev;

public final class ForesterTrapRenderer extends GeoBlockRenderer<ForesterTrapBlockEntity> {
    public ForesterTrapRenderer() { super(new GeoModel<>() {
        @Override public ResourceLocation getModelResource(ForesterTrapBlockEntity trap) { return Maniacrev.loc("geo/forester_trap.geo.json"); }
        @Override public ResourceLocation getTextureResource(ForesterTrapBlockEntity trap) { return Maniacrev.loc("textures/block/forester_trap.png"); }
        @Override public ResourceLocation getAnimationResource(ForesterTrapBlockEntity trap) { return Maniacrev.loc("animations/forester_trap.animation.json"); }
        @Override public void setCustomAnimations(ForesterTrapBlockEntity trap, long instanceId,
                software.bernie.geckolib.core.animation.AnimationState<ForesterTrapBlockEntity> state) {
            super.setCustomAnimations(trap, instanceId, state);
            Float angle = ForesterWorkAnimation.jawAngle(trap.getBlockPos(), state.getPartialTick());
            if (angle == null) return;
            getAnimationProcessor().getBone("jaw_front").setRotX((float) Math.toRadians(-angle));
            getAnimationProcessor().getBone("jaw_back").setRotX((float) Math.toRadians(angle));
        }
    }); }
}
