package org.example.maniacrevolution.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.SyncFurySwipesPacket;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Capability цели (игрока-выжившего), на которого наложены стаки Fury Swipes.
 *
 * Каждый стак хранит gameTick истечения — независимо друг от друга.
 * При ударе добавляется новый стак не обновляя существующие.
 */
@AutoRegisterCapability
public class FurySwipesCapability {

    /** Время жизни одного стака в тиках (20 сек = 400 тиков) */
    public static final int STACK_LIFETIME_TICKS = 400;

    /** Бонус урона за каждый стак */
    public static final float DAMAGE_PER_STACK = 0.5f;

    /** Список game-тиков истечения каждого стака */
    private final List<Long> stackExpireTicks = new ArrayList<>();

    // ── API ───────────────────────────────────────────────────────────────────

    /** Добавить новый стак. Не обновляет существующие. */
    public void addStack(long currentTick) {
        stackExpireTicks.add(currentTick + STACK_LIFETIME_TICKS);
    }

    /**
     * Убрать протухшие стаки. Вызывать каждый серверный тик.
     * @return true если стаки изменились
     */
    public boolean tickAndPrune(long currentTick) {
        int before = stackExpireTicks.size();
        stackExpireTicks.removeIf(expiry -> currentTick >= expiry);
        return stackExpireTicks.size() != before;
    }

    public int getStackCount() { return stackExpireTicks.size(); }

    public float getBonusDamage() { return getStackCount() * DAMAGE_PER_STACK; }

    public void clearStacks() { stackExpireTicks.clear(); }

    /** Возвращает тик истечения ближайшего к смерти стака (минимальный) */
    public long getNearestExpiry() {
        return stackExpireTicks.stream().mapToLong(Long::longValue).min().orElse(-1L);
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (long t : stackExpireTicks) list.add(LongTag.valueOf(t));
        tag.put("stacks", list);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        stackExpireTicks.clear();
        ListTag list = tag.getList("stacks", 4); // 4 = TAG_Long
        for (int i = 0; i < list.size(); i++) {
            stackExpireTicks.add(((LongTag) list.get(i)).getAsLong());
        }
    }

    // ── Синхронизация ─────────────────────────────────────────────────────────

    public void syncToClient(ServerPlayer player) {
        ModNetworking.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncFurySwipesPacket(stackExpireTicks)
        );
    }

    /**
     * Синхронизировать данные АТАКУЮЩЕМУ (чтобы он видел стаки над головой жертвы).
     * Вызывается с данными жертвы, но отправляется атакующему.
     */
    public void syncToAttacker(ServerPlayer attacker) {
        ModNetworking.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> attacker),
                new SyncFurySwipesPacket(stackExpireTicks)
        );
    }
}