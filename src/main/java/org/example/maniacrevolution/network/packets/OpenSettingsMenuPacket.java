package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenSettingsMenuPacket {

    public OpenSettingsMenuPacket() {}

    public OpenSettingsMenuPacket(FriendlyByteBuf buf) {}

    public static void encode(OpenSettingsMenuPacket msg, FriendlyByteBuf buf) {}

    public static OpenSettingsMenuPacket decode(FriendlyByteBuf buf) {
        return new OpenSettingsMenuPacket();
    }

    public static void handle(OpenSettingsMenuPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () ->
                        org.example.maniacrevolution.client.ClientScreenHelper::openSettingsScreen)
        );
        ctx.get().setPacketHandled(true);
    }
}