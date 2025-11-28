package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.client.renderer.WallhackRenderer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Пакет для синхронизации подсветки маньяков с клиентом.
 * Server -> Client
 */
public class WallhackHighlightPacket {
    private final Set<UUID> highlightedPlayers;
    private final int durationTicks;

    public WallhackHighlightPacket(Set<UUID> highlightedPlayers, int durationTicks) {
        this.highlightedPlayers = highlightedPlayers;
        this.durationTicks = durationTicks;
    }

    // Декодер
    public WallhackHighlightPacket(FriendlyByteBuf buf) {
        int count = buf.readInt();
        this.highlightedPlayers = new HashSet<>();

        for (int i = 0; i < count; i++) {
            highlightedPlayers.add(buf.readUUID());
        }

        this.durationTicks = buf.readInt();
    }

    // Энкодер
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(highlightedPlayers.size());

        for (UUID uuid : highlightedPlayers) {
            buf.writeUUID(uuid);
        }

        buf.writeInt(durationTicks);
    }

    // Статические методы для регистрации
    public static void encode(WallhackHighlightPacket packet, FriendlyByteBuf buf) {
        packet.toBytes(buf);
    }

    public static WallhackHighlightPacket decode(FriendlyByteBuf buf) {
        return new WallhackHighlightPacket(buf);
    }

    // Обработчик (выполняется на клиенте)
    public static void handle(WallhackHighlightPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Обновляем список подсвеченных игроков на клиенте
            WallhackRenderer.setHighlightedPlayers(
                    packet.highlightedPlayers,
                    packet.durationTicks
            );
        });
        ctx.get().setPacketHandled(true);
    }
}