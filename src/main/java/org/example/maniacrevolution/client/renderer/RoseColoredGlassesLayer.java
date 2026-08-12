package org.example.maniacrevolution.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.item.RoseColoredGlassesItem;

/** A bright textured glasses plane attached directly to the animated player head. */
public final class RoseColoredGlassesLayer
        extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            Maniacrev.MODID, "textures/entity/rose_colored_glasses.png");

    private static final float MIN_X = -4.3F / 16.0F;
    private static final float MAX_X = 4.3F / 16.0F;
    private static final float MIN_Y = -5.9F / 16.0F;
    private static final float MAX_Y = -2.0F / 16.0F;
    private static final float FACE_Z = -4.18F / 16.0F;

    public RoseColoredGlassesLayer(
            RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()
                || !player.hasEffect(ModEffects.ROSE_COLORED_GLASSES.get())
                || !player.getOffhandItem().is(ModItems.ROSE_COLORED_GLASSES.get())
                || !RoseColoredGlassesItem.belongsTo(
                        player.getOffhandItem(), player.getUUID())) {
            return;
        }

        poseStack.pushPose();
        getParentModel().head.translateAndRotate(poseStack);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        PoseStack.Pose pose = poseStack.last();

        vertex(consumer, pose, MIN_X, MAX_Y, FACE_Z, 0.0F, 1.0F, packedLight);
        vertex(consumer, pose, MAX_X, MAX_Y, FACE_Z, 1.0F, 1.0F, packedLight);
        vertex(consumer, pose, MAX_X, MIN_Y, FACE_Z, 1.0F, 0.0F, packedLight);
        vertex(consumer, pose, MIN_X, MIN_Y, FACE_Z, 0.0F, 0.0F, packedLight);
        poseStack.popPose();
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose,
                               float x, float y, float z, float u, float v, int light) {
        consumer.vertex(pose.pose(), x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(pose.normal(), 0.0F, 0.0F, -1.0F)
                .endVertex();
    }
}
