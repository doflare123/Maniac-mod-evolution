package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.client.ClientAddictionData;

import java.util.function.Supplier;

public class SyncAddictionPacket {

    private final float addiction;
    private final int   totalSyringes;

    public SyncAddictionPacket(float addiction, int totalSyringes) {
        this.addiction     = addiction;
        this.totalSyringes = totalSyringes;
    }

    public static void encode(SyncAddictionPacket p, FriendlyByteBuf buf) {
        buf.writeFloat(p.addiction);
        buf.writeVarInt(p.totalSyringes);
    }

    public static SyncAddictionPacket decode(FriendlyByteBuf buf) {
        return new SyncAddictionPacket(buf.readFloat(), buf.readVarInt());
    }

    public static void handle(SyncAddictionPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                ClientAddictionData.set(packet.addiction, packet.totalSyringes)
        );
        ctx.get().setPacketHandled(true);
    }
}
