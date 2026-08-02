package org.example.maniacrevolution.perk.perks.survivor;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.perk.*;

/**
 * Пассивный перк: улучшает QTE для генераторов.
 * +0.25 секунды на реакцию
 * +10% пространства для успешного нажатия
 */
public class QuickReflexesPerk extends Perk {

    // Бонусы
    public static final int BONUS_TIME_MS = 450;
    public static final float GREEN_ZONE_MULTIPLIER = 1.06f;
    public static final float SUCCESS_TOLERANCE_MULTIPLIER = 1.05f;
    public static final float CRIT_ZONE_MULTIPLIER = 1.10f;

    public QuickReflexesPerk() {
        super(new Builder("quick_reflexes")
                .type(PerkType.PASSIVE)
                .team(PerkTeam.SURVIVOR)
                .phases(PerkPhase.ANY));
    }

    @Override
    public Component getDescription() {
        return Component.translatable("perk.maniacrev.quick_reflexes.desc",
                BONUS_TIME_MS,
                Math.round((GREEN_ZONE_MULTIPLIER - 1.0f) * 100.0f),
                Math.round((SUCCESS_TOLERANCE_MULTIPLIER - 1.0f) * 100.0f),
                Math.round((CRIT_ZONE_MULTIPLIER - 1.0f) * 100.0f));
    }

    /**
     * Проверяет, есть ли у игрока этот перк.
     * Вызывается из QTEState и QTEClientHandler.
     */
    public static boolean hasQuickReflexes(ServerPlayer player) {
        org.example.maniacrevolution.data.PlayerData data =
                org.example.maniacrevolution.data.PlayerDataManager.get(player);

        return data.getSelectedPerks().stream()
                .anyMatch(inst -> inst.getPerk() instanceof QuickReflexesPerk);
    }
}
