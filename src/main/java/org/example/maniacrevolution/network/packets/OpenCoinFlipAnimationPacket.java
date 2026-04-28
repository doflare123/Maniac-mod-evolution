package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.dodepovich.DodepovichCoin;

import java.util.function.Supplier;

public class OpenCoinFlipAnimationPacket {
    private final DodepovichCoin coin;
    private final boolean good;

    public OpenCoinFlipAnimationPacket(DodepovichCoin coin, boolean good) {
        this.coin = coin;
        this.good = good;
    }

    public static void encode(OpenCoinFlipAnimationPacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.coin);
        buf.writeBoolean(packet.good);
    }

    public static OpenCoinFlipAnimationPacket decode(FriendlyByteBuf buf) {
        return new OpenCoinFlipAnimationPacket(buf.readEnum(DodepovichCoin.class), buf.readBoolean());
    }

    public static void handle(OpenCoinFlipAnimationPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        org.example.maniacrevolution.client.DodepovichAnimationOverlay.startCoinFlip(packet.coin, packet.good))
        );
        ctx.get().setPacketHandled(true);
    }
}
