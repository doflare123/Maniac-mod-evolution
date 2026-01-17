package org.example.maniacrevolution.util;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Система отслеживания союзников для планшета медика
 * Показывает путь партиклами и автоматически отключается при достижении цели
 */
@Mod.EventBusSubscriber
public class MedicTabletTracker {

    private static class TrackingData {
        ServerPlayer medic;
        ServerPlayer target;
        long startTime;
        long endTime;
        boolean isActive;

        TrackingData(ServerPlayer medic, ServerPlayer target) {
            this.medic = medic;
            this.target = target;
            this.startTime = System.currentTimeMillis();
            this.endTime = startTime + 10000; // 10 секунд
            this.isActive = true;
        }
    }

    private static final Map<UUID, TrackingData> activeTracking = new HashMap<>();
    private static final double REACH_DISTANCE = 3.0; // Расстояние достижения цели

    /**
     * Начинает отслеживание цели
     */
    public static void startTracking(ServerPlayer medic, ServerPlayer target) {
        activeTracking.put(medic.getUUID(), new TrackingData(medic, target));
    }

    /**
     * Останавливает отслеживание для медика
     */
    public static void stopTracking(ServerPlayer medic) {
        TrackingData data = activeTracking.remove(medic.getUUID());
        if (data != null) {
            // Убираем подсветку
            SelectiveGlowingEffect.removeGlowing(data.target, medic);
        }
    }

    /**
     * Проверяет, активно ли отслеживание
     */
    public static boolean isTracking(ServerPlayer medic) {
        return activeTracking.containsKey(medic.getUUID());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, TrackingData>> iterator = activeTracking.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, TrackingData> entry = iterator.next();
            TrackingData data = entry.getValue();

            // Проверяем валидность игроков
            if (!isPlayerValid(data.medic) || !isPlayerValid(data.target)) {
                iterator.remove();
                continue;
            }

            // Проверяем время
            if (currentTime >= data.endTime) {
                // Время истекло
                data.medic.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§eОтслеживание завершено"),
                        true
                );
                SelectiveGlowingEffect.removeGlowing(data.target, data.medic);
                iterator.remove();
                continue;
            }

            // Проверяем расстояние
            double distance = data.medic.distanceTo(data.target);
            if (distance <= REACH_DISTANCE) {
                // Медик достиг цели
                data.medic.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§aВы достигли союзника!"),
                        true
                );
                data.medic.level().playSound(null, data.medic.blockPosition(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.5F);
                SelectiveGlowingEffect.removeGlowing(data.target, data.medic);
                iterator.remove();
            }
        }
    }

    /**
     * Отображает путь из партиклов от медика к цели
     */
    private static void showParticlePath(ServerPlayer medic, ServerPlayer target) {
        if (!(medic.level() instanceof ServerLevel serverLevel)) return;

        Vec3 medicPos = medic.position().add(0, 0.5, 0);
        Vec3 targetPos = target.position().add(0, 0.5, 0);

        // Направление к цели
        Vec3 direction = targetPos.subtract(medicPos).normalize();
        double distance = medicPos.distanceTo(targetPos);

        // Рисуем партиклы каждые 0.5 блока
        int particleCount = Math.min((int) (distance / 0.5), 40); // Максимум 40 партиклов

        for (int i = 0; i < particleCount; i++) {
            double progress = (i + 1) / (double) particleCount;
            Vec3 particlePos = medicPos.add(direction.scale(distance * progress));

            // Используем разные партиклы в зависимости от расстояния
            if (progress < 0.3) {
                // Ближние партиклы - зеленые
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0.1, 0.1, 0.1, 0.01);
            } else if (progress < 0.7) {
                // Средние партиклы - желтые
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0.05, 0.05, 0.05, 0.01);
            } else {
                // Дальние партиклы - красные (возле цели)
                serverLevel.sendParticles(ParticleTypes.FLAME,
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0.05, 0.05, 0.05, 0.01);
            }
        }

        // Дополнительные партиклы вокруг цели
        double angle = (System.currentTimeMillis() % 2000) / 2000.0 * Math.PI * 2;
        for (int i = 0; i < 4; i++) {
            double currentAngle = angle + (i * Math.PI / 2);
            double offsetX = Math.cos(currentAngle) * 0.5;
            double offsetZ = Math.sin(currentAngle) * 0.5;

            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    targetPos.x + offsetX, targetPos.y + 1.0, targetPos.z + offsetZ,
                    1, 0, 0, 0, 0);
        }
    }

    /**
     * Проверяет валидность игрока
     */
    private static boolean isPlayerValid(ServerPlayer player) {
        return player != null && !player.isRemoved() && player.isAlive();
    }

    /**
     * Очистка при выходе игрока
     */
    public static void onPlayerLogout(ServerPlayer player) {
        // Удаляем отслеживание, если игрок был медиком
        TrackingData data = activeTracking.remove(player.getUUID());
        if (data != null && data.target != null) {
            SelectiveGlowingEffect.removeGlowing(data.target, player);
        }

        // Удаляем отслеживание, если игрок был целью
        activeTracking.entrySet().removeIf(entry -> {
            if (entry.getValue().target.getUUID().equals(player.getUUID())) {
                SelectiveGlowingEffect.removeGlowing(player, entry.getValue().medic);
                return true;
            }
            return false;
        });
    }
}