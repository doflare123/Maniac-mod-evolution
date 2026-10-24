package org.example.maniacrevolution.forester;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record ForesterInputPacket(Action action, BlockPos pos, InteractionHand hand, int sequence, int age) {
    public enum Action { SPACE, REARM, COLLECT, PLACE, CANCEL, RELEASE }
    public void encode(FriendlyByteBuf b) { b.writeEnum(action); b.writeBlockPos(pos); b.writeEnum(hand); b.writeInt(sequence); b.writeInt(age); }
    public static ForesterInputPacket decode(FriendlyByteBuf b) { return new ForesterInputPacket(b.readEnum(Action.class), b.readBlockPos(), b.readEnum(InteractionHand.class), b.readInt(), b.readInt()); }
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        var context = ctx.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player != null) ForesterManager.input(player, this);
        });
        context.setPacketHandled(true);
    }
}
