package org.example.maniacrevolution.perk.perks.survivor;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.perk.Perk;
import org.example.maniacrevolution.perk.PerkPhase;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.perk.PerkType;
import org.example.maniacrevolution.pinkorchid.PinkOrchidManager;

/** Розовая Орхидея — запись собственного маршрута и запуск движущейся копии. */
public final class PinkOrchidPerk extends Perk {
    public static final String ID = "pink_orchid";
    public static final int TICKS_PER_SECOND = 20;
    public static final int RECORDING_SECONDS = 6;
    public static final int RECORDING_TICKS = RECORDING_SECONDS * TICKS_PER_SECOND;
    public static final int MANA_COST = 10;
    public static final int COOLDOWN_SECONDS = 45;
    public static final int MATERIALIZE_TICKS = 10;
    public static final int ROUTE_SAMPLE_INTERVAL_TICKS = 1;
    public static final int MILLISECONDS_PER_SECOND = 1000;
    public static final PerkPhase FIRST_ACTIVE_PHASE = PerkPhase.HUNT;
    public static final PerkPhase LAST_ACTIVE_PHASE = PerkPhase.MIDGAME;

    public PinkOrchidPerk() {
        super(new Builder(ID)
                .type(PerkType.HYBRID)
                .team(PerkTeam.SURVIVOR)
                .phases(FIRST_ACTIVE_PHASE, LAST_ACTIVE_PHASE)
                .cooldown(COOLDOWN_SECONDS)
                .manaCost(MANA_COST));
    }

    @Override
    public Component getDescription() {
        return Component.translatable(
                "perk.maniacrev." + ID + ".desc",
                RECORDING_SECONDS,
                MANA_COST,
                COOLDOWN_SECONDS,
                MATERIALIZE_TICKS * MILLISECONDS_PER_SECOND / TICKS_PER_SECOND,
                FIRST_ACTIVE_PHASE.getScoreboardValue(),
                LAST_ACTIVE_PHASE.getScoreboardValue()
        );
    }

    @Override
    public void onGameStart(ServerPlayer player) {
        PinkOrchidManager.beginForOwner(player);
    }

    @Override
    public void onPhaseChange(ServerPlayer player, PerkPhase newPhase) {
        if (newPhase == PerkPhase.REVERSAL) {
            PinkOrchidManager.clearOwnerRuntime(player, false);
        }
    }

    @Override
    public boolean meetsActivationCondition(ServerPlayer player) {
        return PinkOrchidManager.hasRecordedRoute(player);
    }

    @Override
    public Component getConditionNotMetMessage(ServerPlayer player) {
        return Component.translatable("message.maniacrev.pink_orchid.no_route");
    }

    @Override
    public void onActivate(ServerPlayer player) {
        PinkOrchidManager.activate(player);
    }
}
