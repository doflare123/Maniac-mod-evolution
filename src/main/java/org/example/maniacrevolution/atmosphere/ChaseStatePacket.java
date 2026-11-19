package org.example.maniacrevolution.atmosphere;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Retired wire message: consume old servers' packets without restoring the removed effect. */
public record ChaseStatePacket(float intensity) {
    public static void encode(ChaseStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.intensity);
    }

    public static ChaseStatePacket decode(FriendlyByteBuf buffer) {
        return new ChaseStatePacket(AtmosphereRules.clamp(buffer.readFloat()));
    }

    public static void handle(ChaseStatePacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().setPacketHandled(true);
    }
}
