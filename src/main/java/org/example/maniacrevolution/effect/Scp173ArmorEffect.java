package org.example.maniacrevolution.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Leather's 1+3+2+1 armor points, zero toughness; removable by milk/commands. */
public final class Scp173ArmorEffect extends MobEffect {
    public Scp173ArmorEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xC3B69D);
        addAttributeModifier(Attributes.ARMOR, "4e0d0c85-2f27-4b3f-b95c-d932b0e9df13", 7, AttributeModifier.Operation.ADDITION);
    }
    @Override public double getAttributeModifierValue(int amplifier, AttributeModifier modifier) { return modifier.getAmount(); }
}
