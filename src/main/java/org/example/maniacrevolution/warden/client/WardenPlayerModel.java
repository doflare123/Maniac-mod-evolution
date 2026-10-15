package org.example.maniacrevolution.warden.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.animation.definitions.WardenAnimation;
import org.example.maniacrevolution.warden.WardenAnimationTimeline;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

/** Vanilla Warden geometry animated from the real player, with no surrogate mob or AI. */
public final class WardenPlayerModel extends HierarchicalModel<AbstractClientPlayer> implements ArmedModel {
    private final ModelPart root, bone, body, head, leftArm, rightArm, leftLeg, rightLeg;
    private final org.joml.Vector3f animationVector = new org.joml.Vector3f();

    public WardenPlayerModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
        bone = root.getChild("bone");
        body = bone.getChild("body");
        head = body.getChild("head");
        leftArm = body.getChild("left_arm");
        rightArm = body.getChild("right_arm");
        leftLeg = bone.getChild("left_leg");
        rightLeg = bone.getChild("right_leg");
    }

    @Override
    public void setupAnim(AbstractClientPlayer player, float walk, float speed, float age, float yaw, float pitch) {
        animate(walk, speed, age, yaw, pitch, player.isCrouching());
        swim(player.getSwimAmount(Mth.clamp(age - player.tickCount, 0, 1)), age);
        applyAbilities(WardenAnimationsClient.frame(player, age - player.tickCount));
    }
    @Override public ModelPart root() { return root; }
    void swim(float weight, float age) {
        weight = Mth.clamp(weight, 0, 1);
        if (weight == 0) return;
        float stroke = age * 0.22F;
        body.xRot = Mth.lerp(weight, body.xRot, 0);
        body.zRot = Mth.lerp(weight, body.zRot, 0.08F * Mth.sin(stroke));
        head.xRot = Mth.lerp(weight, head.xRot, -0.2F);
        head.zRot *= 1 - weight;
        leftArm.xRot = Mth.rotLerp(weight, leftArm.xRot, -Mth.PI + 0.55F * Mth.sin(stroke));
        rightArm.xRot = Mth.rotLerp(weight, rightArm.xRot, -Mth.PI - 0.55F * Mth.sin(stroke));
        leftArm.zRot = Mth.lerp(weight, leftArm.zRot, -0.22F - 0.18F * Mth.cos(stroke));
        rightArm.zRot = Mth.lerp(weight, rightArm.zRot, 0.22F - 0.18F * Mth.cos(stroke));
        leftLeg.xRot = Mth.lerp(weight, leftLeg.xRot, 0.28F * Mth.sin(stroke * 2));
        rightLeg.xRot = Mth.lerp(weight, rightLeg.xRot, -0.28F * Mth.sin(stroke * 2));
    }
    void applyAbilities(WardenAnimationTimeline.Frame frame) {
        applyAbilities(frame, 1);
    }
    void applyAbilities(WardenAnimationTimeline.Frame frame, float amplitude) {
        // One combat action takes visual priority; sniff's clock keeps running underneath it.
        switch (frame.action()) {
            case ATTACK -> KeyframeAnimations.animate(this, WardenAnimation.WARDEN_ATTACK, (long) (frame.elapsed() * 50), amplitude, animationVector);
            case CHARGE -> KeyframeAnimations.animate(this, WardenAnimation.WARDEN_SONIC_BOOM, (long) (Math.min(20, frame.elapsed()) * 85), amplitude, animationVector);
            case RELEASE -> KeyframeAnimations.animate(this, WardenAnimation.WARDEN_SONIC_BOOM, (long) (1700 + frame.elapsed() * 50), amplitude, animationVector);
            case RECOVER -> {
                float t = Mth.clamp((float) frame.elapsed() / WardenAnimationTimeline.RECOVER, 0, 1);
                float weight = amplitude * (1 - t * t * (3 - 2 * t));
                KeyframeAnimations.animate(this, WardenAnimation.WARDEN_SONIC_BOOM, frame.charge() * 85L, weight, animationVector);
            }
            case NONE -> { if (frame.sniff() >= 0) KeyframeAnimations.animate(this, WardenAnimation.WARDEN_SNIFF, (long) (frame.sniff() * 50), amplitude, animationVector); }
        }
    }
    void renderArm(HumanoidArm side, PoseStack pose, VertexConsumer buffer, int light) {
        var arm = side == HumanoidArm.LEFT ? leftArm : rightArm;
        // Remove only the original shoulder pivot; retain animated shoulder displacement/rotation.
        pose.translate(side == HumanoidArm.LEFT ? -13.0 / 16 : 13.0 / 16, 13.0 / 16, -1.0 / 16);
        arm.render(pose, buffer, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
    }

    void animate(float walk, float speed, float age, float yaw, float pitch, boolean crouching) {
        root.getAllParts().forEach(ModelPart::resetPose);
        head.yRot = yaw * Mth.DEG_TO_RAD;
        head.xRot = pitch * Mth.DEG_TO_RAD;
        // Vanilla idle sway and heavy gait, driven by the player's walkAnimation through LivingEntityRenderer.
        float stride = Math.min(0.5F, 3 * speed);
        float step = walk * 0.8662F;
        float bend = Math.min(0.35F, stride);
        head.zRot += 0.3F * Mth.sin(step) * stride + 0.06F * Mth.cos(age * 0.1F);
        head.xRot += 1.2F * Mth.cos(step + Mth.HALF_PI) * bend + 0.06F * Mth.sin(age * 0.1F);
        body.zRot = 0.1F * Mth.sin(step) * stride + 0.025F * Mth.sin(age * 0.1F);
        body.xRot = Mth.cos(step) * bend + 0.025F * Mth.cos(age * 0.1F);
        leftLeg.xRot = Mth.cos(step) * stride;
        rightLeg.xRot = Mth.cos(step + Mth.PI) * stride;
        leftArm.xRot = -0.8F * Mth.cos(step) * stride;
        rightArm.xRot = -0.8F * Mth.sin(step) * stride;
        if (crouching) {
            // PlayerRenderer's dispatcher offset already lowers a crouching player by 0.125 blocks.
            // Compensate at the legs, and lower only the torso so the feet stay on the floor.
            bone.y -= 2;
            body.y += 2;
            body.xRot += 0.2F;
            head.xRot -= 0.2F;
        }
    }

    @Override
    public void translateToHand(HumanoidArm arm, PoseStack pose) {
        root.translateAndRotate(pose);
        bone.translateAndRotate(pose);
        body.translateAndRotate(pose);
        (arm == HumanoidArm.LEFT ? leftArm : rightArm).translateAndRotate(pose);
        pose.translate(0, 1.5, 0); // Move the vanilla held-item layer from shoulder to Warden wrist.
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay,
                               float r, float g, float b, float alpha) {
        root.render(pose, buffer, light, overlay, r, g, b, alpha);
    }
}
