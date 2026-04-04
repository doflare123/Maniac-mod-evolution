package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClosePerkScreenPacket {

    public ClosePerkScreenPacket() {}

    public ClosePerkScreenPacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public static ClosePerkScreenPacket decode(FriendlyByteBuf buf) {
        return new ClosePerkScreenPacket();
    }

    public static void handle(ClosePerkScreenPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () ->
                        org.example.maniacrevolution.client.ClientScreenHelper::closePerkScreenIfOpen)
        );
        ctx.get().setPacketHandled(true);
    }
}