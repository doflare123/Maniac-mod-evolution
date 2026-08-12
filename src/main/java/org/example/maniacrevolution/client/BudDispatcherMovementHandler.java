package org.example.maniacrevolution.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.client.screen.BudDispatcherScreen;

/** Keeps normal movement input active while the non-pausing flower screen is open. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class BudDispatcherMovementHandler {
    private BudDispatcherMovementHandler() {
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof BudDispatcherScreen)
                || event.getEntity() != minecraft.player) return;

        Input input = event.getInput();
        input.up = isDown(minecraft, minecraft.options.keyUp);
        input.down = isDown(minecraft, minecraft.options.keyDown);
        input.left = isDown(minecraft, minecraft.options.keyLeft);
        input.right = isDown(minecraft, minecraft.options.keyRight);
        input.jumping = isDown(minecraft, minecraft.options.keyJump);
        input.shiftKeyDown = isDown(minecraft, minecraft.options.keyShift);
        float movementScale = input.shiftKeyDown ? 0.3F : 1.0F;
        input.forwardImpulse = ((input.up ? 1.0F : 0.0F)
                - (input.down ? 1.0F : 0.0F)) * movementScale;
        input.leftImpulse = ((input.left ? 1.0F : 0.0F)
                - (input.right ? 1.0F : 0.0F)) * movementScale;
    }

    private static boolean isDown(Minecraft minecraft, KeyMapping mapping) {
        InputConstants.Key key = mapping.getKey();
        return key.getType() == InputConstants.Type.KEYSYM
                && key.getValue() != InputConstants.UNKNOWN.getValue()
                && InputConstants.isKeyDown(minecraft.getWindow().getWindow(), key.getValue());
    }
}
