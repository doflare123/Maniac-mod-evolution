package org.example.maniacrevolution.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Эффект "Тишина" — блокирует использование активных перков.
 * Проверяется в ActivatePerkPacket.
 */
public class SilenceEffect extends MobEffect {

    public SilenceEffect() {
        super(MobEffectCategory.HARMFUL, 0x1a1a2e); // тёмно-синий цвет
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false; // ничего не делаем каждый тик
    }

    @Override
    public boolean isInstantenous() {
        return false;
    }
}
