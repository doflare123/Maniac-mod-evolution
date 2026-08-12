package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.client.PaintQteClientHandler;
import org.example.maniacrevolution.paint.PaintColor;

import java.util.function.Supplier;

public class PaintQteStatePacket {
    private final boolean active;
    private final int sessionId;
    private final int colorId;
    private final int attempts;
    private final int totalDurationMs;
    private final int missPercent;
    private final int nearPercent;
    private final int hitPercent;
    private final int overlayDurationTicks;

    private PaintQteStatePacket(boolean active, int sessionId, int colorId,
                                int attempts, int totalDurationMs,
                                int missPercent, int nearPercent, int hitPercent,
                                int overlayDurationTicks) {
        this.active = active;
        this.sessionId = sessionId;
        this.colorId = colorId;
        this.attempts = attempts;
        this.totalDurationMs = totalDurationMs;
        this.missPercent = missPercent;
        this.nearPercent = nearPercent;
        this.hitPercent = hitPercent;
        this.overlayDurationTicks = overlayDurationTicks;
    }

    public static PaintQteStatePacket clear() {
        return new PaintQteStatePacket(false, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    public static PaintQteStatePacket start(int sessionId, PaintColor color,
                                             int attempts, int totalDurationMs,
                                             int missPercent, int nearPercent,
                                             int hitPercent, int overlayDurationTicks) {
        return new PaintQteStatePacket(true, sessionId, color.getNetworkId(),
                attempts, totalDurationMs, missPercent, nearPercent, hitPercent,
                overlayDurationTicks);
    }

    public static void encode(PaintQteStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.active);
        if (!packet.active) return;
        buffer.writeVarInt(packet.sessionId);
        buffer.writeVarInt(packet.colorId);
        buffer.writeVarInt(packet.attempts);
        buffer.writeVarInt(packet.totalDurationMs);
        buffer.writeVarInt(packet.missPercent);
        buffer.writeVarInt(packet.nearPercent);
        buffer.writeVarInt(packet.hitPercent);
        buffer.writeVarInt(packet.overlayDurationTicks);
    }

    public static PaintQteStatePacket decode(FriendlyByteBuf buffer) {
        if (!buffer.readBoolean()) return clear();
        return new PaintQteStatePacket(true,
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(PaintQteStatePacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (!packet.active) {
                PaintQteClientHandler.stopQte();
            } else {
                PaintQteClientHandler.startQte(
                        packet.sessionId,
                        PaintColor.fromNetworkId(packet.colorId),
                        packet.attempts,
                        packet.totalDurationMs,
                        packet.missPercent,
                        packet.nearPercent,
                        packet.hitPercent,
                        packet.overlayDurationTicks
                );
            }
        }));
        context.setPacketHandled(true);
    }
}
