package org.example.maniacrevolution.perk.perks.maniac;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.example.maniacrevolution.perk.Perk;
import org.example.maniacrevolution.perk.PerkPhase;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.perk.PerkType;

public class PredatorPerk extends Perk {
    public PredatorPerk() {
        super(new Builder("predator")
                .type(PerkType.ACTIVE)
                .team(PerkTeam.MANIAC)
                .phases(PerkPhase.HUNT, PerkPhase.MIDGAME)
                .cooldown(45));
    }

    @Override
    public void onActivate(ServerPlayer player) {
        ServerLevel level = player.serverLevel();

        // Находим ближайшего выжившего в радиусе 30 блоков
        AABB area = player.getBoundingBox().inflate(30);
        Player closest = level.getEntitiesOfClass(Player.class, area, p -> {
                    if (p == player) return false;
                    var team = p.getTeam();
                    return team != null && "survivors".equals(team.getName());
                }).stream()
                .min((a, b) -> Double.compare(
                        a.distanceToSqr(player),
                        b.distanceToSqr(player)))
                .orElse(null);

        if (closest != null) {
            // Подсвечиваем цель на 5 секунд
            closest.addEffect(new MobEffectInstance(MobEffects.GLOWING, 5 * 20, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 5 * 20, 1, false, false));

            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§4Цель обнаружена! Охота началась!"),
                    true
            );
        } else {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§7Целей поблизости нет..."),
                    true
            );
        }
    }
}
