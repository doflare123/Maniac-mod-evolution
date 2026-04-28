package org.example.maniacrevolution.network.packets;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.client.ClientPlagueData;

import java.util.function.Supplier;

/**
 * Пакет синхронизации накопленных тиков чумы: Сервер → Клиент.
 */
public class SyncPlaguePacket {

    private final int accumulatedTicks;

    public SyncPlaguePacket(int accumulatedTicks) {
        this.accumulatedTicks = accumulatedTicks;
    }

    // ─── Сериализация ────────────────────────────────────────────────────────

    public static void encode(SyncPlaguePacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.accumulatedTicks);
    }

    public static SyncPlaguePacket decode(FriendlyByteBuf buf) {
        return new SyncPlaguePacket(buf.readVarInt());
    }

    // ─── Обработка на клиенте ────────────────────────────────────────────────

    public static void handle(SyncPlaguePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Выполняется на главном потоке клиента
            ClientPlagueData.setAccumulatedTicks(packet.accumulatedTicks);
        });
        ctx.get().setPacketHandled(true);
    }
}