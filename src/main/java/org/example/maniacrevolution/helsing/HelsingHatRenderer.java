package org.example.maniacrevolution.helsing;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import org.example.maniacrevolution.Maniacrev;

public final class HelsingHatRenderer extends GeoArmorRenderer<HelsingHatItem> {
    public HelsingHatRenderer() { super(new GeoModel<>() {
        public ResourceLocation getModelResource(HelsingHatItem item) { return Maniacrev.loc("geo/helsing_hat.geo.json"); }
        public ResourceLocation getTextureResource(HelsingHatItem item) { return Maniacrev.loc("textures/armor/helsing_hat.png"); }
        public ResourceLocation getAnimationResource(HelsingHatItem item) { return Maniacrev.loc("animations/helsing_hat.animation.json"); }
    }); }
}
