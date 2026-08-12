package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.client.BouquetClientHandler;
import org.example.maniacrevolution.flower.FlowerVariant;
import org.example.maniacrevolution.perk.perks.maniac.BouquetToTheOtherSidePerk;

import java.util.List;
import java.util.function.Supplier;

/** Синхронизирует HUD букета и его цветочные анимации. */
public class BouquetPacket {
    private static final int ACTION_STATE = 0;
    private static final int ACTION_COLLECT = 1;
    private static final int ACTION_CONSUME = 2;

    private static final int MAX_NETWORK_FLOWERS =
            BouquetToTheOtherSidePerk.MAX_FLOWERS;

    private final int action;
    private final int[] variantIds;
    private final double startX;
    private final double startY;
    private final double startZ;
    private final double targetX;
    private final double targetY;
    private final double targetZ;

    private BouquetPacket(int action, int[] variantIds,
                          double startX, double startY, double startZ,
                          double targetX, double targetY, double targetZ) {
        this.action = action;
        this.variantIds = variantIds;
        this.startX = startX;
        this.startY = startY;
        this.startZ = startZ;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
    }

    public static BouquetPacket state(List<FlowerVariant> variants) {
        return create(ACTION_STATE, variants,
                0.0D, 0.0D, 0.0D,
                0.0D, 0.0D, 0.0D);
    }

    public static BouquetPacket collect(FlowerVariant variant,
                                        double startX, double startY, double startZ,
                                        double targetX, double targetY, double targetZ) {
        return create(ACTION_COLLECT, List.of(variant),
                startX, startY, startZ,
                targetX, targetY, targetZ);
    }

    public static BouquetPacket consume(List<FlowerVariant> variants,
                                        double x, double y, double z) {
        return create(ACTION_CONSUME, variants,
                x, y, z,
                x, y, z);
    }

    private static BouquetPacket create(int action, List<FlowerVariant> variants,
                                         double startX, double startY, double startZ,
                                         double targetX, double targetY, double targetZ) {
        int count = Math.min(MAX_NETWORK_FLOWERS, variants.size());
        int[] ids = new int[count];
        for (int index = 0; index < count; index++) {
            ids[index] = variants.get(index).getNetworkId();
        }
        return new BouquetPacket(
                action,
                ids,
                startX,
                startY,
                startZ,
                targetX,
                targetY,
                targetZ
        );
    }

    public static void encode(BouquetPacket packet, FriendlyByteBuf buffer) {
        buffer.writeByte(packet.action);
        buffer.writeVarInt(packet.variantIds.length);
        for (int variantId : packet.variantIds) {
            buffer.writeVarInt(variantId);
        }
        buffer.writeDouble(packet.startX);
        buffer.writeDouble(packet.startY);
        buffer.writeDouble(packet.startZ);
        buffer.writeDouble(packet.targetX);
        buffer.writeDouble(packet.targetY);
        buffer.writeDouble(packet.targetZ);
    }

    public static BouquetPacket decode(FriendlyByteBuf buffer) {
        int action = buffer.readByte();
        int encodedCount = Math.max(0, buffer.readVarInt());
        int count = Math.min(MAX_NETWORK_FLOWERS, encodedCount);
        int[] ids = new int[count];
        for (int index = 0; index < encodedCount; index++) {
            int id = buffer.readVarInt();
            if (index < count) {
                ids[index] = id;
            }
        }
        return new BouquetPacket(
                action,
                ids,
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble()
        );
    }

    public static void handle(BouquetPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            List<FlowerVariant> variants = java.util.Arrays.stream(packet.variantIds)
                    .mapToObj(FlowerVariant::fromNetworkId)
                    .toList();
            switch (packet.action) {
                case ACTION_STATE -> BouquetClientHandler.setHudFlowers(variants);
                case ACTION_COLLECT -> {
                    if (!variants.isEmpty()) {
                        BouquetClientHandler.addFlight(
                                variants.get(0),
                                packet.startX,
                                packet.startY,
                                packet.startZ,
                                packet.targetX,
                                packet.targetY,
                                packet.targetZ
                        );
                    }
                }
                case ACTION_CONSUME -> BouquetClientHandler.addBurst(
                        variants,
                        packet.startX,
                        packet.startY,
                        packet.startZ
                );
                default -> {
                }
            }
        }));
        context.setPacketHandled(true);
    }
}
