package org.example.maniacrevolution.lich.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.lwjgl.glfw.GLFW;

/** Consumes the ritual's space press through screen closure and OS key repeats. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class LichRitualInput {
    private static boolean waitingForRelease;
    private LichRitualInput() {}

    public static void consumeSpaceUntilRelease() { waitingForRelease = true; }

    public static void filterJump(Input input, KeyMapping jump, boolean spaceHeld) {
        if (!waitingForRelease) return;
        input.jumping = false;
        jump.setDown(false);
        while (jump.consumeClick()) { /* Discard clicks belonging to the ritual press. */ }
        // Suppress the release frame too: the mapping may still contain a stale press.
        if (!spaceHeld) waitingForRelease = false;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void movement(MovementInputUpdateEvent event) {
        if (!waitingForRelease) return;
        var mc = Minecraft.getInstance();
        if (!mc.options.keyJump.matches(GLFW.GLFW_KEY_SPACE, 0)) {
            waitingForRelease = false;
            return;
        }
        filterJump(event.getInput(), mc.options.keyJump,
                InputConstants.isKeyDown(mc.getWindow().getWindow(), GLFW.GLFW_KEY_SPACE));
    }

    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { waitingForRelease = false; }
}
