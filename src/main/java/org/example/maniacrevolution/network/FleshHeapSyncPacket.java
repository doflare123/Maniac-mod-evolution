package org.example.maniacrevolution.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.fleshheap.ClientFleshHeapData;

import java.util.function.Supplier;

public class FleshHeapSyncPacket {
    private final int stacks;

    public FleshHeapSyncPacket(int stacks) {
        this.stacks = stacks;
    }

    public static void encode(FleshHeapSyncPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.stacks);
    }

    public static FleshHeapSyncPacket decode(FriendlyByteBuf buf) {
        return new FleshHeapSyncPacket(buf.readInt());
    }

    public static void handle(FleshHeapSyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientFleshHeapData.setStacks(packet.stacks);
        });
        ctx.get().setPacketHandled(true);
    }
}