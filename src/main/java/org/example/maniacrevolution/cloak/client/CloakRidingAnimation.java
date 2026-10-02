package org.example.maniacrevolution.cloak.client;

import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Vec3f;

/** A relaxed riding stance; retains the exported deploy/land timing and root transforms. */
public final class CloakRidingAnimation extends KeyframeAnimationPlayer {
    public CloakRidingAnimation(KeyframeAnimation animation, int tick) { super(animation, tick); }

    @Override public Vec3f get3DTransform(String bone, TransformType type, float partialTick, Vec3f original) {
        Vec3f exported = super.get3DTransform(bone, type, partialTick, original);
        if (type != TransformType.ROTATION) return exported;
        String name = String.valueOf(getData().extraData.get("name"));
        float blend = Math.max(0, Math.min(1, (getTick() + partialTick) / Math.max(1, getData().endTick)));
        if (name.endsWith("carpet_ride")) blend = 1;
        else if (name.endsWith("carpet_land")) blend = 1 - blend;
        float pitch, roll;
        switch (bone) {
            case "leftArm" -> { pitch = -12; roll = -5; }
            case "rightArm" -> { pitch = -12; roll = 5; }
            case "leftLeg" -> { pitch = 8; roll = -3; }
            case "rightLeg" -> { pitch = -8; roll = 3; }
            case "torso" -> { pitch = 4; roll = 0; }
            default -> { return exported; }
        }
        return new Vec3f((float)Math.toRadians(pitch) * blend, 0, (float)Math.toRadians(roll) * blend);
    }
}
