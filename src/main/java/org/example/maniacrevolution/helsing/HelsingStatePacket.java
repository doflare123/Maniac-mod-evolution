package org.example.maniacrevolution.helsing;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;
import java.util.*;
import java.util.function.Supplier;

/** Recipient-specific snapshot. Protection is never serialized to a maniac client. */
public record HelsingStatePacket(int ally, int prey, Component preyName, float progress,
                                 float resource, float maximum, boolean blood, List<Readiness> abilities,
                                 Map<Integer, Integer> shields, Map<Integer, Integer> gestures) {
    public record Readiness(Component name, int seconds) {}
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(ally); buf.writeInt(prey); buf.writeComponent(preyName); buf.writeFloat(progress);
        buf.writeFloat(resource); buf.writeFloat(maximum); buf.writeBoolean(blood);
        buf.writeCollection(abilities, (b, row) -> { b.writeComponent(row.name); b.writeVarInt(row.seconds); });
        buf.writeMap(shields, FriendlyByteBuf::writeVarInt, FriendlyByteBuf::writeVarInt);
        buf.writeMap(gestures, FriendlyByteBuf::writeVarInt, FriendlyByteBuf::writeVarInt);
    }
    public static HelsingStatePacket decode(FriendlyByteBuf buf) {
        return new HelsingStatePacket(buf.readInt(), buf.readInt(), buf.readComponent(), buf.readFloat(),
                buf.readFloat(), buf.readFloat(), buf.readBoolean(),
                buf.readList(b -> new Readiness(b.readComponent(), b.readVarInt())),
                buf.readMap(FriendlyByteBuf::readVarInt, FriendlyByteBuf::readVarInt),
                buf.readMap(FriendlyByteBuf::readVarInt, FriendlyByteBuf::readVarInt));
    }
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> HelsingClient.receive(this));
        context.get().setPacketHandled(true);
    }
}
