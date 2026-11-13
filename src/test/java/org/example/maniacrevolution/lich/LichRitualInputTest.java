package org.example.maniacrevolution.lich;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.Input;
import org.example.maniacrevolution.lich.client.LichRitualInput;
import org.lwjgl.glfw.GLFW;

final class LichRitualInputTest {
    static void verify() throws Exception {
        var input = new Input();
        var jump = new KeyMapping("key.lich.test_jump", GLFW.GLFW_KEY_SPACE, "key.categories.misc");
        // Forge's static click dispatch queries physical modifier keys via GLFW.
        // Seed its queued count directly so the real consumeClick runs headlessly.
        var queued = KeyMapping.class.getDeclaredField("clickCount"); queued.setAccessible(true);
        LichRitualInput.consumeSpaceUntilRelease();
        for (int frame=0;frame<30;frame++) {
            // Closing a screen and keyboard repeats can restore the held mapping.
            input.jumping = true; input.forwardImpulse = .75f;
            jump.setDown(true); queued.setInt(jump, 2);
            LichRitualInput.filterJump(input, jump, true);
            if(input.jumping || jump.isDown() || jump.consumeClick())throw new AssertionError("Ritual space leaked into jump");
            if(input.forwardImpulse != .75f)throw new AssertionError("Unrelated movement blocked");
        }
        input.jumping = true; jump.setDown(true);
        LichRitualInput.filterJump(input, jump, false);
        if(input.jumping || jump.isDown())throw new AssertionError("Release frame leaked stale jump");
        input.jumping = true; jump.setDown(true); queued.setInt(jump, 1);
        LichRitualInput.filterJump(input, jump, true);
        if(!input.jumping || !jump.isDown() || !jump.consumeClick())throw new AssertionError("Fresh jump after release blocked");
        jump.setDown(false);
        System.out.println("Ritual input: held space/repeats and release consumed; fresh jump and other movement preserved.");
    }
}
