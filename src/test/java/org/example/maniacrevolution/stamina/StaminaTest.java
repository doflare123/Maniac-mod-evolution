package org.example.maniacrevolution.stamina;

import com.google.gson.GsonBuilder;
import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.data.gson.AnimationJson;
import dev.kosmx.playerAnim.core.util.Vec3f;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class StaminaTest {
    public static void main(String[] args) throws Exception {
        verifyStamina();
        verifyClips();
    }

    private static void verifyStamina() {
        var stamina = new Stamina(Stamina.MAX);
        for (int i = 0; i < 50; i++) stamina.tick(true, true);
        check(stamina.amount() == 900, "Two and a half seconds of sprinting cost one cell");
        for (int i = 0; i < 50; i++) stamina.tick(true, true);
        check(stamina.amount() == 800, "Sprint keeps draining after the first cell");
        check(stamina.spendJump() && stamina.amount() == 700, "Jump costs a full cell");
        stamina.tick(false, false);
        check(stamina.amount() == 700, "Time airborne leaves the reserve unchanged");
        for (int i = 0; i < 100; i++) stamina.tick(false, true);
        check(stamina.amount() == 800, "Five seconds of recovery restore one cell");
        check(stamina.spend(2 * Stamina.CELL) && stamina.amount() == 600, "Dash costs two full cells");
        for (int i = 0; i < 300; i++) stamina.tick(true, true);
        check(stamina.amount() == 0 && !stamina.spendJump(), "Exhaustion blocks a jump");
        for (int i = 0; i < 99; i++) stamina.tick(false, true);
        check(!stamina.spendJump(), "Partial cells cannot pay for a jump");
        check(!stamina.spend(2 * Stamina.CELL) && stamina.amount() == 99, "A rejected dash preserves stamina");
        stamina.tick(false, true);
        check(stamina.spendJump() && stamina.amount() == 0, "A fully recovered cell pays for a jump");
    }

    private static void verifyClips() throws Exception {
        var type = AnimationJson.getListedTypeToken();
        var gson = new GsonBuilder().registerTypeAdapter(type, new AnimationJson()).create();
        for (String name : List.of("walking", "running", "sneak_idle", "sneak_walk", "jump_first", "falling", "landing", "dash")) {
            var stream = StaminaTest.class.getResourceAsStream("/assets/maniacrev/player_animation/" + name + ".json");
            check(stream != null, "Missing animation " + name);
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                List<KeyframeAnimation> clips = gson.fromJson(reader, type);
                check(clips.size() == 1, "Unexpected clip count for " + name);
                KeyframeAnimation clip = clips.get(0);
                check(name.equals(clip.extraData.get("name")) && clip.getLength() > 0, "Invalid clip " + name);
                check(clip.getPart("leftLeg").pitch.length() > 0 && clip.getPart("rightLeg").pitch.length() > 0,
                        "Missing leg keyframes for " + name);
                boolean looping = !List.of("jump_first", "landing", "dash").contains(name);
                check(clip.isInfinite == looping, "Invalid loop for " + name);
                if (name.equals("walking") || name.equals("dash")) verifyHeadPivot(clip);
                if (name.equals("dash")) check(clip.getLength() >= 11, "Dash pose includes recovery after movement");
            }
        }
    }

    private static void verifyHeadPivot(KeyframeAnimation clip) {
        var player = new KeyframeAnimationPlayer(clip);
        for (int tick = 0; tick < clip.getLength(); tick++) {
            for (float partial : new float[]{0.0f, 0.5f}) {
                player.setupAnim(partial);
                var head = player.get3DTransform("head", TransformType.POSITION, partial, Vec3f.ZERO);
                var torso = player.get3DTransform("torso", TransformType.POSITION, partial, Vec3f.ZERO);
                check(head.subtract(torso).dotProduct(head.subtract(torso)) < 0.0001f,
                        "The " + clip.extraData.get("name") + " head and torso must share the same pivot at tick " + tick);
            }
            player.tick();
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
