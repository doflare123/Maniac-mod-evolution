package org.example.maniacrevolution.perk.perks.survivor;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.perk.Perk;
import org.example.maniacrevolution.perk.PerkPhase;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.perk.PerkType;
import org.example.maniacrevolution.roseglasses.RoseColoredGlassesManager;

/** «Розовые очки» — привязанный предмет, смягчающий урон во второй руке. */
public final class RoseColoredGlassesPerk extends Perk {
    public static final String ID = "rose_colored_glasses";
    public static final int DAMAGE_REDUCTION_PERCENT = 25;
    public static final int MAX_DURABILITY = 15;
    public static final int MINIMUM_FINAL_DAMAGE = 1;
    public static final int MINIMUM_DURABILITY_COST = 1;
    public static final float FILTER_FADE_SECONDS = 0.25F;
    public static final PerkPhase FIRST_ACTIVE_PHASE = PerkPhase.HUNT;
    public static final PerkPhase LAST_ACTIVE_PHASE = PerkPhase.MIDGAME;

    public static final int TICKS_PER_SECOND = 20;
    public static final int FILTER_FADE_TICKS = Math.max(1,
            Math.round(FILTER_FADE_SECONDS * TICKS_PER_SECOND));
    public static final float DAMAGE_MULTIPLIER = 1.0F - DAMAGE_REDUCTION_PERCENT / 100.0F;

    public RoseColoredGlassesPerk() {
        super(new Builder(ID)
                .type(PerkType.PASSIVE)
                .team(PerkTeam.SURVIVOR)
                .phases(FIRST_ACTIVE_PHASE, LAST_ACTIVE_PHASE));
    }

    @Override
    public Component getDescription() {
        return Component.translatable(
                "perk.maniacrev." + ID + ".desc",
                MAX_DURABILITY,
                DAMAGE_REDUCTION_PERCENT,
                MINIMUM_FINAL_DAMAGE,
                FIRST_ACTIVE_PHASE.getScoreboardValue(),
                LAST_ACTIVE_PHASE.getScoreboardValue(),
                MINIMUM_DURABILITY_COST
        );
    }

    @Override
    public void onGameStart(ServerPlayer player) {
        RoseColoredGlassesManager.beginForOwner(player);
    }
}
