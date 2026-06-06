package org.example.maniacrevolution.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import org.example.maniacrevolution.block.entity.NightmareCocoonBlockEntity;
import org.example.maniacrevolution.client.model.NightmareCocoonGeoModel;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class NightmareCocoonRenderer extends GeoBlockRenderer<NightmareCocoonBlockEntity> {
    private static final String BAR = "||||||||||||";
    private static final int FULL_COLOR = 0xFFFF2020;
    private static final int LOST_COLOR = 0xFF4B0000;

    public NightmareCocoonRenderer() {
        super(new NightmareCocoonGeoModel());
    }

    @Override
    public void postRender(PoseStack poseStack, NightmareCocoonBlockEntity cocoon, BakedGeoModel model,
                           MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                           float partialTick, int packedLight, int packedOverlay, float red,
                           float green, float blue, float alpha) {
        super.postRender(poseStack, cocoon, model, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, red, green, blue, alpha);
        renderHealthBar(cocoon, poseStack, bufferSource);
    }

    private void renderHealthBar(NightmareCocoonBlockEntity cocoon, PoseStack poseStack,
                                 MultiBufferSource bufferSource) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        int filledChars = Math.round(BAR.length() * cocoon.getHealthProgress());
        float x = -font.width(BAR) / 2.0F;

        poseStack.pushPose();
        poseStack.translate(0.0D, 1.35D, 0.0D);
        poseStack.mulPose(minecraft.gameRenderer.getMainCamera().rotation());
        poseStack.scale(-0.02F, -0.02F, 0.02F);

        font.drawInBatch(BAR, x, 0.0F, LOST_COLOR, false, poseStack.last().pose(),
                bufferSource, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        if (filledChars > 0) {
            font.drawInBatch(BAR.substring(0, filledChars), x, 0.0F, FULL_COLOR, false,
                    poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0,
                    LightTexture.FULL_BRIGHT);
        }
        poseStack.popPose();
    }
}
