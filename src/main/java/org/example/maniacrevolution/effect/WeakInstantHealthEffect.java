package org.example.maniacrevolution.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class WeakInstantHealthEffect extends MobEffect {
    public WeakInstantHealthEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xF82423);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide) {
            entity.heal(1.0F);
        }
    }

    @Override
    public void applyInstantenousEffect(@Nullable net.minecraft.world.entity.Entity source,
                                        @Nullable net.minecraft.world.entity.Entity indirectSource,
                                        LivingEntity entity,
                                        int amplifier,
                                        double health) {
        if (!entity.level().isClientSide) {
            entity.heal(1.0F);
        }
    }

    @Override
    public boolean isInstantenous() {
        return true;
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}