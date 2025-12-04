package org.example.maniacrevolution.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.entity.BloodMarkerEntity;

/**
 * Рендерер для BloodMarkerEntity
 * Ничего не рендерит, т.к. маркер невидимый (только частицы)
 */
public class BloodMarkerRenderer extends EntityRenderer<BloodMarkerEntity> {

    public BloodMarkerRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(BloodMarkerEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // Ничего не рендерим - маркер невидимый
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(BloodMarkerEntity entity) {
        // Возвращаем пустую текстуру (не используется)
        return new ResourceLocation("minecraft", "textures/block/air.png");
    }
}