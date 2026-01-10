package org.example.maniacrevolution.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class SlowRegenEffect extends MobEffect {
    public SlowRegenEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xCD5CAB);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide && entity.getHealth() < entity.getMaxHealth()) {
            // 5 хп за 60 секунд = 0.0833 хп/сек = 1 хп за 12 секунд = 1 хп за 240 тиков
            // Срабатывает каждые 240 тиков
            entity.heal(1.0F);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // Срабатывает каждые 240 тиков (12 секунд)
        return duration % 240 == 0;
    }
}