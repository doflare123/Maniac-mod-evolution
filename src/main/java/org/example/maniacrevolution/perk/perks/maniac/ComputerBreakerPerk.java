package org.example.maniacrevolution.perk.perks.maniac;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import org.example.maniacrevolution.perk.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Ломание компа (Активный) (Мидгейм)
 * Отнимает 500 очков у самого заряженного компьютера
 */
public class ComputerBreakerPerk extends Perk {

    private static final int COOLDOWN_SECONDS = 200;
    private static final int POINTS_TO_REMOVE = 500;

    // Максимальное количество компьютеров (можно увеличить при необходимости)
    private static final int MAX_COMPUTERS = 10;

    public ComputerBreakerPerk() {
        super(new Builder("computer_breaker")
                .type(PerkType.ACTIVE)
                .team(PerkTeam.MANIAC)
                .phases(PerkPhase.MIDGAME)
                .cooldown(COOLDOWN_SECONDS)
        );
    }

    @Override
    public void onActivate(ServerPlayer player) {
        Scoreboard scoreboard = player.getServer().getScoreboard();

        // Получаем objectives
        Objective hackObjective = scoreboard.getObjective("hack");
        Objective hackGoalObjective = scoreboard.getObjective("hackGoal");

        if (hackObjective == null || hackGoalObjective == null) {
            player.displayClientMessage(
                    Component.literal("Ошибка: scoreboard objectives не найдены!"),
                    true
            );
            System.out.println("[ComputerBreaker] ERROR: hack or hackGoal objective not found!");
            return;
        }

        // Собираем информацию о всех компьютерах
        List<ComputerInfo> computers = new ArrayList<>();

        for (int i = 1; i <= MAX_COMPUTERS; i++) {
            String progressName = "Progress" + i;
            String goalName = "comp" + i;

            // Получаем текущий прогресс
            Score progressScore = scoreboard.getOrCreatePlayerScore(progressName, hackObjective);
            int currentProgress = progressScore.getScore();

            // Получаем цель
            Score goalScore = scoreboard.getOrCreatePlayerScore(goalName, hackGoalObjective);
            int goal = goalScore.getScore();

            // Пропускаем компьютеры с нулевой целью (возможно не существуют)
            if (goal == 0) continue;

            // Проверяем, не заряжен ли компьютер
            boolean isCharged = currentProgress >= goal;

            computers.add(new ComputerInfo(i, progressName, currentProgress, goal, isCharged));

            System.out.println(String.format("[ComputerBreaker] Computer %d: Progress=%d, Goal=%d, Charged=%b",
                    i, currentProgress, goal, isCharged));
        }

        if (computers.isEmpty()) {
            player.displayClientMessage(
                    Component.literal("Нет доступных компьютеров!"),
                    true
            );
            System.out.println("[ComputerBreaker] No computers found!");
            return;
        }

        // Находим самый заряженный НЕзаряженный компьютер
        ComputerInfo targetComputer = findMostChargedUnfinishedComputer(computers);

        if (targetComputer == null) {
            player.displayClientMessage(
                    Component.literal("Все компьютеры уже заряжены!"),
                    true
            );
            System.out.println("[ComputerBreaker] All computers are already charged!");
            return;
        }

        // Отнимаем очки
        int newProgress = Math.max(0, targetComputer.currentProgress - POINTS_TO_REMOVE);
        Score progressScore = scoreboard.getOrCreatePlayerScore(targetComputer.progressName, hackObjective);
        progressScore.setScore(newProgress);

        System.out.println(String.format("[ComputerBreaker] Computer %d: %d -> %d (removed %d points)",
                targetComputer.id, targetComputer.currentProgress, newProgress,
                targetComputer.currentProgress - newProgress));

        // Отправляем сообщение игроку
        player.displayClientMessage(
                Component.literal(String.format("Компьютер %d сломан! -%d очков (%d -> %d)",
                        targetComputer.id, POINTS_TO_REMOVE, targetComputer.currentProgress, newProgress)),
                true
        );
    }

    /**
     * Находит самый заряженный незаряженный компьютер
     */
    private ComputerInfo findMostChargedUnfinishedComputer(List<ComputerInfo> computers) {
        return computers.stream()
                .filter(c -> !c.isCharged) // Только незаряженные
                .max((c1, c2) -> Integer.compare(c1.currentProgress, c2.currentProgress)) // Максимальный прогресс
                .orElse(null);
    }

    /**
     * Вспомогательный класс для хранения информации о компьютере
     */
    private static class ComputerInfo {
        final int id;
        final String progressName;
        final int currentProgress;
        final int goal;
        final boolean isCharged;

        ComputerInfo(int id, String progressName, int currentProgress, int goal, boolean isCharged) {
            this.id = id;
            this.progressName = progressName;
            this.currentProgress = currentProgress;
            this.goal = goal;
            this.isCharged = isCharged;
        }
    }
}