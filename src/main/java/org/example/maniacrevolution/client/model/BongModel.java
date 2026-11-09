package org.example.maniacrevolution.client.model;

import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.item.BongItem;
import software.bernie.geckolib.model.GeoModel;

public final class BongModel extends GeoModel<BongItem> {
    @Override
    public ResourceLocation getModelResource(BongItem animatable) {
        return Maniacrev.loc("geo/bong.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BongItem animatable) {
        return Maniacrev.loc("textures/item/bong.png");
    }

    @Override
    public ResourceLocation getAnimationResource(BongItem animatable) {
        return Maniacrev.loc("animations/bong.animation.json");
    }
}
