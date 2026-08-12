package org.example.maniacrevolution.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Хранит силу собранного букета: отображаемый уровень I–III равен числу цветов.
 */
public class BouquetEffect extends MobEffect {
    private static final int EFFECT_COLOR = 0xFF4F93;

    public BouquetEffect() {
        super(MobEffectCategory.BENEFICIAL, EFFECT_COLOR);
    }
}
