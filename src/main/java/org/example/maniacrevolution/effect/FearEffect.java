package org.example.maniacrevolution.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * Эффект "Страх" - существо теряет контроль и бежит в противоположную сторону.
 */
public class FearEffect extends MobEffect {

    public FearEffect() {
        super(MobEffectCategory.HARMFUL, 0x1a1a1a); // Тёмно-серый цвет

        // Добавляем немного скорости для эффекта паники
        this.addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                "7f8b42c4-9f5e-4e5a-8b3a-2c1f9d8e7a6b",
                0.3,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Получаем направление взгляда существа
        Vec3 lookDirection = entity.getLookAngle();

        // Инвертируем направление (противоположная сторона)
        Vec3 fleeDirection = new Vec3(-lookDirection.x, 0, -lookDirection.z).normalize();

        // Применяем движение в противоположную сторону
        Vec3 movement = fleeDirection.scale(0.15); // Скорость бега

        // Сохраняем вертикальную скорость (чтобы не ломать прыжки/падения)
        entity.setDeltaMovement(
                movement.x,
                entity.getDeltaMovement().y,
                movement.z
        );

        entity.hurtMarked = true;
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // Применяем каждый тик для плавного движения
        return true;
    }
}