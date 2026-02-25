package org.example.maniacrevolution.downed;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Сервер → Клиент.
 * Отправляется каждые 10 тиков каждому игроку который находится рядом с лежачим
 * (или самому лежачему).
 *
 * role:
 *   0 = ты сам лежишь
 *   1 = ты союзник рядом (можешь поднять)
 *   2 = ты враг рядом (можешь тащить)
 *  -1 = очистить HUD (лежачий поднялся / умер / ушёл далеко)
 */
public class DownedHudPacket {

    public static final int ROLE_SELF    = 0;
    public static final int ROLE_ALLY    = 1;
    public static final int ROLE_ENEMY   = 2;
    public static final int ROLE_CLEAR   = -1;

    public final int    role;
    public final String downedName;   // имя лежачего (для союзника/врага)
    public final int    remainingTicks; // сколько тиков осталось до смерти
    public final float  reviveProgress; // 0.0–1.0, прогресс подъёма (для союзника)
    public final boolean isPaused;      // таймер на паузе (идёт подъём)

    public DownedHudPacket(int role, String downedName, int remainingTicks,
                           float reviveProgress, boolean isPaused) {
        this.role = role;
        this.downedName = downedName;
        this.remainingTicks = remainingTicks;
        this.reviveProgress = reviveProgress;
        this.isPaused = isPaused;
    }

    /** Очищающий пакет */
    public static DownedHudPacket clear() {
        return new DownedHudPacket(ROLE_CLEAR, "", 0, 0f, false);
    }

    public static void encode(DownedHudPacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.role);
        buf.writeUtf(pkt.downedName);
        buf.writeInt(pkt.remainingTicks);
        buf.writeFloat(pkt.reviveProgress);
        buf.writeBoolean(pkt.isPaused);
    }

    public static DownedHudPacket decode(FriendlyByteBuf buf) {
        return new DownedHudPacket(
                buf.readInt(),
                buf.readUtf(),
                buf.readInt(),
                buf.readFloat(),
                buf.readBoolean()
        );
    }

    public static void handle(DownedHudPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DownedHudClient.update(pkt));
        ctx.get().setPacketHandled(true);
    }
}
