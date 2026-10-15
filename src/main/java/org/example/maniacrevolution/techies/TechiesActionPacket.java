package org.example.maniacrevolution.techies;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record TechiesActionPacket(int action, int value) {
    public static final int SELECT=0, PLACE=1, CANCEL=2, REMOTE=3, HIT=4;
    public void encode(FriendlyByteBuf b) { b.writeVarInt(action); b.writeVarInt(value); }
    public static TechiesActionPacket decode(FriendlyByteBuf b) { return new TechiesActionPacket(b.readVarInt(), b.readVarInt()); }
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        var c=ctx.get(); c.enqueueWork(() -> { if (c.getSender()!=null) TechiesManager.action(c.getSender(),action,value); });
        c.setPacketHandled(true);
    }
}
