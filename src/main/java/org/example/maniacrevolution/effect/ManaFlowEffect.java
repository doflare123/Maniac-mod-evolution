package org.example.maniacrevolution.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.example.maniacrevolution.mana.ManaProvider;

public class ManaFlowEffect extends MobEffect {

    public static final float BONUS_REGEN_PER_LEVEL = 2.0f; // +2 маны/сек за уровень

    public ManaFlowEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x00DDFF); // Голубой цвет
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity instanceof Player player && !player.level().isClientSide()) {
            player.getCapability(ManaProvider.MANA).ifPresent(mana -> {
                // Устанавливаем бонусный реген
                float bonusRegen = BONUS_REGEN_PER_LEVEL * (amplifier + 1);
                mana.setBonusRegenRate(bonusRegen);
            });
        }
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap attributeMap, int amplifier) {
        // Этот метод вызывается когда эффект заканчивается
        if (entity instanceof Player player && !player.level().isClientSide()) {
            player.getCapability(ManaProvider.MANA).ifPresent(mana -> {
                // Сбрасываем бонусный реген
                mana.setBonusRegenRate(0.0f);
            });
        }
        super.removeAttributeModifiers(entity, attributeMap, amplifier);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true; // Применяется каждый тик
    }
}