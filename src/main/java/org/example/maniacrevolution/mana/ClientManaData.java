package org.example.maniacrevolution.mana;

public class ClientManaData {
    private static float mana = 100.0f;
    private static float maxMana = 100.0f;
    private static float regenRate = 0.0f;
    private static float lastMana = 100.0f;
    private static int tickCounter = 0;
    private static float measuredRegen = 0.0f;

    public static void set(float mana, float maxMana, float regenRate) {
        ClientManaData.mana = mana;
        ClientManaData.maxMana = maxMana;
        ClientManaData.regenRate = regenRate;
    }

    public static void tick() {
        tickCounter++;

        // Обновляем измеренный реген каждые 20 тиков
        if (tickCounter >= 20) {
            measuredRegen = (mana - lastMana); // Мана за секунду
            lastMana = mana;
            tickCounter = 0;
        }
    }

    public static float getMana() {
        return mana;
    }

    public static float getMaxMana() {
        return maxMana;
    }

    public static float getRegenRate() {
        return regenRate;
    }

    public static float getMeasuredRegen() {
        return measuredRegen;
    }

    public static float getManaPercentage() {
        return maxMana > 0 ? mana / maxMana : 0;
    }
}