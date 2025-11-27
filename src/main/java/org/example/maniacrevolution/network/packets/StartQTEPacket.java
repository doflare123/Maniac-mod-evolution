package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.client.QTEClientHandler;

import java.util.function.Supplier;

public class StartQTEPacket {
    private final int generatorNumber;

    public StartQTEPacket(int generatorNumber) {
        this.generatorNumber = generatorNumber;
    }

    public static void encode(StartQTEPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.generatorNumber);
    }

    public static StartQTEPacket decode(FriendlyByteBuf buf) {
        return new StartQTEPacket(buf.readInt());
    }

    public static void handle(StartQTEPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            QTEClientHandler.startQTE(packet.generatorNumber);
        });
        ctx.get().setPacketHandled(true);
    }
}
