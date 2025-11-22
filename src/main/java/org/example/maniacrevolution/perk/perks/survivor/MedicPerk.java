package org.example.maniacrevolution.perk.perks.survivor;

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

public class MedicPerk extends Perk {
    public MedicPerk() {
        super(new Builder("medic")
                .type(PerkType.HYBRID)
                .team(PerkTeam.SURVIVOR)
                .phases(PerkPhase.ANY)
                .cooldown(60));
    }

    @Override
    public void applyPassiveEffect(ServerPlayer player) {
        // Пассивка: медленная регенерация
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, false, false));
    }

    @Override
    public void onTick(ServerPlayer player) {
        if (player.tickCount % 30 == 0) { // Каждые 1.5 сек
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, false, false));
        }
    }

    @Override
    public void onActivate(ServerPlayer player) {
        // Лечит всех союзников в радиусе 5 блоков
        ServerLevel level = player.serverLevel();
        AABB area = player.getBoundingBox().inflate(5);
        var team = player.getTeam();

        level.getEntitiesOfClass(Player.class, area, p -> p.getTeam() == team)
                .forEach(p -> {
                    p.heal(6.0f); // 3 сердца
                    p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 5 * 20, 1, false, true));
                });

        player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§aЛечебная волна!"), true);
    }
}
