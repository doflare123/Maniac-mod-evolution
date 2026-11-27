package org.example.maniacrevolution.event;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.example.maniacrevolution.util.PlayerModeUtil;

public class PenaltySlotManager {

    private static final int PENALTY_SLOT_START = 6; // Слоты 6, 7, 8
    private static final int PENALTY_SLOT_END = 8;

    /**
     * Проверяет, находится ли игрок в пенальти-слоте
     */
    public static boolean isInPenaltySlot(Player player) {
        if (!PlayerModeUtil.isSurvivalOrAdventure(player)) {
            return false;
        }

        int selected = player.getInventory().selected;
        return selected >= PENALTY_SLOT_START
                && selected <= PENALTY_SLOT_END;
    }

    /**
     * Получает количество заполненных пенальти-слотов
     */
    public static int getFilledPenaltySlots(Player player) {
        int count = 0;
        Inventory inv = player.getInventory();

        for (int i = PENALTY_SLOT_START; i <= PENALTY_SLOT_END; i++) {
            if (!inv.getItem(i).isEmpty()) {
                count++;
            }
        }

        return count;
    }
}
