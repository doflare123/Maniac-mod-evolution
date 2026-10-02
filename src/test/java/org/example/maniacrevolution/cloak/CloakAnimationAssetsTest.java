package org.example.maniacrevolution.cloak;

import dev.kosmx.playerAnim.core.data.gson.AnimationSerializing;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/** Load the actual exported files through the bundled library, rather than only validating JSON syntax. */
public final class CloakAnimationAssetsTest {
    public static void main(String[] args) throws Exception {
        var directory = Path.of("src/main/resources/assets/maniacrev/player_animation");
        Set<String> loops = Set.of("idle", "carpet_ride", "fly_to_target", "bound_struggle", "fly_return");
        int count = 0;
        try (var files = Files.list(directory)) {
            for (Path file : files.filter(p -> p.getFileName().toString().startsWith("cloak_")).toList()) {
                try (var stream = Files.newInputStream(file)) {
                    var animations = AnimationSerializing.deserializeAnimation(stream);
                    if (animations.size() != 1) throw new AssertionError("Expected one clip in " + file);
                    var animation = animations.get(0);
                    String clip = file.getFileName().toString().replace("cloak_", "").replace(".json", "");
                    if (!animation.extraData.get("name").equals("animation.cloak." + clip)) throw new AssertionError("Registry name: " + file);
                    if (animation.isInfinite != loops.contains(clip)) throw new AssertionError("Loop flag: " + file);
                    if (animation.endTick <= 0 || animation.getPart("body") == null) throw new AssertionError("Empty clip: " + file);
                    if (clip.equals("carpet_ride")) {
                        var pose = new org.example.maniacrevolution.cloak.client.CloakRidingAnimation(animation, 10);
                        pose.setupAnim(0);
                        for (String arm : new String[]{"leftArm", "rightArm"}) {
                            var rotation = pose.get3DTransform(arm, dev.kosmx.playerAnim.api.TransformType.ROTATION,
                                    0, new dev.kosmx.playerAnim.core.util.Vec3f(0, 0, 0));
                            if (Math.abs(rotation.getZ()) > Math.toRadians(6)) throw new AssertionError("Arms spread sideways: " + arm);
                        }
                        var root = pose.get3DTransform("body", dev.kosmx.playerAnim.api.TransformType.POSITION,
                                0, new dev.kosmx.playerAnim.core.util.Vec3f(0, 0, 0));
                        if (root.getY() != 0) throw new AssertionError("Riding pose must not bob vertically");
                    }
                    count++;
                }
            }
        }
        if (count != 14) throw new AssertionError("Expected all 14 player clips, got " + count);
        checkHoverMotion();
        System.out.println("All 14 cloak player animations loaded through PlayerAnimator.");
    }

    private static void checkHoverMotion() {
        double y = 64;
        for (int tick = 0; tick < 200; tick++) {
            var motion = CloakHoverMotion.calculate(0, 1, 0, 0.1, y, 64.5);
            if (Math.abs(motion.z() - 0.21585) > 0.000001) throw new AssertionError("Walking-speed hover lost");
            y += motion.y();
            if (y > 64.5 + 1e-9) throw new AssertionError("Height overshoot");
            if (tick >= 6 && Math.abs(y - 64.5) > 1e-9) throw new AssertionError("Unstable hover height");
        }
        var diagonal = CloakHoverMotion.calculate(1, 1, 90, 0.1, 64.5, 64.5);
        if (Math.abs(Math.hypot(diagonal.x(), diagonal.z()) - 0.21585) > 1e-9) throw new AssertionError("Diagonal speed boost");
        var idle = CloakHoverMotion.calculate(0, 0, 0, 0.1, 64.5, 64.5);
        if (idle.x() != 0 || idle.y() != 0 || idle.z() != 0) throw new AssertionError("Idle hover must remain still");
        System.out.println("Hover checks passed: stable height, walking speed, normalized diagonals, relaxed riding pose.");
    }
}
