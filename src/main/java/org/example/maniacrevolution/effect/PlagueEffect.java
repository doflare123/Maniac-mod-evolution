package org.example.maniacrevolution.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;

/**
 * Эффект чумы — накопительный.
 * Сам по себе эффект НЕ наносит урон при каждом тике.
 * Урон наносится через PlagueLanternEventHandler, который отслеживает
 * суммарное накопленное время в PlagueCapability.
 *
 * Эффект накладывается снаружи на 1 секунду (20 тиков) и постоянно
 * обновляется лампой. PlagueCapability считает реальное суммарное время.
 */
public class PlagueEffect extends MobEffect {

    public PlagueEffect() {
        super(MobEffectCategory.HARMFUL, 0x2D5A1B); // тёмно-зелёный цвет частиц
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Логика накопления и урона обрабатывается в PlagueLanternEventHandler
        // через Capability, а не здесь — чтобы работало суммарное время
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // Возвращаем false — не хотим вызывать applyEffectTick каждый тик
        return false;
    }
}