package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.data.ClientKeeperFormData;

import java.util.UUID;
import java.util.function.Supplier;

public class SyncRemotePlayerClassPacket {
    private final UUID playerId;
    private final int maniacClassId;

    public SyncRemotePlayerClassPacket(UUID playerId, int maniacClassId) {
        this.playerId = playerId;
        this.maniacClassId = maniacClassId;
    }

    public static void encode(SyncRemotePlayerClassPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.playerId);
        buf.writeVarInt(packet.maniacClassId);
    }

    public static SyncRemotePlayerClassPacket decode(FriendlyByteBuf buf) {
        return new SyncRemotePlayerClassPacket(buf.readUUID(), buf.readVarInt());
    }

    public static void handle(SyncRemotePlayerClassPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        ClientKeeperFormData.setManiacClass(packet.playerId, packet.maniacClassId)
                )
        );
        ctx.get().setPacketHandled(true);
    }
}
