package org.example.maniacrevolution.readiness;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.example.maniacrevolution.Maniacrev;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Менеджер готовности игроков к началу игры
 */
public class ReadinessManager {
    private static final Map<UUID, Boolean> readyPlayers = new ConcurrentHashMap<>();
    private static CountdownTask countdownTask = null;
    private static MinecraftServer server = null;

    /**
     * Установить сервер для выполнения команд
     */
    public static void setServer(MinecraftServer serverInstance) {
        server = serverInstance;
    }

    /**
     * Установить статус готовности игрока
     */
    public static void setPlayerReady(ServerPlayer player, boolean ready) {
        UUID playerId = player.getUUID();

        if (ready) {
            readyPlayers.put(playerId, true);
        } else {
            readyPlayers.put(playerId, false);
        }

        checkAllPlayersReady(player.getServer());
    }

    /**
     * Проверить готовность конкретного игрока
     */
    public static boolean isPlayerReady(UUID playerId) {
        return readyPlayers.getOrDefault(playerId, false);
    }

    /**
     * Проверить готовность конкретного игрока
     */
    public static boolean isPlayerReady(Player player) {
        return isPlayerReady(player.getUUID());
    }

    /**
     * Сбросить готовность игрока или всех игроков
     */
    public static void resetReadiness(ServerPlayer player) {
        if (player == null) {
            // Сбросить всех
            readyPlayers.clear();
            cancelCountdown();
            Maniacrev.LOGGER.info("All players readiness reset");
        } else {
            // Сбросить конкретного игрока
            readyPlayers.put(player.getUUID(), false);
            checkAllPlayersReady(player.getServer());
            Maniacrev.LOGGER.info("Player {} readiness reset", player.getName().getString());
        }
    }

    /**
     * Проверить, готовы ли все игроки
     */
    private static void checkAllPlayersReady(MinecraftServer server) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();

        if (players.isEmpty()) {
            cancelCountdown();
            return;
        }

        boolean allReady = true;
        for (ServerPlayer player : players) {
            if (!isPlayerReady(player.getUUID())) {
                allReady = false;
                break;
            }
        }

        if (allReady) {
            startCountdown(server);
        } else {
            cancelCountdown();
        }
    }

    /**
     * Начать 10-секундный отсчёт
     */
    private static void startCountdown(MinecraftServer server) {
        if (countdownTask != null && countdownTask.isRunning()) {
            return; // Отсчёт уже идёт
        }

        countdownTask = new CountdownTask(server);
        countdownTask.start();

        Maniacrev.LOGGER.info("Countdown started - all players ready!");
    }

    /**
     * Отменить отсчёт
     */
    private static void cancelCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
            Maniacrev.LOGGER.info("Countdown cancelled");
        }
    }

    /**
     * Получить оставшееся время отсчёта
     */
    public static int getRemainingSeconds() {
        if (countdownTask != null && countdownTask.isRunning()) {
            return countdownTask.getRemainingSeconds();
        }
        return -1;
    }

    /**
     * Очистить все данные (вызывать при остановке сервера)
     */
    public static void clear() {
        readyPlayers.clear();
        cancelCountdown();
    }
}