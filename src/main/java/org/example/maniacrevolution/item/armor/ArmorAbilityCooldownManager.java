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

    // Кэш кулдаунов на сервере (для оптимизации)
    private static final Map<UUID, Map<Item, Long>> cooldownCache = new HashMap<>();

    /**
     * Проверить, на кулдауне ли способность
     */
    public static boolean isOnCooldown(ServerPlayer player, Item armorItem) {
        long expiryTime = getCooldownExpiry(player, armorItem);
        long currentTime = player.getServer().getTickCount();

        return currentTime < expiryTime;
    }

    /**
     * Получить оставшееся время кулдауна в тиках
     */
    public static int getRemainingCooldown(ServerPlayer player, Item armorItem) {
        long expiryTime = getCooldownExpiry(player, armorItem);
        long currentTime = player.getServer().getTickCount();

        return Math.max(0, (int)(expiryTime - currentTime));
    }

    /**
     * Установить кулдаун
     */
    public static void setCooldown(ServerPlayer player, Item armorItem, int cooldownTicks) {
        long expiryTime = player.getServer().getTickCount() + cooldownTicks;

        // Сохраняем в NBT
        CompoundTag data = player.getPersistentData();
        String key = NBT_KEY_PREFIX + getItemKey(armorItem);
        data.putLong(key, expiryTime);

        // Обновляем кэш
        cooldownCache
                .computeIfAbsent(player.getUUID(), k -> new HashMap<>())
                .put(armorItem, expiryTime);
    }

    /**
     * Очистить кулдаун
     */
    public static void clearCooldown(ServerPlayer player, Item armorItem) {
        CompoundTag data = player.getPersistentData();
        String key = NBT_KEY_PREFIX + getItemKey(armorItem);
        data.remove(key);

        cooldownCache
                .computeIfAbsent(player.getUUID(), k -> new HashMap<>())
                .remove(armorItem);
    }

    /**
     * Очистить все кулдауны игрока (например, при смерти)
     */
    public static void clearAllCooldowns(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();

        // Удаляем все ключи с префиксом
        data.getAllKeys().removeIf(key -> key.startsWith(NBT_KEY_PREFIX));

        cooldownCache.remove(player.getUUID());
    }

    /**
     * Получить время истечения кулдауна
     */
    private static long getCooldownExpiry(ServerPlayer player, Item armorItem) {
        // Проверяем кэш
        Map<Item, Long> playerCache = cooldownCache.get(player.getUUID());
        if (playerCache != null && playerCache.containsKey(armorItem)) {
            return playerCache.get(armorItem);
        }

        // Читаем из NBT
        CompoundTag data = player.getPersistentData();
        String key = NBT_KEY_PREFIX + getItemKey(armorItem);
        long expiry = data.getLong(key);

        // Обновляем кэш
        cooldownCache
                .computeIfAbsent(player.getUUID(), k -> new HashMap<>())
                .put(armorItem, expiry);

        return expiry;
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