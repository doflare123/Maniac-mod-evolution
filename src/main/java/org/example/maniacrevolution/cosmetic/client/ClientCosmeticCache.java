package org.example.maniacrevolution.cosmetic.client;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Кэш косметики других игроков на клиенте
 * Синхронизируется с сервера через пакеты
 */
@OnlyIn(Dist.CLIENT)
public class ClientCosmeticCache {

    // UUID игрока -> Список включённых косметических ID
    private static final Map<UUID, Set<String>> playerCosmetics = new ConcurrentHashMap<>();

    /**
     * Обновить косметику игрока
     */
    public static void updatePlayerCosmetics(UUID playerUuid, Set<String> enabledCosmetics) {
        if (enabledCosmetics.isEmpty()) {
            playerCosmetics.remove(playerUuid);
        } else {
            playerCosmetics.put(playerUuid, new HashSet<>(enabledCosmetics));
        }
    }

    /**
     * Проверить, включена ли косметика у игрока
     */
    public static boolean hasCosmetic(Player player, String cosmeticId) {
        Set<String> cosmetics = playerCosmetics.get(player.getUUID());
        return cosmetics != null && cosmetics.contains(cosmeticId);
    }

    /**
     * Получить все косметики игрока
     */
    public static Set<String> getPlayerCosmetics(UUID playerUuid) {
        return playerCosmetics.getOrDefault(playerUuid, Collections.emptySet());
    }

    /**
     * Удалить игрока из кэша (при выходе)
     */
    public static void removePlayer(UUID playerUuid) {
        playerCosmetics.remove(playerUuid);
    }

    /**
     * Очистить весь кэш
     */
    public static void clear() {
        playerCosmetics.clear();
    }
}