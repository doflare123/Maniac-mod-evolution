package org.example.maniacrevolution.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.flower.FlowerVariant;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** Клиентские данные букета, полёт собранного цветка и вспышка при нокдауне. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class BouquetClientHandler {
    private static final int FLIGHT_DURATION_TICKS = 14;
    private static final int BURST_DURATION_TICKS = 16;
    private static final int TRAIL_PARTICLE_INTERVAL_TICKS = 2;
    private static final int BURST_PARTICLE_DURATION_TICKS = 8;

    private static final double FLIGHT_ARC_HEIGHT_BLOCKS = 0.75D;
    private static final double FLIGHT_SWAY_BLOCKS = 0.16D;
    private static final double BURST_MIN_RADIUS_BLOCKS = 0.22D;
    private static final double BURST_TRAVEL_BLOCKS = 0.70D;
    private static final double BURST_LIFT_BLOCKS = 0.58D;
    private static final double PARTICLE_SPREAD_BLOCKS = 0.22D;

    private static final float FLIGHT_START_SCALE = 1.0F;
    private static final float FLIGHT_END_SCALE = 0.55F;
    private static final float BURST_FLOWER_SCALE = 0.82F;
    private static final float TRAIL_PARTICLE_SCALE = 0.75F;
    private static final float FULL_ALPHA = 1.0F;
    private static final float FULL_ROTATION_DEGREES = 360.0F;
    private static final float FLIGHT_ROTATION_DEGREES = 720.0F;
    private static final float FLIGHT_TILT_DEGREES = 28.0F;
    private static final float BURST_TILT_DEGREES = 18.0F;
    private static final float BURST_FADE_START = 0.55F;
    private static final float FLIGHT_FADE_START = 0.82F;
    private static final float BURST_POP_END = 0.32F;
    private static final double DUST_HORIZONTAL_SPEED = 0.10D;
    private static final double DUST_VERTICAL_SPEED = 0.035D;
    private static final double LEAF_HORIZONTAL_SPEED = 0.08D;
    private static final double LEAF_VERTICAL_SPEED = 0.025D;
    private static final double SMOKE_VERTICAL_SPEED = 0.015D;

    private static final List<FlyingFlower> ACTIVE_FLIGHTS = new ArrayList<>();
    private static final List<FlowerBurst> ACTIVE_BURSTS = new ArrayList<>();
    private static final RandomSource PARTICLE_RANDOM = RandomSource.create();
    private static List<FlowerVariant> hudFlowers = List.of();

    private BouquetClientHandler() {
    }

    public static void setHudFlowers(List<FlowerVariant> variants) {
        hudFlowers = List.copyOf(variants);
    }

    public static List<FlowerVariant> getHudFlowers() {
        return hudFlowers;
    }

    public static void addFlight(FlowerVariant variant,
                                 double startX, double startY, double startZ,
                                 double targetX, double targetY, double targetZ) {
        ACTIVE_FLIGHTS.add(new FlyingFlower(
                variant,
                new Vec3(startX, startY, startZ),
                new Vec3(targetX, targetY, targetZ)
        ));
    }

    public static void addBurst(List<FlowerVariant> variants,
                                double x, double y, double z) {
        if (!variants.isEmpty()) {
            ACTIVE_BURSTS.add(new FlowerBurst(List.copyOf(variants), new Vec3(x, y, z)));
        }
    }

    public static void clear() {
        hudFlowers = List.of();
        ACTIVE_FLIGHTS.clear();
        ACTIVE_BURSTS.clear();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || minecraft.isPaused()) {
            return;
        }

        for (FlyingFlower flight : ACTIVE_FLIGHTS) {
            flight.ageTicks++;
            if (flight.ageTicks % TRAIL_PARTICLE_INTERVAL_TICKS == 0) {
                spawnFlightParticle(minecraft, flight);
            }
        }
        ACTIVE_FLIGHTS.removeIf(flight -> flight.ageTicks >= FLIGHT_DURATION_TICKS);

        for (FlowerBurst burst : ACTIVE_BURSTS) {
            burst.ageTicks++;
            if (burst.ageTicks <= BURST_PARTICLE_DURATION_TICKS) {
                spawnBurstParticles(minecraft, burst);
            }
        }
        ACTIVE_BURSTS.removeIf(burst -> burst.ageTicks >= BURST_DURATION_TICKS);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
                || (ACTIVE_FLIGHTS.isEmpty() && ACTIVE_BURSTS.isEmpty())) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        Vec3 camera = event.getCamera().getPosition();
        float partialTick = event.getPartialTick();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        RenderType renderType = RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS);
        VertexConsumer consumer = buffers.getBuffer(renderType);

        for (FlyingFlower flight : ACTIVE_FLIGHTS) {
            float age = flight.ageTicks + partialTick;
            float progress = Mth.clamp(age / FLIGHT_DURATION_TICKS, 0.0F, 1.0F);
            Vec3 position = calculateFlightPosition(flight, progress);
            float scale = Mth.lerp(progress, FLIGHT_START_SCALE, FLIGHT_END_SCALE);
            float alpha = 1.0F - Mth.clamp(
                    (progress - FLIGHT_FADE_START) / (1.0F - FLIGHT_FADE_START),
                    0.0F,
                    1.0F
            );
            renderFlower(
                    minecraft,
                    poseStack,
                    consumer,
                    camera,
                    flight.variant,
                    position,
                    scale,
                    alpha,
                    FLIGHT_ROTATION_DEGREES * progress,
                    FLIGHT_TILT_DEGREES
            );
        }

        for (FlowerBurst burst : ACTIVE_BURSTS) {
            renderBurst(minecraft, poseStack, consumer, camera, burst, partialTick);
        }

        buffers.endBatch(renderType);
    }

    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    private static Vec3 calculateFlightPosition(FlyingFlower flight, float progress) {
        float eased = 1.0F - (float) Math.pow(1.0F - progress, 3.0D);
        Vec3 delta = flight.target.subtract(flight.start);
        double horizontalLength = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        double perpendicularX = horizontalLength <= 0.0001D ? 0.0D : -delta.z / horizontalLength;
        double perpendicularZ = horizontalLength <= 0.0001D ? 0.0D : delta.x / horizontalLength;
        double sway = Math.sin(progress * Math.PI * 2.0D) * FLIGHT_SWAY_BLOCKS;
        double arc = Math.sin(progress * Math.PI) * FLIGHT_ARC_HEIGHT_BLOCKS;
        return new Vec3(
                Mth.lerp(eased, flight.start.x, flight.target.x) + perpendicularX * sway,
                Mth.lerp(eased, flight.start.y, flight.target.y) + arc,
                Mth.lerp(eased, flight.start.z, flight.target.z) + perpendicularZ * sway
        );
    }

    private static void renderBurst(Minecraft minecraft, PoseStack poseStack,
                                    VertexConsumer consumer, Vec3 camera,
                                    FlowerBurst burst, float partialTick) {
        float progress = Mth.clamp(
                (burst.ageTicks + partialTick) / BURST_DURATION_TICKS,
                0.0F,
                1.0F
        );
        float pop = easeOutBack(Mth.clamp(progress / BURST_POP_END, 0.0F, 1.0F));
        float alpha = 1.0F - Mth.clamp(
                (progress - BURST_FADE_START) / (1.0F - BURST_FADE_START),
                0.0F,
                1.0F
        );

        int count = burst.variants.size();
        for (int index = 0; index < count; index++) {
            double angle = Math.PI * 2.0D * index / count - Math.PI / 2.0D;
            double radius = BURST_MIN_RADIUS_BLOCKS + BURST_TRAVEL_BLOCKS * progress;
            Vec3 position = new Vec3(
                    burst.center.x + Math.cos(angle) * radius,
                    burst.center.y + Math.sin(Math.PI * progress) * BURST_LIFT_BLOCKS
                            + index * 0.08D,
                    burst.center.z + Math.sin(angle) * radius
            );
            renderFlower(
                    minecraft,
                    poseStack,
                    consumer,
                    camera,
                    burst.variants.get(index),
                    position,
                    BURST_FLOWER_SCALE * pop,
                    alpha,
                    (float) Math.toDegrees(angle) + FULL_ROTATION_DEGREES * progress,
                    BURST_TILT_DEGREES
            );
        }
    }

    private static void renderFlower(Minecraft minecraft, PoseStack poseStack,
                                     VertexConsumer consumer, Vec3 camera,
                                     FlowerVariant variant, Vec3 position,
                                     float scale, float alpha,
                                     float rotationDegrees, float tiltDegrees) {
        if (alpha <= 0.0F || scale <= 0.0F) {
            return;
        }
        TextureAtlasSprite sprite = FlowerTrailClientHandler.resolveSprite(
                minecraft,
                variant
        );
        int packedLight = LevelRenderer.getLightColor(
                minecraft.level,
                BlockPos.containing(position)
        );

        poseStack.pushPose();
        poseStack.translate(
                position.x - camera.x,
                position.y - camera.y,
                position.z - camera.z
        );
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationDegrees));
        poseStack.mulPose(Axis.XP.rotationDegrees(tiltDegrees));
        FlowerTrailClientHandler.renderFlower(
                poseStack,
                consumer,
                sprite,
                variant,
                packedLight,
                scale,
                alpha
        );
        poseStack.popPose();
    }

    private static void spawnFlightParticle(Minecraft minecraft, FlyingFlower flight) {
        if (minecraft.level == null) {
            return;
        }
        float progress = Mth.clamp(
                flight.ageTicks / (float) FLIGHT_DURATION_TICKS,
                0.0F,
                1.0F
        );
        Vec3 position = calculateFlightPosition(flight, progress);
        minecraft.level.addParticle(
                createDust(flight.variant),
                position.x,
                position.y,
                position.z,
                0.0D,
                0.0D,
                0.0D
        );
    }

    private static void spawnBurstParticles(Minecraft minecraft, FlowerBurst burst) {
        if (minecraft.level == null || burst.variants.isEmpty()) {
            return;
        }
        FlowerVariant variant = burst.variants.get(
                PARTICLE_RANDOM.nextInt(burst.variants.size())
        );
        double offsetX = randomOffset(PARTICLE_SPREAD_BLOCKS);
        double offsetY = PARTICLE_RANDOM.nextDouble() * PARTICLE_SPREAD_BLOCKS;
        double offsetZ = randomOffset(PARTICLE_SPREAD_BLOCKS);
        minecraft.level.addParticle(
                createDust(variant),
                burst.center.x + offsetX,
                burst.center.y + offsetY,
                burst.center.z + offsetZ,
                offsetX * DUST_HORIZONTAL_SPEED,
                DUST_VERTICAL_SPEED,
                offsetZ * DUST_HORIZONTAL_SPEED
        );
        minecraft.level.addParticle(
                ParticleTypes.CHERRY_LEAVES,
                burst.center.x - offsetX,
                burst.center.y + offsetY,
                burst.center.z - offsetZ,
                -offsetX * LEAF_HORIZONTAL_SPEED,
                LEAF_VERTICAL_SPEED,
                -offsetZ * LEAF_HORIZONTAL_SPEED
        );
        if (burst.ageTicks % TRAIL_PARTICLE_INTERVAL_TICKS == 0) {
            minecraft.level.addParticle(
                    ParticleTypes.SMOKE,
                    burst.center.x,
                    burst.center.y + offsetY,
                    burst.center.z,
                    0.0D,
                    SMOKE_VERTICAL_SPEED,
                    0.0D
            );
        }
    }

    private static DustParticleOptions createDust(FlowerVariant variant) {
        int color = variant.getSaturatedColor();
        Vector3f vector = new Vector3f(
                ((color >> 16) & 0xFF) / 255.0F,
                ((color >> 8) & 0xFF) / 255.0F,
                (color & 0xFF) / 255.0F
        );
        return new DustParticleOptions(vector, TRAIL_PARTICLE_SCALE);
    }

    private static double randomOffset(double spread) {
        return (PARTICLE_RANDOM.nextDouble() * 2.0D - 1.0D) * spread;
    }

    private static float easeOutBack(float value) {
        float shifted = value - 1.0F;
        return 1.0F + 2.70158F * shifted * shifted * shifted
                + 1.70158F * shifted * shifted;
    }

    private static final class FlyingFlower {
        private final FlowerVariant variant;
        private final Vec3 start;
        private final Vec3 target;
        private int ageTicks;

        private FlyingFlower(FlowerVariant variant, Vec3 start, Vec3 target) {
            this.variant = variant;
            this.start = start;
            this.target = target;
        }
    }

    private static final class FlowerBurst {
        private final List<FlowerVariant> variants;
        private final Vec3 center;
        private int ageTicks;

        private FlowerBurst(List<FlowerVariant> variants, Vec3 center) {
            this.variants = variants;
            this.center = center;
        }
    }
}
