package org.example.maniacrevolution.strange.client;

import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.item.SpaceAmuletItem;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class SpaceAmuletRenderer extends GeoItemRenderer<SpaceAmuletItem> {
    @Override public void preRender(com.mojang.blaze3d.vertex.PoseStack poses, SpaceAmuletItem item,
            software.bernie.geckolib.cache.object.BakedGeoModel model, net.minecraft.client.renderer.MultiBufferSource buffers,
            com.mojang.blaze3d.vertex.VertexConsumer consumer, boolean reRender, float partial, int light, int overlay,
            float red, float green, float blue, float alpha) {
        super.preRender(poses,item,model,buffers,consumer,reRender,partial,light,overlay,red,green,blue,alpha);
        if (!reRender && (renderPerspective==net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || renderPerspective==net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || renderPerspective==net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                || renderPerspective==net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_LEFT_HAND)) {
            // Geometry's gem center is at Y=6; held-item origin must be the grip, not the model's feet.
            poses.translate(0,-6d/16,0);
        }
    }
    public SpaceAmuletRenderer() {
        super(new GeoModel<>() {
            @Override public ResourceLocation getModelResource(SpaceAmuletItem item) { return Maniacrev.loc("geo/space_amulet.geo.json"); }
            @Override public ResourceLocation getTextureResource(SpaceAmuletItem item) { return Maniacrev.loc("textures/item/space_amulet.png"); }
            @Override public ResourceLocation getAnimationResource(SpaceAmuletItem item) { return Maniacrev.loc("animations/space_amulet.animation.json"); }
        });
    }
}
