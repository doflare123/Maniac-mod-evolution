package org.example.maniacrevolution.cloak;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record CloakFlapPacket(int cloakId) {
    public void encode(FriendlyByteBuf buf) { buf.writeVarInt(cloakId); }
    public static CloakFlapPacket decode(FriendlyByteBuf buf) { return new CloakFlapPacket(buf.readVarInt()); }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) CloakManager.flap(context.getSender(), cloakId);
        });
        context.setPacketHandled(true);
    }
}
