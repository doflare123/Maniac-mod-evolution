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
import net.minecraft.util.Mth;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.entity.NetherSwapProjectile;
import org.joml.Matrix4f;

public class NetherSwapProjectileRenderer extends EntityRenderer<NetherSwapProjectile> {

    // Используем текстуру из ванили — можно заменить на свою
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("minecraft", "textures/entity/end_crystal/end_crystal_beam.png");

    // Цвет лазера: ярко-фиолетовый
    private static final float R = 0.6f;
    private static final float G = 0.0f;
    private static final float B = 1.0f;
    private static final float A = 0.9f;

    public NetherSwapProjectileRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(NetherSwapProjectile entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {

        poseStack.pushPose();

        // Интерполируем поворот
        float yaw   = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());

        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw - 90f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch));

        // Пульсация — лазер немного мерцает
        float pulse = (float)(Math.sin(entity.tickCount * 0.5 + partialTick) * 0.15 + 0.85);

        VertexConsumer vc = bufferSource.getBuffer(
                RenderType.entityTranslucentEmissive(TEXTURE));

        Matrix4f pose = poseStack.last().pose();

        // Рисуем два перекрещённых квада — классический лазер
        float w = 0.08f;  // ширина
        float l = 0.6f;   // длина (снаряд "вытянут" в направлении движения)

        drawQuad(vc, pose, -l, -w, 0,  l, w, 0,  R, G, B, A * pulse);
        drawQuad(vc, pose, -l, 0, -w,  l, 0, w,  R, G, B, A * pulse);

        // Яркое ядро
        float cw = 0.03f;
        drawQuad(vc, pose, -l, -cw, 0, l, cw, 0, 1f, 0.8f, 1f, 1f);
        drawQuad(vc, pose, -l, 0, -cw, l, 0, cw, 1f, 0.8f, 1f, 1f);

        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private void drawQuad(VertexConsumer vc, Matrix4f pose,
                          float x1, float y1, float z1,
                          float x2, float y2, float z2,
                          float r, float g, float b, float a) {
        int ri = (int)(r * 255);
        int gi = (int)(g * 255);
        int bi = (int)(b * 255);
        int ai = (int)(a * 255);

        vc.vertex(pose, x1,  y1,  z1).color(ri, gi, bi, ai).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(0, 1, 0).endVertex();
        vc.vertex(pose, x1, -y1, -z1).color(ri, gi, bi, ai).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(0, 1, 0).endVertex();
        vc.vertex(pose, x2, -y2, -z2).color(ri, gi, bi, ai).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(0, 1, 0).endVertex();
        vc.vertex(pose, x2,  y2,  z2).color(ri, gi, bi, ai).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(0, 1, 0).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(NetherSwapProjectile entity) {
        return TEXTURE;
    }
}