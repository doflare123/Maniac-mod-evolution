package org.example.maniacrevolution.util;

import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Утилита для выборочной подсветки любых сущностей.
 * Позволяет подсвечивать мобов/игроков только для определенных наблюдателей.
 */
@Mod.EventBusSubscriber
public class SelectiveGlowingEffect {

    // Структура: целевая сущность -> (наблюдатель -> время окончания подсветки)
    private static final Map<Integer, Map<UUID, Long>> glowingTargets = new ConcurrentHashMap<>();

    // Счётчик для периодического обновления пакетов (каждые 10 тиков = 0.5 сек)
    private static int tickCounter = 0;
    private static final int UPDATE_INTERVAL = 1;

    // Флаг подсветки в битовой маске (6-й бит = 0x40)
    private static final byte GLOWING_FLAG = 0x40;

    /**
     * Добавляет подсветку для конкретного наблюдателя
     *
     * @param target Кого подсвечивать (любая Entity)
     * @param viewer Кто видит подсветку
     * @param durationTicks Длительность в тиках (20 тиков = 1 секунда)
     */
    public static void addGlowing(Entity target, ServerPlayer viewer, int durationTicks) {
        if (target == null || viewer == null) return;

        long endTime = System.currentTimeMillis() + (durationTicks * 50L); // 50ms = 1 tick

        glowingTargets
                .computeIfAbsent(target.getId(), k -> new ConcurrentHashMap<>())
                .put(viewer.getUUID(), endTime);

        // Отправляем пакет с флагом подсветки
        sendGlowingPacket(target, viewer, true);
    }

    /**
     * Добавляет подсветку для нескольких наблюдателей
     */
    public static void addGlowing(Entity target, List<ServerPlayer> viewers, int durationTicks) {
        for (ServerPlayer viewer : viewers) {
            addGlowing(target, viewer, durationTicks);
        }
    }

    /**
     * Добавляет подсветку нескольких целей для одного наблюдателя
     */
    public static void addGlowingMultiple(List<? extends Entity> targets, ServerPlayer viewer, int durationTicks) {
        for (Entity target : targets) {
            addGlowing(target, viewer, durationTicks);
        }
    }

    /**
     * Убирает подсветку для конкретного наблюдателя
     */
    public static void removeGlowing(Entity target, ServerPlayer viewer) {
        if (target == null || viewer == null) return;

        Map<UUID, Long> viewers = glowingTargets.get(target.getId());
        if (viewers != null) {
            viewers.remove(viewer.getUUID());
            if (viewers.isEmpty()) {
                glowingTargets.remove(target.getId());
            }
        }

        sendGlowingPacket(target, viewer, false);
    }

    /**
     * Убирает все подсветки с сущности
     */
    public static void removeAllGlowing(Entity target) {
        if (target == null || target.level().isClientSide()) return;

        Map<UUID, Long> viewers = glowingTargets.remove(target.getId());
        if (viewers != null && !target.level().isClientSide()) {
            for (UUID viewerUUID : viewers.keySet()) {
                ServerPlayer viewer = target.getServer().getPlayerList().getPlayer(viewerUUID);
                if (viewer != null) {
                    sendGlowingPacket(target, viewer, false);
                }
            }
        }
    }

    /**
     * Проверяет, подсвечена ли сущность для конкретного наблюдателя
     */
    public static boolean isGlowing(Entity target, ServerPlayer viewer) {
        Map<UUID, Long> viewers = glowingTargets.get(target.getId());
        if (viewers == null) return false;

        Long endTime = viewers.get(viewer.getUUID());
        return endTime != null && System.currentTimeMillis() < endTime;
    }

    /**
     * Отправляет пакет с изменением флага подсветки
     * Использует прямой доступ к EntityData без рефлексии
     */
    private static void sendGlowingPacket(Entity target, ServerPlayer viewer, boolean glowing) {
        try {
            // Получаем флаги напрямую через публичный метод
            boolean wasGlowing = target.isCurrentlyGlowing();

            // Временно устанавливаем флаг подсветки
            target.setGlowingTag(glowing);

            // Получаем данные и отправляем пакет
            SynchedEntityData entityData = target.getEntityData();
            viewer.connection.send(new ClientboundSetEntityDataPacket(
                    target.getId(),
                    entityData.getNonDefaultValues()
            ));

            // Восстанавливаем оригинальное состояние
            target.setGlowingTag(wasGlowing);

        } catch (Exception e) {
            System.err.println("Error sending glowing packet: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Обработчик тиков для автоматического снятия подсветки и периодического обновления
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        tickCounter++;
        long currentTime = System.currentTimeMillis();
        List<Integer> toRemove = new ArrayList<>();

        // Периодически обновляем пакеты (каждые 10 тиков)
        boolean shouldUpdate = (tickCounter % UPDATE_INTERVAL == 0);

        for (Map.Entry<Integer, Map<UUID, Long>> entry : glowingTargets.entrySet()) {
            Integer targetId = entry.getKey();
            Map<UUID, Long> viewers = entry.getValue();

            // Ищем сущность по ID во всех мирах
            Entity target = null;
            for (var level : event.getServer().getAllLevels()) {
                target = level.getEntity(targetId);
                if (target != null) break;
            }

            if (target == null) {
                // Сущность не найдена - удаляем запись
                toRemove.add(targetId);
                continue;
            }

            Entity finalTarget = target;

            // Проверяем каждого наблюдателя
            viewers.entrySet().removeIf(viewerEntry -> {
                ServerPlayer viewer = event.getServer().getPlayerList().getPlayer(viewerEntry.getKey());

                if (viewer == null) {
                    // Игрок не найден - удаляем запись
                    return true;
                }

                if (currentTime >= viewerEntry.getValue()) {
                    // Время истекло - убираем подсветку
                    sendGlowingPacket(finalTarget, viewer, false);
                    return true;
                }

                // Периодически обновляем пакет для поддержания подсветки
                if (shouldUpdate) {
                    sendGlowingPacket(finalTarget, viewer, true);
                }

                return false;
            });

            // Если у цели не осталось наблюдателей, помечаем её для удаления
            if (viewers.isEmpty()) {
                toRemove.add(targetId);
            }
        }

        // Удаляем пустые записи
        toRemove.forEach(glowingTargets::remove);
    }

    /**
     * Очистка при выходе игрока
     */
    public static void onPlayerLogout(ServerPlayer player) {
        // Убираем все подсветки с этого игрока (если он сущность)
        removeAllGlowing(player);

        // Убираем этого игрока из всех наблюдателей
        for (Map<UUID, Long> viewers : glowingTargets.values()) {
            viewers.remove(player.getUUID());
        }
    }
}