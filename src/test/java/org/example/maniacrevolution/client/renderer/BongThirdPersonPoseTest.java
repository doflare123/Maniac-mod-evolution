package org.example.maniacrevolution.client.renderer;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import software.bernie.geckolib.loading.object.BakedAnimations;
import software.bernie.geckolib.util.JsonUtil;
import org.joml.Vector3f;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** Checks the actual hand attachment through every action frame, for both hands. */
public final class BongThirdPersonPoseTest {
    private static InputStreamReader playerAnimation() {
        var input = BongThirdPersonPoseTest.class.getResourceAsStream("/assets/maniacrev/animations/bong_player.animation.json");
        if (input == null) throw new AssertionError("Missing paired player animation");
        return new InputStreamReader(input, StandardCharsets.UTF_8);
    }

    private static ModelPart arm(boolean left, boolean holding) {
        var arm = new ModelPart(List.of(), Map.of());
        arm.x = left ? 5 : -5;
        arm.y = 2;
        arm.xRot = holding ? (float) Math.toRadians(-18) : 0;
        arm.zRot = left ? .05F : -.05F;
        return arm;
    }

    public static void verify() throws Exception {
        var raw = JsonParser.parseReader(playerAnimation()).getAsJsonObject().getAsJsonObject("animations");
        if (JsonUtil.GEO_GSON.fromJson(raw, BakedAnimations.class).getAnimation("using") == null)
            throw new AssertionError("GeckoLib cannot parse the player timeline");
        var clips = SyringeAnimationClips.read(playerAnimation());
        for (boolean mirror : new boolean[]{false, true}) for (int frame = 0; frame <= 96; frame++) {
            float seconds = frame / 40F;
            ModelPart active = arm(mirror, true);
            ModelPart support = arm(!mirror, false);
            BongThirdPersonPose.applyAngles(active, SyringeAnimationClips.sample(clips, "using", "right_arm", "rotation", seconds), mirror, seconds);
            BongThirdPersonPose.applyAngles(support, SyringeAnimationClips.sample(clips, "using", "left_arm", "rotation", seconds), mirror, seconds);
            var pose = new PoseStack();
            active.translateAndRotate(pose);
            Vector3f fist = pose.last().pose().transformPosition(new Vector3f((mirror ? 1 : -1) / 16F, 8.5F / 16F, -2.3F / 16F));
            BongThirdPersonPose.mount(pose, active, mirror);
            Vector3f grip = pose.last().pose().transformPosition(new Vector3f(0, -3F / 16, 0));
            if (grip.distance(fist) > .00001F) throw new AssertionError("Bong detached from fist at " + seconds);
            Vector3f rim = pose.last().pose().transformPosition(new Vector3f(0, 8F / 16, 2F / 16)).mul(16);
            if (!rim.isFinite()) throw new AssertionError("Invalid bong transform");
            if (seconds >= .85F && seconds <= 1.65F) {
                if (Math.abs(rim.x) > .5F || rim.y < -2.5F || rim.y > -1F || rim.z < -4.5F || rim.z > -3.5F)
                    throw new AssertionError("Bong rim misses the mouth: " + mirror + " " + seconds + " " + rim);
                var supportPose = new PoseStack();
                support.translateAndRotate(supportPose);
                Vector3f supportingFist = supportPose.last().pose().transformPosition(
                        new Vector3f((mirror ? -1 : 1) / 16F, 8.5F / 16F, -2.3F / 16F));
                if (supportingFist.distance(grip) > 4F / 16) throw new AssertionError("Supporting hand misses the bong");
            }
            if (frame == 0 || frame == 96) {
                if (Math.abs(active.xRot - Math.toRadians(-18)) > .00001
                        || Math.abs(active.zRot - (mirror ? .05 : -.05)) > .00001)
                    throw new AssertionError("Using must blend back to the ordinary held-item pose");
            }
        }
        System.out.println("Bong third person: 194 poses, both hands; grip fixed to fist, rim at mouth, support hand and recovery checked.");
    }
}
