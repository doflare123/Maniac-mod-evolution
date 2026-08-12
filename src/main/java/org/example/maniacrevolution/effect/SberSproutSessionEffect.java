package org.example.maniacrevolution.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Отображаемое состояние активного сеанса СберРостка. */
public final class SberSproutSessionEffect extends MobEffect {
    private static final int EFFECT_COLOR = 0x20E89A;

    public SberSproutSessionEffect() {
        super(MobEffectCategory.BENEFICIAL, EFFECT_COLOR);
    }
}
