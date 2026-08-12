package org.example.maniacrevolution.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Отображаемое состояние подготовки телепортации к незабудке. */
public final class ForgetMeNotCallEffect extends MobEffect {
    private static final int EFFECT_COLOR = 0x19CFFF;

    public ForgetMeNotCallEffect() {
        super(MobEffectCategory.BENEFICIAL, EFFECT_COLOR);
    }
}
