package org.example.maniacrevolution.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Короткое отображаемое состояние, через которое запускается общая механика крика.
 */
public class ScreamEffect extends MobEffect {
    private static final int EFFECT_COLOR = 0xD93636;

    public ScreamEffect() {
        super(MobEffectCategory.NEUTRAL, EFFECT_COLOR);
    }
}
