package org.example.maniacrevolution.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Display state for the active Rose-Colored Glasses protection. */
public final class RoseColoredGlassesEffect extends MobEffect {
    private static final int EFFECT_COLOR = 0xFF3FB4;

    public RoseColoredGlassesEffect() {
        super(MobEffectCategory.BENEFICIAL, EFFECT_COLOR);
    }
}
