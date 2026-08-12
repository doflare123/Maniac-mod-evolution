package org.example.maniacrevolution.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Показывает время, в течение которого недавний удар маньяка блокирует телепортацию. */
public final class ShakenMemoryEffect extends MobEffect {
    private static final int EFFECT_COLOR = 0x3254B8;

    public ShakenMemoryEffect() {
        super(MobEffectCategory.HARMFUL, EFFECT_COLOR);
    }
}
