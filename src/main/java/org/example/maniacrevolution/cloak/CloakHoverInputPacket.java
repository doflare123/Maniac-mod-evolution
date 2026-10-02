package org.example.maniacrevolution.cloak;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Sneak is an ability button while hovering, not a vanilla crouch command. */
public record CloakHoverInputPacket(boolean held) {
    public void encode(FriendlyByteBuf buffer) { buffer.writeBoolean(held); }
    public static CloakHoverInputPacket decode(FriendlyByteBuf buffer) { return new CloakHoverInputPacket(buffer.readBoolean()); }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) CloakManager.hoverInput(context.getSender(), held);
        });
        context.setPacketHandled(true);
    }
}
