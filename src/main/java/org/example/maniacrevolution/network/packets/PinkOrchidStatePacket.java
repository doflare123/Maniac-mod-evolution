package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.pinkorchid.client.PinkOrchidClientHandler;

import java.util.function.Supplier;

/** Синхронизация локальной записи и видимого только владельцу маркера маршрута. */
public record PinkOrchidStatePacket(int state, double x, double y, double z,
                                    int durationTicks) {
    public static final int CLEAR = 0;
    public static final int MARKER = 1;
    public static final int RECORDING_STARTED = 2;
    public static final int RECORDING_FINISHED = 3;

    public static PinkOrchidStatePacket clear() {
        return new PinkOrchidStatePacket(CLEAR, 0.0D, 0.0D, 0.0D, 0);
    }

    public static PinkOrchidStatePacket marker(double x, double y, double z) {
        return new PinkOrchidStatePacket(MARKER, x, y, z, 0);
    }

    public static PinkOrchidStatePacket recordingStarted(int durationTicks) {
        return new PinkOrchidStatePacket(RECORDING_STARTED, 0.0D, 0.0D, 0.0D,
                durationTicks);
    }

    public static PinkOrchidStatePacket recordingFinished() {
        return new PinkOrchidStatePacket(RECORDING_FINISHED, 0.0D, 0.0D, 0.0D, 0);
    }

    public static void encode(PinkOrchidStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.state);
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeVarInt(packet.durationTicks);
    }

    public static PinkOrchidStatePacket decode(FriendlyByteBuf buffer) {
        return new PinkOrchidStatePacket(
                buffer.readVarInt(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readVarInt()
        );
    }

    public static void handle(PinkOrchidStatePacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> PinkOrchidClientHandler.handleState(packet)));
        context.setPacketHandled(true);
    }
}
