package org.example.maniacrevolution.client.model;

import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.client.renderer.KeeperNightmareAnimatable;
import software.bernie.geckolib.model.GeoModel;

public class KeeperNightmareModel extends GeoModel<KeeperNightmareAnimatable> {
    @Override
    public ResourceLocation getModelResource(KeeperNightmareAnimatable animatable) {
        return Maniacrev.loc("geo/windigo.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(KeeperNightmareAnimatable animatable) {
        return Maniacrev.loc("textures/entity/windigo.png");
    }

    @Override
    public ResourceLocation getAnimationResource(KeeperNightmareAnimatable animatable) {
        return Maniacrev.loc("animations/windigo.animation.json");
    }
}
