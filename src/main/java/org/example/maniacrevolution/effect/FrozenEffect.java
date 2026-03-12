package org.example.maniacrevolution.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * Эффект "Заморозка" для перка Футбольный фанат.
 * Не даёт игроку двигаться (сильное замедление).
 * Показывает синие частицы по телу.
 */
public class FrozenEffect extends MobEffect {

    public FrozenEffect() {
        super(MobEffectCategory.HARMFUL, 0x88CCFF); // Светло-синий цвет
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) return;

        // Накладываем замедление каждый тик чтобы его нельзя было снять
        entity.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                10,       // 0.5 сек — обновляется каждый тик
                127,      // максимальный уровень = полная остановка
                false, false, false
        ));

        // Синие частицы по телу
        if (entity.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 2; i++) {
                double px = entity.getX() + (entity.level().random.nextDouble() - 0.5) * 0.6;
                double py = entity.getY() + entity.level().random.nextDouble() * 1.8;
                double pz = entity.getZ() + (entity.level().random.nextDouble() - 0.5) * 0.6;

                serverLevel.sendParticles(
                        ParticleTypes.SNOWFLAKE,
                        px, py, pz,
                        1, 0, 0.01, 0, 0.01
                );
            }
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 4 == 0; // ~5 раз в секунду
    }

    @Override
    public boolean isInstantenous() {
        return false;
    }
}
