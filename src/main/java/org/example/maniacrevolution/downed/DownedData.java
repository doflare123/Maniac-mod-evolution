package org.example.maniacrevolution.downed;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public class DownedData {

    // ── Константы ──────────────────────────────────────────────────────────
    /** Тиков до автосмерти (60 сек × 20 тиков) */
    public static final int DOWNED_TIMEOUT_TICKS = 60 * 20;

    /** Тиков нужно держать ПКМ чтобы поднять (5 сек × 20 тиков) */
    public static final int REVIVE_HOLD_TICKS = 5 * 20;

    // ── Поля ───────────────────────────────────────────────────────────────
    private DownedState state = DownedState.ALIVE;

    /** Сколько тиков игрок уже лежит */
    private int downedTicksElapsed = 0;

    /**
     * UUID игрока который сейчас поднимает этого игрока.
     * null если никто не поднимает.
     */
    private UUID reviverUUID = null;

    /** Сколько тиков текущий союзник уже держит ПКМ */
    private int reviveProgressTicks = 0;

    /**
     * Сколько тиков нужно для подъёма (зависит от класса хелпера).
     * 120 = 6 сек (обычный), 60 = 3 сек (медик).
     * Устанавливается при начале подъёма.
     */
    private int requiredReviveTicks = 120;

    /**
     * Серверный тик последнего клика хелпера.
     * Если текущий тик - lastReviveInteractTick > 5 — хелпер отпустил ПКМ, сбрасываем.
     */
    private long lastReviveInteractTick = -1;

    /**
     * Использован ли уже второй шанс.
     * Если true — следующая смерть фатальна даже в состоянии WEAKENED.
     */
    private boolean usedSecondChance = false;

    /**
     * Оригинальный макс HP до урезания (сохраняем при подъёме союзником).
     * -1 означает что HP не были урезаны (подъём командой или ещё не поднят).
     */
    private double originalMaxHp = -1;

    // ── Геттеры / Сеттеры ─────────────────────────────────────────────────
    public DownedState getState() { return state; }
    public void setState(DownedState state) { this.state = state; }

    public int getDownedTicksElapsed() { return downedTicksElapsed; }
    public void setDownedTicksElapsed(int t) { this.downedTicksElapsed = t; }
    public void incrementDownedTicks() { downedTicksElapsed++; }

    public UUID getReviverUUID() { return reviverUUID; }
    public void setReviverUUID(UUID uuid) { this.reviverUUID = uuid; }

    public int getReviveProgressTicks() { return reviveProgressTicks; }
    public void setReviveProgressTicks(int t) { this.reviveProgressTicks = t; }
    public void incrementReviveProgress() { reviveProgressTicks++; }
    public int getRequiredReviveTicks() { return requiredReviveTicks; }
    public void setRequiredReviveTicks(int t) { this.requiredReviveTicks = t; }

    public boolean hasUsedSecondChance() { return usedSecondChance; }
    public void setUsedSecondChance(boolean v) { this.usedSecondChance = v; }

    public long getLastReviveInteractTick() { return lastReviveInteractTick; }
    public void setLastReviveInteractTick(long tick) { this.lastReviveInteractTick = tick; }

    public double getOriginalMaxHp() { return originalMaxHp; }
    public void setOriginalMaxHp(double hp) { this.originalMaxHp = hp; }
    public boolean hasHpPenalty() { return originalMaxHp > 0; }

    // ── Утилиты ───────────────────────────────────────────────────────────

    /** Сбросить всё в начальное состояние (для команды reset) */
    public void fullReset() {
        state = DownedState.ALIVE;
        downedTicksElapsed = 0;
        reviverUUID = null;
        reviveProgressTicks = 0;
        usedSecondChance = false;
        originalMaxHp = -1;
        lastReviveInteractTick = -1;
    }

    /** Прервать текущий прогресс подъёма, не меняя состояние */
    public void cancelRevive() {
        reviverUUID = null;
        reviveProgressTicks = 0;
        requiredReviveTicks = 120;
        lastReviveInteractTick = -1;
    }

    /** Прогресс подъёма от 0.0 до 1.0 */
    public float getReviveProgress() {
        if (requiredReviveTicks <= 0) return 0f;
        return (float) reviveProgressTicks / requiredReviveTicks;
    }

    // ── NBT ───────────────────────────────────────────────────────────────
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("state", state.name());
        tag.putInt("downedTicks", downedTicksElapsed);
        tag.putBoolean("usedSecondChance", usedSecondChance);
        tag.putDouble("originalMaxHp", originalMaxHp);
        // reviverUUID и reviveProgressTicks не сохраняем — они сессионные
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        try {
            state = DownedState.valueOf(tag.getString("state"));
        } catch (IllegalArgumentException e) {
            state = DownedState.ALIVE;
        }
        downedTicksElapsed = tag.getInt("downedTicks");
        usedSecondChance = tag.getBoolean("usedSecondChance");
        originalMaxHp = tag.contains("originalMaxHp") ? tag.getDouble("originalMaxHp") : -1;
        reviverUUID = null;
        reviveProgressTicks = 0;
    }
}
