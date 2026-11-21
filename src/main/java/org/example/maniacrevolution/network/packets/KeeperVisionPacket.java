package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.nightmare.ClientKeeperVisionData;
import org.example.maniacrevolution.nightmare.KeeperSoulState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record KeeperVisionPacket(List<KeeperSoulState> souls) {
    public KeeperVisionPacket {
        souls = List.copyOf(souls);
        if (souls.size() > KeeperSoulState.MAX_SOULS) throw new IllegalArgumentException("Too many souls");
    }

    public static void encode(KeeperVisionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.souls.size());
        for (var soul : packet.souls) {
            buffer.writeUUID(soul.playerId());
            buffer.writeFloat(soul.sanity());
            buffer.writeBoolean(soul.feeding());
        }
    }

    public static KeeperVisionPacket decode(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > KeeperSoulState.MAX_SOULS) throw new IllegalArgumentException("Invalid soul count: " + count);
        var souls = new ArrayList<KeeperSoulState>(count);
        for (int i = 0; i < count; i++)
            souls.add(new KeeperSoulState(buffer.readUUID(), buffer.readFloat(), buffer.readBoolean()));
        return new KeeperVisionPacket(souls);
    }

    public static void handle(KeeperVisionPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> ClientKeeperVisionData.update(packet.souls));
        context.get().setPacketHandled(true);
    }
}
