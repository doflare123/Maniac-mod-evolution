package org.example.maniacrevolution.client.model;

import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.client.renderer.DeathEmbraceAnimatable;
import software.bernie.geckolib.model.GeoModel;

public final class DeathEmbraceModel extends GeoModel<DeathEmbraceAnimatable> {
    @Override
    public ResourceLocation getModelResource(DeathEmbraceAnimatable animatable) {
        return Maniacrev.loc("geo/bone_arms.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(DeathEmbraceAnimatable animatable) {
        return Maniacrev.loc("textures/entity/bone_arms.png");
    }

    @Override
    public ResourceLocation getAnimationResource(DeathEmbraceAnimatable animatable) {
        return Maniacrev.loc("animations/bone_arms.animation.json");
    }
}
