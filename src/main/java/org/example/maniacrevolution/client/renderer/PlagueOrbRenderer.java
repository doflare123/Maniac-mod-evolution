package org.example.maniacrevolution.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.entity.PlagueOrbEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Рендерер зелёного сгустка чумы.
 *
 * Рисует текстурированный квад (билборд — всегда повёрнут к камере).
 * Текстура: assets/maniacrev/textures/entity/plague_orb.png (16×16 или 32×32)
 *
 * Зарегистрируйте в ClientSetupEvent:
 *   EntityRenderers.register(ModEntities.PLAGUE_ORB.get(), PlagueOrbRenderer::new);
 */
public class PlagueOrbRenderer extends EntityRenderer<PlagueOrbEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("maniacrev", "textures/entity/plague_orb.png");

    // Размер отображаемого сгустка в мировых единицах
    private static final float SIZE = 0.5f;

    public PlagueOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
        // Тень под сгустком (маленькая)
        this.shadowRadius = 0.15f;
    }

    @Override
    public void render(PlagueOrbEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {

        poseStack.pushPose();

        // Билборд: поворачиваем квад к камере
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));

        // Пульсация масштаба
        float pulse = 1.0f + (float) Math.sin(entity.tickCount * 0.3f + partialTick * 0.3f) * 0.08f;
        poseStack.scale(SIZE * pulse, SIZE * pulse, SIZE * pulse);

        // Получаем вертекс-буфер с прозрачностью (additive blend — светится)
        VertexConsumer consumer = bufferSource.getBuffer(
                RenderType.entityTranslucentEmissive(TEXTURE)
        );

        Matrix4f pose   = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        float half = 0.5f;
        // Рисуем билборд-квад (2 треугольника = 4 вершины)
        consumer.vertex(pose, -half, -half, 0).color(0.2f, 1.0f, 0.2f, 0.95f)
                .uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight).normal(normal, 0, 1, 0).endVertex();
        consumer.vertex(pose,  half, -half, 0).color(0.2f, 1.0f, 0.2f, 0.95f)
                .uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight).normal(normal, 0, 1, 0).endVertex();
        consumer.vertex(pose,  half,  half, 0).color(0.2f, 1.0f, 0.2f, 0.95f)
                .uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight).normal(normal, 0, 1, 0).endVertex();
        consumer.vertex(pose, -half,  half, 0).color(0.2f, 1.0f, 0.2f, 0.95f)
                .uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight).normal(normal, 0, 1, 0).endVertex();

        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(PlagueOrbEntity entity) {
        return TEXTURE;
    }
}