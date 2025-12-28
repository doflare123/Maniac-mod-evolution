package org.example.maniacrevolution.mana;

public class ClientManaData {
    private static float mana = 100.0f;
    private static float maxMana = 100.0f;

    public static void set(float mana, float maxMana) {
        ClientManaData.mana = mana;
        ClientManaData.maxMana = maxMana;
    }

    public static float getMana() {
        return mana;
    }

    public static float getMaxMana() {
        return maxMana;
    }

    public static float getManaPercentage() {
        return maxMana > 0 ? mana / maxMana : 0;
    }
}