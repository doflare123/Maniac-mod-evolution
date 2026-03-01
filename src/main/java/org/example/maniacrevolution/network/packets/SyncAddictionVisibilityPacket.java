package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.client.ClientAddictionData;

import java.util.function.Supplier;

public class SyncAddictionVisibilityPacket {

    private final boolean visible;

    public SyncAddictionVisibilityPacket(boolean visible) { this.visible = visible; }

    public static void encode(SyncAddictionVisibilityPacket p, FriendlyByteBuf buf) {
        buf.writeBoolean(p.visible);
    }

    public static SyncAddictionVisibilityPacket decode(FriendlyByteBuf buf) {
        return new SyncAddictionVisibilityPacket(buf.readBoolean());
    }

    public static void handle(SyncAddictionVisibilityPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientAddictionData.setVisible(p.visible));
        ctx.get().setPacketHandled(true);
    }
}