package org.example.maniacrevolution.client;

import org.example.maniacrevolution.capability.AddictionCapability;

/**
 * Клиентская копия данных зависимости.
 * Обновляется пакетами с сервера.
 * Используется в CustomHud для рисования вертикальной шкалы.
 */
public class ClientAddictionData {

    private static float   addiction     = 0f;
    private static int     totalSyringes = 0;
    private static boolean visible       = false;

    public static void set(float addiction, int totalSyringes) {
        ClientAddictionData.addiction     = Math.max(0f, addiction);
        ClientAddictionData.totalSyringes = totalSyringes;
    }

    public static void setVisible(boolean v) { visible = v; }

    public static float   getAddiction()     { return addiction; }
    public static int     getTotalSyringes() { return totalSyringes; }
    public static boolean isVisible()        { return visible; }

    /** Прогресс 0.0..1.0 */
    public static float getProgress() {
        return Math.min(1.0f, addiction / AddictionCapability.ADDICTION_MAX);
    }

    /** Текущий этап 0..3 */
    public static int getStage() {
        float p = getProgress();
        if (p >= 0.75f) return 3;
        if (p >= 0.50f) return 2;
        if (p >= 0.25f) return 1;
        return 0;
    }
}