package org.example.maniacrevolution.util;

import net.minecraft.world.entity.player.Player;

public final class PlayerModeUtil {
    private PlayerModeUtil() {}

    public static boolean isSurvivalOrAdventure(Player player) {
        return player != null && !player.isCreative() && !player.isSpectator();
    }
}
