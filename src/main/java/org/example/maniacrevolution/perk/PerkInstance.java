package org.example.maniacrevolution.perk;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.data.PlayerDataManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Экземпляр перка у конкретного игрока.
 * Хранит состояние кулдауна и другие данные.
 */
public class PerkInstance {
    private final Perk perk;
    private int cooldownRemaining = 0;
    private boolean passiveApplied = false;
    private final List<Integer> chargeDurationsRemaining = new ArrayList<>();
    private int chargesGrantedThisGame = 0;
    private final Set<String> matchFlags = new HashSet<>();

    public PerkInstance(Perk perk) {
        this.perk = perk;
    }

    public Perk getPerk() { return perk; }

    // === Кулдаун ===

    public boolean isOnCooldown() {
        return cooldownRemaining > 0;
    }

    public int getCooldownRemaining() {
        return cooldownRemaining;
    }

    public int getCooldownRemainingSeconds() {
        return (cooldownRemaining + 19) / 20;
    }

    public float getCooldownProgress() {
        if (perk.getCooldownTicks() == 0) return 0;
        return (float) cooldownRemaining / perk.getCooldownTicks();
    }

    public void startCooldown() {
        this.cooldownRemaining = perk.getCooldownTicks();
    }

    public void resetCooldown() {
        this.cooldownRemaining = 0;
    }

    // === Временные заряды ===

    public boolean isChargedPerk() {
        return perk instanceof ChargedPerk;
    }

    public int getChargeCount() {
        return chargeDurationsRemaining.size();
    }

    /** Оставшееся время самого старого заряда — именно его показывает HUD. */
    public int getChargeRemainingTicks() {
        return chargeDurationsRemaining.stream().min(Integer::compareTo).orElse(0);
    }

    public int getChargeDurationTicks() {
        return perk instanceof ChargedPerk charged ? charged.getChargeDurationTicks() : 0;
    }

    public int getChargesGrantedThisGame() {
        return chargesGrantedThisGame;
    }

    public boolean grantCharge(ServerPlayer player) {
        if (!(perk instanceof ChargedPerk charged)) return false;
        if (chargeDurationsRemaining.size() >= charged.getMaxStoredCharges()) return false;
        if (chargesGrantedThisGame >= charged.getMaxChargesPerGame()) return false;

        chargeDurationsRemaining.add(charged.getChargeDurationTicks());
        chargesGrantedThisGame++;
        charged.onChargeGained(player, chargeDurationsRemaining.size());
        PlayerDataManager.syncToClient(player);
        return true;
    }

    public boolean consumeCharge(ServerPlayer player) {
        if (!(perk instanceof ChargedPerk charged) || chargeDurationsRemaining.isEmpty()) {
            return false;
        }

        int oldestIndex = 0;
        for (int i = 1; i < chargeDurationsRemaining.size(); i++) {
            if (chargeDurationsRemaining.get(i) < chargeDurationsRemaining.get(oldestIndex)) {
                oldestIndex = i;
            }
        }
        chargeDurationsRemaining.remove(oldestIndex);
        charged.onChargeConsumed(player, chargeDurationsRemaining.size());
        PlayerDataManager.syncToClient(player);
        return true;
    }

    public boolean hasMatchFlag(String flag) {
        return matchFlags.contains(flag);
    }

    public void setMatchFlag(String flag) {
        matchFlags.add(flag);
    }

    private void resetMatchState() {
        chargeDurationsRemaining.clear();
        chargesGrantedThisGame = 0;
        matchFlags.clear();
    }

    private void tickCharges(ServerPlayer player) {
        if (!(perk instanceof ChargedPerk charged) || chargeDurationsRemaining.isEmpty()) return;

        int expired = 0;
        for (int i = chargeDurationsRemaining.size() - 1; i >= 0; i--) {
            int remaining = chargeDurationsRemaining.get(i) - 1;
            if (remaining <= 0) {
                chargeDurationsRemaining.remove(i);
                expired++;
            } else {
                chargeDurationsRemaining.set(i, remaining);
            }
        }
        for (int i = 0; i < expired; i++) {
            charged.onChargeExpired(player, chargeDurationsRemaining.size());
        }
    }

    // === Тик ===

    public void tick(ServerPlayer player, PerkPhase currentPhase) {
        tickCharges(player);

        // Уменьшаем кулдаун
        if (cooldownRemaining > 0) {
            cooldownRemaining--;
        }

        // Обработка PASSIVE_COOLDOWN перков
        if (perk.getType() == PerkType.PASSIVE_COOLDOWN) {
            handlePassiveCooldownTick(player, currentPhase);
            return;
        }

        // Обычная логика для других типов перков
        if (perk.getType().hasPassiveAbility() && perk.isActiveInPhase(currentPhase)) {
            if (!passiveApplied) {
                perk.applyPassiveEffect(player);
                passiveApplied = true;
            }
            perk.onTick(player);
        } else if (passiveApplied) {
            perk.removePassiveEffect(player);
            passiveApplied = false;
        }
    }

    /**
     * Обработка тика для PASSIVE_COOLDOWN перков.
     * Проверяет условие срабатывания и запускает эффект.
     */
    private void handlePassiveCooldownTick(ServerPlayer player, PerkPhase currentPhase) {
        // Проверяем активность в текущей фазе
        if (!perk.isActiveInPhase(currentPhase)) {
            if (passiveApplied) {
                perk.removePassiveEffect(player);
                passiveApplied = false;
            }
            return;
        }

        // Если перк на кулдауне, пассивный эффект не работает
        if (isOnCooldown()) {
            if (passiveApplied) {
                perk.removePassiveEffect(player);
                passiveApplied = false;
            }
            return;
        }

        // Применяем пассивный эффект если он еще не применен
        if (!passiveApplied) {
            perk.applyPassiveEffect(player);
            passiveApplied = true;
        }

        // Вызываем тик перка
        perk.onTick(player);

        // Проверяем условие срабатывания
        if (perk.shouldTrigger(player)) {
            // Срабатываем и запускаем кулдаун
            perk.onTrigger(player);
            perk.removePassiveEffect(player);
            passiveApplied = false;
            startCooldown();
        }
    }

    // === Активация ===

    public ActivationResult tryActivate(ServerPlayer player, PerkPhase currentPhase) {
        // Проверка типа
        if (!perk.getType().hasActiveAbility()) {
            return ActivationResult.NOT_ACTIVE_PERK;
        }

        // Проверка режима игры
        if (player.gameMode.getGameModeForPlayer() !=
                net.minecraft.world.level.GameType.ADVENTURE) {
            return ActivationResult.WRONG_GAMEMODE;
        }

        // Проверка фазы
        if (!perk.isActiveInPhase(currentPhase)) {
            return ActivationResult.WRONG_PHASE;
        }

        // Проверка кулдауна
        if (isOnCooldown()) {
            return ActivationResult.ON_COOLDOWN;
        }

        // Проверка маны
        if (!perk.hasMana(player)) {
            return ActivationResult.NOT_ENOUGH_MANA;
        }

        // Проверка кастомного условия активации
        if (!perk.meetsActivationCondition(player)) {
            return ActivationResult.CONDITION_NOT_MET;
        }

        // Активация
        perk.consumeMana(player); // тратим ману
        perk.onActivate(player);
        startCooldown();
        return ActivationResult.SUCCESS;
    }

    // === Игровые события ===

    public void onGameStart(ServerPlayer player) {
        resetMatchState();
        perk.onGameStart(player);
    }

    public void onPhaseChange(ServerPlayer player, PerkPhase newPhase) {
        perk.onPhaseChange(player, newPhase);

        // Обновляем пассивный эффект
        if (perk.getType().hasPassiveAbility() || perk.getType() == PerkType.PASSIVE_COOLDOWN) {
            if (perk.isActiveInPhase(newPhase) && !passiveApplied && !isOnCooldown()) {
                perk.applyPassiveEffect(player);
                passiveApplied = true;
            } else if (!perk.isActiveInPhase(newPhase) && passiveApplied) {
                perk.removePassiveEffect(player);
                passiveApplied = false;
            }
        }
    }

    public void onRemove(ServerPlayer player) {
        if (passiveApplied) {
            perk.removePassiveEffect(player);
            passiveApplied = false;
        }
        resetMatchState();
    }

    // === Сериализация ===

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("perkId", perk.getId());
        tag.putInt("cooldown", cooldownRemaining);
        tag.putBoolean("passiveApplied", passiveApplied);
        tag.putIntArray("chargeDurations",
                chargeDurationsRemaining.stream().mapToInt(Integer::intValue).toArray());
        tag.putInt("chargesGrantedThisGame", chargesGrantedThisGame);
        ListTag flagsTag = new ListTag();
        for (String flag : matchFlags) {
            flagsTag.add(StringTag.valueOf(flag));
        }
        tag.put("matchFlags", flagsTag);
        return tag;
    }

    public static PerkInstance load(CompoundTag tag) {
        String perkId = tag.getString("perkId");
        Perk perk = PerkRegistry.getPerk(perkId);
        if (perk == null) return null;

        PerkInstance instance = new PerkInstance(perk);
        instance.cooldownRemaining = tag.getInt("cooldown");
        instance.passiveApplied = tag.getBoolean("passiveApplied");
        if (perk instanceof ChargedPerk charged) {
            int[] durations = tag.getIntArray("chargeDurations");
            for (int duration : durations) {
                if (duration > 0 && instance.chargeDurationsRemaining.size() < charged.getMaxStoredCharges()) {
                    instance.chargeDurationsRemaining.add(Math.min(duration, charged.getChargeDurationTicks()));
                }
            }
            instance.chargesGrantedThisGame = Math.min(
                    tag.getInt("chargesGrantedThisGame"), charged.getMaxChargesPerGame());
        }
        ListTag flagsTag = tag.getList("matchFlags", Tag.TAG_STRING);
        for (int i = 0; i < flagsTag.size(); i++) {
            instance.matchFlags.add(flagsTag.getString(i));
        }
        return instance;
    }

    public enum ActivationResult {
        SUCCESS,
        ON_COOLDOWN,
        WRONG_PHASE,
        WRONG_GAMEMODE,
        NOT_ACTIVE_PERK,
        NOT_ENOUGH_MANA,
        CONDITION_NOT_MET
    }
}
