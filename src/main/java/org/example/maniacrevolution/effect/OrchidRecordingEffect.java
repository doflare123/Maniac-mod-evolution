package org.example.maniacrevolution.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Отображаемое состояние записи маршрута Розовой Орхидеи. */
public final class OrchidRecordingEffect extends MobEffect {
    private static final int EFFECT_COLOR = 0xFF2FB9;

    public OrchidRecordingEffect() {
        super(MobEffectCategory.BENEFICIAL, EFFECT_COLOR);
    }
}
