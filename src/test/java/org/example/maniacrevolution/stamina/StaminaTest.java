package org.example.maniacrevolution.stamina;

import com.google.gson.GsonBuilder;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.data.gson.AnimationJson;

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
        for (int i = 0; i < 100; i++) stamina.tick(true, true);
        check(stamina.amount() == 900, "Five seconds of sprinting cost one cell even when grounded");
        for (int i = 0; i < 100; i++) stamina.tick(true, true);
        check(stamina.amount() == 800, "Sprint keeps draining after the first cell");
        check(stamina.spendJump() && stamina.amount() == 700, "Jump costs a full cell");
        stamina.tick(false, false);
        check(stamina.amount() == 700, "Time airborne leaves the reserve unchanged");
        for (int i = 0; i < 100; i++) stamina.tick(false, true);
        check(stamina.amount() == 800, "Five seconds of recovery restore one cell");
        for (int i = 0; i < 800; i++) stamina.tick(true, true);
        check(stamina.amount() == 0 && !stamina.spendJump(), "Exhaustion blocks a jump");
        for (int i = 0; i < 99; i++) stamina.tick(false, true);
        check(!stamina.spendJump(), "Partial cells cannot pay for a jump");
        stamina.tick(false, true);
        check(stamina.spendJump() && stamina.amount() == 0, "A fully recovered cell pays for a jump");
    }

    private static void verifyClips() throws Exception {
        var type = AnimationJson.getListedTypeToken();
        var gson = new GsonBuilder().registerTypeAdapter(type, new AnimationJson()).create();
        for (String name : List.of("walking", "running", "sneak_idle", "sneak_walk", "jump_first", "falling", "landing")) {
            var stream = StaminaTest.class.getResourceAsStream("/assets/maniacrev/player_animation/" + name + ".json");
            check(stream != null, "Missing animation " + name);
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                List<KeyframeAnimation> clips = gson.fromJson(reader, type);
                check(clips.size() == 1, "Unexpected clip count for " + name);
                KeyframeAnimation clip = clips.get(0);
                check(name.equals(clip.extraData.get("name")) && clip.getLength() > 0, "Invalid clip " + name);
                check(clip.getPart("leftLeg").pitch.length() > 0 && clip.getPart("rightLeg").pitch.length() > 0,
                        "Missing leg keyframes for " + name);
                boolean looping = !name.equals("jump_first") && !name.equals("landing");
                check(clip.isInfinite == looping, "Invalid loop for " + name);
            }
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
