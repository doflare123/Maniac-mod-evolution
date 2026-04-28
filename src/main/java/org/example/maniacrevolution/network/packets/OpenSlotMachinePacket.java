package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.dodepovich.DodepovichCoin;
import org.example.maniacrevolution.dodepovich.SlotMachineResult;

import java.util.function.Supplier;

public class OpenSlotMachinePacket {
    private final DodepovichCoin coin;
    private final SlotMachineResult result;

    public OpenSlotMachinePacket(DodepovichCoin coin, SlotMachineResult result) {
        this.coin = coin;
        this.result = result;
    }

    public static void encode(OpenSlotMachinePacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.coin);
        buf.writeEnum(packet.result);
    }

    public static OpenSlotMachinePacket decode(FriendlyByteBuf buf) {
        return new OpenSlotMachinePacket(buf.readEnum(DodepovichCoin.class), buf.readEnum(SlotMachineResult.class));
    }

    public static void handle(OpenSlotMachinePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        org.example.maniacrevolution.client.DodepovichAnimationOverlay.startSlotMachine(packet.coin, packet.result))
        );
        ctx.get().setPacketHandled(true);
    }
}
