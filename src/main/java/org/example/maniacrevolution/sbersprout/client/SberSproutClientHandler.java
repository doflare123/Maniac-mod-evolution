package org.example.maniacrevolution.sbersprout.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.network.packets.SberSproutStatePacket;
import org.joml.Vector3f;

/** Локальный росток над компьютером и владелец-локальные анимации переноса. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class SberSproutClientHandler {
    private static final int ACTION_ANIMATION_TICKS = 16;
    private static final int DISSOLVE_TICKS = 14;
    private static final int IDLE_PARTICLE_INTERVAL_TICKS = 4;
    private static final int ACTION_PARTICLES_PER_TICK = 4;
    private static final float MINIMUM_MARKER_SCALE = 0.32F;
    private static final float MAXIMUM_MARKER_SCALE = 1.02F;
    private static final float FULL_GROWTH_PERCENT = 25.0F;
    private static final float SECOND_PLANE_ROTATION = 90.0F;
    private static final double MARKER_HEIGHT = 0.92D;
    private static final double BOB_HEIGHT = 0.055D;
    private static final Vector3f GREEN = new Vector3f(0.05F, 1.0F, 0.32F);
    private static final Vector3f CYAN = new Vector3f(0.0F, 0.94F, 1.0F);
    private static final Vector3f GOLD = new Vector3f(1.0F, 0.72F, 0.08F);

    private static BlockPos activePosition;
    private static float currentPercent;
    private static float savedPercent;
    private static ActionAnimation action;
    private static BlockPos dissolvePosition;
    private static int dissolveTicks;
    private static int clientTicks;

    private SberSproutClientHandler() {
    }

    public static void handleState(SberSproutStatePacket packet) {
        switch (packet.state()) {
            case SberSproutStatePacket.CLEAR -> {
                if (activePosition != null) {
                    dissolvePosition = activePosition;
                    dissolveTicks = DISSOLVE_TICKS;
                }
                activePosition = null;
                currentPercent = 0.0F;
                savedPercent = 0.0F;
            }
            case SberSproutStatePacket.UPDATE -> {
                activePosition = packet.position();
                currentPercent = Math.max(0.0F, packet.currentPercent());
                savedPercent = Math.max(0.0F, packet.savedPercent());
            }
            case SberSproutStatePacket.EXTRACTED -> {
                activePosition = packet.position();
                currentPercent = 0.0F;
                savedPercent = Math.max(0.0F, packet.savedPercent());
                action = new ActionAnimation(packet.position(), true,
                        ACTION_ANIMATION_TICKS);
            }
            case SberSproutStatePacket.PLANTED ->
                    action = new ActionAnimation(packet.position(), false,
                            ACTION_ANIMATION_TICKS);
            default -> {
            }
        }
    }

    public static boolean isSessionActive() {
        return activePosition != null;
    }

    public static float getCurrentPercent() {
        return currentPercent;
    }

    public static float getSavedPercent() {
        return savedPercent;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || minecraft.isPaused()) return;
        clientTicks++;
        if (action != null) {
            spawnActionParticles(minecraft, action);
            action = action.tick();
            if (action.remainingTicks() <= 0) action = null;
        }
        if (dissolveTicks > 0) {
            spawnDissolveParticles(minecraft);
            dissolveTicks--;
            if (dissolveTicks <= 0) dissolvePosition = null;
        }
        spawnIdleParticles(minecraft);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
                || activePosition == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;

        float time = clientTicks + event.getPartialTick();
        float growth = Mth.clamp(currentPercent / FULL_GROWTH_PERCENT, 0.0F, 1.0F);
        float scale = Mth.lerp(growth, MINIMUM_MARKER_SCALE, MAXIMUM_MARKER_SCALE);
        double bob = Math.sin(time * 0.15D) * BOB_HEIGHT;
        float rotation = time * 2.15F;
        Vec3 position = Vec3.atCenterOf(activePosition);
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        ItemStack sprout = new ItemStack(ModItems.SBER_SPROUT.get());

        renderPlane(minecraft, poseStack, buffers, sprout, position, camera,
                bob, rotation, scale, 0);
        renderPlane(minecraft, poseStack, buffers, sprout, position, camera,
                bob, rotation + SECOND_PLANE_ROTATION, scale, 1);
        buffers.endBatch();
    }

    private static void renderPlane(Minecraft minecraft, PoseStack poseStack,
                                    MultiBufferSource buffer, ItemStack sprout,
                                    Vec3 position, Vec3 camera, double bob,
                                    float rotation, float scale, int seed) {
        poseStack.pushPose();
        poseStack.translate(position.x - camera.x,
                position.y - camera.y + MARKER_HEIGHT + bob,
                position.z - camera.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        poseStack.scale(scale, scale, scale);
        minecraft.getItemRenderer().renderStatic(sprout, ItemDisplayContext.FIXED,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                poseStack, buffer, minecraft.level, seed);
        poseStack.popPose();
    }

    private static void spawnIdleParticles(Minecraft minecraft) {
        if (activePosition == null || minecraft.level == null
                || clientTicks % IDLE_PARTICLE_INTERVAL_TICKS != 0) return;
        Vec3 center = Vec3.atCenterOf(activePosition).add(0.0D, 0.7D, 0.0D);
        double angle = clientTicks * 0.22D;
        minecraft.level.addParticle(new DustParticleOptions(GREEN, 0.68F),
                center.x + Math.cos(angle) * 0.28D, center.y,
                center.z + Math.sin(angle) * 0.28D, 0.0D, 0.025D, 0.0D);
        minecraft.level.addParticle(new DustParticleOptions(
                        clientTicks % 8 == 0 ? GOLD : CYAN, 0.58F),
                center.x - Math.cos(angle) * 0.22D, center.y + 0.3D,
                center.z - Math.sin(angle) * 0.22D, 0.0D, 0.02D, 0.0D);
    }

    private static void spawnActionParticles(Minecraft minecraft,
                                             ActionAnimation animation) {
        if (minecraft.level == null || minecraft.player == null) return;
        float progress = 1.0F - animation.remainingTicks()
                / (float) ACTION_ANIMATION_TICKS;
        Vec3 computer = Vec3.atCenterOf(animation.position()).add(0.0D, 0.75D, 0.0D);
        Vec3 player = minecraft.player.position().add(0.0D, 1.0D, 0.0D);
        Vec3 start = animation.extracting() ? computer : player;
        Vec3 end = animation.extracting() ? player : computer;
        for (int index = 0; index < ACTION_PARTICLES_PER_TICK; index++) {
            float offset = Mth.clamp(progress + index * 0.035F, 0.0F, 1.0F);
            double arc = Math.sin(Math.PI * offset) * 0.6D;
            Vec3 point = start.lerp(end, offset).add(0.0D, arc, 0.0D);
            minecraft.level.addParticle(new DustParticleOptions(
                            index % 3 == 0 ? GOLD : index % 2 == 0 ? CYAN : GREEN,
                            0.85F), point.x, point.y, point.z,
                    0.0D, 0.015D, 0.0D);
        }
    }

    private static void spawnDissolveParticles(Minecraft minecraft) {
        if (minecraft.level == null || dissolvePosition == null) return;
        Vec3 center = Vec3.atCenterOf(dissolvePosition).add(0.0D, 0.7D, 0.0D);
        for (int index = 0; index < 3; index++) {
            double angle = (clientTicks + index * 5) * 0.42D;
            minecraft.level.addParticle(index == 0 ? ParticleTypes.COMPOSTER
                            : new DustParticleOptions(index == 1 ? CYAN : GREEN, 0.65F),
                    center.x + Math.cos(angle) * 0.25D,
                    center.y + (DISSOLVE_TICKS - dissolveTicks) * 0.035D,
                    center.z + Math.sin(angle) * 0.25D,
                    Math.cos(angle) * 0.025D, 0.045D, Math.sin(angle) * 0.025D);
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        activePosition = null;
        currentPercent = 0.0F;
        savedPercent = 0.0F;
        action = null;
        dissolvePosition = null;
        dissolveTicks = 0;
    }

    private record ActionAnimation(BlockPos position, boolean extracting,
                                   int remainingTicks) {
        private ActionAnimation tick() {
            return new ActionAnimation(position, extracting, remainingTicks - 1);
        }
    }
}
