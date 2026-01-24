package org.example.maniacrevolution.item.armor;

import net.minecraft.server.level.ServerPlayer;

/**
 * Интерфейс для брони с активируемыми способностями
 */
public interface IActivatableArmor {

    /**
     * Активация способности брони
     * @return true если активация успешна
     */
    boolean activateAbility(ServerPlayer player);

    /**
     * Проверка, доступна ли способность для активации
     */
    boolean canActivate(ServerPlayer player);

    /**
     * Получить стоимость активации в мане
     */
    float getManaCost();

    /**
     * Получить длительность способности в тиках
     */
    int getDuration();

    /**
     * Получить кулдаун в тиках
     */
    int getCooldown();

    /**
     * Получить название способности
     */
    String getAbilityName();

    /**
     * Получить описание способности
     */
    String getAbilityDescription();
}