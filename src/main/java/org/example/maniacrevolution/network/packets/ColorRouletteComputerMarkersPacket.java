package org.example.maniacrevolution.network.packets;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.client.ColorRouletteComputerMarkers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Server -> client: through-wall computer markers granted by the red maniac card. */
public final class ColorRouletteComputerMarkersPacket {
    private final List<BlockPos> positions;
    private final int durationTicks;

    public ColorRouletteComputerMarkersPacket(List<BlockPos> positions, int durationTicks) {
        this.positions = List.copyOf(positions);
        this.durationTicks = durationTicks;
    }

    public static void encode(ColorRouletteComputerMarkersPacket packet,
                              FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.positions.size());
        for (BlockPos pos : packet.positions) buffer.writeBlockPos(pos);
        buffer.writeVarInt(packet.durationTicks);
    }

    public static ColorRouletteComputerMarkersPacket decode(FriendlyByteBuf buffer) {
        int size = Math.min(3, Math.max(0, buffer.readVarInt()));
        List<BlockPos> positions = new ArrayList<>(size);
        for (int i = 0; i < size; i++) positions.add(buffer.readBlockPos());
        return new ColorRouletteComputerMarkersPacket(positions, buffer.readVarInt());
    }

    public static void handle(ColorRouletteComputerMarkersPacket packet,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ColorRouletteComputerMarkers.set(packet.positions, packet.durationTicks)));
        context.setPacketHandled(true);
    }
}
