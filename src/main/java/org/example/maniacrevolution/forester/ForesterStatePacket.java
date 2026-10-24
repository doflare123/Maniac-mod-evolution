package org.example.maniacrevolution.forester;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Sent only to the affected player; age and sequence belong to the server. */
public record ForesterStatePacket(int age, int successes, float target, int sequence, int work, boolean collecting) {
    public void encode(FriendlyByteBuf b) { b.writeInt(age); b.writeInt(successes); b.writeFloat(target); b.writeInt(sequence); b.writeInt(work); b.writeBoolean(collecting); }
    public static ForesterStatePacket decode(FriendlyByteBuf b) { return new ForesterStatePacket(b.readInt(), b.readInt(), b.readFloat(), b.readInt(), b.readInt(), b.readBoolean()); }
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ForesterClient.receive(this));
        ctx.get().setPacketHandled(true);
    }
}
