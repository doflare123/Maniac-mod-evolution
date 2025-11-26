//package org.example.maniacrevolution.perk.perks.survivor;
//
//import net.minecraft.server.level.ServerPlayer;
//import net.minecraft.world.effect.MobEffectInstance;
//import net.minecraft.world.effect.MobEffects;
//import org.example.maniacrevolution.perk.*;
//
///**
// * Пример PASSIVE_COOLDOWN перка.
// * Дает щит когда ХП игрока падает ниже 20%.
// * После срабатывания уходит в кулдаун на 60 секунд.
// */
//public class LastStandPerk extends Perk {
//
//    private static final float TRIGGER_HEALTH_PERCENT = 0.2f; // 20% ХП
//    private static final int SHIELD_DURATION = 10 * 20; // 10 секунд
//    private static final int SHIELD_AMPLIFIER = 1; // Уровень 2
//
//    public LastStandPerk() {
//        super(new Builder("last_stand")
//                .type(PerkType.PASSIVE_COOLDOWN)
//                .team(PerkTeam.ALL)
//                .phases(PerkPhase.ANY)
//                .cooldown(60)); // 60 секунд кулдауна
//    }
//
//    @Override
//    public boolean shouldTrigger(ServerPlayer player) {
//        float healthPercent = player.getHealth() / player.getMaxHealth();
//        return healthPercent <= TRIGGER_HEALTH_PERCENT;
//    }
//
//    @Override
//    public void onTrigger(ServerPlayer player) {
//        // Даем эффект сопротивления
//        player.addEffect(new MobEffectInstance(
//                MobEffects.DAMAGE_RESISTANCE,
//                SHIELD_DURATION,
//                SHIELD_AMPLIFIER,
//                false,
//                true,
//                true
//        ));
//
//        // Даем эффект регенерации
//        player.addEffect(new MobEffectInstance(
//                MobEffects.REGENERATION,
//                SHIELD_DURATION,
//                1,
//                false,
//                true,
//                true
//        ));
//
//        // Визуальный эффект
//        player.level().broadcastEntityEvent(player, (byte) 35); // Totem эффект
//    }
//}