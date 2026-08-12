package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.client.ScreamClientHandler;

import java.util.function.Supplier;

/** Server -> client: проигрывает общий крик и при необходимости добавляет отметку. */
public class ScreamPacket {
    private final double x;
    private final double y;
    private final double z;
    private final int markerDurationTicks;
    private final boolean showMarker;

    public ScreamPacket(double x, double y, double z, int markerDurationTicks, boolean showMarker) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.markerDurationTicks = markerDurationTicks;
        this.showMarker = showMarker;
    }

    public static void encode(ScreamPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeVarInt(packet.markerDurationTicks);
        buffer.writeBoolean(packet.showMarker);
    }

    public static ScreamPacket decode(FriendlyByteBuf buffer) {
        return new ScreamPacket(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readVarInt(),
                buffer.readBoolean()
        );
    }

    public static void handle(ScreamPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ScreamClientHandler.receive(
                        packet.x,
                        packet.y,
                        packet.z,
                        packet.markerDurationTicks,
                        packet.showMarker
                )));
        context.setPacketHandled(true);
    }
}
