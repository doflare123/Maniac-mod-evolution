package org.example.maniacrevolution.perk.perks.maniac;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.example.maniacrevolution.perk.Perk;
import org.example.maniacrevolution.perk.PerkPhase;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.perk.PerkType;

public class ExecutionerPerk extends Perk {
    public ExecutionerPerk() {
        super(new Builder("executioner")
                .type(PerkType.ACTIVE)
                .team(PerkTeam.MANIAC)
                .phases(PerkPhase.REVERSAL) // Только в фазе переворота
                .cooldown(120));
    }

    @Override
    public void onActivate(ServerPlayer player) {
        // Режим казни - огромный урон но низкая скорость
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 15 * 20, 3, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 15 * 20, 1, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 15 * 20, 1, false, true));

        player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§4§lРЕЖИМ КАЗНИ!"), true);
    }
}