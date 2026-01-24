package org.example.maniacrevolution.client;

import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;

/**
 * Клиентская синхронизация кулдаунов способностей предметов
 */
public class ClientAbilityData {

    private static final Map<Item, AbilityData> abilityDataMap = new HashMap<>();

    public static class AbilityData {
        private int cooldownSeconds;
        private int maxCooldownSeconds;
        private int remainingDuration; // НОВОЕ
        private long lastUpdateTime;

        public AbilityData(int cooldownSeconds, int maxCooldownSeconds, int remainingDuration) {
            this.cooldownSeconds = cooldownSeconds;
            this.maxCooldownSeconds = maxCooldownSeconds;
            this.remainingDuration = remainingDuration;
            this.lastUpdateTime = System.currentTimeMillis();
        }

        public int getCooldownSeconds() {
            long elapsed = (System.currentTimeMillis() - lastUpdateTime) / 1000;
            return Math.max(0, cooldownSeconds - (int)elapsed);
        }

        public int getRemainingDuration() {
            long elapsed = (System.currentTimeMillis() - lastUpdateTime) / 1000;
            return Math.max(0, remainingDuration - (int)elapsed);
        }

        public float getCooldownProgress() {
            if (maxCooldownSeconds <= 0) return 0.0f;
            return Math.min(1.0f, (float) getCooldownSeconds() / maxCooldownSeconds);
        }

        public void update(int cooldownSeconds, int maxCooldownSeconds, int remainingDuration) {
            this.cooldownSeconds = cooldownSeconds;
            this.maxCooldownSeconds = maxCooldownSeconds;
            this.remainingDuration = remainingDuration;
            this.lastUpdateTime = System.currentTimeMillis();
        }
    }

    /**
     * Установить данные о способности
     */
    public static void setAbilityData(Item item, int cooldownSeconds, int maxCooldownSeconds, int remainingDuration) {
        AbilityData data = abilityDataMap.get(item);
        if (data == null) {
            abilityDataMap.put(item, new AbilityData(cooldownSeconds, maxCooldownSeconds, remainingDuration));
        } else {
            data.update(cooldownSeconds, maxCooldownSeconds, remainingDuration);
        }
    }

    public static int getCooldownSeconds(Item item) {
        AbilityData data = abilityDataMap.get(item);
        return data != null ? data.getCooldownSeconds() : 0;
    }

    public static int getRemainingDuration(Item item) {
        AbilityData data = abilityDataMap.get(item);
        return data != null ? data.getRemainingDuration() : 0;
    }

    public static float getCooldownProgress(Item item) {
        AbilityData data = abilityDataMap.get(item);
        return data != null ? data.getCooldownProgress() : 0.0f;
    }

    public static void clear() {
        abilityDataMap.clear();
    }
}