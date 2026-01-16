package org.example.maniacrevolution.system;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Система управления деньгами Агента 47
 * Хранит баланс каждого агента для магазина
 */
public class Agent47MoneyManager {

    // UUID игрока -> количество монет
    private static final Map<UUID, Integer> playerMoney = new ConcurrentHashMap<>();

    // NBT ключ для сохранения денег
    private static final String MONEY_NBT_KEY = "Agent47Money";

    /**
     * Получает баланс игрока
     */
    public static int getMoney(Player player) {
        return playerMoney.getOrDefault(player.getUUID(), 0);
    }

    /**
     * Устанавливает баланс игрока
     */
    public static void setMoney(Player player, int amount) {
        if (amount < 0) amount = 0;
        playerMoney.put(player.getUUID(), amount);
        saveMoney(player);
    }

    /**
     * Добавляет деньги игроку
     */
    public static void addMoney(Player player, int amount) {
        int current = getMoney(player);
        setMoney(player, current + amount);
    }

    /**
     * Убирает деньги у игрока
     */
    public static boolean removeMoney(Player player, int amount) {
        int current = getMoney(player);
        if (current < amount) return false;

        setMoney(player, current - amount);
        return true;
    }

    /**
     * Проверяет, достаточно ли денег у игрока
     */
    public static boolean hasMoney(Player player, int amount) {
        return getMoney(player) >= amount;
    }

    /**
     * Сохраняет деньги в NBT игрока
     */
    private static void saveMoney(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        CompoundTag persistentData = serverPlayer.getPersistentData();
        persistentData.putInt(MONEY_NBT_KEY, getMoney(player));
    }

    /**
     * Загружает деньги из NBT игрока
     */
    public static void loadMoney(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        CompoundTag persistentData = serverPlayer.getPersistentData();
        if (persistentData.contains(MONEY_NBT_KEY)) {
            int money = persistentData.getInt(MONEY_NBT_KEY);
            playerMoney.put(player.getUUID(), money);
        }
    }

    /**
     * Очищает деньги при выходе игрока (сохраняются в NBT)
     */
    public static void onPlayerLogout(UUID playerId) {
        // Не удаляем из мапы, т.к. данные сохранены в NBT
        // playerMoney.remove(playerId);
    }

    /**
     * Загружает деньги при входе игрока
     */
    public static void onPlayerLogin(ServerPlayer player) {
        loadMoney(player);
    }
}