package org.example.maniacrevolution.perk.perks.common;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.perk.*;

/**
 * Пассивный перк: увеличивает получаемый опыт на 20%.
 * Работает через проверку в команде /maniacrev addexp.
 */
public class MegamindPerk extends Perk {

    public static final float BONUS_MULTIPLIER = 1.5f; // +20%

    public MegamindPerk() {
        super(new Builder("megamind")
                .type(PerkType.PASSIVE)
                .team(PerkTeam.ALL)
                .phases(PerkPhase.ANY));
    }

    @Override
    public Component getDescription() {
        return Component.translatable("perk.maniacrev.megamind.desc",
                Math.round((BONUS_MULTIPLIER - 1.0f) * 100.0f));
    }

    /**
     * Проверяет, есть ли у игрока этот перк и он активен.
     */
    public static boolean hasActivePerk(ServerPlayer player) {
        return PlayerDataManager.get(player)
                .getSelectedPerks()
                .stream()
                .anyMatch(inst -> inst.getPerk() instanceof MegamindPerk);
    }

    /**
     * Применяет бонус к количеству опыта.
     * @param baseAmount базовое количество опыта
     * @return увеличенное количество (округление вверх)
     */
    public static int applyBonus(int baseAmount) {
        return (int) Math.ceil(baseAmount * BONUS_MULTIPLIER);
    }
}
