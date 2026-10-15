package org.example.maniacrevolution.scp173.client;

import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.Maniacrev;
import software.bernie.geckolib.model.GeoModel;

public final class Scp173Model extends GeoModel<Scp173Statue> {
    @Override public ResourceLocation getModelResource(Scp173Statue statue) {
        return Maniacrev.loc("geo/scp173.geo.json");
    }
    @Override public ResourceLocation getTextureResource(Scp173Statue statue) {
        return Maniacrev.loc("textures/entity/scp173.png");
    }
    @Override public ResourceLocation getAnimationResource(Scp173Statue statue) {
        return Maniacrev.loc("animations/scp173.animation.json");
    }
}
