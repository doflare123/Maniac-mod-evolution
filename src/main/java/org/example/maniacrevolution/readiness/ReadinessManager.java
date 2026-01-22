package org.example.maniacrevolution.readiness;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
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
    private static boolean waitingForMap = false; // Флаг: ждём выбора карты

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
            waitingForMap = false;
            Maniacrev.LOGGER.info("All players readiness reset");
        } else {
            // Сбросить конкретного игрока
            readyPlayers.put(player.getUUID(), false);
            checkAllPlayersReady(player.getServer());
            Maniacrev.LOGGER.info("Player {} readiness reset", player.getName().getString());
        }
    }

    /**
     * Вызывается каждый тик для проверки карты (только когда ждём карту)
     */
    public static void tick(MinecraftServer minecraftServer) {
        if (minecraftServer == null || !waitingForMap) return;

        // Если ждём карту и карта появилась
        if (isMapSelected(minecraftServer)) {
            waitingForMap = false;
            Maniacrev.LOGGER.info("Map selected! Starting countdown...");
            startCountdown(minecraftServer);
        }
    }

    /**
     * Проверить, готовы ли все игроки
     */
    private static void checkAllPlayersReady(MinecraftServer server) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();

        if (players.isEmpty()) {
            cancelCountdown();
            waitingForMap = false;
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
            // Все нажали готово - проверяем карту
            if (isMapSelected(server)) {
                // Карта уже выбрана - запускаем отсчёт сразу
                startCountdown(server);
                waitingForMap = false;
            } else {
                // Карты ещё нет - ждём её выбора каждый тик
                waitingForMap = true;
                cancelCountdown();
                broadcastMessage("§eИгра начнётся, когда будет выбрана карта");
                Maniacrev.LOGGER.info("All players ready but map not selected - waiting for map...");
            }
        } else {
            // Не все готовы - отменяем отсчёт и перестаём ждать карту
            cancelCountdown();
            waitingForMap = false;
        }
    }

    /**
     * Проверить: выбрана ли карта
     * Условие: scoreboard Game (псевдоигрок) map != 0 означает что карта выбрана
     */
    private static boolean isMapSelected(MinecraftServer server) {
        try {
            Scoreboard scoreboard = server.getScoreboard();
            Objective mapObjective = scoreboard.getObjective("map");

            if (mapObjective == null) {
                return false; // Нет objective - карта не выбрана
            }

            Score mapScore = scoreboard.getOrCreatePlayerScore("Game", mapObjective);
            return mapScore.getScore() != 0; // map != 0 означает выбрана
        } catch (Exception e) {
            Maniacrev.LOGGER.error("Error checking map selection", e);
            return false;
        }
    }

    /**
     * Начать 5-секундный отсчёт
     */
    private static void startCountdown(MinecraftServer server) {
        if (countdownTask != null && countdownTask.isRunning()) {
            return; // Отсчёт уже идёт
        }

        countdownTask = new CountdownTask(server);
        countdownTask.start();

        Maniacrev.LOGGER.info("Countdown started - all players ready and map selected!");
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
        waitingForMap = false;
    }

    /**
     * Отправить сообщение всем игрокам
     */
    private static void broadcastMessage(String message) {
        if (server == null) return;

        server.execute(() -> {
            net.minecraft.network.chat.Component component =
                    net.minecraft.network.chat.Component.literal(message);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.sendSystemMessage(component);
            }
        });
    }
}