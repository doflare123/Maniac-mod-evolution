package org.example.maniacrevolution.perk;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/**
 * Экземпляр перка у конкретного игрока.
 * Хранит состояние кулдауна и другие данные.
 */
public class PerkInstance {
    private final Perk perk;
    private int cooldownRemaining = 0;
    private boolean passiveApplied = false;

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

    // === Тик ===

    public void tick(ServerPlayer player, PerkPhase currentPhase) {
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

        // Активация
        perk.onActivate(player);
        startCooldown();
        return ActivationResult.SUCCESS;
    }

    // === Игровые события ===

    public void onGameStart(ServerPlayer player) {
        if (perk.getActivePhases().contains(PerkPhase.START)) {
            perk.onGameStart(player);
        }
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
    }

    // === Сериализация ===

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("perkId", perk.getId());
        tag.putInt("cooldown", cooldownRemaining);
        tag.putBoolean("passiveApplied", passiveApplied);
        return tag;
    }

    public static PerkInstance load(CompoundTag tag) {
        String perkId = tag.getString("perkId");
        Perk perk = PerkRegistry.getPerk(perkId);
        if (perk == null) return null;

        PerkInstance instance = new PerkInstance(perk);
        instance.cooldownRemaining = tag.getInt("cooldown");
        instance.passiveApplied = tag.getBoolean("passiveApplied");
        return instance;
    }

    public enum ActivationResult {
        SUCCESS,
        ON_COOLDOWN,
        WRONG_PHASE,
        WRONG_GAMEMODE,
        NOT_ACTIVE_PERK
    }
}