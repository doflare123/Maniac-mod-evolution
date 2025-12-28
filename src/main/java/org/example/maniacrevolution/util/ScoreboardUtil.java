package org.example.maniacrevolution.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public class ScoreboardUtil {
    private static final String SCOREBOARD_NAME = "hack";

    /**
     * Инициализирует скорборд и создаёт все необходимые цели, если их нет
     */
    public static void ensureScoreboardExists(MinecraftServer server) {
        if (server == null) return;

        Scoreboard scoreboard = server.getScoreboard();

        // Создаём основной objective "hack", если его нет
        Objective hackObjective = scoreboard.getObjective(SCOREBOARD_NAME);
        if (hackObjective == null) {
            hackObjective = scoreboard.addObjective(
                    SCOREBOARD_NAME,
                    ObjectiveCriteria.DUMMY,
                    net.minecraft.network.chat.Component.literal("Hack Progress"),
                    ObjectiveCriteria.RenderType.INTEGER
            );
            System.out.println("[Scoreboard] Created objective: " + SCOREBOARD_NAME);
        }

        // Создаём игроков Progress1-Progress9, если их нет
        for (int i = 1; i <= 9; i++) {
            String progressName = "Progress" + i;
            if (!scoreboard.hasPlayerScore(progressName, hackObjective)) {
                scoreboard.getOrCreatePlayerScore(progressName, hackObjective).setScore(0);
                System.out.println("[Scoreboard] Created player: " + progressName);
            }
        }
    }

    /**
     * Добавляет очки к Progress{generatorNumber}
     */
    public static void addHackProgress(ServerPlayer player, int generatorNumber, int amount) {
        if (player == null || generatorNumber < 1 || generatorNumber > 9) {
            System.out.println("[Scoreboard] Invalid parameters: gen=" + generatorNumber);
            return;
        }

        MinecraftServer server = player.getServer();
        if (server == null) return;

        // Проверяем и создаём скорборд при необходимости
        ensureScoreboardExists(server);

        Scoreboard scoreboard = server.getScoreboard();
        Objective hackObjective = scoreboard.getObjective(SCOREBOARD_NAME);

        if (hackObjective == null) {
            System.out.println("[Scoreboard] ERROR: Could not create/find objective!");
            return;
        }

        String progressName = "Progress" + generatorNumber;

        // Получаем текущее значение
        int currentScore = scoreboard.getOrCreatePlayerScore(progressName, hackObjective).getScore();
        int newScore = currentScore + amount;

        // Устанавливаем новое значение
        scoreboard.getOrCreatePlayerScore(progressName, hackObjective).setScore(newScore);

        System.out.println(String.format(
                "[Scoreboard] Player %s hacked generator %d: %d + %d = %d",
                player.getName().getString(),
                generatorNumber,
                currentScore,
                amount,
                newScore
        ));
    }

    /**
     * Получает текущий прогресс Progress{generatorNumber}
     */
    public static int getHackProgress(MinecraftServer server, int generatorNumber) {
        if (server == null || generatorNumber < 1 || generatorNumber > 9) {
            return 0;
        }

        ensureScoreboardExists(server);

        Scoreboard scoreboard = server.getScoreboard();
        Objective hackObjective = scoreboard.getObjective(SCOREBOARD_NAME);

        if (hackObjective == null) {
            return 0;
        }

        String progressName = "Progress" + generatorNumber;
        return scoreboard.getOrCreatePlayerScore(progressName, hackObjective).getScore();
    }
}