package org.example.maniacrevolution.perk.perks.survivor;

import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.perk.*;

/**
 * Пассивный перк: улучшает QTE для генераторов.
 * +0.25 секунды на реакцию
 * +10% пространства для успешного нажатия
 */
public class QuickReflexesPerk extends Perk {

    // Бонусы
    public static final int BONUS_TIME_MS = 2500; // +0.25 секунды
    public static final float BONUS_SPACE_MULTIPLIER = 2.1f; // +10%

    public QuickReflexesPerk() {
        super(new Builder("quick_reflexes")
                .type(PerkType.PASSIVE)
                .team(PerkTeam.SURVIVOR)
                .phases(PerkPhase.ANY));
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