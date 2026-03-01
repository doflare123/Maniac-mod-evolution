package org.example.maniacrevolution.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import net.minecraftforge.network.PacketDistributor;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.SyncPlaguePacket;

/**
 * Capability для хранения накопленного времени чумы на игроке.
 *
 * ВАЖНО: зарегистрируйте эту capability через CapabilityManager и
 * AttachCapabilitiesEvent (см. PlagueCapabilityProvider).
 *
 * Логика:
 *  - accumulatedTicks — суммарные тики активного эффекта чумы
 *  - THRESHOLD = 100 тиков = 5 секунд (при 20 TPS)
 *  - При достижении порога: наносим 3.0f урона (1.5 сердца), сбрасываем счётчик
 */
@AutoRegisterCapability
public class PlagueCapability {

    /** 8 секунд × 20 тиков/сек = 160 тиков */
    public static final int THRESHOLD_TICKS = 160;

    /** 1.5 сердца = 3.0f единицы урона */
    public static final float PLAGUE_DAMAGE = 3.0f;

    /** Суммарные накопленные тики чумы */
    private int accumulatedTicks = 0;

    /** Флаг: был ли эффект на игроке в ПРОШЛОМ тике (для инкремента) */
    private boolean hadEffectLastTick = false;

    // ─── Геттеры / сеттеры ───────────────────────────────────────────────────

    public int getAccumulatedTicks() {
        return accumulatedTicks;
    }

    public void setAccumulatedTicks(int ticks) {
        this.accumulatedTicks = Math.max(0, ticks);
    }

    /** Прогресс накопления от 0.0 до 1.0 (для HUD) */
    public float getProgress() {
        return Math.min(1.0f, (float) accumulatedTicks / THRESHOLD_TICKS);
    }

    public boolean hadEffectLastTick() {
        return hadEffectLastTick;
    }

    public void setHadEffectLastTick(boolean value) {
        this.hadEffectLastTick = value;
    }

    // ─── Накопление ──────────────────────────────────────────────────────────

    /**
     * Вызывается каждый серверный тик для игрока с активным эффектом чумы.
     * Возвращает true, если порог достигнут и нужно нанести урон.
     */
    public boolean tickPlague() {
        accumulatedTicks++;
        if (accumulatedTicks >= THRESHOLD_TICKS) {
            accumulatedTicks = 0;
            return true; // нанести урон!
        }
        return false;
    }

    // ─── NBT сериализация (для сохранения при перелогине) ────────────────────

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("accumulatedTicks", accumulatedTicks);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        accumulatedTicks = tag.getInt("accumulatedTicks");
    }

    // ─── Синхронизация с клиентом ────────────────────────────────────────────

    /**
     * Отправляет текущий прогресс чумы клиенту.
     * Вызывайте после каждого изменения accumulatedTicks.
     */
    public void syncToClient(ServerPlayer player) {
        ModNetworking.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncPlaguePacket(accumulatedTicks)
        );
    }
}