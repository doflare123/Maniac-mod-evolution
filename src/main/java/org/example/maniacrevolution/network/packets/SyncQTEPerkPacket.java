package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.client.QTEClientHandler;

import java.util.function.Supplier;

/**
 * Пакет для синхронизации наличия перка Quick Reflexes с клиентом.
 * Server -> Client
 */
public class SyncQTEPerkPacket {
    private final boolean hasQuickReflexes;

    public SyncQTEPerkPacket(boolean hasQuickReflexes) {
        this.hasQuickReflexes = hasQuickReflexes;
    }

    public SyncQTEPerkPacket(FriendlyByteBuf buf) {
        this.hasQuickReflexes = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(hasQuickReflexes);
    }

    public static void encode(SyncQTEPerkPacket packet, FriendlyByteBuf buf) {
        packet.toBytes(buf);
    }

    public static SyncQTEPerkPacket decode(FriendlyByteBuf buf) {
        return new SyncQTEPerkPacket(buf);
    }

    public static void handle(SyncQTEPerkPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Обновляем флаг на клиенте
            QTEClientHandler.setQuickReflexes(packet.hasQuickReflexes);
        });
        ctx.get().setPacketHandled(true);
    }
}