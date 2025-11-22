package org.example.maniacrevolution.perk.perks.common;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.example.maniacrevolution.perk.Perk;
import org.example.maniacrevolution.perk.PerkPhase;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.perk.PerkType;

public class ScoutPerk extends Perk {
    public ScoutPerk() {
        super(new Builder("scout")
                .type(PerkType.ACTIVE)
                .team(PerkTeam.ALL)
                .phases(PerkPhase.ANY)
                .cooldown(45));
    }

    @Override
    public void onActivate(ServerPlayer player) {
        // Даёт ночное зрение и скорость на 10 секунд
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 10 * 20, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 10 * 20, 1, false, false));
        player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§bРежим разведки активирован!"), true);
    }
}

