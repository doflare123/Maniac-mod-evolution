package org.example.maniacrevolution.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Quaternionf;

/** Player-model axes: down is +Y, forward is -Z; the bong's grip is model Y=-3. */
final class BongThirdPersonPose {
    private BongThirdPersonPose() {}

    static float weight(float seconds) {
        float t = Math.max(0, Math.min(1, Math.min(seconds / .6F, (2.4F - seconds) / .55F)));
        return t * t * (3 - 2 * t);
    }

    static void rotate(ModelPart part, boolean holdingArm, boolean mirror, float seconds) {
        // Both paired exports use the same numeric timeline sampler as the syringe.
        float[] angles = SyringeAnimationClips.sample("bong_player", "using",
                holdingArm ? "right_arm" : "left_arm", "rotation", seconds);
        applyAngles(part, angles, mirror, seconds);
    }

    static void applyAngles(ModelPart part, float[] angles, boolean mirror, float seconds) {
        float w = weight(seconds);
        part.xRot += ((float) Math.toRadians(angles[0]) - part.xRot) * w;
        part.yRot += ((float) Math.toRadians(mirror ? -angles[1] : angles[1]) - part.yRot) * w;
        part.zRot += ((float) Math.toRadians(mirror ? -angles[2] : angles[2]) - part.zRot) * w;
    }

    static void mount(PoseStack pose, ModelPart arm, boolean mirror) {
        pose.translate((mirror ? 1 : -1) / 16F, 8.5F / 16F, -2.3F / 16F);
        // The shoulder moves the fist. Cancel its rotation once at the wrist so the
        // long neck stays upright instead of pointing down the trouser leg.
        pose.mulPose(new Quaternionf().rotationZYX(arm.zRot, arm.yRot, arm.xRot).conjugate());
        pose.mulPose(Axis.XP.rotationDegrees(-8));
        pose.scale(mirror ? -.7F : .7F, -.7F, .7F);
        pose.translate(0, 3F / 16, 0);
    }
}
