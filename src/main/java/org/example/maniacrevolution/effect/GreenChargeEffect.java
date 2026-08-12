package org.example.maniacrevolution.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Amplifier + 1 is the number of stored green-card damage charges. */
public final class GreenChargeEffect extends MobEffect {
    public GreenChargeEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x49E34D);
    }
}
