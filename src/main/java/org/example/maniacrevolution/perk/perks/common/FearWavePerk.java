package org.example.maniacrevolution.perk.perks.common;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.example.maniacrevolution.effect.FearEffect;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.FearDirectionPacket;
import org.example.maniacrevolution.perk.*;
import org.joml.Vector3f;

import java.util.List;

/**
 * Активный перк: волна страха.
 * Создаёт расширяющуюся волну тёмно-зелёных частиц радиусом 5 блоков,
 * которая накладывает эффект страха на всех существ кроме кастера на 5 секунд.
 */
public class FearWavePerk extends Perk {

    private static final double RADIUS = 5.0;
    private static final int FEAR_DURATION = 5 * 20; // 5 секунд в тиках
    private static final int WAVE_ANIMATION_TICKS = 20; // Длительность анимации волны

    // Тёмно-зелёный цвет (близкий к чёрному)
    private static final DustParticleOptions PARTICLE_COLOR = new DustParticleOptions(
            new Vector3f(0.05f, 0.15f, 0.05f), // RGB: тёмно-зелёный
            1.0f // Размер частицы
    );

    public FearWavePerk() {
        super(new Builder("fear_wave")
                .type(PerkType.ACTIVE)
                .team(PerkTeam.ALL)
                .phases(PerkPhase.ANY)
                .cooldown(60)); // 60 секунд КД
    }

    @Override
    public void onActivate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        Vec3 center = player.position();

        // Звук активации
        serverLevel.playSound(
                null,
                center.x, center.y, center.z,
                SoundEvents.WITHER_SPAWN,
                SoundSource.PLAYERS,
                1.0F,
                0.5F // Низкий тон
        );

        // Запускаем анимацию волны и применение эффекта
        spawnFearWave(serverLevel, player, center);
        applyFearEffect(serverLevel, player, center);
    }

    /**
     * Создаёт визуальную волну частиц, расширяющуюся от центра.
     */
    private void spawnFearWave(ServerLevel level, ServerPlayer caster, Vec3 center) {
        // Создаём волну, которая расширяется со временем
        new Thread(() -> {
            try {
                for (int tick = 0; tick <= WAVE_ANIMATION_TICKS; tick++) {
                    final double currentRadius = (RADIUS / WAVE_ANIMATION_TICKS) * tick;
                    final int finalTick = tick;

                    // Выполняем в главном потоке сервера
                    level.getServer().execute(() -> {
                        spawnWaveRing(level, center, currentRadius);

                        // Звук распространения волны
                        if (finalTick % 5 == 0) {
                            level.playSound(
                                    null,
                                    center.x, center.y, center.z,
                                    SoundEvents.SOUL_ESCAPE,
                                    SoundSource.PLAYERS,
                                    0.3F,
                                    0.8F
                            );
                        }
                    });

                    Thread.sleep(50); // 50мс между кадрами анимации
                }
            } catch (Exception ignored) {}
        }).start();
    }

    /**
     * Создаёт кольцо частиц на определённом радиусе.
     */
    private void spawnWaveRing(ServerLevel level, Vec3 center, double radius) {
        int particleCount = (int) (radius * 40); // Больше частиц на больших радиусах

        for (int i = 0; i < particleCount; i++) {
            double angle = (2 * Math.PI * i) / particleCount;
            double x = center.x + radius * Math.cos(angle);
            double z = center.z + radius * Math.sin(angle);
            double y = center.y + 0.5;

            // Основная волна
            level.sendParticles(
                    PARTICLE_COLOR,
                    x, y, z,
                    2, // Количество частиц в точке
                    0.1, 0.3, 0.1, // Разброс
                    0.02 // Скорость
            );

            // Дополнительные частицы выше для объёма
            level.sendParticles(
                    PARTICLE_COLOR,
                    x, y + 1.0, z,
                    1,
                    0.1, 0.2, 0.1,
                    0.01
            );
        }
    }

    /**
     * Применяет эффект страха ко всем существам в радиусе (кроме кастера).
     */
    private void applyFearEffect(ServerLevel level, ServerPlayer caster, Vec3 center) {
        AABB searchBox = new AABB(center, center).inflate(RADIUS);
        List<LivingEntity> entities = level.getEntitiesOfClass(
                LivingEntity.class,
                searchBox,
                entity -> entity != caster && entity.distanceToSqr(center) <= RADIUS * RADIUS
        );

        for (LivingEntity entity : entities) {
            // Сохраняем направление взгляда ДО наложения эффекта
            FearEffect.setFearDirection(entity);

            // Если это игрок, отправляем пакет с направлением на клиент
            if (entity instanceof ServerPlayer serverPlayer) {
                Vec3 fleeDirection = FearEffect.getFearDirection(entity.getUUID());
                if (fleeDirection != null) {
                    ModNetworking.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> serverPlayer),
                            new FearDirectionPacket(fleeDirection)
                    );
                }
            }

            // Накладываем эффект страха
            entity.addEffect(new MobEffectInstance(
                    ModEffects.FEAR.get(),
                    FEAR_DURATION,
                    0, // Уровень 1
                    false,
                    true, // Видимые частицы
                    true  // Иконка
            ));

            // Визуальный эффект наложения страха
            level.sendParticles(
                    PARTICLE_COLOR,
                    entity.getX(),
                    entity.getY() + entity.getBbHeight() / 2,
                    entity.getZ(),
                    20,
                    0.3, 0.5, 0.3,
                    0.1
            );

            // Звук испуга для жертвы
            level.playSound(
                    null,
                    entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.GHAST_SCREAM,
                    SoundSource.HOSTILE,
                    0.5F,
                    1.5F
            );
        }
    }
}