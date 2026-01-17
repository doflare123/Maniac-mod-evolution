package org.example.maniacrevolution.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Эффект "Гонка со смертью"
 * Увеличивает урон Смерти за каждого убитого выжившего
 * Каждый уровень добавляет +2 урона
 */
public class DeathRaceEffect extends MobEffect {

    public DeathRaceEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x8B0000); // Темно-красный цвет

        // Добавляем модификатор атаки (+2 урона за уровень)
        addAttributeModifier(
                Attributes.ATTACK_DAMAGE,
                "b9f4c3e8-7a2c-4c4e-8f3d-2e9a7b1c5d4f",
                2.0D,
                AttributeModifier.Operation.ADDITION
        );
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Эффект постоянный, не нужно ничего делать каждый тик
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