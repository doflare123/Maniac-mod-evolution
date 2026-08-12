package org.example.maniacrevolution.perk.perks.survivor;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.forgetmenot.ForgetMeNotManager;
import org.example.maniacrevolution.perk.Perk;
import org.example.maniacrevolution.perk.PerkPhase;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.perk.PerkType;

/** «Незабудка» — личная устанавливаемая точка с прерываемой телепортацией. */
public final class ForgetMeNotPerk extends Perk {
    public static final String ID = "forget_me_not";
    public static final int TICKS_PER_SECOND = 20;
    public static final int CHANNEL_DURATION_SECONDS = 3;
    public static final int RECENT_DAMAGE_LOCK_SECONDS = 10;
    public static final int SUCCESS_COOLDOWN_SECONDS = 60;
    public static final int INTERRUPTED_COOLDOWN_SECONDS = 30;
    public static final float MANA_COST = 10.0F;
    public static final float INTERRUPTED_MANA_REFUND = MANA_COST / 2.0F;

    public static final int CHANNEL_DURATION_TICKS = CHANNEL_DURATION_SECONDS * TICKS_PER_SECOND;
    public static final int RECENT_DAMAGE_LOCK_TICKS =
            RECENT_DAMAGE_LOCK_SECONDS * TICKS_PER_SECOND;
    public static final int SUCCESS_COOLDOWN_TICKS =
            SUCCESS_COOLDOWN_SECONDS * TICKS_PER_SECOND;
    public static final int INTERRUPTED_COOLDOWN_TICKS =
            INTERRUPTED_COOLDOWN_SECONDS * TICKS_PER_SECOND;

    public ForgetMeNotPerk() {
        super(new Builder(ID)
                .type(PerkType.HYBRID)
                .team(PerkTeam.SURVIVOR)
                .phases(PerkPhase.HUNT, PerkPhase.MIDGAME, PerkPhase.REVERSAL)
                .cooldown(SUCCESS_COOLDOWN_SECONDS)
                .manaCost(MANA_COST));
    }

    @Override
    public Component getDescription() {
        return Component.translatable(
                "perk.maniacrev." + ID + ".desc",
                (int) MANA_COST,
                CHANNEL_DURATION_SECONDS,
                RECENT_DAMAGE_LOCK_SECONDS,
                SUCCESS_COOLDOWN_SECONDS,
                INTERRUPTED_COOLDOWN_SECONDS,
                (int) INTERRUPTED_MANA_REFUND
        );
    }

    @Override
    public void onGameStart(ServerPlayer player) {
        ForgetMeNotManager.beginForOwner(player);
    }

    @Override
    public boolean meetsActivationCondition(ServerPlayer player) {
        return ForgetMeNotManager.canStartTeleport(player);
    }

    @Override
    public Component getConditionNotMetMessage(ServerPlayer player) {
        return ForgetMeNotManager.getActivationFailure(player).message();
    }

    @Override
    public void onActivate(ServerPlayer player) {
        ForgetMeNotManager.startTeleport(player);
    }
}
