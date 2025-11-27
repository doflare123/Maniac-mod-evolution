package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.client.QTEClientHandler;

import java.util.function.Supplier;

public class StopQTEPacket {

    public StopQTEPacket() {
    }

    public static void encode(StopQTEPacket packet, FriendlyByteBuf buf) {
        // Пустой пакет
    }

    public static StopQTEPacket decode(FriendlyByteBuf buf) {
        return new StopQTEPacket();
    }

    public static void handle(StopQTEPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            QTEClientHandler.stopQTE();
        });
        ctx.get().setPacketHandled(true);
    }
}
