package org.example.maniacrevolution.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.scream.ScreamManager;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class ScreamClientHandler {
    private static final int APPEARANCE_TICKS = 8;
    private static final int POP_PEAK_TICK = 3;
    private static final int RECOIL_TICK = 5;
    private static final int FADE_TICKS = 10;

    private static final float START_SCALE = 0.0F;
    private static final float PEAK_SCALE = 1.45F;
    private static final float RECOIL_SCALE = 0.88F;
    private static final float SETTLED_SCALE = 1.0F;

    private static final float CORE_DIAMETER_BLOCKS = 0.12F;
    private static final float HALO_DIAMETER_BLOCKS = 0.22F;
    private static final float CORE_RED = 1.0F;
    private static final float CORE_GREEN = 0.03F;
    private static final float CORE_BLUE = 0.03F;
    private static final float CORE_ALPHA = 1.0F;
    private static final float HALO_RED = 1.0F;
    private static final float HALO_GREEN = 0.0F;
    private static final float HALO_BLUE = 0.0F;
    private static final float HALO_ALPHA = 0.38F;

    private static final List<ScreamMarker> ACTIVE_MARKERS = new ArrayList<>();

    private ScreamClientHandler() {
    }

    public static void receive(double x, double y, double z, int markerDurationTicks, boolean showMarker) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getSoundManager().play(new MapWideScreamSound(x, y, z));

        if (showMarker) {
            ACTIVE_MARKERS.add(new ScreamMarker(x, y, z, markerDurationTicks));
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || Minecraft.getInstance().isPaused()) {
            return;
        }

        for (ScreamMarker marker : ACTIVE_MARKERS) {
            marker.ageTicks++;
        }
        ACTIVE_MARKERS.removeIf(marker -> marker.ageTicks >= marker.durationTicks);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
                || ACTIVE_MARKERS.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        double cameraX = event.getCamera().getPosition().x;
        double cameraY = event.getCamera().getPosition().y;
        double cameraZ = event.getCamera().getPosition().z;
        float partialTick = event.getPartialTick();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        for (ScreamMarker marker : ACTIVE_MARKERS) {
            float age = marker.ageTicks + partialTick;
            float scale = calculateScale(age);
            float alpha = calculateAlpha(age, marker.durationTicks);
            if (scale <= START_SCALE || alpha <= START_SCALE) {
                continue;
            }

            poseStack.pushPose();
            poseStack.translate(marker.x - cameraX, marker.y - cameraY, marker.z - cameraZ);
            poseStack.mulPose(event.getCamera().rotation());
            renderMarker(poseStack.last().pose(), scale, alpha);
            poseStack.popPose();
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        ACTIVE_MARKERS.clear();
    }

    private static float calculateScale(float ageTicks) {
        if (ageTicks < POP_PEAK_TICK) {
            return Mth.lerp(easeOutCubic(ageTicks / POP_PEAK_TICK), START_SCALE, PEAK_SCALE);
        }
        if (ageTicks < RECOIL_TICK) {
            float progress = (ageTicks - POP_PEAK_TICK) / (RECOIL_TICK - POP_PEAK_TICK);
            return Mth.lerp(progress, PEAK_SCALE, RECOIL_SCALE);
        }
        if (ageTicks < APPEARANCE_TICKS) {
            float progress = (ageTicks - RECOIL_TICK) / (APPEARANCE_TICKS - RECOIL_TICK);
            return Mth.lerp(easeOutCubic(progress), RECOIL_SCALE, SETTLED_SCALE);
        }
        return SETTLED_SCALE;
    }

    private static float calculateAlpha(float ageTicks, int durationTicks) {
        float fadeStart = durationTicks - FADE_TICKS;
        if (ageTicks <= fadeStart) {
            return SETTLED_SCALE;
        }
        return Mth.clamp((durationTicks - ageTicks) / FADE_TICKS, START_SCALE, SETTLED_SCALE);
    }

    private static float easeOutCubic(float value) {
        float inverse = SETTLED_SCALE - Mth.clamp(value, START_SCALE, SETTLED_SCALE);
        return SETTLED_SCALE - inverse * inverse * inverse;
    }

    private static void renderMarker(Matrix4f matrix, float scale, float alpha) {
        drawQuad(matrix, HALO_DIAMETER_BLOCKS * scale, HALO_RED, HALO_GREEN, HALO_BLUE,
                HALO_ALPHA * alpha);
        drawQuad(matrix, CORE_DIAMETER_BLOCKS * scale, CORE_RED, CORE_GREEN, CORE_BLUE,
                CORE_ALPHA * alpha);
    }

    private static void drawQuad(Matrix4f matrix, float diameter,
                                 float red, float green, float blue, float alpha) {
        float halfSize = diameter * 0.5F;
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, -halfSize, -halfSize, 0.0F).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, halfSize, -halfSize, 0.0F).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, halfSize, halfSize, 0.0F).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, -halfSize, halfSize, 0.0F).color(red, green, blue, alpha).endVertex();
        tesselator.end();
    }

    private static final class MapWideScreamSound extends AbstractSoundInstance {
        private MapWideScreamSound(double x, double y, double z) {
            super(SoundEvents.FOX_SCREECH, SoundSource.PLAYERS, RandomSource.create());
            this.x = x;
            this.y = y;
            this.z = z;
            this.volume = ScreamManager.SOUND_VOLUME;
            this.pitch = ScreamManager.SOUND_PITCH;
            this.looping = false;
            this.delay = 0;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.relative = false;
        }
    }

    private static final class ScreamMarker {
        private final double x;
        private final double y;
        private final double z;
        private final int durationTicks;
        private int ageTicks;

        private ScreamMarker(double x, double y, double z, int durationTicks) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.durationTicks = durationTicks;
        }
    }
}
