package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.client.PaintPuddleClientHandler;
import org.example.maniacrevolution.paint.PaintColor;

import java.util.function.Supplier;

public class PaintPuddlePacket {
    private static final int ACTION_CLEAR = 0;
    private static final int ACTION_ADD = 1;
    private static final int ACTION_REMOVE = 2;

    private final int action;
    private final long id;
    private final double x;
    private final double y;
    private final double z;
    private final int colorId;
    private final float rotation;

    private PaintPuddlePacket(int action, long id, double x, double y, double z,
                              int colorId, float rotation) {
        this.action = action;
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.colorId = colorId;
        this.rotation = rotation;
    }

    public static PaintPuddlePacket clear() {
        return new PaintPuddlePacket(ACTION_CLEAR, 0L, 0, 0, 0, 0, 0);
    }

    public static PaintPuddlePacket remove(long id) {
        return new PaintPuddlePacket(ACTION_REMOVE, id, 0, 0, 0, 0, 0);
    }

    public static PaintPuddlePacket add(long id, double x, double y, double z,
                                         PaintColor color, float rotation) {
        return new PaintPuddlePacket(ACTION_ADD, id, x, y, z,
                color.getNetworkId(), rotation);
    }

    public static void encode(PaintPuddlePacket packet, FriendlyByteBuf buffer) {
        buffer.writeByte(packet.action);
        if (packet.action == ACTION_CLEAR) return;
        buffer.writeVarLong(packet.id);
        if (packet.action == ACTION_REMOVE) return;
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeVarInt(packet.colorId);
        buffer.writeFloat(packet.rotation);
    }

    public static PaintPuddlePacket decode(FriendlyByteBuf buffer) {
        int action = buffer.readByte();
        if (action == ACTION_CLEAR) return clear();
        long id = buffer.readVarLong();
        if (action == ACTION_REMOVE) return remove(id);
        return new PaintPuddlePacket(action, id,
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readVarInt(), buffer.readFloat());
    }

    public static void handle(PaintPuddlePacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (packet.action == ACTION_CLEAR) {
                PaintPuddleClientHandler.clear();
            } else if (packet.action == ACTION_REMOVE) {
                PaintPuddleClientHandler.remove(packet.id);
            } else {
                PaintPuddleClientHandler.add(packet.id, packet.x, packet.y, packet.z,
                        PaintColor.fromNetworkId(packet.colorId), packet.rotation);
            }
        }));
        context.setPacketHandled(true);
    }
}
