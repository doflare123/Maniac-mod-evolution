package org.example.maniacrevolution.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.SyncAddictionPacket;
import net.minecraftforge.network.PacketDistributor;

@AutoRegisterCapability
public class AddictionCapability {

    // ── Настройки ─────────────────────────────────────────────────────────────
    public static final int   ADDICTION_MAX        = 6000;
    public static final float FILL_RATE_PER_TICK   = (float) ADDICTION_MAX / 6000f; // 5 минут базово

    /**
     * На сколько % ускоряется заполнение шкалы за каждый применённый шприц.
     * 0.05 = +5% за каждый шприц.
     * Итоговая скорость: FILL_RATE_PER_TICK * (1 + totalSyringes * SYRINGE_SPEED_BONUS)
     * Например, после 3 шприцов: скорость x1.15 (на 15% быстрее базовой).
     */
    public static final float SYRINGE_SPEED_BONUS  = 0.05f;

    public static final int   BONG_PAUSE_TICKS     = 200;
    public static final float SYRINGE_REDUCE_PCT   = 0.20f;
    public static final int   SYRINGE_WINDOW_TICKS = 400;
    public static final int   DEATH_CHECK_INTERVAL = 20;
    public static final float STAGE3_DEATH_CHANCE  = 0.10f;

    // ── Данные ────────────────────────────────────────────────────────────────
    private float addiction         = 0f;
    private int   bongPauseTicks    = 0;
    private int   totalSyringeCount = 0;
    private int   consecSyringes    = 0;
    private long  lastSyringeTick   = -999_999L;
    private int   deathCheckTimer   = 0;

    // ── Геттеры / сеттеры ─────────────────────────────────────────────────────
    public float getAddiction()                 { return addiction; }
    public void  setAddiction(float v)          { addiction = Math.max(0f, Math.min(ADDICTION_MAX, v)); }
    public float getProgress()                  { return addiction / ADDICTION_MAX; }

    public int getStage() {
        float p = getProgress();
        if (p >= 0.75f) return 3;
        if (p >= 0.50f) return 2;
        if (p >= 0.25f) return 1;
        return 0;
    }

    /**
     * Текущая скорость заполнения с учётом бонуса от шприцов.
     * Вызывается из AddictionEventHandler каждый тик.
     */
    public float getCurrentFillRate() {
        float bonus = 1f + totalSyringeCount * SYRINGE_SPEED_BONUS;
        return FILL_RATE_PER_TICK * bonus;
    }

    public boolean isPaused()                   { return bongPauseTicks > 0; }
    public int  getBongPauseTicks()             { return bongPauseTicks; }
    public void setBongPauseTicks(int t)        { bongPauseTicks = Math.max(0, t); }

    public int  getTotalSyringeCount()          { return totalSyringeCount; }
    public void setTotalSyringeCount(int c)     { totalSyringeCount = Math.max(0, c); }

    public int  getConsecSyringes()             { return consecSyringes; }
    public void setConsecSyringes(int c)        { consecSyringes = c; }

    public long getLastSyringeTick()            { return lastSyringeTick; }
    public void setLastSyringeTick(long t)      { lastSyringeTick = t; }

    public int  getDeathCheckTimer()            { return deathCheckTimer; }
    public void tickDeathCheck()                { deathCheckTimer++; }
    public void resetDeathCheck()               { deathCheckTimer = 0; }

    // ── Сброс ─────────────────────────────────────────────────────────────────
    public void resetAddiction()  { addiction = 0f; bongPauseTicks = 0; }
    public void resetSyringes()   { totalSyringeCount = 0; consecSyringes = 0; lastSyringeTick = -999_999L; }
    public void resetAll()        { resetAddiction(); resetSyringes(); deathCheckTimer = 0; }

    // ── NBT ───────────────────────────────────────────────────────────────────
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("addiction",      addiction);
        tag.putInt("bongPause",        bongPauseTicks);
        tag.putInt("totalSyringes",    totalSyringeCount);
        tag.putInt("consecSyringes",   consecSyringes);
        tag.putLong("lastSyringeTick", lastSyringeTick);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        addiction         = tag.getFloat("addiction");
        bongPauseTicks    = tag.getInt("bongPause");
        totalSyringeCount = tag.getInt("totalSyringes");
        consecSyringes    = tag.getInt("consecSyringes");
        lastSyringeTick   = tag.getLong("lastSyringeTick");
    }

    // ── Синхронизация ─────────────────────────────────────────────────────────
    public void syncToClient(ServerPlayer player) {
        ModNetworking.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncAddictionPacket(addiction, totalSyringeCount)
        );
    }
}