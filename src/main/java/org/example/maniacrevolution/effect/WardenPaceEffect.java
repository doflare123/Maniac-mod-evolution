package org.example.maniacrevolution.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Separate UUIDs keep class passives independent from potion/perk speed effects. */
public final class WardenPaceEffect extends MobEffect {
    public WardenPaceEffect(boolean boost) {
        super(boost ? MobEffectCategory.BENEFICIAL : MobEffectCategory.HARMFUL, boost ? 0x39CFC3 : 0x769397);
        addAttributeModifier(Attributes.MOVEMENT_SPEED,
                boost ? "cc9c819b-27a6-40f4-9470-d7178e6ad9c6" : "dfacdbb3-1e92-49a9-a172-dd9a5bc3f481",
                boost ? 0.01 : -0.01, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}
