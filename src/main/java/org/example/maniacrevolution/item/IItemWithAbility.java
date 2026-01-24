package org.example.maniacrevolution.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Интерфейс для предметов с отображаемыми способностями
 * Расширяет систему активируемой брони на любые предметы
 */
public interface IItemWithAbility {

    /**
     * Получить иконку способности для HUD
     */
    ResourceLocation getAbilityIcon();

    /**
     * Получить название способности
     */
    String getAbilityName();

    /**
     * Получить стоимость активации (0 если не требует маны)
     */
    float getManaCost();

    /**
     * Получить кулдаун в секундах (0 если нет кулдауна)
     */
    int getCooldownSeconds(Player player);

    /**
     * Получить максимальный кулдаун в секундах
     */
    int getMaxCooldownSeconds();

    /**
     * Проверка активности способности (для косы смерти - всегда активна, для брони - по NBT)
     */
    default boolean isAbilityActive(Player player) {
        return true;
    }

    /**
     * Получить прогресс кулдауна (0.0 - 1.0)
     */
    default float getCooldownProgress(Player player) {
        int current = getCooldownSeconds(player);
        int max = getMaxCooldownSeconds();
        if (max <= 0) return 0.0f;
        return Math.min(1.0f, (float) current / max);
    }

    /**
     * Проверка, на кулдауне ли способность
     */
    default boolean isOnCooldown(Player player) {
        return getCooldownSeconds(player) > 0;
    }
}