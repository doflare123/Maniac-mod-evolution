package org.example.maniacrevolution.perk.perks.common;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import org.example.maniacrevolution.perk.*;

/**
 * Активный перк: мощный рывок вперёд.
 * Толкает игрока на 1 блок вверх и 4 блока вперёд по направлению взгляда.
 * Кулдаун: 80 секунд.
 */
public class GtoMedalPerk extends Perk {

    private static final double FORWARD_POWER = 2.0; // 4 блока вперёд
    private static final double UPWARD_POWER = .5;  // 1 блок вверх

    public GtoMedalPerk() {
        super(new Builder("gto_medal")
                .type(PerkType.ACTIVE)
                .team(PerkTeam.ALL)
                .phases(PerkPhase.ANY)
                .cooldown(2)); // 80 секунд
    }

    @Override
    public void onActivate(ServerPlayer player) {
        // Получаем направление взгляда игрока
        Vec3 lookDirection = player.getLookAngle();

        // Нормализуем горизонтальную составляющую (игнорируем вертикальный взгляд)
        Vec3 horizontalDirection = new Vec3(lookDirection.x, 0, lookDirection.z).normalize();

        // Вычисляем вектор толчка: горизонтально вперёд + вертикально вверх
        Vec3 pushVector = new Vec3(
                horizontalDirection.x * FORWARD_POWER,
                UPWARD_POWER,
                horizontalDirection.z * FORWARD_POWER
        );

        // Применяем импульс к игроку
        player.setDeltaMovement(pushVector);
        player.hurtMarked = true; // Важно! Синхронизирует движение с клиентом

        // Краткий эффект скорости для плавности
        player.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SPEED,
                10, // 0.5 секунды
                2,  // Уровень 3
                false,
                false,
                false
        ));

        // Звуковой эффект
        player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENDER_DRAGON_FLAP,
                SoundSource.PLAYERS,
                1.5F,
                1.5F
        );

        // Дополнительный звук рывка
        player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.PISTON_EXTEND,
                SoundSource.PLAYERS,
                0.8F,
                2.0F
        );

        // Визуальные эффекты - частицы взрыва позади игрока
        if (player.level() instanceof ServerLevel serverLevel) {
            Vec3 particlePos = player.position().subtract(horizontalDirection.scale(0.5));

            // Облако дыма позади
            serverLevel.sendParticles(
                    ParticleTypes.CLOUD,
                    particlePos.x,
                    particlePos.y + 0.5,
                    particlePos.z,
                    15,  // количество частиц
                    0.3, 0.3, 0.3,  // разброс
                    0.02  // скорость
            );

            // Частицы ветра вперёд
            Vec3 forwardPos = player.position().add(horizontalDirection.scale(0.5));
            serverLevel.sendParticles(
                    ParticleTypes.SWEEP_ATTACK,
                    forwardPos.x,
                    forwardPos.y + 1.0,
                    forwardPos.z,
                    8,
                    0.2, 0.2, 0.2,
                    0.1
            );

            // Искры под ногами
            serverLevel.sendParticles(
                    ParticleTypes.FIREWORK,
                    player.getX(),
                    player.getY() + 0.1,
                    player.getZ(),
                    10,
                    0.3, 0.1, 0.3,
                    0.05
            );
        }

        // Визуальный эффект для самого игрока
        player.level().broadcastEntityEvent(player, (byte) 47); // Эффект рывка
    }
}