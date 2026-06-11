package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.ghost.GhostVisibilityClientState;

import java.util.UUID;
import java.util.function.Supplier;

public class SyncGhostVisibilityPacket {
    private final UUID playerId;
    private final boolean hidden;

    public SyncGhostVisibilityPacket(UUID playerId, boolean hidden) {
        this.playerId = playerId;
        this.hidden = hidden;
    }

    public static void encode(SyncGhostVisibilityPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.playerId);
        buf.writeBoolean(packet.hidden);
    }

    public static SyncGhostVisibilityPacket decode(FriendlyByteBuf buf) {
        return new SyncGhostVisibilityPacket(buf.readUUID(), buf.readBoolean());
    }

    public static void handle(SyncGhostVisibilityPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        GhostVisibilityClientState.setHidden(packet.playerId, packet.hidden)));
        ctx.get().setPacketHandled(true);
    }
}
