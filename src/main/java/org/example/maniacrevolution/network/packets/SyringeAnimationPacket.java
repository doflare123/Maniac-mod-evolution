package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Cosmetic only: the existing syringe effects and consumption remain server-authoritative. */
public record SyringeAnimationPacket(int playerId, InteractionHand hand, long startedAt) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(playerId);
        buffer.writeEnum(hand);
        buffer.writeLong(startedAt);
    }

    public static SyringeAnimationPacket decode(FriendlyByteBuf buffer) {
        return new SyringeAnimationPacket(buffer.readVarInt(), buffer.readEnum(InteractionHand.class), buffer.readLong());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() ->
                org.example.maniacrevolution.client.renderer.SyringeRenderEvents.start(this));
        context.get().setPacketHandled(true);
    }
}
