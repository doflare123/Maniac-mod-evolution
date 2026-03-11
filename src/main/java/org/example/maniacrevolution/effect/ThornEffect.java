package org.example.maniacrevolution.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class ThornEffect extends MobEffect {

    public static final float REFLECT_PERCENT = 0.5f; // 50% отражения

    public ThornEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFFFFF);
    }

    // Белые частицы по телу каждый тик
    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) return;
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;

        // Случайные точки по телу игрока
        for (int i = 0; i < 3; i++) {
            double px = entity.getX() + (entity.level().random.nextDouble() - 0.5) * 0.6;
            double py = entity.getY() + entity.level().random.nextDouble() * 1.8;
            double pz = entity.getZ() + (entity.level().random.nextDouble() - 0.5) * 0.6;

            serverLevel.sendParticles(
                    ParticleTypes.SNOWFLAKE,
                    px, py, pz,
                    1, 0, 0.02, 0, 0.01
            );
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // Каждые 4 тика (~5 раз в секунду) — плавный эффект частиц
        return duration % 4 == 0;
    }

    @Override
    public boolean isInstantenous() {
        return false;
    }

    // Отражение урона — обрабатывается здесь через ивент
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        if (!victim.hasEffect(ModEffects.THORN.get())) return;

        // Атакующий должен быть живой сущностью
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        // Не отражаем урон от самого себя
        if (attacker == victim) return;

        float reflected = event.getAmount() * REFLECT_PERCENT;
        if (reflected <= 0) return;

        // Наносим отражённый урон — не отменяем оригинальный
        attacker.hurt(
                victim.level().damageSources().thorns(victim),
                reflected
        );

        // Частицы у атакующего при отражении
        if (attacker.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.SNOWFLAKE,
                    attacker.getX(), attacker.getY() + 1.0, attacker.getZ(),
                    12, 0.3, 0.4, 0.3, 0.05
            );
        }
    }
}
