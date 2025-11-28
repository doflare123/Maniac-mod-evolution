package org.example.maniacrevolution.perk.perks.common;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.example.maniacrevolution.perk.Perk;
import org.example.maniacrevolution.perk.PerkPhase;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.perk.PerkType;

public class StrPerk extends Perk {
    public StrPerk(){
        super(new Builder("str_perk")
                .type(PerkType.PASSIVE)
                .team(PerkTeam.ALL)
                .phases(PerkPhase.REVERSAL)
        );
    }

    @Override
    public  void applyPassiveEffect(ServerPlayer player){
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, false ));
    }

    @Override
    public void onTick(ServerPlayer player) {
        if (player.tickCount % 20 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, false));
        }
    }
}
