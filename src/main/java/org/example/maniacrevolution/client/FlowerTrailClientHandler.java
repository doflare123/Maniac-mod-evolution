package org.example.maniacrevolution.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.flower.FlowerVariant;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.LinkedHashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class FlowerTrailClientHandler {
    private static final int WIND_DURATION_TICKS = 24;
    private static final int LANDING_BOUNCE_TICKS = 6;

    private static final double DROP_ARC_BLOCKS = 0.14D;
    private static final double LANDING_BOUNCE_HEIGHT_BLOCKS = 0.055D;
    private static final double WIND_TRAVEL_BLOCKS = 1.50D;
    private static final double WIND_LIFT_BLOCKS = 0.55D;
    private static final double WIND_FLUTTER_HEIGHT_BLOCKS = 0.06D;
    private static final double WIND_FLUTTER_CYCLES = 3.0D;

    private static final float DROP_START_TILT_DEGREES = 55.0F;
    private static final float DROP_TUMBLE_TILT_DEGREES = 34.0F;
    private static final float DROP_SIDE_TILT_DEGREES = 20.0F;
    private static final float DROP_ROTATION_DEGREES = 540.0F;
    private static final float WIND_ROTATION_DEGREES = 720.0F;
    private static final float WIND_FORWARD_TILT_DEGREES = 55.0F;
    private static final float WIND_SIDE_TILT_DEGREES = 32.0F;
    private static final float WIND_END_SCALE = 0.80F;

    private static final float FLOWER_DIAMETER_BLOCKS = 0.55F;
    private static final float UNDERLAY_SIZE_MULTIPLIER = 1.10F;
    private static final float MIDDLE_LAYER_SIZE_MULTIPLIER = 0.82F;
    private static final float TOP_LAYER_SIZE_MULTIPLIER = 0.66F;
    private static final float UNDERLAY_ALPHA = 0.38F;
    private static final float MAIN_LAYER_ALPHA = 0.96F;
    private static final float MIDDLE_LAYER_ALPHA = 0.88F;
    private static final float TOP_LAYER_ALPHA = 0.80F;
    private static final float MAIN_LAYER_WHITE_MIX = 0.18F;
    private static final float MIDDLE_LAYER_WHITE_MIX = 0.30F;
    private static final float TOP_LAYER_WHITE_MIX = 0.42F;
    private static final float MIDDLE_LAYER_ROTATION_DEGREES = 37.0F;
    private static final float TOP_LAYER_ROTATION_DEGREES = -29.0F;
    private static final double MIDDLE_LAYER_HEIGHT_BLOCKS = 0.008D;
    private static final double TOP_LAYER_HEIGHT_BLOCKS = 0.014D;

    private static final float MIN_PROGRESS = 0.0F;
    private static final float MAX_PROGRESS = 1.0F;
    private static final float FULL_TEXTURE_PIXELS = 16.0F;
    private static final float SMOOTHSTEP_FIRST_FACTOR = 3.0F;
    private static final float SMOOTHSTEP_SECOND_FACTOR = 2.0F;
    private static final double DOUBLE_ANGLE_FACTOR = 2.0D;

    private static final Map<Long, FlowerMarker> ACTIVE_MARKERS = new LinkedHashMap<>();

    private FlowerTrailClientHandler() {
    }

    public static void add(long traceId,
                           double startX, double startY, double startZ,
                           double targetX, double targetY, double targetZ,
                           FlowerVariant variant, int durationTicks, int ageTicks,
                           int fallDurationTicks, float windX, float windZ,
                           float baseRotationDegrees, float scale) {
        ACTIVE_MARKERS.put(traceId, new FlowerMarker(
                startX,
                startY,
                startZ,
                targetX,
                targetY,
                targetZ,
                variant,
                durationTicks,
                ageTicks,
                fallDurationTicks,
                windX,
                windZ,
                baseRotationDegrees,
                scale
        ));
    }

    public static void clear() {
        ACTIVE_MARKERS.clear();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || Minecraft.getInstance().isPaused()) {
            return;
        }

        for (FlowerMarker marker : ACTIVE_MARKERS.values()) {
            marker.ageTicks++;
        }
        ACTIVE_MARKERS.values().removeIf(marker -> marker.ageTicks >= marker.durationTicks);
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

        double cameraX = event.getCamera().getPosition().x;
        double cameraY = event.getCamera().getPosition().y;
        double cameraZ = event.getCamera().getPosition().z;
        float partialTick = event.getPartialTick();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        RenderType renderType = RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS);
        VertexConsumer consumer = buffers.getBuffer(renderType);

        for (FlowerMarker marker : ACTIVE_MARKERS.values()) {
            float age = marker.ageTicks + partialTick;
            FlowerPose flowerPose = calculatePose(marker, age);
            if (flowerPose.alpha <= MIN_PROGRESS) {
                continue;
            }

            TextureAtlasSprite sprite = resolveSprite(minecraft, marker.variant);
            int packedLight = LevelRenderer.getLightColor(
                    minecraft.level,
                    BlockPos.containing(flowerPose.x, flowerPose.y, flowerPose.z)
            );

            poseStack.pushPose();
            poseStack.translate(
                    flowerPose.x - cameraX,
                    flowerPose.y - cameraY,
                    flowerPose.z - cameraZ
            );
            poseStack.mulPose(Axis.YP.rotationDegrees(flowerPose.rotationDegrees));
            poseStack.mulPose(Axis.XP.rotationDegrees(flowerPose.tiltXDegrees));
            poseStack.mulPose(Axis.ZP.rotationDegrees(flowerPose.tiltZDegrees));
            renderFlower(
                    poseStack,
                    consumer,
                    sprite,
                    marker.variant,
                    packedLight,
                    flowerPose.scale,
                    flowerPose.alpha
            );
            poseStack.popPose();
        }

        buffers.endBatch(renderType);
    }

    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    private static FlowerPose calculatePose(FlowerMarker marker, float ageTicks) {
        if (ageTicks < marker.fallDurationTicks) {
            float progress = Mth.clamp(
                    ageTicks / marker.fallDurationTicks,
                    MIN_PROGRESS,
                    MAX_PROGRESS
            );
            float easedProgress = progress * progress;
            double arc = Math.sin(Math.PI * progress) * DROP_ARC_BLOCKS;
            double perpendicularX = -marker.windZ;
            double perpendicularZ = marker.windX;

            return new FlowerPose(
                    Mth.lerp(easedProgress, marker.startX, marker.targetX)
                            + perpendicularX * arc,
                    Mth.lerp(easedProgress, marker.startY, marker.targetY),
                    Mth.lerp(easedProgress, marker.startZ, marker.targetZ)
                            + perpendicularZ * arc,
                    marker.baseRotationDegrees + DROP_ROTATION_DEGREES * progress,
                    Mth.lerp(progress, DROP_START_TILT_DEGREES, MIN_PROGRESS)
                            + (float) Math.sin(Math.PI * progress)
                            * DROP_TUMBLE_TILT_DEGREES,
                    (float) Math.sin(Math.PI * DOUBLE_ANGLE_FACTOR * progress)
                            * DROP_SIDE_TILT_DEGREES,
                    marker.scale,
                    MAX_PROGRESS
            );
        }

        float windStartTick = marker.durationTicks - WIND_DURATION_TICKS;
        if (ageTicks < windStartTick) {
            float landedAge = ageTicks - marker.fallDurationTicks;
            float bounce = MIN_PROGRESS;
            if (landedAge < LANDING_BOUNCE_TICKS) {
                float bounceProgress = Mth.clamp(
                        landedAge / LANDING_BOUNCE_TICKS,
                        MIN_PROGRESS,
                        MAX_PROGRESS
                );
                bounce = (float) (Math.sin(Math.PI * bounceProgress)
                        * LANDING_BOUNCE_HEIGHT_BLOCKS
                        * (MAX_PROGRESS - bounceProgress));
            }

            return new FlowerPose(
                    marker.targetX,
                    marker.targetY + bounce,
                    marker.targetZ,
                    marker.baseRotationDegrees + DROP_ROTATION_DEGREES,
                    MIN_PROGRESS,
                    MIN_PROGRESS,
                    marker.scale,
                    MAX_PROGRESS
            );
        }

        float windProgress = Mth.clamp(
                (ageTicks - windStartTick) / WIND_DURATION_TICKS,
                MIN_PROGRESS,
                MAX_PROGRESS
        );
        float easedWind = windProgress * windProgress;
        double flutter = Math.sin(
                Math.PI * WIND_FLUTTER_CYCLES * windProgress
        ) * WIND_FLUTTER_HEIGHT_BLOCKS * windProgress;

        return new FlowerPose(
                marker.targetX + marker.windX * WIND_TRAVEL_BLOCKS * easedWind,
                marker.targetY + WIND_LIFT_BLOCKS * easedWind + flutter,
                marker.targetZ + marker.windZ * WIND_TRAVEL_BLOCKS * easedWind,
                marker.baseRotationDegrees + DROP_ROTATION_DEGREES
                        + WIND_ROTATION_DEGREES * windProgress,
                (float) Math.sin(Math.PI * windProgress)
                        * WIND_FORWARD_TILT_DEGREES,
                (float) Math.sin(Math.PI * DOUBLE_ANGLE_FACTOR * windProgress)
                        * WIND_SIDE_TILT_DEGREES,
                marker.scale * Mth.lerp(
                        windProgress,
                        MAX_PROGRESS,
                        WIND_END_SCALE
                ),
                MAX_PROGRESS - smoothStep(windProgress)
        );
    }

    private static float smoothStep(float value) {
        float clamped = Mth.clamp(value, MIN_PROGRESS, MAX_PROGRESS);
        return clamped * clamped * (
                SMOOTHSTEP_FIRST_FACTOR - SMOOTHSTEP_SECOND_FACTOR * clamped
        );
    }

    static TextureAtlasSprite resolveSprite(Minecraft minecraft,
                                            FlowerVariant variant) {
        BlockState state = BuiltInRegistries.BLOCK.get(variant.getBlockId())
                .defaultBlockState();
        if (variant.usesUpperHalf() && state.hasProperty(DoublePlantBlock.HALF)) {
            state = state.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER);
        }

        BlockRenderDispatcher blockRenderer = minecraft.getBlockRenderer();
        return blockRenderer.getBlockModel(state).getParticleIcon();
    }

    static void renderFlower(PoseStack poseStack,
                             VertexConsumer consumer,
                             TextureAtlasSprite sprite,
                             FlowerVariant variant,
                             int packedLight,
                             float scale,
                             float alpha) {
        int color = variant.getSaturatedColor();
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        float diameter = FLOWER_DIAMETER_BLOCKS * scale;

        drawLayer(
                poseStack,
                consumer,
                sprite,
                variant.getCropPixels(),
                packedLight,
                diameter * UNDERLAY_SIZE_MULTIPLIER,
                red,
                green,
                blue,
                UNDERLAY_ALPHA * alpha,
                MIN_PROGRESS,
                MIN_PROGRESS
        );
        drawLayer(
                poseStack,
                consumer,
                sprite,
                variant.getCropPixels(),
                packedLight,
                diameter,
                mixWithWhite(red, MAIN_LAYER_WHITE_MIX),
                mixWithWhite(green, MAIN_LAYER_WHITE_MIX),
                mixWithWhite(blue, MAIN_LAYER_WHITE_MIX),
                MAIN_LAYER_ALPHA * alpha,
                MIN_PROGRESS,
                MIN_PROGRESS
        );
        drawLayer(
                poseStack,
                consumer,
                sprite,
                variant.getCropPixels(),
                packedLight,
                diameter * MIDDLE_LAYER_SIZE_MULTIPLIER,
                mixWithWhite(red, MIDDLE_LAYER_WHITE_MIX),
                mixWithWhite(green, MIDDLE_LAYER_WHITE_MIX),
                mixWithWhite(blue, MIDDLE_LAYER_WHITE_MIX),
                MIDDLE_LAYER_ALPHA * alpha,
                MIDDLE_LAYER_ROTATION_DEGREES,
                MIDDLE_LAYER_HEIGHT_BLOCKS
        );
        drawLayer(
                poseStack,
                consumer,
                sprite,
                variant.getCropPixels(),
                packedLight,
                diameter * TOP_LAYER_SIZE_MULTIPLIER,
                mixWithWhite(red, TOP_LAYER_WHITE_MIX),
                mixWithWhite(green, TOP_LAYER_WHITE_MIX),
                mixWithWhite(blue, TOP_LAYER_WHITE_MIX),
                TOP_LAYER_ALPHA * alpha,
                TOP_LAYER_ROTATION_DEGREES,
                TOP_LAYER_HEIGHT_BLOCKS
        );
    }

    private static float mixWithWhite(float channel, float amount) {
        return Mth.lerp(amount, channel, MAX_PROGRESS);
    }

    private static void drawLayer(PoseStack poseStack,
                                  VertexConsumer consumer,
                                  TextureAtlasSprite sprite,
                                  float cropPixels,
                                  int packedLight,
                                  float diameter,
                                  float red,
                                  float green,
                                  float blue,
                                  float alpha,
                                  float rotationDegrees,
                                  double heightOffset) {
        poseStack.pushPose();
        poseStack.translate(0.0D, heightOffset, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationDegrees));

        float half = diameter * 0.5F;
        float minU = sprite.getU(MIN_PROGRESS);
        float maxU = sprite.getU(FULL_TEXTURE_PIXELS);
        float minV = sprite.getV(MIN_PROGRESS);
        float maxV = sprite.getV(Mth.clamp(
                cropPixels,
                MIN_PROGRESS,
                FULL_TEXTURE_PIXELS
        ));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        vertex(consumer, matrix, normal, -half, -half,
                minU, minV, red, green, blue, alpha, packedLight);
        vertex(consumer, matrix, normal, -half, half,
                minU, maxV, red, green, blue, alpha, packedLight);
        vertex(consumer, matrix, normal, half, half,
                maxU, maxV, red, green, blue, alpha, packedLight);
        vertex(consumer, matrix, normal, half, -half,
                maxU, minV, red, green, blue, alpha, packedLight);

        poseStack.popPose();
    }

    private static void vertex(VertexConsumer consumer,
                               Matrix4f matrix,
                               Matrix3f normal,
                               float x,
                               float z,
                               float u,
                               float v,
                               float red,
                               float green,
                               float blue,
                               float alpha,
                               int packedLight) {
        consumer.vertex(matrix, x, 0.0F, z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normal, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    private static final class FlowerMarker {
        private final double startX;
        private final double startY;
        private final double startZ;
        private final double targetX;
        private final double targetY;
        private final double targetZ;
        private final FlowerVariant variant;
        private final int durationTicks;
        private final int fallDurationTicks;
        private final float windX;
        private final float windZ;
        private final float baseRotationDegrees;
        private final float scale;
        private int ageTicks;

        private FlowerMarker(double startX, double startY, double startZ,
                             double targetX, double targetY, double targetZ,
                             FlowerVariant variant, int durationTicks, int ageTicks,
                             int fallDurationTicks, float windX, float windZ,
                             float baseRotationDegrees, float scale) {
            this.startX = startX;
            this.startY = startY;
            this.startZ = startZ;
            this.targetX = targetX;
            this.targetY = targetY;
            this.targetZ = targetZ;
            this.variant = variant;
            this.durationTicks = durationTicks;
            this.ageTicks = ageTicks;
            this.fallDurationTicks = fallDurationTicks;
            this.windX = windX;
            this.windZ = windZ;
            this.baseRotationDegrees = baseRotationDegrees;
            this.scale = scale;
        }
    }

    private record FlowerPose(double x, double y, double z,
                              float rotationDegrees,
                              float tiltXDegrees,
                              float tiltZDegrees,
                              float scale,
                              float alpha) {
    }
}
