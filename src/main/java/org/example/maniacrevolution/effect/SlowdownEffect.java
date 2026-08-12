package org.example.maniacrevolution.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** The displayed effect level is the exact movement-speed penalty in percent. */
public class SlowdownEffect extends MobEffect {
    public static final double SPEED_PENALTY_PER_DISPLAYED_LEVEL = -0.01D;

    private static final int EFFECT_COLOR = 0xFFD43B;
    private static final String SPEED_MODIFIER_ID = "be6a8056-9dbe-4f9d-bc8d-95dd17409643";

    public SlowdownEffect() {
        super(MobEffectCategory.HARMFUL, EFFECT_COLOR);
        addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                SPEED_MODIFIER_ID,
                SPEED_PENALTY_PER_DISPLAYED_LEVEL,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );
    }
}
