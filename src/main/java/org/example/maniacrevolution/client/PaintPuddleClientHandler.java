package org.example.maniacrevolution.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.paint.PaintColor;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.LinkedHashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class PaintPuddleClientHandler {
    private static final ResourceLocation SPLAT_TEXTURE =
            new ResourceLocation(Maniacrev.MODID, "textures/paint/paint_splat.png");
    private static final float PUDDLE_DIAMETER_BLOCKS = 1.0F;
    private static final int APPEARANCE_TICKS = 7;
    private static final float UNDERLAY_SCALE = 1.06F;
    private static final float GLOSS_SCALE = 0.54F;
    private static final double UNDERLAY_HEIGHT = 0.001D;
    private static final double MAIN_HEIGHT = 0.004D;
    private static final double GLOSS_HEIGHT = 0.008D;

    private static final Map<Long, ClientPuddle> PUDDLES = new LinkedHashMap<>();

    private PaintPuddleClientHandler() {
    }

    public static void add(long id, double x, double y, double z,
                           PaintColor color, float rotation) {
        PUDDLES.put(id, new ClientPuddle(x, y, z, color, rotation));
    }

    public static void remove(long id) {
        PUDDLES.remove(id);
    }

    public static void clear() {
        PUDDLES.clear();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || Minecraft.getInstance().isPaused()) return;
        for (ClientPuddle puddle : PUDDLES.values()) {
            puddle.ageTicks++;
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
                || PUDDLES.isEmpty()) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;

        double cameraX = event.getCamera().getPosition().x;
        double cameraY = event.getCamera().getPosition().y;
        double cameraZ = event.getCamera().getPosition().z;
        float partialTick = event.getPartialTick();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        RenderType renderType = RenderType.entityTranslucent(SPLAT_TEXTURE);
        VertexConsumer consumer = buffers.getBuffer(renderType);

        for (ClientPuddle puddle : PUDDLES.values()) {
            float appearance = Mth.clamp(
                    (puddle.ageTicks + partialTick) / APPEARANCE_TICKS, 0.0F, 1.0F);
            float popScale = 1.0F + 0.16F * (float) Math.sin(Math.PI * appearance);
            float scale = easeOutBack(appearance) * popScale;
            float shimmer = 0.72F + 0.12F * (float) Math.sin(
                    (puddle.ageTicks + partialTick) * 0.16F + puddle.rotation);
            int light = LevelRenderer.getLightColor(minecraft.level,
                    BlockPos.containing(puddle.x, puddle.y, puddle.z));

            poseStack.pushPose();
            poseStack.translate(puddle.x - cameraX, puddle.y - cameraY, puddle.z - cameraZ);
            poseStack.mulPose(Axis.YP.rotationDegrees(puddle.rotation));
            drawQuad(poseStack, consumer, light,
                    PUDDLE_DIAMETER_BLOCKS * scale * UNDERLAY_SCALE,
                    puddle.color.red() * 0.55F,
                    puddle.color.green() * 0.55F,
                    puddle.color.blue() * 0.55F,
                    0.65F, UNDERLAY_HEIGHT);
            drawQuad(poseStack, consumer, light,
                    PUDDLE_DIAMETER_BLOCKS * scale,
                    puddle.color.red(), puddle.color.green(), puddle.color.blue(),
                    0.96F, MAIN_HEIGHT);
            poseStack.mulPose(Axis.YP.rotationDegrees(31.0F));
            drawQuad(poseStack, consumer, light,
                    PUDDLE_DIAMETER_BLOCKS * scale * GLOSS_SCALE,
                    Mth.lerp(0.58F, puddle.color.red(), 1.0F),
                    Mth.lerp(0.58F, puddle.color.green(), 1.0F),
                    Mth.lerp(0.58F, puddle.color.blue(), 1.0F),
                    shimmer, GLOSS_HEIGHT);
            poseStack.popPose();
        }

        buffers.endBatch(renderType);
    }

    @SubscribeEvent
    public static void onDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    private static float easeOutBack(float value) {
        float shifted = value - 1.0F;
        float overshoot = 1.70158F;
        return 1.0F + (overshoot + 1.0F) * shifted * shifted * shifted
                + overshoot * shifted * shifted;
    }

    private static void drawQuad(PoseStack poseStack, VertexConsumer consumer,
                                 int light, float diameter,
                                 float red, float green, float blue, float alpha,
                                 double height) {
        float half = diameter * 0.5F;
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        vertex(consumer, matrix, normal, -half, -half, 0, 0,
                red, green, blue, alpha, light, height);
        vertex(consumer, matrix, normal, -half, half, 0, 1,
                red, green, blue, alpha, light, height);
        vertex(consumer, matrix, normal, half, half, 1, 1,
                red, green, blue, alpha, light, height);
        vertex(consumer, matrix, normal, half, -half, 1, 0,
                red, green, blue, alpha, light, height);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal,
                               float x, float z, float u, float v,
                               float red, float green, float blue, float alpha,
                               int light, double height) {
        consumer.vertex(matrix, x, (float) height, z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normal, 0, 1, 0)
                .endVertex();
    }

    private static final class ClientPuddle {
        private final double x;
        private final double y;
        private final double z;
        private final PaintColor color;
        private final float rotation;
        private int ageTicks;

        private ClientPuddle(double x, double y, double z,
                             PaintColor color, float rotation) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.color = color;
            this.rotation = rotation;
        }
    }
}
