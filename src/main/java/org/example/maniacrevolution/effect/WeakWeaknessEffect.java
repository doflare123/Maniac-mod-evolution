package org.example.maniacrevolution.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class WeakWeaknessEffect extends MobEffect {
    public WeakWeaknessEffect() {
        super(MobEffectCategory.HARMFUL, 0x484D48);
        this.addAttributeModifier(Attributes.ATTACK_DAMAGE,
                "22653B89-116E-49DC-9B6B-9971489B5BE5",
                -2.0,
                AttributeModifier.Operation.ADDITION);
    }
}