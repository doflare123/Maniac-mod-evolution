package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.ghost.GhostPossessionClientState;

import java.util.function.Supplier;

public class SyncGhostPossessionPacket {
    private final boolean active;
    private final boolean controller;
    private final int targetEntityId;

    public SyncGhostPossessionPacket(boolean active, boolean controller, int targetEntityId) {
        this.active = active;
        this.controller = controller;
        this.targetEntityId = targetEntityId;
    }

    public static void encode(SyncGhostPossessionPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.active);
        buf.writeBoolean(packet.controller);
        buf.writeInt(packet.targetEntityId);
    }

    public static SyncGhostPossessionPacket decode(FriendlyByteBuf buf) {
        return new SyncGhostPossessionPacket(buf.readBoolean(), buf.readBoolean(), buf.readInt());
    }

    public static void handle(SyncGhostPossessionPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    if (packet.active) {
                        GhostPossessionClientState.apply(true, packet.controller, packet.targetEntityId);
                    } else {
                        GhostPossessionClientState.clear();
                    }
                })
        );
        ctx.get().setPacketHandled(true);
    }
}
