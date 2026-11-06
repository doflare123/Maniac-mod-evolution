package org.example.maniacrevolution.atmosphere;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ChaseStatePacket(float intensity) {
    public static void encode(ChaseStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.intensity);
    }

    public static ChaseStatePacket decode(FriendlyByteBuf buffer) {
        return new ChaseStatePacket(AtmosphereRules.clamp(buffer.readFloat()));
    }

    public static void handle(ChaseStatePacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> org.example.maniacrevolution.atmosphere.client.AtmosphereClient.chase(packet.intensity)));
        context.get().setPacketHandled(true);
    }
}
