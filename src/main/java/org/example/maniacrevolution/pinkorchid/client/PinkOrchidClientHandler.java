package org.example.maniacrevolution.pinkorchid.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.network.packets.PinkOrchidAppearancePacket;
import org.example.maniacrevolution.network.packets.PinkOrchidStatePacket;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Владелец-локальный маркер, HUD-состояние и появление настоящего игрока. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class PinkOrchidClientHandler {
    private static final int RECORD_FINISH_FLASH_TICKS = 8;
    private static final int MARKER_PARTICLE_INTERVAL_TICKS = 4;
    private static final int PLAYER_PARTICLES_PER_TICK = 3;
    private static final double PLAYER_PARTICLE_RADIUS = 0.45D;
    private static final double PLAYER_PARTICLE_HEIGHT = 1.8D;
    private static final float MARKER_SCALE = 0.78F;
    private static final double MARKER_HEIGHT = 0.62D;
    private static final double MARKER_BOB_HEIGHT = 0.055D;
    private static final float MARKER_ROTATION_SPEED_DEGREES = 2.4F;
    private static final float SECOND_PLANE_DEGREES = 90.0F;
    private static final float PLAYER_MINIMUM_SCALE = 0.04F;
    private static final Vector3f PINK = new Vector3f(1.0F, 0.05F, 0.68F);
    private static final Vector3f CYAN = new Vector3f(0.0F, 0.93F, 1.0F);

    private static final Map<UUID, AppearanceState> APPEARANCES = new HashMap<>();
    private static final Set<UUID> PUSHED_PLAYER_RENDERS = new HashSet<>();
    private static Vec3 markerPosition;
    private static boolean recording;
    private static int recordingDurationTicks;
    private static int recordingElapsedTicks;
    private static int finishFlashTicks;
    private static int clientTicks;

    private PinkOrchidClientHandler() {
    }

    public static void handleState(PinkOrchidStatePacket packet) {
        switch (packet.state()) {
            case PinkOrchidStatePacket.CLEAR -> {
                markerPosition = null;
                recording = false;
                recordingDurationTicks = 0;
                recordingElapsedTicks = 0;
                finishFlashTicks = 0;
            }
            case PinkOrchidStatePacket.MARKER ->
                    markerPosition = new Vec3(packet.x(), packet.y(), packet.z());
            case PinkOrchidStatePacket.RECORDING_STARTED -> {
                markerPosition = null;
                recording = true;
                recordingDurationTicks = Math.max(1, packet.durationTicks());
                recordingElapsedTicks = 0;
                finishFlashTicks = 0;
            }
            case PinkOrchidStatePacket.RECORDING_FINISHED -> {
                recording = false;
                finishFlashTicks = RECORD_FINISH_FLASH_TICKS;
            }
            default -> {
            }
        }
    }

    public static void handleAppearance(PinkOrchidAppearancePacket packet) {
        APPEARANCES.put(packet.playerId(), new AppearanceState(
                Math.max(1, packet.durationTicks()), Math.max(1, packet.durationTicks())));
    }

    public static boolean isRecording() {
        return recording;
    }

    public static int getRecordingDurationTicks() {
        return recordingDurationTicks;
    }

    public static int getRecordingElapsedTicks() {
        return recordingElapsedTicks;
    }

    public static int getFinishFlashTicks() {
        return finishFlashTicks;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || minecraft.isPaused()) return;
        clientTicks++;
        if (recording) {
            recordingElapsedTicks = Math.min(recordingDurationTicks,
                    recordingElapsedTicks + 1);
        }
        if (finishFlashTicks > 0) finishFlashTicks--;

        APPEARANCES.replaceAll((id, state) -> state.tick());
        APPEARANCES.values().removeIf(state -> state.remainingTicks() <= 0);
        spawnAppearanceParticles(minecraft);
        spawnMarkerParticles(minecraft);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
                || markerPosition == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;

        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        float time = clientTicks + event.getPartialTick();
        double bob = Math.sin(time * 0.14D) * MARKER_BOB_HEIGHT;
        float rotation = time * MARKER_ROTATION_SPEED_DEGREES;
        ItemStack orchid = new ItemStack(ModItems.PINK_ORCHID.get());

        renderMarkerPlane(minecraft, poseStack, buffers, orchid, camera,
                markerPosition, bob, rotation, MARKER_SCALE, 0);
        renderMarkerPlane(minecraft, poseStack, buffers, orchid, camera,
                markerPosition, bob, rotation + SECOND_PLANE_DEGREES,
                MARKER_SCALE, 1);
        buffers.endBatch();
    }

    private static void renderMarkerPlane(Minecraft minecraft, PoseStack poseStack,
                                          MultiBufferSource buffer, ItemStack orchid,
                                          Vec3 camera, Vec3 position, double bob,
                                          float rotation, float scale, int seed) {
        poseStack.pushPose();
        poseStack.translate(position.x - camera.x,
                position.y - camera.y + MARKER_HEIGHT + bob,
                position.z - camera.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        poseStack.scale(scale, scale, scale);
        minecraft.getItemRenderer().renderStatic(
                orchid,
                ItemDisplayContext.FIXED,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                minecraft.level,
                seed
        );
        poseStack.popPose();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        AppearanceState state = APPEARANCES.get(event.getEntity().getUUID());
        if (state == null) return;
        float progress = 1.0F - Mth.clamp(
                (state.remainingTicks() - event.getPartialTick()) / state.durationTicks(),
                0.0F,
                1.0F
        );
        float scale = Math.max(PLAYER_MINIMUM_SCALE, easeOutBack(progress));
        event.getPoseStack().pushPose();
        event.getPoseStack().scale(scale, scale, scale);
        PUSHED_PLAYER_RENDERS.add(event.getEntity().getUUID());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (PUSHED_PLAYER_RENDERS.remove(event.getEntity().getUUID())) {
            event.getPoseStack().popPose();
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        markerPosition = null;
        recording = false;
        recordingDurationTicks = 0;
        recordingElapsedTicks = 0;
        finishFlashTicks = 0;
        APPEARANCES.clear();
        PUSHED_PLAYER_RENDERS.clear();
    }

    private static void spawnAppearanceParticles(Minecraft minecraft) {
        if (minecraft.level == null) return;
        for (UUID playerId : APPEARANCES.keySet()) {
            net.minecraft.world.entity.player.Player player = minecraft.level.getPlayerByUUID(playerId);
            if (player == null) continue;
            for (int index = 0; index < PLAYER_PARTICLES_PER_TICK; index++) {
                double offsetX = (minecraft.level.random.nextDouble() * 2.0D - 1.0D)
                        * PLAYER_PARTICLE_RADIUS;
                double offsetY = minecraft.level.random.nextDouble() * PLAYER_PARTICLE_HEIGHT;
                double offsetZ = (minecraft.level.random.nextDouble() * 2.0D - 1.0D)
                        * PLAYER_PARTICLE_RADIUS;
                minecraft.level.addParticle(index % 3 == 0
                                ? ParticleTypes.CHERRY_LEAVES
                                : new DustParticleOptions(index % 2 == 0 ? CYAN : PINK, 0.7F),
                        player.getX() + offsetX,
                        player.getY() + offsetY,
                        player.getZ() + offsetZ,
                        offsetX * 0.04D,
                        0.035D,
                        offsetZ * 0.04D);
            }
        }
    }

    private static void spawnMarkerParticles(Minecraft minecraft) {
        if (markerPosition == null || minecraft.level == null
                || clientTicks % MARKER_PARTICLE_INTERVAL_TICKS != 0) return;
        double angle = clientTicks * 0.24D;
        minecraft.level.addParticle(new DustParticleOptions(PINK, 0.7F),
                markerPosition.x + Math.cos(angle) * 0.34D,
                markerPosition.y + 0.12D,
                markerPosition.z + Math.sin(angle) * 0.34D,
                0.0D, 0.025D, 0.0D);
        minecraft.level.addParticle(ParticleTypes.CHERRY_LEAVES,
                markerPosition.x - Math.cos(angle) * 0.25D,
                markerPosition.y + 0.48D,
                markerPosition.z - Math.sin(angle) * 0.25D,
                0.0D, 0.012D, 0.0D);
    }

    private static float easeOutBack(float value) {
        float shifted = Mth.clamp(value, 0.0F, 1.0F) - 1.0F;
        return 1.0F + 2.70158F * shifted * shifted * shifted
                + 1.70158F * shifted * shifted;
    }

    private record AppearanceState(int remainingTicks, int durationTicks) {
        private AppearanceState tick() {
            return new AppearanceState(remainingTicks - 1, durationTicks);
        }
    }
}
