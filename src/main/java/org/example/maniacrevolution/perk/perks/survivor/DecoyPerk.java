package org.example.maniacrevolution.perk.perks.survivor;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.example.maniacrevolution.perk.Perk;
import org.example.maniacrevolution.perk.PerkPhase;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.perk.PerkType;

public class DecoyPerk extends Perk {
    public DecoyPerk() {
        super(new Builder("decoy")
                .type(PerkType.ACTIVE)
                .team(PerkTeam.SURVIVOR)
                .phases(PerkPhase.ANY)
                .cooldown(120));
    }

    @Override
    public void onActivate(ServerPlayer player) {
        ServerLevel level = player.serverLevel();

        // Создаём шум и частицы чтобы отвлечь маньяка
        level.playSound(null, player.blockPosition().offset(10, 0, 10),
                SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1.0f, 1.0f);
        level.sendParticles(ParticleTypes.CLOUD,
                player.getX() + 10, player.getY() + 1, player.getZ() + 10,
                20, 1, 1, 1, 0.1);

        player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§eОбманка активирована!"), true);
    }
}

