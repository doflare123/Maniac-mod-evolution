package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.warden.WardenSniffManager;
import java.util.function.Supplier;

/** Empty client request: the server supplies eligibility, time and all trail data. */
public record WardenSniffPacket() {
    public void encode(FriendlyByteBuf buf) {}
    public static WardenSniffPacket decode(FriendlyByteBuf buf) { return new WardenSniffPacket(); }
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> { var sender = context.get().getSender(); if (sender != null) WardenSniffManager.activate(sender); });
        context.get().setPacketHandled(true);
    }
}
