package org.example.maniacrevolution.perk.perks.survivor;

import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.perk.Perk;
import org.example.maniacrevolution.perk.PerkPhase;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.perk.PerkType;

public class HackerPerk extends Perk {
    public HackerPerk() {
        super(new Builder("hacker")
                .type(PerkType.PASSIVE)
                .team(PerkTeam.SURVIVOR)
                .phases(PerkPhase.HUNT, PerkPhase.MIDGAME));
    }

    // Пассивный бонус: ускоренный взлом компьютеров
    // Реализация зависит от системы компьютеров в датапаке

    @Override
    public void applyPassiveEffect(ServerPlayer player) {
        // Добавляем тег для датапака
        player.addTag("maniacrev_hacker_boost");
    }

    @Override
    public void removePassiveEffect(ServerPlayer player) {
        player.removeTag("maniacrev_hacker_boost");
    }
}
