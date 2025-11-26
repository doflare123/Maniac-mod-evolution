package org.example.maniacrevolution.perk.perks.common;

import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.perk.*;

/**
 * Пассивный перк: увеличивает получаемые монеты на 20%.
 * Работает через проверку в команде /maniacrev addmoney.
 */
public class BigmoneyPerk extends Perk {

    public static final float BONUS_MULTIPLIER = 1.2f; // +20%

    public BigmoneyPerk() {
        super(new Builder("bigmoney")
                .type(PerkType.PASSIVE)
                .team(PerkTeam.ALL)
                .phases(PerkPhase.ANY));
    }

    /**
     * Проверяет, есть ли у игрока этот перк и он активен.
     */
    public static boolean hasActivePerk(ServerPlayer player) {
        return PlayerDataManager.get(player)
                .getSelectedPerks()
                .stream()
                .anyMatch(inst -> inst.getPerk() instanceof BigmoneyPerk);
    }

    /**
     * Применяет бонус к количеству монет.
     * @param baseAmount базовое количество монет
     * @return увеличенное количество (округление вверх)
     */
    public static int applyBonus(int baseAmount) {
        return (int) Math.ceil(baseAmount * BONUS_MULTIPLIER);
    }
}