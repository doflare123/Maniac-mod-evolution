package org.example.maniacrevolution.network.packets;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.scp173.Scp173GameplayManager;

public record Scp173InputPacket(Action action) {
    public enum Action { BLINK, LIGHT, MELEE }
    public void encode(FriendlyByteBuf b) { b.writeEnum(action); }
    public static Scp173InputPacket decode(FriendlyByteBuf b) { return new Scp173InputPacket(b.readEnum(Action.class)); }
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> { var p = ctx.get().getSender(); if (p != null) Scp173GameplayManager.input(p, action); });
        ctx.get().setPacketHandled(true);
    }
}
