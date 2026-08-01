package org.example.maniacrevolution.perk;

import net.minecraft.server.level.ServerPlayer;

/**
 * Базовый класс перка, который получает временные заряды от игровых триггеров.
 * Состояние зарядов хранится в {@link PerkInstance}, поэтому оно индивидуально
 * для каждого игрока и синхронизируется вместе с остальными данными перков.
 */
public abstract class ChargedPerk extends Perk {
    private final int chargeDurationTicks;
    private final int maxStoredCharges;
    private final int maxChargesPerGame;

    protected ChargedPerk(Builder builder, int chargeDurationSeconds,
                          int maxStoredCharges, int maxChargesPerGame) {
        super(builder);
        if (getType() != PerkType.CHARGED) {
            throw new IllegalArgumentException("Charged perks must use PerkType.CHARGED");
        }
        if (chargeDurationSeconds <= 0 || maxStoredCharges <= 0 || maxChargesPerGame <= 0) {
            throw new IllegalArgumentException("Charge duration and limits must be positive");
        }
        this.chargeDurationTicks = chargeDurationSeconds * 20;
        this.maxStoredCharges = maxStoredCharges;
        this.maxChargesPerGame = maxChargesPerGame;
    }

    public int getChargeDurationTicks() {
        return chargeDurationTicks;
    }

    public int getMaxStoredCharges() {
        return maxStoredCharges;
    }

    public int getMaxChargesPerGame() {
        return maxChargesPerGame;
    }

    /** Вызывается после успешного получения нового заряда. */
    public void onChargeGained(ServerPlayer player, int currentCharges) {}

    /** Вызывается, когда неиспользованный заряд истёк. */
    public void onChargeExpired(ServerPlayer player, int currentCharges) {}

    /** Вызывается после расходования заряда. */
    public void onChargeConsumed(ServerPlayer player, int currentCharges) {}
}
