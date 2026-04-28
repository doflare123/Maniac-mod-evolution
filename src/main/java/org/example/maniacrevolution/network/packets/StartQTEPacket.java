package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.util.ClientOnlyExecutor;

import java.util.function.Supplier;

/**
 * Пакет для запуска QTE с информацией о перках.
 * Server -> Client
 */
public class StartQTEPacket {
    private final int generatorNumber;
    private final boolean hasQuickReflexes;

    public StartQTEPacket(int generatorNumber, boolean hasQuickReflexes) {
        this.generatorNumber = generatorNumber;
        this.hasQuickReflexes = hasQuickReflexes;
    }

    public static void encode(StartQTEPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.generatorNumber);
        buf.writeBoolean(packet.hasQuickReflexes);
    }

    public static StartQTEPacket decode(FriendlyByteBuf buf) {
        return new StartQTEPacket(buf.readInt(), buf.readBoolean());
    }

    public static void handle(StartQTEPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Устанавливаем флаг перка
            // Запускаем QTE
            ClientOnlyExecutor.startQTE(packet.generatorNumber, packet.hasQuickReflexes);
        });
        ctx.get().setPacketHandled(true);
    }
}
