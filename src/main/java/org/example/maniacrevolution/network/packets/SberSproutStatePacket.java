package org.example.maniacrevolution.network.packets;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.sbersprout.client.SberSproutClientHandler;

import java.util.function.Supplier;

/** Синхронизация личного ростка, HUD и коротких анимаций извлечения/посадки. */
public record SberSproutStatePacket(int state, long blockPos, float currentPercent,
                                   float savedPercent) {
    public static final int CLEAR = 0;
    public static final int UPDATE = 1;
    public static final int EXTRACTED = 2;
    public static final int PLANTED = 3;

    public static SberSproutStatePacket clear() {
        return new SberSproutStatePacket(CLEAR, BlockPos.ZERO.asLong(), 0.0F, 0.0F);
    }

    public static SberSproutStatePacket update(BlockPos pos, float currentPercent,
                                               float savedPercent) {
        return new SberSproutStatePacket(UPDATE, pos.asLong(), currentPercent, savedPercent);
    }

    public static SberSproutStatePacket extracted(BlockPos pos, float savedPercent) {
        return new SberSproutStatePacket(EXTRACTED, pos.asLong(), 0.0F, savedPercent);
    }

    public static SberSproutStatePacket planted(BlockPos pos, float plantedPercent) {
        return new SberSproutStatePacket(PLANTED, pos.asLong(), plantedPercent, 0.0F);
    }

    public BlockPos position() {
        return BlockPos.of(blockPos);
    }

    public static void encode(SberSproutStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.state);
        buffer.writeLong(packet.blockPos);
        buffer.writeFloat(packet.currentPercent);
        buffer.writeFloat(packet.savedPercent);
    }

    public static SberSproutStatePacket decode(FriendlyByteBuf buffer) {
        return new SberSproutStatePacket(buffer.readVarInt(), buffer.readLong(),
                buffer.readFloat(), buffer.readFloat());
    }

    public static void handle(SberSproutStatePacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> SberSproutClientHandler.handleState(packet)));
        context.setPacketHandled(true);
    }
}
