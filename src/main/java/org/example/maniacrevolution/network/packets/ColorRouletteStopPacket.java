package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.colorroulette.ColorCard;
import org.example.maniacrevolution.colorroulette.ColorRouletteManager;

import java.util.function.Supplier;

/** Client -> server: stop after the intro and claim the currently centered card. */
public final class ColorRouletteStopPacket {
    private final int color;

    public ColorRouletteStopPacket(ColorCard color) {
        this.color = color.ordinal();
    }

    private ColorRouletteStopPacket(int color) {
        this.color = color;
    }

    public static void encode(ColorRouletteStopPacket packet, FriendlyByteBuf buffer) {
        buffer.writeByte(packet.color);
    }

    public static ColorRouletteStopPacket decode(FriendlyByteBuf buffer) {
        return new ColorRouletteStopPacket(buffer.readByte());
    }

    public static void handle(ColorRouletteStopPacket packet,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && packet.color >= 0 && packet.color < ColorCard.values().length) {
                ColorRouletteManager.stop(player, ColorCard.byOrdinal(packet.color));
            }
        });
        context.setPacketHandled(true);
    }
}
