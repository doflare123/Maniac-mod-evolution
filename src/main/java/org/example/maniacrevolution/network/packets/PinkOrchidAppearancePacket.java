package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.pinkorchid.client.PinkOrchidClientHandler;

import java.util.UUID;
import java.util.function.Supplier;

/** Запускает чисто визуальное появление настоящего игрока через лепестки. */
public record PinkOrchidAppearancePacket(UUID playerId, int durationTicks) {
    public static void encode(PinkOrchidAppearancePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.playerId);
        buffer.writeVarInt(packet.durationTicks);
    }

    public static PinkOrchidAppearancePacket decode(FriendlyByteBuf buffer) {
        return new PinkOrchidAppearancePacket(buffer.readUUID(), buffer.readVarInt());
    }

    public static void handle(PinkOrchidAppearancePacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> PinkOrchidClientHandler.handleAppearance(packet)));
        context.setPacketHandled(true);
    }
}
