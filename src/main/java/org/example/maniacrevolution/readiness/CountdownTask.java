package org.example.maniacrevolution.readiness;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import org.example.maniacrevolution.Maniacrev;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Задача для 5-секундного отсчёта
 * Перед началом проверяет, что карта выбрана
 */
public class CountdownTask {
    private final MinecraftServer server;
    private Timer timer;
    private int remainingSeconds;
    private boolean running;

    public CountdownTask(MinecraftServer server) {
        this.server = server;
        this.remainingSeconds = 5;
        this.running = false;
    }

    public void start() {
        if (running) return;

        running = true;
        timer = new Timer();

        broadcastMessage("§aИгра начнётся через 5 секунд...");

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (remainingSeconds > 0) {
                    // Показываем title с отсчётом
                    showCountdownTitle(remainingSeconds);
                    remainingSeconds--;
                } else {
                    finish();
                }
            }
        }, 1000, 1000);
    }

    private void showCountdownTitle(int seconds) {
        server.execute(() -> {
            String color = seconds <= 3 ? "§c" : "§e";
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(
                        10, 20, 10
                ));
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
                        Component.literal(color + "§l" + seconds)
                ));
            }
        });
    }

    public void cancel() {
        if (!running) return;

        running = false;
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        broadcastMessage("§cОтсчёт отменён - не все игроки готовы");
    }

    private void finish() {
        running = false;
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        // Финальная проверка: карта должна быть выбрана при завершении отсчёта
        if (!isMapSelected()) {
            broadcastMessage("§cИгра отменена - карта больше не выбрана!");
            return;
        }

        broadcastMessage("§6Игра начинается!");

        // Выполняем команду для начала игры
        server.execute(() -> {
            try {
                server.getCommands().performPrefixedCommand(
                        server.createCommandSourceStack(),
                        "function maniac:game/play_game"
                );

                // Сбрасываем готовность всех игроков для следующей игры
                org.example.maniacrevolution.readiness.ReadinessManager.resetReadiness(null);

                Maniacrev.LOGGER.info("Game started via countdown completion");
            } catch (Exception e) {
                Maniacrev.LOGGER.error("Failed to start game", e);
            }
        });
    }

    /**
     * Проверка: выбрана ли карта на сервере
     * Условие: scoreboard Game (псевдоигрок) map = 0 означает что карта НЕ выбрана
     */
    private boolean isMapSelected() {
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

    private void broadcastMessage(String message) {
        server.execute(() -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.sendSystemMessage(Component.literal(message));
            }
        });
    }

    public boolean isRunning() {
        return running;
    }

    public int getRemainingSeconds() {
        return remainingSeconds;
    }
}