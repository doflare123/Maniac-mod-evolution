package org.example.maniacrevolution.helsing;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** The server selects an explicit type in the requested hand; loaded ammunition is preserved. */
public record SelectHelsingBoltPacket(int kind, InteractionHand hand) {
    public void encode(FriendlyByteBuf buf) { buf.writeVarInt(kind); buf.writeEnum(hand); }
    public static SelectHelsingBoltPacket decode(FriendlyByteBuf buf) {
        return new SelectHelsingBoltPacket(buf.readVarInt(), buf.readEnum(InteractionHand.class));
    }
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (context.get().getSender() != null && kind >= 0 && kind < HelsingBoltItem.Kind.values().length)
                HelsingCrossbowItem.select(context.get().getSender(), hand, HelsingBoltItem.Kind.values()[kind]);
        });
        context.get().setPacketHandled(true);
    }
}
