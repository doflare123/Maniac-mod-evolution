package org.example.maniacrevolution.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class PaintProtectionEffect extends MobEffect {
    private static final int EFFECT_COLOR = 0xFF55D8;

    public PaintProtectionEffect() {
        super(MobEffectCategory.BENEFICIAL, EFFECT_COLOR);
    }
}
