package org.example.maniacrevolution.perk.perks.maniac;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.example.maniacrevolution.perk.Perk;
import org.example.maniacrevolution.perk.PerkPhase;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.perk.PerkType;

public class ShadowPerk extends Perk {
    public ShadowPerk() {
        super(new Builder("shadow")
                .type(PerkType.ACTIVE)
                .team(PerkTeam.MANIAC)
                .phases(PerkPhase.HUNT, PerkPhase.MIDGAME)
                .cooldown(70));
    }

    @Override
    public void onActivate(ServerPlayer player) {
        // Невидимость + скорость на короткое время
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 5 * 20, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 5 * 20, 2, false, false));

        player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§8Слиться с тенями..."), true);
    }
}