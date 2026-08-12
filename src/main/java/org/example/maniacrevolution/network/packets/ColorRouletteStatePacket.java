package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.client.ColorRouletteClientHandler;
import org.example.maniacrevolution.colorroulette.ColorCard;

import java.util.function.Supplier;

/** Server -> client state changes for the roulette HUD. */
public final class ColorRouletteStatePacket {
    private static final int START = 0;
    private static final int RESULT = 1;
    private static final int CLEAR = 2;

    private final int action;
    private final int color;

    private ColorRouletteStatePacket(int action, int color) {
        this.action = action;
        this.color = color;
    }

    public static ColorRouletteStatePacket start(ColorCard initialCenter) {
        return new ColorRouletteStatePacket(START, initialCenter.ordinal());
    }

    public static ColorRouletteStatePacket result(ColorCard result) {
        return new ColorRouletteStatePacket(RESULT, result.ordinal());
    }

    public static ColorRouletteStatePacket clear() {
        return new ColorRouletteStatePacket(CLEAR, 0);
    }

    public static void encode(ColorRouletteStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeByte(packet.action);
        buffer.writeByte(packet.color);
    }

    public static ColorRouletteStatePacket decode(FriendlyByteBuf buffer) {
        return new ColorRouletteStatePacket(buffer.readByte(), buffer.readByte());
    }

    public static void handle(ColorRouletteStatePacket packet,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            ColorCard color = ColorCard.byOrdinal(packet.color);
            if (packet.action == START) ColorRouletteClientHandler.start(color);
            else if (packet.action == RESULT) ColorRouletteClientHandler.result(color);
            else ColorRouletteClientHandler.clear();
        }));
        context.setPacketHandled(true);
    }
}
