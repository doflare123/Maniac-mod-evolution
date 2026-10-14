package org.example.maniacrevolution.helsing;

import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.Maniacrev;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/** Uses the wearable geometry in inventory too; recenter its head attachment at item origin. */
public final class HelsingHatItemRenderer extends GeoItemRenderer<HelsingHatItem> {
    public HelsingHatItemRenderer() { super(new GeoModel<>() {
        public ResourceLocation getModelResource(HelsingHatItem item) { return Maniacrev.loc("geo/helsing_hat.geo.json"); }
        public ResourceLocation getTextureResource(HelsingHatItem item) { return Maniacrev.loc("textures/armor/helsing_hat.png"); }
        public ResourceLocation getAnimationResource(HelsingHatItem item) { return Maniacrev.loc("animations/helsing_hat.animation.json"); }
    }); }
    @Override public void preRender(com.mojang.blaze3d.vertex.PoseStack pose, HelsingHatItem item,
            software.bernie.geckolib.cache.object.BakedGeoModel model, net.minecraft.client.renderer.MultiBufferSource buffers,
            com.mojang.blaze3d.vertex.VertexConsumer vertices, boolean reRender, float partial, int light, int overlay,
            float red, float green, float blue, float alpha) {
        super.preRender(pose, item, model, buffers, vertices, reRender, partial, light, overlay, red, green, blue, alpha);
        pose.translate(0, -2, 0);
    }
}
