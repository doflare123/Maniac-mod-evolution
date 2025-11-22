package org.example.maniacrevolution.perk.perks.common;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.example.maniacrevolution.perk.Perk;
import org.example.maniacrevolution.perk.PerkPhase;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.perk.PerkType;

public class VampirePerk extends Perk {
    public VampirePerk() {
        super(new Builder("vampire")
                .type(PerkType.PASSIVE)
                .team(PerkTeam.ALL)
                .phases(PerkPhase.ANY));
    }

    // Вызывается из ServerEvents при нанесении урона
    public static void onDamageDealt(ServerPlayer attacker, float damage) {
        // Вампиризм: лечение 20% от нанесенного урона
        float heal = damage * 0.2f;
        attacker.heal(heal);
    }
}
