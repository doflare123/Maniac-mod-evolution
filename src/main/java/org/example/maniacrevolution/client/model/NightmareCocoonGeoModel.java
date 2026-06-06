package org.example.maniacrevolution.client.model;

import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.block.entity.NightmareCocoonBlockEntity;
import software.bernie.geckolib.model.GeoModel;

public class NightmareCocoonGeoModel extends GeoModel<NightmareCocoonBlockEntity> {
    @Override
    public ResourceLocation getModelResource(NightmareCocoonBlockEntity animatable) {
        return Maniacrev.loc("geo/nightmare_cocoon.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(NightmareCocoonBlockEntity animatable) {
        return Maniacrev.loc("textures/block/nightmare_cocoon.png");
    }

    @Override
    public ResourceLocation getAnimationResource(NightmareCocoonBlockEntity animatable) {
        return Maniacrev.loc("animations/nightmare_cocoon.animation.json");
    }
}
