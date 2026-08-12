package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.paint.PaintPuddleManager;

import java.util.function.Supplier;

public class PaintQtePressPacket {
    private final int sessionId;
    private final int attemptIndex;
    private final int score;

    public PaintQtePressPacket(int sessionId, int attemptIndex, int score) {
        this.sessionId = sessionId;
        this.attemptIndex = attemptIndex;
        this.score = score;
    }

    public static void encode(PaintQtePressPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.sessionId);
        buffer.writeVarInt(packet.attemptIndex);
        buffer.writeVarInt(packet.score);
    }

    public static PaintQtePressPacket decode(FriendlyByteBuf buffer) {
        return new PaintQtePressPacket(
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(PaintQtePressPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> PaintPuddleManager.handleQtePress(
                    sender, packet.sessionId, packet.attemptIndex, packet.score));
        }
        context.setPacketHandled(true);
    }
}
