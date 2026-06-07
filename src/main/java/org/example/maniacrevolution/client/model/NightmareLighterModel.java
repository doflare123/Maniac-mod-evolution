package org.example.maniacrevolution.client.model;

import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.item.NightmareLighterItem;
import software.bernie.geckolib.model.GeoModel;

public class NightmareLighterModel extends GeoModel<NightmareLighterItem> {
    @Override
    public ResourceLocation getModelResource(NightmareLighterItem animatable) {
        return Maniacrev.loc("geo/nightmare_lighter.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(NightmareLighterItem animatable) {
        return Maniacrev.loc("textures/item/nightmare_lighter.png");
    }

    @Override
    public ResourceLocation getAnimationResource(NightmareLighterItem animatable) {
        return Maniacrev.loc("animations/nightmare_lighter.animation.json");
    }
}
