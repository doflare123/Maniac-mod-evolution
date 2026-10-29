package org.example.maniacrevolution.engineer;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record EngineerActionPacket(Action action, BlockPos pos, int value) {
    public enum Action { SELECT, PLACE, CANCEL, PLAY, BREAK, COLLECT, KEEP_WORKING, OPEN_WORKSHOP, OPEN_REMOTE, USE_HELD }
    public void encode(FriendlyByteBuf b) { b.writeEnum(action); b.writeBlockPos(pos); b.writeVarInt(value); }
    public static EngineerActionPacket decode(FriendlyByteBuf b) { return new EngineerActionPacket(b.readEnum(Action.class), b.readBlockPos(), b.readVarInt()); }
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        var c = ctx.get(); c.enqueueWork(() -> { if (c.getSender() != null) EngineerManager.action(c.getSender(), this); }); c.setPacketHandled(true);
    }
}
