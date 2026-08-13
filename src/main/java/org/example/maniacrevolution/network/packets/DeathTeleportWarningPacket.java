package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.client.renderer.DeathEmbraceClientHandler;

import java.util.function.Supplier;

/** Starts the private first-person warning shown only to Death's selected target. */
public record DeathTeleportWarningPacket(int durationTicks) {
    public static void encode(DeathTeleportWarningPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.durationTicks);
    }

    public static DeathTeleportWarningPacket decode(FriendlyByteBuf buffer) {
        return new DeathTeleportWarningPacket(buffer.readVarInt());
    }

    public static void handle(DeathTeleportWarningPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> DeathEmbraceClientHandler.start(packet.durationTicks)));
        context.setPacketHandled(true);
    }
}
