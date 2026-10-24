package org.example.maniacrevolution.forester;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Server-timed work poses for the worker and other players tracking them. */
public record ForesterWorkPacket(int player, BlockPos pos, int kind, int elapsed, int duration) {
    public void encode(FriendlyByteBuf b) { b.writeInt(player); b.writeBlockPos(pos); b.writeInt(kind); b.writeInt(elapsed); b.writeInt(duration); }
    public static ForesterWorkPacket decode(FriendlyByteBuf b) { return new ForesterWorkPacket(b.readInt(), b.readBlockPos(), b.readInt(), b.readInt(), b.readInt()); }
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> ForesterWorkAnimation.receive(this));
        context.get().setPacketHandled(true);
    }
}
