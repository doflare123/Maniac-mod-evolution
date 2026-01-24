package org.example.maniacrevolution.item;

import net.minecraft.world.entity.player.Player;

/**
 * Интерфейс для способностей с длительностью
 */
public interface ITimedAbility extends IItemWithAbility {

    /**
     * Получить длительность способности в секундах
     */
    int getDurationSeconds();

    /**
     * Получить оставшееся время активности в секундах
     */
    int getRemainingDurationSeconds(Player player);

    /**
     * Получить прогресс длительности (0.0 = закончилась, 1.0 = только началась)
     */
    default float getDurationProgress(Player player) {
        int remaining = getRemainingDurationSeconds(player);
        int max = getDurationSeconds();
        if (max <= 0) return 0.0f;
        return Math.min(1.0f, (float) remaining / max);
    }
}