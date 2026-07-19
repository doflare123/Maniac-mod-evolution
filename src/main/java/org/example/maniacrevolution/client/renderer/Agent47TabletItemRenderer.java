package org.example.maniacrevolution.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.client.ClientAgent47TargetData;

import java.util.UUID;

public class Agent47TabletItemRenderer extends BlockEntityWithoutLevelRenderer {
    public static final ResourceLocation BASE_MODEL =
            new ResourceLocation(Maniacrev.MODID, "item/agent47_tablet_base");

    private static final float SURFACE_Y = 0.75F / 16.0F + 0.002F;
    private static final float FACE_MIN_X = 5.3F / 16.0F;
    private static final float FACE_MAX_X = 9.7F / 16.0F;
    private static final float FACE_MIN_Z = 4.5F / 16.0F;
    private static final float FACE_MAX_Z = 8.9F / 16.0F;
    private static final float TEXT_CENTER_X = 7.5F / 16.0F;
    private static final float TEXT_CENTER_Z = 10.0F / 16.0F;
    private static final float TEXT_SCALE = 0.0055F;

    public Agent47TabletItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext,
                             PoseStack poseStack, MultiBufferSource buffer,
                             int combinedLight, int combinedOverlay) {
        renderBaseModel(stack, poseStack, buffer, combinedLight, combinedOverlay);

        if (isHeld(displayContext)) {
            renderTargetScreen(poseStack, buffer);
        }
    }

    private static void renderBaseModel(ItemStack stack, PoseStack poseStack,
                                        MultiBufferSource buffer, int combinedLight,
                                        int combinedOverlay) {
        Minecraft minecraft = Minecraft.getInstance();
        BakedModel model = minecraft.getModelManager().getModel(BASE_MODEL);

        for (BakedModel pass : model.getRenderPasses(stack, true)) {
            for (RenderType renderType : pass.getRenderTypes(stack, true)) {
                VertexConsumer consumer = ItemRenderer.getFoilBufferDirect(
                        buffer, renderType, true, stack.hasFoil());
                minecraft.getItemRenderer().renderModelLists(
                        pass, stack, combinedLight, combinedOverlay, poseStack, consumer);
            }
        }
    }

    private static void renderTargetScreen(PoseStack poseStack, MultiBufferSource buffer) {
        UUID targetUuid = ClientAgent47TargetData.getTargetUuid();
        if (targetUuid == null) {
            renderHealthText(poseStack, buffer, "--", 0xFFFF5555);
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        PlayerInfo playerInfo = minecraft.getConnection() == null
                ? null
                : minecraft.getConnection().getPlayerInfo(targetUuid);
        ResourceLocation skin = playerInfo == null
                ? DefaultPlayerSkin.getDefaultSkin(targetUuid)
                : playerInfo.getSkinLocation();

        renderFaceLayer(poseStack, buffer, skin, 8.0F / 64.0F, 8.0F / 64.0F,
                16.0F / 64.0F, 16.0F / 64.0F, SURFACE_Y);
        renderFaceLayer(poseStack, buffer, skin, 40.0F / 64.0F, 8.0F / 64.0F,
                48.0F / 64.0F, 16.0F / 64.0F, SURFACE_Y + 0.001F);

        int health = ClientAgent47TargetData.getHealthPercent();
        int color = health > 60 ? 0xFF55FF55 : health > 30 ? 0xFFFFFF55 : 0xFFFF5555;
        renderHealthText(poseStack, buffer, health + "%", color);
    }

    private static void renderFaceLayer(PoseStack poseStack, MultiBufferSource buffer,
                                        ResourceLocation skin, float minU, float minV,
                                        float maxU, float maxV, float y) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(skin));
        PoseStack.Pose pose = poseStack.last();

        vertex(consumer, pose, FACE_MIN_X, y, FACE_MAX_Z, maxU, maxV);
        vertex(consumer, pose, FACE_MAX_X, y, FACE_MAX_Z, maxU, minV);
        vertex(consumer, pose, FACE_MAX_X, y, FACE_MIN_Z, minU, minV);
        vertex(consumer, pose, FACE_MIN_X, y, FACE_MIN_Z, minU, maxV);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose,
                               float x, float y, float z, float u, float v) {
        consumer.vertex(pose.pose(), x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    private static void renderHealthText(PoseStack poseStack, MultiBufferSource buffer,
                                         String text, int color) {
        Font font = Minecraft.getInstance().font;

        poseStack.pushPose();
        poseStack.translate(TEXT_CENTER_X, SURFACE_Y + 0.003F, TEXT_CENTER_Z);
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);
        font.drawInBatch(text, -font.width(text) / 2.0F, -font.lineHeight / 2.0F, color, false,
                poseStack.last().pose(), buffer, Font.DisplayMode.POLYGON_OFFSET,
                0, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    private static boolean isHeld(ItemDisplayContext context) {
        return context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }
}
