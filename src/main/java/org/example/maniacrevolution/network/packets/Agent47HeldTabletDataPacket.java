package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.util.ClientOnlyExecutor;

import java.util.UUID;
import java.util.function.Supplier;

public class Agent47HeldTabletDataPacket {
    private final UUID targetUuid;
    private final int healthPercent;

    public Agent47HeldTabletDataPacket(UUID targetUuid, int healthPercent) {
        this.targetUuid = targetUuid;
        this.healthPercent = healthPercent;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(targetUuid != null);
        if (targetUuid != null) {
            buffer.writeUUID(targetUuid);
        }
        buffer.writeVarInt(healthPercent);
    }

    public static Agent47HeldTabletDataPacket decode(FriendlyByteBuf buffer) {
        UUID targetUuid = buffer.readBoolean() ? buffer.readUUID() : null;
        return new Agent47HeldTabletDataPacket(targetUuid, buffer.readVarInt());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
                ClientOnlyExecutor.updateAgent47HeldTabletData(targetUuid, healthPercent));
        context.setPacketHandled(true);
    }
}
