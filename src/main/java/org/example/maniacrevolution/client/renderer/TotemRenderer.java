package org.example.maniacrevolution.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.client.model.TotemModel;
import org.example.maniacrevolution.entity.TotemEntity;

/**
 * Рендерер тотема шамана.
*/
public class TotemRenderer extends MobRenderer<TotemEntity, TotemModel> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("maniacrev", "textures/entity/shaman_totem.png");

    public TotemRenderer(EntityRendererProvider.Context ctx) {
        super(ctx,
                new TotemModel(ctx.bakeLayer(TotemModel.LAYER_LOCATION)),
                0.5f); // shadow radius
    }

    @Override
    public ResourceLocation getTextureLocation(TotemEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(TotemEntity entity, PoseStack poseStack, float partialTick) {
        // Масштаб при необходимости — по умолчанию 1:1
        poseStack.scale(0.45f, 0.45f, 0.45f);
    }
}