package org.example.maniacrevolution.item.armor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import org.example.maniacrevolution.item.IItemWithAbility;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.SyncAbilityCooldownPacket;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Менеджер кулдаунов для способностей брони
 */
public class ArmorAbilityCooldownManager {

    private static final String NBT_KEY_PREFIX = "ArmorAbilityCooldown_";
    private static final String NBT_KEY_TIMESTAMP_PREFIX = "ArmorAbilityCooldownTimestamp_"; // НОВОЕ

    // Кэш кулдаунов на сервере (для оптимизации)
    private static final Map<UUID, Map<Item, CooldownData>> cooldownCache = new HashMap<>();

    // НОВОЕ: Класс для хранения данных кулдауна
    private static class CooldownData {
        long timestamp; // Время в миллисекундах когда был установлен кулдаун
        int durationTicks; // Длительность кулдауна в тиках

        CooldownData(long timestamp, int durationTicks) {
            this.timestamp = timestamp;
            this.durationTicks = durationTicks;
        }

        // Получить оставшееся время в тиках
        int getRemaining() {
            long currentTime = System.currentTimeMillis();
            long elapsed = currentTime - timestamp;
            long elapsedTicks = elapsed / 50; // 50 мс = 1 тик

            return Math.max(0, (int)(durationTicks - elapsedTicks));
        }

        boolean isExpired() {
            return getRemaining() <= 0;
        }
    }

    /**
     * Проверить, на кулдауне ли способность
     */
    public static boolean isOnCooldown(ServerPlayer player, Item armorItem) {
        CooldownData data = getCooldownData(player, armorItem);
        return data != null && !data.isExpired();
    }

    /**
     * Получить оставшееся время кулдауна в тиках
     */
    public static int getRemainingCooldown(ServerPlayer player, Item armorItem) {
        CooldownData data = getCooldownData(player, armorItem);
        return data != null ? data.getRemaining() : 0;
    }

    /**
     * Установить кулдаун
     */
    public static void setCooldown(ServerPlayer player, Item armorItem, int cooldownTicks) {
        long currentTime = System.currentTimeMillis();
        CooldownData data = new CooldownData(currentTime, cooldownTicks);

        // Сохраняем в NBT
        CompoundTag nbtData = player.getPersistentData();
        String timestampKey = NBT_KEY_TIMESTAMP_PREFIX + getItemKey(armorItem);
        String durationKey = NBT_KEY_PREFIX + getItemKey(armorItem);

        nbtData.putLong(timestampKey, currentTime);
        nbtData.putInt(durationKey, cooldownTicks);

        // Обновляем кэш
        cooldownCache
                .computeIfAbsent(player.getUUID(), k -> new HashMap<>())
                .put(armorItem, data);
    }

    /**
     * Очистить кулдаун
     */
    public static void clearCooldown(ServerPlayer player, Item armorItem) {
        CompoundTag data = player.getPersistentData();
        String timestampKey = NBT_KEY_TIMESTAMP_PREFIX + getItemKey(armorItem);
        String durationKey = NBT_KEY_PREFIX + getItemKey(armorItem);

        data.remove(timestampKey);
        data.remove(durationKey);

        Map<Item, CooldownData> playerCache = cooldownCache.get(player.getUUID());
        if (playerCache != null) {
            playerCache.remove(armorItem);
        }
    }

    /**
     * Очистить все кулдауны игрока (например, при смерти)
     */
    public static void clearAllCooldowns(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();

        // Удаляем все ключи с префиксом
        data.getAllKeys().removeIf(key ->
                key.startsWith(NBT_KEY_PREFIX) || key.startsWith(NBT_KEY_TIMESTAMP_PREFIX)
        );

        cooldownCache.remove(player.getUUID());
    }

    /**
     * ИСПРАВЛЕНО: Получить данные кулдауна
     */
    private static CooldownData getCooldownData(ServerPlayer player, Item armorItem) {
        // Проверяем кэш
        Map<Item, CooldownData> playerCache = cooldownCache.get(player.getUUID());
        if (playerCache != null && playerCache.containsKey(armorItem)) {
            CooldownData cached = playerCache.get(armorItem);
            // Если кулдаун истек, очищаем
            if (cached.isExpired()) {
                clearCooldown(player, armorItem);
                return null;
            }
            return cached;
        }

        // Читаем из NBT
        CompoundTag nbtData = player.getPersistentData();
        String timestampKey = NBT_KEY_TIMESTAMP_PREFIX + getItemKey(armorItem);
        String durationKey = NBT_KEY_PREFIX + getItemKey(armorItem);

        if (!nbtData.contains(timestampKey) || !nbtData.contains(durationKey)) {
            return null;
        }

        long timestamp = nbtData.getLong(timestampKey);
        int durationTicks = nbtData.getInt(durationKey);

        CooldownData data = new CooldownData(timestamp, durationTicks);

        // Проверяем не истек ли
        if (data.isExpired()) {
            clearCooldown(player, armorItem);
            return null;
        }

        // Обновляем кэш
        cooldownCache
                .computeIfAbsent(player.getUUID(), k -> new HashMap<>())
                .put(armorItem, data);

        return data;
    }

    /**
     * Получить уникальный ключ для предмета
     */
    private static String getItemKey(Item item) {
        return item.getDescriptionId().replace(".", "_");
    }

    /**
     * Очистка кэша при выходе игрока
     */
    public static void onPlayerLogout(ServerPlayer player) {
        cooldownCache.remove(player.getUUID());
    }

    /**
     * Синхронизировать кулдаун с клиентом
     */
    public static void syncToClient(ServerPlayer player, Item item, int remainingDuration) {
        int remaining = getRemainingCooldown(player, item);

        if (item instanceof IItemWithAbility ability) {
            int maxCooldown = ability.getMaxCooldownSeconds();
            ModNetworking.sendToPlayer(
                    new SyncAbilityCooldownPacket(item, remaining / 20, maxCooldown, remainingDuration),
                    player
            );
        }
    }
}