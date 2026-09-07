package org.example.maniacrevolution.perk.perks.common;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.colorroulette.ColorRouletteManager;
import org.example.maniacrevolution.perk.Perk;
import org.example.maniacrevolution.perk.PerkPhase;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.perk.PerkType;

/** Active common perk that awards one of three short-lived personal cards. */
public final class ColorRoulettePerk extends Perk {
    public static final String ID = "color_roulette";
    public static final int MANA_COST = 10;
    public static final int INTRO_TICKS = 30;
    public static final int SPIN_TICKS = 120;
    public static final int COOLDOWN_SECONDS = 45;
    public static final int CARD_LIFETIME_SECONDS = 45;
    public static final int COMPUTER_PERCENT = 5;
    public static final int SURVIVOR_HEAL_HP = 4;
    public static final int TOP_COMPUTERS_REVEALED = 3;
    public static final int RED_GUIDANCE_SECONDS = 15;
    public static final float GREEN_DAMAGE_PER_STACK = 1.5F;
    public static final int RED_CARD_KNOCKBACK_LEVEL = 2;

    public ColorRoulettePerk() {
        super(new Builder(ID)
                .type(PerkType.ACTIVE)
                .team(PerkTeam.ALL)
                .phases(PerkPhase.HUNT, PerkPhase.MIDGAME, PerkPhase.REVERSAL)
                .manaCost(MANA_COST)
                .cooldown(COOLDOWN_SECONDS));
    }

    @Override
    public boolean meetsActivationCondition(ServerPlayer player) {
        return ColorRouletteManager.canStart(player);
    }

    @Override
    public Component getConditionNotMetMessage(ServerPlayer player) {
        return ColorRouletteManager.getStartFailure(player);
    }

    @Override
    public Component getDescription() {
        return Component.translatable("perk.maniacrev." + ID + ".desc",
                MANA_COST,
                INTRO_TICKS / 20.0F,
                SPIN_TICKS / 20,
                COOLDOWN_SECONDS,
                CARD_LIFETIME_SECONDS,
                RED_CARD_KNOCKBACK_LEVEL,
                TOP_COMPUTERS_REVEALED,
                COMPUTER_PERCENT,
                SURVIVOR_HEAL_HP,
                RED_GUIDANCE_SECONDS,
                GREEN_DAMAGE_PER_STACK,
                PerkPhase.HUNT.getScoreboardValue(),
                PerkPhase.REVERSAL.getScoreboardValue());
    }

    @Override
    public void onActivate(ServerPlayer player) {
        ColorRouletteManager.start(player);
    }
}
