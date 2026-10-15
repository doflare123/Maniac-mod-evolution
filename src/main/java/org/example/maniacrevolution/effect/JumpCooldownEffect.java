package org.example.maniacrevolution.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Shared player jump restriction. Duration is the cooldown clock; not tied to one class. */
public final class JumpCooldownEffect extends MobEffect {
    public JumpCooldownEffect() { super(MobEffectCategory.NEUTRAL, 0xC4AE7D); }
}
