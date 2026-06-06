package org.example.maniacrevolution.client.model;

import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.entity.KeeperNightmareDummyEntity;
import software.bernie.geckolib.model.GeoModel;

public class KeeperNightmareDummyModel extends GeoModel<KeeperNightmareDummyEntity> {
    @Override
    public ResourceLocation getModelResource(KeeperNightmareDummyEntity animatable) {
        return Maniacrev.loc("geo/windigo.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(KeeperNightmareDummyEntity animatable) {
        return Maniacrev.loc("textures/entity/windigo.png");
    }

    @Override
    public ResourceLocation getAnimationResource(KeeperNightmareDummyEntity animatable) {
        return Maniacrev.loc("animations/windigo.animation.json");
    }
}
