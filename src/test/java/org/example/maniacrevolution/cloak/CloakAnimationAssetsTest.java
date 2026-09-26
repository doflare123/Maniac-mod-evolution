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
                    count++;
                }
            }
        }
        if (count != 14) throw new AssertionError("Expected all 14 player clips, got " + count);
        System.out.println("All 14 cloak player animations loaded through PlayerAnimator.");
    }
}
