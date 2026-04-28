package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.util.ClientOnlyExecutor;

import java.util.function.Supplier;

public class ClosePerkScreenPacket {

    public ClosePerkScreenPacket() {}

    public ClosePerkScreenPacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public static ClosePerkScreenPacket decode(FriendlyByteBuf buf) {
        return new ClosePerkScreenPacket();
    }

    public static void handle(ClosePerkScreenPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(ClientOnlyExecutor::closePerkScreenIfOpen);
        ctx.get().setPacketHandled(true);
    }
}
