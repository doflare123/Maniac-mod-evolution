package org.example.maniacrevolution.data;

import org.example.maniacrevolution.perk.PerkPhase;

/**
 * Клиентское состояние игры для HUD
 */
public class ClientGameState {
    private static int phase = 0;
    private static int currentTimeTicks = 0;
    private static int maxTimeTicks = 0;
    private static boolean timerRunning = false;

    public static void update(int newPhase, int time, int maxTime, boolean running) {
        phase = newPhase;
        currentTimeTicks = time;
        maxTimeTicks = maxTime;
        timerRunning = running;
    }

    public static int getPhase() { return phase; }
    public static boolean isGameRunning() { return phase > 0; }
    public static boolean isTimerRunning() { return timerRunning; }

    public static int getCurrentTimeSeconds() { return currentTimeTicks / 20; }
    public static int getMaxTimeSeconds() { return maxTimeTicks / 20; }

    public static String getFormattedTime() {
        int totalSec = currentTimeTicks / 20;
        int min = totalSec / 60;
        int sec = totalSec % 60;
        return String.format("%02d:%02d", min, sec);
    }

    public static String getPhaseName() {
        return switch (phase) {
            case 0 -> "";
            case 1 -> "Охота";
            case 2 -> "Мидгейм";
            case 3 -> "Переворот";
            default -> "???";
        };
    }

    public static int getPhaseColor() {
        return switch (phase) {
            case 1 -> 0xFFFF55; // Жёлтый
            case 2 -> 0xFFAA00; // Оранжевый
            case 3 -> 0xFF5555; // Красный
            default -> 0xFFFFFF;
        };
    }

    public static PerkPhase getCurrentPerkPhase() {
        return PerkPhase.fromScoreboardValue(phase);
    }
}
