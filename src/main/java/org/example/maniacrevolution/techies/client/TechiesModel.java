package org.example.maniacrevolution.techies.client;

import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.Maniacrev;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.core.animation.AnimationState;

public final class TechiesModel extends GeoModel<TechiesVisual> {
    @Override public ResourceLocation getModelResource(TechiesVisual v) { return Maniacrev.loc("geo/"+v.stem+".geo.json"); }
    @Override public ResourceLocation getTextureResource(TechiesVisual v) { return Maniacrev.loc("textures/item/"+v.stem+".png"); }
    @Override public ResourceLocation getAnimationResource(TechiesVisual v) { return Maniacrev.loc("animations/"+v.stem+".animation.json"); }
    @Override public void setCustomAnimations(TechiesVisual v,long id,AnimationState<TechiesVisual> state) {
        TechiesAnimation.apply(this,v.stem,v.clip,v.seconds,v.weight);
    }
}
