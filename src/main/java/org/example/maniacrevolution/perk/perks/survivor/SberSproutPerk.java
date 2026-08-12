package org.example.maniacrevolution.perk.perks.survivor;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.perk.Perk;
import org.example.maniacrevolution.perk.PerkPhase;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.perk.PerkType;
import org.example.maniacrevolution.sbersprout.SberSproutManager;

/** СберРосток — перенос части лично внесённого прогресса между компьютерами. */
public final class SberSproutPerk extends Perk {
    public static final String ID = "sber_sprout";
    public static final int PRESERVED_PROGRESS_PERCENT = 75;
    public static final int EXTRACTION_MANA_COST = 5;
    public static final int PLANTING_MANA_COST = 5;
    public static final int MINIMUM_EXTRACTION_PERCENT = 1;
    public static final int DISPLAY_DECIMAL_PLACES = 2;
    public static final int ROUNDING_FACTOR = 100;
    public static final int TICKS_PER_SECOND = 20;
    public static final PerkPhase FIRST_ACTIVE_PHASE = PerkPhase.HUNT;
    public static final PerkPhase LAST_ACTIVE_PHASE = PerkPhase.MIDGAME;

    public SberSproutPerk() {
        super(new Builder(ID)
                .type(PerkType.HYBRID)
                .team(PerkTeam.SURVIVOR)
                .phases(FIRST_ACTIVE_PHASE, LAST_ACTIVE_PHASE)
                .manaCost(EXTRACTION_MANA_COST));
    }

    @Override
    public Component getDescription() {
        return Component.translatable(
                "perk.maniacrev." + ID + ".desc",
                EXTRACTION_MANA_COST,
                PRESERVED_PROGRESS_PERCENT,
                PLANTING_MANA_COST,
                MINIMUM_EXTRACTION_PERCENT,
                DISPLAY_DECIMAL_PLACES,
                FIRST_ACTIVE_PHASE.getScoreboardValue(),
                LAST_ACTIVE_PHASE.getScoreboardValue()
        );
    }

    @Override
    public void onGameStart(ServerPlayer player) {
        SberSproutManager.beginForOwner(player);
    }

    @Override
    public void onPhaseChange(ServerPlayer player, PerkPhase newPhase) {
        if (newPhase == PerkPhase.REVERSAL) {
            SberSproutManager.deactivateOwnerSession(player, false);
        }
    }

    @Override
    public boolean meetsActivationCondition(ServerPlayer player) {
        return SberSproutManager.getExtractionFailure(player)
                == SberSproutManager.ExtractionFailure.NONE;
    }

    @Override
    public Component getConditionNotMetMessage(ServerPlayer player) {
        return SberSproutManager.getExtractionFailure(player).message();
    }

    @Override
    public void onActivate(ServerPlayer player) {
        SberSproutManager.extract(player);
    }
}
