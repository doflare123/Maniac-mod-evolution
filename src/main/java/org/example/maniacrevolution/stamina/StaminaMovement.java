package org.example.maniacrevolution.stamina;

import net.minecraft.world.entity.player.Player;
import org.example.maniacrevolution.util.PlayerModeUtil;

public final class StaminaMovement {
    private StaminaMovement() {}

    public static boolean usesStamina(Player player) {
        return PlayerModeUtil.isSurvivalOrAdventure(player) && player.isAlive() && !player.isSleeping()
                && !player.isPassenger() && !player.getAbilities().flying && !player.isFallFlying()
                && !player.isInWaterOrBubble() && !player.isInLava() && !player.onClimbable()
                && !player.isVisuallySwimming();
    }
}
