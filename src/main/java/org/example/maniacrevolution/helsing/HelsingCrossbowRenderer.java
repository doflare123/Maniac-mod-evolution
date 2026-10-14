package org.example.maniacrevolution.helsing;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import org.example.maniacrevolution.Maniacrev;

public final class HelsingCrossbowRenderer extends GeoItemRenderer<HelsingCrossbowItem> {
    public HelsingCrossbowRenderer() { super(new GeoModel<>() {
        public ResourceLocation getModelResource(HelsingCrossbowItem item) { return Maniacrev.loc("geo/helsing_crossbow.geo.json"); }
        public ResourceLocation getTextureResource(HelsingCrossbowItem item) { return Maniacrev.loc("textures/item/helsing_crossbow.png"); }
        public ResourceLocation getAnimationResource(HelsingCrossbowItem item) { return Maniacrev.loc("animations/helsing_crossbow.animation.json"); }
    }); }
    @Override public void renderRecursively(com.mojang.blaze3d.vertex.PoseStack pose, HelsingCrossbowItem item,
            software.bernie.geckolib.cache.object.GeoBone bone, net.minecraft.client.renderer.RenderType type,
            net.minecraft.client.renderer.MultiBufferSource buffers, com.mojang.blaze3d.vertex.VertexConsumer vertices,
            boolean reRender, float partial, int light, int overlay, float red, float green, float blue, float alpha) {
        String prefix = "bolt_variant_";
        if (bone.getName().startsWith(prefix)) {
            String visible = prefix + HelsingCrossbowItem.visualKind(getCurrentItemStack()).name().toLowerCase(java.util.Locale.ROOT);
            if (!bone.getName().equals(visible)) return;
        }
        super.renderRecursively(pose, item, bone, type, buffers, vertices, reRender, partial, light, overlay, red, green, blue, alpha);
    }
}
