package org.example.maniacrevolution.client;

import org.example.maniacrevolution.capability.PlagueCapability;

/**
 * Клиентская копия данных о накоплении чумы.
 * Обновляется через SyncPlaguePacket.
 * Используется в CustomHud для покраски полоски HP.
 */
public class ClientPlagueData {

    private static int accumulatedTicks = 0;

    public static void setAccumulatedTicks(int ticks) {
        accumulatedTicks = Math.max(0, ticks);
    }

    public static int getAccumulatedTicks() {
        return accumulatedTicks;
    }

    /**
     * Прогресс накопления от 0.0 до 1.0.
     * 0.0 = чума не накоплена, 1.0 = порог достигнут (сразу сбрасывается).
     */
    public static float getProgress() {
        return Math.min(1.0f, (float) accumulatedTicks / PlagueCapability.THRESHOLD_TICKS);
    }

    /** Есть ли хоть какое-то накопление чумы (для отображения в HUD) */
    public static boolean hasPlague() {
        return accumulatedTicks > 0;
    }
}