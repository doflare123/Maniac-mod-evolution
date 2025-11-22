package org.example.maniacrevolution.perk.perks.common;

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

import java.util.List;

public class BerserkerPerk extends Perk {
    public BerserkerPerk() {
        super(new Builder("berserker")
                .type(PerkType.HYBRID)
                .team(PerkTeam.ALL)
                .phases(PerkPhase.ANY)
                .cooldown(60));
    }

    @Override
    public void applyPassiveEffect(ServerPlayer player) {
        // Пассивка: Сила 1
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, false));
    }

    @Override
    public void onTick(ServerPlayer player) {
        // Обновляем эффект силы каждую секунду
        if (player.tickCount % 20 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, false));
        }
    }

    @Override
    public void removePassiveEffect(ServerPlayer player) {
        player.removeEffect(MobEffects.DAMAGE_BOOST);
    }

    @Override
    public void onActivate(ServerPlayer player) {
        // Активка: регенерация в зависимости от союзников
        int allies = countAlliesNearby(player);
        int regenLevel = Math.max(0, allies - 1);
        int duration = (5 + allies) * 20;

        if (regenLevel >= 0) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, regenLevel, false, true));
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§cБерсерк! Регенерация " + (regenLevel + 1) + " на " + (duration / 20) + " сек"),
                    true
            );
        }
    }

    private int countAlliesNearby(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        AABB area = player.getBoundingBox().inflate(7);

        var team = player.getTeam();
        if (team == null) return 0;

        List<Player> nearby = level.getEntitiesOfClass(Player.class, area,
                p -> p != player && p.getTeam() == team);

        return nearby.size();
    }
}
