package org.example.maniacrevolution.perk.perks.common;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.perk.Perk;
import org.example.maniacrevolution.perk.PerkType;
import org.example.maniacrevolution.stamina.DashMovement;
import org.example.maniacrevolution.stamina.StaminaManager;

public final class DashPerk extends Perk {
    public static final String ID = "dash";

    public DashPerk() {
        super(new Builder(ID).type(PerkType.ACTIVE).cooldown(90));
    }

    @Override
    public boolean meetsActivationCondition(ServerPlayer player) {
        return DashMovement.canStart(player) && StaminaManager.has(player, DashMovement.COST);
    }

    @Override
    public Component getConditionNotMetMessage(ServerPlayer player) {
        return Component.translatable(StaminaManager.has(player, DashMovement.COST)
                ? "message.maniacrev.dash.blocked" : "message.maniacrev.dash.stamina");
    }

    @Override
    public void onActivate(ServerPlayer player) {
        DashMovement.start(player);
    }
}
