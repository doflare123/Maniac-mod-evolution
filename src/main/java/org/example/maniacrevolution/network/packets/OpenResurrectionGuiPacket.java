package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.util.ClientOnlyExecutor;

import java.util.UUID;
import java.util.function.Supplier;

public class OpenResurrectionGuiPacket {
    private final UUID ritualId;

    public OpenResurrectionGuiPacket(UUID ritualId) {
        this.ritualId = ritualId;
    }

    public OpenResurrectionGuiPacket(FriendlyByteBuf buf) {
        this(buf.readUUID());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(ritualId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        supplier.get().enqueueWork(() -> ClientOnlyExecutor.openResurrectionScreen(ritualId));
        return true;
    }
}
