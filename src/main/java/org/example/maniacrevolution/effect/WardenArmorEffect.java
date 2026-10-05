package org.example.maniacrevolution.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Iron-set armor protection without occupying equipment slots or changing damage-event ordering. */
public final class WardenArmorEffect extends MobEffect {
    public static final double ARMOR = 15;
    public WardenArmorEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x176B71);
        addAttributeModifier(Attributes.ARMOR, "a8291ebf-ff8a-46e2-b730-2c0b3e93e584", ARMOR,
                AttributeModifier.Operation.ADDITION);
    }
    @Override public double getAttributeModifierValue(int amplifier, AttributeModifier modifier) {
        return modifier.getAmount(); // The class passive has a fixed strength, even with a command amplifier.
    }
}
