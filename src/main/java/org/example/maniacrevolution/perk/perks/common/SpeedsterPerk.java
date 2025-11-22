package org.example.maniacrevolution.perk.perks.common;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.example.maniacrevolution.perk.Perk;
import org.example.maniacrevolution.perk.PerkPhase;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.perk.PerkType;

public class SpeedsterPerk extends Perk {
    public SpeedsterPerk() {
        super(new Builder("speedster")
                .type(PerkType.PASSIVE)
                .team(PerkTeam.ALL)
                .phases(PerkPhase.ANY));
    }

    @Override
    public void applyPassiveEffect(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false));
    }

    @Override
    public void onTick(ServerPlayer player) {
        if (player.tickCount % 20 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false));
        }
    }

    @Override
    public void removePassiveEffect(ServerPlayer player) {
        player.removeEffect(MobEffects.MOVEMENT_SPEED);
    }
}
