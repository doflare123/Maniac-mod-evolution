package org.example.maniacrevolution.network.packets;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.util.ClientOnlyExecutor;

import java.util.function.Supplier;

public class SyncGeneratorPacket {

    private final BlockPos pos;
    private final int charge;
    private final boolean powered;

    public SyncGeneratorPacket(BlockPos pos, int charge, boolean powered) {
        this.pos = pos;
        this.charge = charge;
        this.powered = powered;
    }

    public SyncGeneratorPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.charge = buf.readInt();
        this.powered = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(charge);
        buf.writeBoolean(powered);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientOnlyExecutor.syncGenerator(pos, charge, powered));
        ctx.get().setPacketHandled(true);
        return true;
    }
}
