package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.client.ClientPreGameReadyData;

import java.util.function.Supplier;

/** Synchronizes the temporary pre-game readiness panel with one client. */
public record PreGameReadyStatePacket(boolean active, String initiatorName, int readyCount,
                                      int totalCount, boolean localReady) {

    public static void encode(PreGameReadyStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.active);
        buffer.writeUtf(packet.initiatorName);
        buffer.writeVarInt(packet.readyCount);
        buffer.writeVarInt(packet.totalCount);
        buffer.writeBoolean(packet.localReady);
    }

    public static PreGameReadyStatePacket decode(FriendlyByteBuf buffer) {
        return new PreGameReadyStatePacket(
                buffer.readBoolean(),
                buffer.readUtf(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readBoolean()
        );
    }

    public static void handle(PreGameReadyStatePacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ClientPreGameReadyData.update(
                packet.active,
                packet.initiatorName,
                packet.readyCount,
                packet.totalCount,
                packet.localReady
        ));
        context.setPacketHandled(true);
    }
}
