package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.client.FlowerTrailClientHandler;
import org.example.maniacrevolution.flower.FlowerVariant;

import java.util.function.Supplier;

/** Сервер -> клиент: добавляет один цветочный след либо очищает все следы. */
public class FlowerTrailPacket {
    private static final int ACTION_CLEAR = 0;
    private static final int ACTION_ADD = 1;

    private final int action;
    private final long traceId;
    private final double startX;
    private final double startY;
    private final double startZ;
    private final double targetX;
    private final double targetY;
    private final double targetZ;
    private final int variantId;
    private final int durationTicks;
    private final int ageTicks;
    private final int fallDurationTicks;
    private final float windX;
    private final float windZ;
    private final float baseRotationDegrees;
    private final float scale;

    private FlowerTrailPacket(int action, long traceId,
                              double startX, double startY, double startZ,
                              double targetX, double targetY, double targetZ,
                              int variantId, int durationTicks, int ageTicks,
                              int fallDurationTicks, float windX, float windZ,
                              float baseRotationDegrees, float scale) {
        this.action = action;
        this.traceId = traceId;
        this.startX = startX;
        this.startY = startY;
        this.startZ = startZ;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        this.variantId = variantId;
        this.durationTicks = durationTicks;
        this.ageTicks = ageTicks;
        this.fallDurationTicks = fallDurationTicks;
        this.windX = windX;
        this.windZ = windZ;
        this.baseRotationDegrees = baseRotationDegrees;
        this.scale = scale;
    }

    public static FlowerTrailPacket clear() {
        return new FlowerTrailPacket(
                ACTION_CLEAR, 0L,
                0.0D, 0.0D, 0.0D,
                0.0D, 0.0D, 0.0D,
                0, 0, 0, 0,
                0.0F, 0.0F, 0.0F, 1.0F
        );
    }

    public static FlowerTrailPacket add(long traceId,
                                        double startX, double startY, double startZ,
                                        double targetX, double targetY, double targetZ,
                                        FlowerVariant variant, int durationTicks, int ageTicks,
                                        int fallDurationTicks, float windX, float windZ,
                                        float baseRotationDegrees, float scale) {
        return new FlowerTrailPacket(
                ACTION_ADD, traceId,
                startX, startY, startZ,
                targetX, targetY, targetZ,
                variant.getNetworkId(), durationTicks, ageTicks, fallDurationTicks,
                windX, windZ, baseRotationDegrees, scale
        );
    }

    public static void encode(FlowerTrailPacket packet, FriendlyByteBuf buffer) {
        buffer.writeByte(packet.action);
        if (packet.action == ACTION_CLEAR) {
            return;
        }

        buffer.writeVarLong(packet.traceId);
        buffer.writeDouble(packet.startX);
        buffer.writeDouble(packet.startY);
        buffer.writeDouble(packet.startZ);
        buffer.writeDouble(packet.targetX);
        buffer.writeDouble(packet.targetY);
        buffer.writeDouble(packet.targetZ);
        buffer.writeVarInt(packet.variantId);
        buffer.writeVarInt(packet.durationTicks);
        buffer.writeVarInt(packet.ageTicks);
        buffer.writeVarInt(packet.fallDurationTicks);
        buffer.writeFloat(packet.windX);
        buffer.writeFloat(packet.windZ);
        buffer.writeFloat(packet.baseRotationDegrees);
        buffer.writeFloat(packet.scale);
    }

    public static FlowerTrailPacket decode(FriendlyByteBuf buffer) {
        int action = buffer.readByte();
        if (action == ACTION_CLEAR) {
            return clear();
        }

        return new FlowerTrailPacket(
                action,
                buffer.readVarLong(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat()
        );
    }

    public static void handle(FlowerTrailPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (packet.action == ACTION_CLEAR) {
                FlowerTrailClientHandler.clear();
                return;
            }

            FlowerTrailClientHandler.add(
                    packet.traceId,
                    packet.startX,
                    packet.startY,
                    packet.startZ,
                    packet.targetX,
                    packet.targetY,
                    packet.targetZ,
                    FlowerVariant.fromNetworkId(packet.variantId),
                    packet.durationTicks,
                    packet.ageTicks,
                    packet.fallDurationTicks,
                    packet.windX,
                    packet.windZ,
                    packet.baseRotationDegrees,
                    packet.scale
            );
        }));
        context.setPacketHandled(true);
    }
}
