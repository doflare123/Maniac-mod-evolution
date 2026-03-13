package org.example.maniacrevolution.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Эффект "Альтруист.exe" — бонус +5% к скорости взлома.
 * Накладывается после подъёма союзника.
 * Пока эффект активен — HackSession учитывает бонус.
 */
public class AltruistBoostEffect extends MobEffect {

    public AltruistBoostEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x00FF88); // зелёный цвет
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false; // ничего не делаем каждый тик — логика в HackSession
    }

    @Override
    public boolean isInstantenous() {
        return false;
    }
}
