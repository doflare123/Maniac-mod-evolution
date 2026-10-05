package org.example.maniacrevolution.warden.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.warden.WardenAnimationTimeline;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

/** Player-sized Warden hands, with native two-arm strike keyframes on the first-person rig. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class WardenFirstPersonArms {
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/entity/warden/warden.png");
    private static ModelPart leftArm, rightArm;
    private static WardenPlayerModel attackModel;
    private record Frame(Matrix4f pose, Matrix3f normal, Matrix4f projection, Matrix4f view, VertexSorting sorting, float partial, float equip, float swing) {}
    private static Frame captured;
    private WardenFirstPersonArms() {}
    static void setModel(ModelPart root) {
        attackModel = new WardenPlayerModel(root);
        var body = root.getChild("bone").getChild("body");
        leftArm = body.getChild("left_arm"); rightArm = body.getChild("right_arm");
        leftArm.setPos(0, 0, 0); rightArm.setPos(0, 0, 0);
        captured = null;
    }
    private static boolean eligible() {
        var mc = Minecraft.getInstance();
        return leftArm != null && rightArm != null && WardenCombatClient.eligible() && !mc.options.hideGui && mc.options.getCameraType().isFirstPerson()
                && !mc.player.isInvisible() && mc.player.getMainHandItem().isEmpty() && mc.player.getOffhandItem().isEmpty();
    }
    @SubscribeEvent public static void world(RenderLevelStageEvent e) {
        if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) captured = null;
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e) { captured = null; }
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void hand(RenderHandEvent e) {
        if (!eligible()) return;
        e.setCanceled(true);
        if (e.getHand() != InteractionHand.MAIN_HAND) return;
        if (WardenVisionPrototype.renderingVision()) {
            captured = new Frame(new Matrix4f(e.getPoseStack().last().pose()), new Matrix3f(e.getPoseStack().last().normal()),
                    new Matrix4f(RenderSystem.getProjectionMatrix()), new Matrix4f(RenderSystem.getModelViewMatrix()),
                    RenderSystem.getVertexSorting(), e.getPartialTick(), e.getEquipProgress(), e.getSwingProgress());
        } else draw(e.getPoseStack(), e.getMultiBufferSource(), e.getEquipProgress(), e.getSwingProgress(), e.getPartialTick());
    }
    private static void draw(PoseStack pose, MultiBufferSource buffers, float equip, float swing, float partial) {
        var player = Minecraft.getInstance().player;
        var frame = WardenAnimationsClient.frame(player, partial);
        prepareAttack(attackModel, frame);
        boolean attacking = frame.action() == WardenAnimationTimeline.Action.ATTACK;
        var type = RenderType.entityCutoutNoCull(TEXTURE);
        for (var side : HumanoidArm.values()) {
            pose.pushPose();
            try {
                // Avoid adding the player's single-hand swing to the Warden's two-arm strike.
                positionArm(pose, side, equip, !attacking && side == player.getMainArm() ? swing : 0);
                if (!attacking) swimmingArm(pose, side, player.getSwimAmount(partial), player.tickCount + partial);
                renderArm(side == HumanoidArm.RIGHT ? rightArm : leftArm, side,
                        pose, buffers.getBuffer(type), LightTexture.FULL_BRIGHT);
            } finally { pose.popPose(); }
        }
        if (buffers instanceof MultiBufferSource.BufferSource source) source.endBatch(type);
    }
    static void prepareAttack(WardenPlayerModel model, WardenAnimationTimeline.Frame frame) {
        model.root().getAllParts().forEach(ModelPart::resetPose);
        if (frame.action() == WardenAnimationTimeline.Action.ATTACK)
            model.applyAbilities(frame, 0.45F);
        var body = model.root().getChild("bone").getChild("body");
        for (var name : new String[]{"left_arm", "right_arm"}) {
            var arm = body.getChild(name);
            var shoulder = arm.getInitialPose();
            // The ordinary first-person rig supplies the shoulder; retain only the animated offset.
            arm.setPos(arm.x - shoulder.x, arm.y - shoulder.y, arm.z - shoulder.z);
        }
    }
    static void renderArm(ModelPart arm, HumanoidArm side, PoseStack pose, VertexConsumer buffer, int light) {
        // Preserve all six vanilla Warden UV faces, resized from 8x28x8 to the player's 4x12x4.
        // Player arm bounds are centered at +/-6, and run from y=0 to y=12 after shoulder placement.
        pose.pushPose();
        try {
            pose.translate((side == HumanoidArm.RIGHT ? -6 : 6) / 16.0, 0, 0);
            pose.scale(0.5F, 12F / 28, 0.5F);
            arm.render(pose, buffer, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        } finally { pose.popPose(); }
    }
    static void positionArm(PoseStack pose, HumanoidArm side, float equip, float swing) {
        // Minecraft 1.20.1 ItemInHandRenderer.renderPlayerArm, including the native swing.
        float direction = side == HumanoidArm.LEFT ? -1 : 1;
        float root = net.minecraft.util.Mth.sqrt(swing);
        float sine = net.minecraft.util.Mth.sin(root * (float) Math.PI);
        pose.translate(direction * (-0.3F * sine + 0.64000005F),
                0.4F * net.minecraft.util.Mth.sin(root * (float) Math.PI * 2) - 0.6F - equip * 0.6F,
                -0.4F * net.minecraft.util.Mth.sin(swing * (float) Math.PI) - 0.71999997F);
        pose.mulPose(Axis.YP.rotationDegrees(direction * 45));
        pose.mulPose(Axis.YP.rotationDegrees(direction * sine * 70));
        pose.mulPose(Axis.ZP.rotationDegrees(direction * net.minecraft.util.Mth.sin(swing * swing * (float) Math.PI) * -20));
        pose.translate(-direction, 3.6, 3.5);
        pose.mulPose(Axis.ZP.rotationDegrees(direction * 120));
        pose.mulPose(Axis.XP.rotationDegrees(200));
        pose.mulPose(Axis.YP.rotationDegrees(direction * -135));
        pose.translate(direction * 5.6, 0, 0);
    }
    static void swimmingArm(PoseStack pose, HumanoidArm side, float weight, float age) {
        if (weight <= 0) return;
        float direction = side == HumanoidArm.LEFT ? -1 : 1;
        float stroke = age * 0.22F + (side == HumanoidArm.LEFT ? 0 : net.minecraft.util.Mth.PI);
        float sine = net.minecraft.util.Mth.sin(stroke);
        pose.translate(direction * 0.06F * weight * sine, 0.08F * weight * sine,
                0.10F * weight * net.minecraft.util.Mth.cos(stroke));
        pose.mulPose(Axis.XP.rotationDegrees(10 * weight * sine));
        pose.mulPose(Axis.ZP.rotationDegrees(direction * 6 * weight * sine));
    }
    static void renderAfterVision() {
        if (captured == null || !eligible()) return;
        var frame = captured; captured = null;
        var projection = new Matrix4f(RenderSystem.getProjectionMatrix()); var sorting = RenderSystem.getVertexSorting();
        var shader = RenderSystem.getShader(); var color = RenderSystem.getShaderColor().clone();
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST), cull = GL11.glIsEnabled(GL11.GL_CULL_FACE), blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean mask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK); int function = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        int texture = RenderSystem.getShaderTexture(0);
        var view = RenderSystem.getModelViewStack(); view.pushPose();
        try {
            view.last().pose().set(frame.view); RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(frame.projection, frame.sorting);
            var pose = new PoseStack(); pose.last().pose().set(frame.pose); pose.last().normal().set(frame.normal);
            draw(pose, Minecraft.getInstance().renderBuffers().bufferSource(), frame.equip, frame.swing, frame.partial);
        } finally {
            view.popPose(); RenderSystem.applyModelViewMatrix(); RenderSystem.setProjectionMatrix(projection, sorting);
            RenderSystem.setShader(() -> shader); RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]); RenderSystem.setShaderTexture(0, texture);
            RenderSystem.depthMask(mask); RenderSystem.depthFunc(function);
            if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
        }
    }
}
