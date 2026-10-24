package org.example.maniacrevolution.forester;

import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.*;
import java.util.*;

/** Editable player-rig tracks drive vanilla skin arms; gameplay remains on the server. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class ForesterWorkAnimation {
    private record Motion(ForesterWorkPacket packet, long received) {}
    private static final Map<Integer, Motion> MOTIONS = new HashMap<>();
    private static ClientLevel world;
    private static JsonObject clips;
    private static final HumanoidModel.ArmPose WORK_POSE = HumanoidModel.ArmPose.create("MANIACREV_FORESTER_WORK", false, (model, entity, arm) -> {
        Motion motion = motion(entity.getId());
        if (motion == null) return;
        float[] angles = rotation(motion, arm == HumanoidArm.RIGHT ? "right_arm" : "left_arm", Minecraft.getInstance().getFrameTime());
        var part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        part.xRot = (float) Math.toRadians(angles[0]); part.yRot = (float) Math.toRadians(angles[1]); part.zRot = (float) Math.toRadians(angles[2]);
    });
    private ForesterWorkAnimation() {}
    @Mod.EventBusSubscriber(modid = Maniacrev.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Reload {
        @SubscribeEvent public static void register(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) manager -> clips = null);
        }
    }
    public static void clear() { MOTIONS.clear(); world = null; }
    public static void receive(ForesterWorkPacket packet) {
        var mc = Minecraft.getInstance();
        if (mc.level != world) clear();
        world = mc.level;
        if (world == null) return;
        if (packet.kind() < 0) MOTIONS.remove(packet.player());
        else MOTIONS.put(packet.player(), new Motion(packet, world.getGameTime()));
    }
    private static Motion motion(int player) {
        var mc = Minecraft.getInstance();
        if (world == null || world != mc.level) return null;
        Motion motion = MOTIONS.get(player);
        return motion != null && world.getGameTime() - motion.received < 12 ? motion : null;
    }
    private static float progress(Motion motion, float partial) {
        return net.minecraft.util.Mth.clamp((motion.packet.elapsed() + world.getGameTime() - motion.received + partial) / motion.packet.duration(), 0, 1);
    }
    private static float[] rotation(Motion motion, String bone, float partial) {
        if (clips == null) {
            try (var reader = Minecraft.getInstance().getResourceManager().openAsReader(Maniacrev.loc("animations/forester_work.animation.json"))) {
                clips = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("animations");
            } catch (Exception exception) { throw new IllegalStateException("Cannot load forester work animation", exception); }
        }
        var keys = clips.getAsJsonObject(motion.packet.kind() == ForesterManager.COLLECT ? "collect" : "install")
                .getAsJsonObject("bones").getAsJsonObject(bone).getAsJsonObject("rotation");
        float time = progress(motion, partial), before = -1, after = 2;
        JsonArray a = null, b = null;
        for (var frame : keys.entrySet()) {
            float t = Float.parseFloat(frame.getKey());
            if (t <= time && t > before) { before = t; a = frame.getValue().getAsJsonArray(); }
            if (t >= time && t < after) { after = t; b = frame.getValue().getAsJsonArray(); }
        }
        if (a == null) a = b; if (b == null) b = a;
        float factor = after > before && after < 2 && before >= 0 ? (time - before) / (after - before) : 0;
        float[] result = new float[3];
        for (int i = 0; i < 3; i++) result[i] = a.get(i).getAsFloat() + (b.get(i).getAsFloat() - a.get(i).getAsFloat()) * factor;
        return result;
    }
    public static Float jawAngle(BlockPos pos, float partial) {
        if (world == null || world != Minecraft.getInstance().level) return null;
        for (int id : MOTIONS.keySet()) {
            Motion motion = motion(id);
            if (motion != null && motion.packet.pos().equals(pos) && motion.packet.kind() != ForesterManager.PLACE) {
                float p = progress(motion, partial);
                return motion.packet.kind() == ForesterManager.COLLECT ? 45 + 33 * (float) Math.sin(p * Math.PI / 2) : 45 * (1 - p);
            }
        }
        return null;
    }
    @SubscribeEvent public static void player(RenderPlayerEvent.Pre event) {
        if (motion(event.getEntity().getId()) == null) return;
        event.getRenderer().getModel().rightArmPose = WORK_POSE;
        event.getRenderer().getModel().leftArmPose = WORK_POSE;
    }
    @SubscribeEvent public static void hand(RenderHandEvent event) {
        var mc = Minecraft.getInstance(); if (mc.player == null) return;
        Motion motion = motion(mc.player.getId()); if (motion == null) return;
        event.setCanceled(true);
        if (!(mc.getEntityRenderDispatcher().getRenderer(mc.player) instanceof PlayerRenderer renderer)) return;
        boolean right = (event.getHand() == InteractionHand.MAIN_HAND) == (mc.player.getMainArm() == HumanoidArm.RIGHT);
        float side = right ? 1 : -1, p = progress(motion, event.getPartialTick());
        float reach = (float) Math.sin(Math.PI * p);
        float[] angles = rotation(motion, right ? "right_arm" : "left_arm", event.getPartialTick());
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(side * (.64 - .17 * reach), -.6 - .13 * reach, -.72 - .17 * reach);
        pose.mulPose(Axis.YP.rotationDegrees(side * 45));
        pose.mulPose(Axis.XP.rotationDegrees((angles[0] + 15) * .2F));
        pose.mulPose(Axis.ZP.rotationDegrees(angles[2] * .35F));
        // Vanilla first-person skin-arm orientation, followed by the work pose.
        pose.translate(side * -1, 3.6, 3.5); pose.mulPose(Axis.ZP.rotationDegrees(side * 120));
        pose.mulPose(Axis.XP.rotationDegrees(200)); pose.mulPose(Axis.YP.rotationDegrees(side * -135)); pose.translate(side * 5.6, 0, 0);
        if (right) renderer.renderRightHand(pose, event.getMultiBufferSource(), event.getPackedLight(), mc.player);
        else renderer.renderLeftHand(pose, event.getMultiBufferSource(), event.getPackedLight(), mc.player);
        pose.popPose();
        if (event.getHand() == InteractionHand.MAIN_HAND && motion.packet.kind() == ForesterManager.PLACE) {
            pose.pushPose(); pose.translate(.05, -.3 - .3 * p, -.9 - .2 * p);
            pose.mulPose(Axis.XP.rotationDegrees(25)); pose.scale(.7F, .7F, .7F);
            mc.getItemRenderer().renderStatic(new ItemStack(ModItems.FORESTER_TRAP.get()), ItemDisplayContext.NONE,
                    event.getPackedLight(), OverlayTexture.NO_OVERLAY, pose, event.getMultiBufferSource(), mc.level, 0);
            pose.popPose();
        }
    }
    @SubscribeEvent public static void world(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || world == null || world != Minecraft.getInstance().level) return;
        var mc = Minecraft.getInstance(); var pose = event.getPoseStack(); var camera = event.getCamera().getPosition();
        for (int id : MOTIONS.keySet()) {
            Motion motion = motion(id); if (motion == null || motion.packet.kind() != ForesterManager.PLACE) continue;
            float p = progress(motion, event.getPartialTick()); BlockPos pos = motion.packet.pos();
            pose.pushPose(); pose.translate(pos.getX() + .5 - camera.x, pos.getY() + .28 * (1 - p) - camera.y, pos.getZ() + .5 - camera.z);
            mc.getItemRenderer().renderStatic(new ItemStack(ModItems.FORESTER_TRAP.get()), ItemDisplayContext.NONE, LevelRenderer.getLightColor(world, pos),
                    OverlayTexture.NO_OVERLAY, pose, mc.renderBuffers().bufferSource(), world, 0);
            pose.popPose();
        }
    }
}
