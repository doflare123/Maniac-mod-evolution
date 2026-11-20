package org.example.maniacrevolution.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.entity.HookEntity;
import org.example.maniacrevolution.entity.HookDeployment;
import org.example.maniacrevolution.entity.HookTrajectory;
import org.example.maniacrevolution.item.HookItem;
import org.example.maniacrevolution.mana.ClientManaData;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class HookAimClient {
    private static final HookAimState AIM = new HookAimState();

    private static boolean canAim(Minecraft mc) {
        return mc.player != null && mc.level != null && mc.gameMode != null && mc.screen == null
                && mc.isWindowActive() && mc.player.isAlive() && !mc.player.isSpectator()
                && !mc.player.isUsingItem() && !HookDeployment.hasActiveHook(mc.player)
                && mc.player.getMainHandItem().getItem() instanceof HookItem;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void hookAimInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        var mc = Minecraft.getInstance();
        if (event.isAttack() && mc.player != null && HookDeployment.hasActiveHook(mc.player)) {
            AIM.cancelWithButton();
            event.setCanceled(true);
            event.setSwingHand(false);
            return;
        }
        // LMB cancels aiming; outside aiming it remains the weapon's normal melee attack.
        if (event.isAttack() && (AIM.isAiming() || AIM.blocksCancel())) {
            AIM.cancelWithButton();
            event.setCanceled(true);
            event.setSwingHand(false);
            return;
        }
        if (!event.isUseItem() || !canAim(mc)) return;
        AIM.begin(mc.player.getInventory().selected, mc.player.getMainHandItem());
        event.setCanceled(true);
        event.setSwingHand(false);
    }

    @SubscribeEvent
    public static void hookAimTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        if (!canAim(mc)) {
            AIM.cancel();
        } else {
            AIM.validate(mc.player.getInventory().selected, mc.player.getMainHandItem());
        }
        if (!mc.options.keyUse.isDown()) {
            if (AIM.release() && canAim(mc)) {
                // Vanilla use packet preserves server cooldown, mana and Forge interaction checks.
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            }
        }
        AIM.updateCancelKey(mc.options.keyAttack.isDown());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void hookAimScroll(InputEvent.MouseScrollingEvent event) {
        if (event.getScrollDelta() != 0) AIM.cancel();
    }

    @SubscribeEvent
    public static void hookAimHotbarKey(InputEvent.Key event) {
        if (!AIM.isAiming() || event.getAction() != GLFW.GLFW_PRESS) return;
        var mc = Minecraft.getInstance();
        for (int i = 0; i < mc.options.keyHotbarSlots.length; i++) {
            if (mc.options.keyHotbarSlots[i].matches(event.getKey(), event.getScanCode())
                    && mc.player != null && i != mc.player.getInventory().selected) AIM.cancel();
        }
        if (mc.options.keySwapOffhand.matches(event.getKey(), event.getScanCode())
                || mc.options.keyDrop.matches(event.getKey(), event.getScanCode())) AIM.cancel();
    }

    @SubscribeEvent
    public static void hookAimDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        AIM.reset();
    }

    @SubscribeEvent
    public static void hookAimWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || !AIM.isAiming()) return;
        var mc = Minecraft.getInstance();
        if (!canAim(mc)) return;
        AIM.validate(mc.player.getInventory().selected, mc.player.getMainHandItem());
        if (!AIM.isAiming()) return;
        float partial = event.getPartialTick();
        Vec3 look = HookTrajectory.aimDirection(mc.player, partial, InteractionHand.MAIN_HAND);
        Vec3 launch = HookTrajectory.launchPosition(mc.player, partial);
        HitResult hit = HookTrajectory.trace(mc.player, mc.player, launch, launch.add(look.scale(HookEntity.MAX_DISTANCE)));
        Vec3 start = launch;
        var item = (HookItem) mc.player.getMainHandItem().getItem();
        boolean ready = !item.isOnCooldown(mc.player) && ClientManaData.getMana() >= item.getManaCost();
        int red = ready ? (hit.getType() == HitResult.Type.ENTITY ? 110 : 105) : 205;
        int green = ready ? (hit.getType() == HitResult.Type.ENTITY ? 225 : 195) : 115;
        int blue = ready ? 200 : 110;
        var poses = event.getPoseStack();
        poses.pushPose();
        Vec3 camera = event.getCamera().getPosition();
        poses.translate(-camera.x, -camera.y, -camera.z);
        var buffers = mc.renderBuffers().bufferSource();
        var vertices = buffers.getBuffer(RenderType.lines());
        drawGuide(poses, vertices, start, hit.getLocation(),
                new Vec3(new Vector3f(1, 0, 0).rotate(event.getCamera().rotation())),
                new Vec3(new Vector3f(0, 1, 0).rotate(event.getCamera().rotation())), red, green, blue);
        // Stay just outside a struck wall/ceiling, so the downward ray does not start inside it.
        Vec3 projectionTop = hit.getType() == HitResult.Type.BLOCK
                ? hit.getLocation().subtract(look.scale(0.02)) : hit.getLocation();
        HitResult ground = HookTrajectory.groundProjection(mc.player, projectionTop);
        drawGroundProjection(poses, vertices, projectionTop, ground.getLocation(),
                ground.getType() == HitResult.Type.BLOCK, red, green, blue);
        buffers.endBatch(RenderType.lines());
        poses.popPose();
    }

    /** A faint vertical plumb line and a small floor cross give the endpoint a depth cue. */
    public static void drawGroundProjection(PoseStack poses, VertexConsumer vertices, Vec3 top, Vec3 bottom,
                                             boolean grounded, int red, int green, int blue) {
        if (top.y - bottom.y < 0.03) return;
        Vec3 foot = new Vec3(top.x, bottom.y + (grounded ? 0.015 : 0), top.z);
        line(poses, vertices, top, foot, red, green, blue, 85);
        if (grounded) {
            line(poses, vertices, foot.add(-0.12, 0, 0), foot.add(0.12, 0, 0), red, green, blue, 150);
            line(poses, vertices, foot.add(0, 0, -0.12), foot.add(0, 0, 0.12), red, green, blue, 150);
        }
    }

    /** Thin, depth-tested dashes and a hollow impact diamond; no particles or through-wall overlay. */
    public static void drawGuide(PoseStack poses, VertexConsumer vertices, Vec3 start, Vec3 end,
                                 Vec3 right, Vec3 up, int red, int green, int blue) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < 1.0E-5) return;
        Vec3 direction = delta.normalize();
        for (double d = 0.15; d < length; d += 0.65) {
            line(poses, vertices, start.add(direction.scale(d)),
                    start.add(direction.scale(Math.min(length, d + 0.32))), red, green, blue, 135);
        }
        Vec3 marker = end.subtract(direction.scale(0.015));
        Vec3[] corners = {marker.add(up.scale(0.08)), marker.add(right.scale(0.08)),
                marker.subtract(up.scale(0.08)), marker.subtract(right.scale(0.08))};
        for (int i = 0; i < 4; i++) line(poses, vertices, corners[i], corners[(i + 1) % 4], red, green, blue, 180);
    }

    private static void line(PoseStack poses, VertexConsumer vertices, Vec3 a, Vec3 b,
                              int red, int green, int blue, int alpha) {
        Vec3 normal = b.subtract(a).normalize();
        for (Vec3 point : new Vec3[]{a, b}) {
            vertices.vertex(poses.last().pose(), (float) point.x, (float) point.y, (float) point.z)
                    .color(red, green, blue, alpha)
                    .normal(poses.last().normal(), (float) normal.x, (float) normal.y, (float) normal.z).endVertex();
        }
    }
}
