package org.example.maniacrevolution.perk.perks.maniac;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.ClientParticleEffectPacket;
import org.example.maniacrevolution.perk.Perk;
import org.example.maniacrevolution.perk.PerkPhase;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.perk.PerkType;

public class GuidingLightPerk extends Perk {
    private static final int COOLDOWN_SEC = 35;
    private static final float MANA_COST = 2F;
    private static final int TRAIL_DURATION_TICKS = 40;

    public GuidingLightPerk() {
        super(new Builder("guiding_light")
                .type(PerkType.ACTIVE)
                .team(PerkTeam.MANIAC)
                .phases(PerkPhase.ANY)
                .cooldown(COOLDOWN_SEC)
                .manaCost(MANA_COST));
    }

    @Override
    public Component getDescription() {
        return Component.translatable("perk.maniacrev.guiding_light.desc", TRAIL_DURATION_TICKS / 20);
    }

    @Override
    public void onActivate(ServerPlayer player) {
        if (player.getServer() == null) {
            return;
        }

        ServerPlayer nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (ServerPlayer other : player.getServer().getPlayerList().getPlayers()) {
            if (other == player || other.level() != player.level()) {
                continue;
            }
            if (PerkTeam.fromPlayer(other) != PerkTeam.SURVIVOR
                    || other.gameMode.getGameModeForPlayer() != GameType.ADVENTURE) {
                continue;
            }

            double distance = player.distanceToSqr(other);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = other;
            }
        }

        if (nearest == null) {
            player.displayClientMessage(Component.literal("Нет выживших поблизости!")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }

        ModNetworking.sendToPlayer(ClientParticleEffectPacket.guidingLight(nearest, TRAIL_DURATION_TICKS), player);
        player.displayClientMessage(Component.literal("✨ Ближайший выживший в "
                + (int) Math.sqrt(nearestDistance) + " блоках!").withStyle(ChatFormatting.YELLOW), true);
    }
}
