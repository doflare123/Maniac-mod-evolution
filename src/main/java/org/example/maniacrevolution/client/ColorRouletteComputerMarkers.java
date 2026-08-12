package org.example.maniacrevolution.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/** Through-wall red/gold diamonds for Red Guidance. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class ColorRouletteComputerMarkers {
    private static final List<Marker> MARKERS = new ArrayList<>();

    private ColorRouletteComputerMarkers() {}

    public static void set(List<BlockPos> positions, int durationTicks) {
        MARKERS.clear();
        for (BlockPos pos : positions) {
            MARKERS.add(new Marker(pos.immutable(), Math.max(1, durationTicks)));
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || Minecraft.getInstance().isPaused()) return;
        for (Marker marker : MARKERS) marker.age++;
        MARKERS.removeIf(marker -> marker.age >= marker.duration);
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
                || MARKERS.isEmpty() || Minecraft.getInstance().player == null) return;

        PoseStack pose = event.getPoseStack();
        double cameraX = event.getCamera().getPosition().x;
        double cameraY = event.getCamera().getPosition().y;
        double cameraZ = event.getCamera().getPosition().z;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        for (int i = 0; i < MARKERS.size(); i++) {
            Marker marker = MARKERS.get(i);
            float age = marker.age + event.getPartialTick();
            float appear = Mth.clamp(age / 6.0F, 0.0F, 1.0F);
            float fade = Mth.clamp((marker.duration - age) / 12.0F, 0.0F, 1.0F);
            float pulse = 1.0F + Mth.sin(age * 0.22F + i) * 0.12F;
            float scale = easeOutBack(appear) * pulse;
            if (fade <= 0.0F) continue;

            pose.pushPose();
            pose.translate(marker.pos.getX() + 0.5D - cameraX,
                    marker.pos.getY() + 1.55D - cameraY,
                    marker.pos.getZ() + 0.5D - cameraZ);
            pose.mulPose(event.getCamera().rotation());
            drawDiamond(pose.last().pose(), 0.48F * scale,
                    1.0F, 0.04F, 0.08F, 0.88F * fade);
            drawDiamond(pose.last().pose(), 0.25F * scale,
                    1.0F, 0.78F, 0.12F, fade);
            pose.popPose();
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void drawDiamond(Matrix4f matrix, float size,
                                    float red, float green, float blue, float alpha) {
        float half = size * 0.5F;
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, 0.0F, -half, 0.0F).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, half, 0.0F, 0.0F).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, 0.0F, half, 0.0F).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, -half, 0.0F, 0.0F).color(red, green, blue, alpha).endVertex();
        tesselator.end();
    }

    private static float easeOutBack(float value) {
        float shifted = value - 1.0F;
        return 1.0F + 2.70158F * shifted * shifted * shifted
                + 1.70158F * shifted * shifted;
    }

    @SubscribeEvent
    public static void onDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        MARKERS.clear();
    }

    private static final class Marker {
        private final BlockPos pos;
        private final int duration;
        private int age;

        private Marker(BlockPos pos, int duration) {
            this.pos = pos;
            this.duration = duration;
        }
    }
}
