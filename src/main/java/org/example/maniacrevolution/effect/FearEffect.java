package org.example.maniacrevolution.effect;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Эффект "Страх" - существо теряет контроль и бежит в противоположную сторону
 * от направления, в котором смотрело в момент наложения эффекта.
 * Для игроков дополнительно блокируется управление и поворачивается камера.
 */
public class FearEffect extends MobEffect {

    // Храним начальные направления для каждой сущности
    private static final Map<UUID, Vec3> fearDirections = new HashMap<>();

    public FearEffect() {
        super(MobEffectCategory.HARMFUL, 0x1a1a1a); // Тёмно-серый цвет

        // Добавляем скорость для эффекта паники
        this.addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                "7f8b42c4-9f5e-4e5a-8b3a-2c1f9d8e7a6b",
                0.3,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );
    }

    /**
     * Сохраняет направление взгляда существа в момент наложения эффекта.
     */
    public static void setFearDirection(LivingEntity entity) {
        Vec3 lookDirection = entity.getLookAngle();
        // Инвертируем и нормализуем (только горизонтальная составляющая)
        Vec3 fleeDirection = new Vec3(-lookDirection.x, 0, -lookDirection.z).normalize();
        fearDirections.put(entity.getUUID(), fleeDirection);
    }

    /**
     * Получает сохранённое направление бегства.
     */
    public static Vec3 getFearDirection(UUID entityUuid) {
        return fearDirections.get(entityUuid);
    }

    /**
     * Удаляет сохранённое направление после окончания эффекта.
     */
    public static void clearFearDirection(LivingEntity entity) {
        fearDirections.remove(entity.getUUID());
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Получаем сохранённое направление бегства
        Vec3 fleeDirection = fearDirections.get(entity.getUUID());

        if (fleeDirection == null) {
            // Если направление не сохранено, сохраняем текущее
            setFearDirection(entity);
            fleeDirection = fearDirections.get(entity.getUUID());
        }

        // Скорость бегства (увеличивается с уровнем эффекта)
        double speed = 0.15 + (amplifier * 0.05);

        // Для игроков применяем более агрессивное движение
        if (entity instanceof ServerPlayer player) {
            handlePlayerFear(player, fleeDirection, speed);
        } else {
            // Для мобов стандартное движение
            handleMobFear(entity, fleeDirection, speed);
        }
    }

    /**
     * Обработка страха для игроков.
     * Принудительно двигает игрока и поворачивает его.
     */
    private void handlePlayerFear(ServerPlayer player, Vec3 fleeDirection, double speed) {
        // Применяем движение
        Vec3 movement = fleeDirection.scale(speed);
        player.setDeltaMovement(
                movement.x,
                player.getDeltaMovement().y,
                movement.z
        );

        // Поворачиваем игрока в направлении бегства на сервере
        float yaw = (float) Math.toDegrees(Math.atan2(-fleeDirection.x, fleeDirection.z));
        player.setYRot(yaw);
        player.setXRot(0); // Смотрим прямо

        // Синхронизируем
        player.hurtMarked = true;
    }

    /**
     * Обработка страха для мобов.
     */
    private void handleMobFear(LivingEntity entity, Vec3 fleeDirection, double speed) {
        Vec3 movement = fleeDirection.scale(speed);

        // Сохраняем вертикальную скорость
        entity.setDeltaMovement(
                movement.x,
                entity.getDeltaMovement().y,
                movement.z
        );

        // Поворачиваем моба
        float yaw = (float) Math.toDegrees(Math.atan2(-fleeDirection.x, fleeDirection.z));
        entity.setYRot(yaw);
        entity.yRotO = yaw;

        entity.hurtMarked = true;
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // Применяем каждый тик для плавного движения
        return true;
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap attributes, int amplifier) {
        super.removeAttributeModifiers(entity, attributes, amplifier);
        // Очищаем направление при удалении эффекта
        clearFearDirection(entity);
    }
}