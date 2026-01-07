package org.example.maniacrevolution.hud;

public class ClientHealthData {
    private static float lastHealth = 20.0f;
    private static float healthRegen = 0.0f;
    private static int tickCounter = 0;

    public static void update(float currentHealth) {
        tickCounter++;

        // Обновляем реген каждые 20 тиков (1 секунда)
        if (tickCounter >= 20) {
            healthRegen = (currentHealth - lastHealth); // HP за секунду
            lastHealth = currentHealth;
            tickCounter = 0;
        }
    }

    public static float getHealthRegen() {
        return healthRegen;
    }

    public static void reset() {
        lastHealth = 20.0f;
        healthRegen = 0.0f;
        tickCounter = 0;
    }
}