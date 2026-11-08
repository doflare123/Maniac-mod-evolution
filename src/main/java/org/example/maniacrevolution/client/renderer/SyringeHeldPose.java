package org.example.maniacrevolution.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Quaternionf;

/** Holding a tool and preparing an injection have different wrist/shoulder orientations. */
final class SyringeHeldPose {
    private SyringeHeldPose() {}

    static float injectionWeight(float seconds) {
        float t = Math.max(0, Math.min(1, Math.min(seconds / .2F, (3.2F - seconds) / .25F)));
        return t * t * (3 - 2 * t);
    }

    static float gripZ(boolean injecting, float seconds) {
        return -2.3F + (injecting ? 4.6F * injectionWeight(seconds) : 0);
    }

    static void firstPersonCamera(PoseStack pose, boolean mirror, boolean injecting, float seconds, boolean moving) {
        float weight = injecting ? injectionWeight(seconds) : 0;
        pose.translate(mirror ? -.05 : .05, -.08 + .28 * weight, -.30);
        if (!injecting) {
            float phase = (float) (Math.PI * 2 * seconds / (moving ? .8 : 3.2));
            float side = mirror ? -1 : 1;
            pose.translate(side * (moving ? .015 : .002) * Math.sin(phase),
                    (moving ? .018 : .003) * Math.cos(phase * 2), moving ? .01 * Math.cos(phase) : 0);
        }
        // A player's own right shoulder must be on the right of the camera.
        pose.scale(-1, -1, 1);
    }

    static float[] firstPersonArm(boolean sourceArm, boolean left, boolean moving, float seconds) {
        if (!sourceArm) return new float[3];
        float phase = (float) (Math.PI * 2 * seconds / (moving ? .8 : 3.2));
        float sway = (float) Math.sin(phase);
        return new float[]{-70 - (moving ? 5 : .3F) * sway, 0, (left ? -1 : 1) * (5 + (moving ? 1.5F * sway : 0))};
    }

    static Quaternionf itemRotation(float[] motion, boolean injecting, float seconds, boolean firstPerson) {
        // The needle points upward and slightly forward from the fist. The extended
        // first-person arm needs a different wrist angle from the lowered world arm.
        Quaternionf hold = new Quaternionf().rotationZYX((float) Math.toRadians(10), 0,
                (float) Math.toRadians(firstPerson ? -70 : -25));
        Quaternionf animated = new Quaternionf().rotationZYX((float) Math.toRadians(-motion[2]),
                (float) Math.toRadians(motion[1]), (float) Math.toRadians(-motion[0]));
        return injecting ? hold.slerp(animated, injectionWeight(seconds)) : hold.mul(animated);
    }
}
