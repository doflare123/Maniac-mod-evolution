package org.example.maniacrevolution.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * Кастомный эффект "Цель" для Агента 47
 * Помечает игрока как текущую цель агента
 */
public class TargetEffect extends MobEffect {

    public TargetEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF0000); // Красный цвет
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Эффект не делает ничего сам по себе, только маркирует игрока
        super.applyEffectTick(entity, amplifier);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean isInstantenous() {
        return false;
    }
}