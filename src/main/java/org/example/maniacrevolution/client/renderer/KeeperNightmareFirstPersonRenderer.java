package org.example.maniacrevolution.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.data.ClientKeeperFormData;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class KeeperNightmareFirstPersonRenderer {
    private KeeperNightmareFirstPersonRenderer() {}

    @SubscribeEvent
    public static void onRenderArm(RenderArmEvent event) {
        if (!ClientKeeperFormData.isKeeper(event.getPlayer())) return;

        event.setCanceled(true);
        renderKeeperArm(event.getPoseStack(), event.getArm());
    }

    private static void renderKeeperArm(PoseStack poseStack, HumanoidArm arm) {
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;

        poseStack.pushPose();
        poseStack.translate(side * 0.42D, -0.34D, -0.62D);
        poseStack.mulPose(Axis.YP.rotationDegrees(side * 14.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * -8.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-10.0F));

        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        Matrix4f matrix = poseStack.last().pose();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        drawBox(buffer, matrix, -0.09F, -0.10F, -0.30F, 0.09F, 0.10F, 0.24F,
                0.09F, 0.07F, 0.08F, 1.0F);
        drawBox(buffer, matrix, -0.15F, -0.13F, -0.53F, 0.15F, 0.12F, -0.25F,
                0.12F, 0.09F, 0.10F, 1.0F);
        drawBox(buffer, matrix, -0.12F, 0.08F, -0.45F, 0.12F, 0.13F, -0.10F,
                0.18F, 0.03F, 0.03F, 1.0F);

        drawClaw(buffer, matrix, -0.10F);
        drawClaw(buffer, matrix, 0.0F);
        drawClaw(buffer, matrix, 0.10F);

        tesselator.end();
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private static void drawClaw(BufferBuilder buffer, Matrix4f matrix, float x) {
        drawBox(buffer, matrix, x - 0.025F, -0.055F, -0.88F, x + 0.025F, 0.025F, -0.50F,
                0.78F, 0.76F, 0.70F, 1.0F);
    }

    private static void drawBox(BufferBuilder buffer, Matrix4f matrix,
                                float minX, float minY, float minZ,
                                float maxX, float maxY, float maxZ,
                                float red, float green, float blue, float alpha) {
        quad(buffer, matrix, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ,
                red, green, blue, alpha);
        quad(buffer, matrix, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ,
                red, green, blue, alpha);
        quad(buffer, matrix, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ,
                red, green, blue, alpha);
        quad(buffer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, minX, minY, maxZ,
                red, green, blue, alpha);
        quad(buffer, matrix, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, minX, minY, minZ,
                red, green, blue, alpha);
        quad(buffer, matrix, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ,
                red, green, blue, alpha);
    }

    private static void quad(BufferBuilder buffer, Matrix4f matrix,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float x4, float y4, float z4,
                             float red, float green, float blue, float alpha) {
        buffer.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x3, y3, z3).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x4, y4, z4).color(red, green, blue, alpha).endVertex();
    }
}
