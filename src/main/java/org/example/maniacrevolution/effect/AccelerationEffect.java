package org.example.maniacrevolution.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Процентное ускорение: отображаемый уровень эффекта равен бонусу скорости.
 * Базовый модификатор 1% автоматически умножается Minecraft на amplifier + 1.
 */
public class AccelerationEffect extends MobEffect {
    public static final double SPEED_BONUS_PER_DISPLAYED_LEVEL = 0.01D;

    private static final int EFFECT_COLOR = 0x7CCBFF;
    private static final String SPEED_MODIFIER_ID = "30b4bf2d-2484-43fe-9f7c-d4bc72826d7c";

    public AccelerationEffect() {
        super(MobEffectCategory.BENEFICIAL, EFFECT_COLOR);
        addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                SPEED_MODIFIER_ID,
                SPEED_BONUS_PER_DISPLAYED_LEVEL,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );
    }
}
