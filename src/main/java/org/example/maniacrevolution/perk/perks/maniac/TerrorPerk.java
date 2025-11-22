package org.example.maniacrevolution.perk.perks.maniac;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.example.maniacrevolution.perk.Perk;
import org.example.maniacrevolution.perk.PerkPhase;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.perk.PerkType;

public class TerrorPerk extends Perk {
    public TerrorPerk() {
        super(new Builder("terror")
                .type(PerkType.HYBRID)
                .team(PerkTeam.MANIAC)
                .phases(PerkPhase.ANY)
                .cooldown(90));
    }

    @Override
    public void onTick(ServerPlayer player) {
        // Пассивка: выжившие рядом получают слабость
        if (player.tickCount % 40 == 0) {
            ServerLevel level = player.serverLevel();
            AABB area = player.getBoundingBox().inflate(8);

            level.getEntitiesOfClass(Player.class, area, p -> {
                var team = p.getTeam();
                return team != null && "survivors".equals(team.getName());
            }).forEach(p -> {
                ((ServerPlayer)p).addEffect(
                        new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, false));
            });
        }
    }

    @Override
    public void onActivate(ServerPlayer player) {
        // Крик ужаса - все выжившие в радиусе 15 блоков получают слепоту
        ServerLevel level = player.serverLevel();
        AABB area = player.getBoundingBox().inflate(15);

        level.playSound(null, player.blockPosition(),
                SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.5f, 0.5f);

        level.getEntitiesOfClass(Player.class, area, p -> {
            var team = p.getTeam();
            return team != null && "survivors".equals(team.getName());
        }).forEach(p -> {
            ((ServerPlayer)p).addEffect(
                    new MobEffectInstance(MobEffects.BLINDNESS, 3 * 20, 0, false, false));
            ((ServerPlayer)p).addEffect(
                    new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 3 * 20, 1, false, false));
        });

        player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§4ТЕРРОР!"), true);
    }
}