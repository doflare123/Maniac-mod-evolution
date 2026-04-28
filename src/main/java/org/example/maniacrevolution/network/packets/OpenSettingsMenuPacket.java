package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.client.screen.SettingsScreen;

import java.util.function.Supplier;

public class OpenSettingsMenuPacket {

    public OpenSettingsMenuPacket() {
    }

    public OpenSettingsMenuPacket(FriendlyByteBuf buf) {
        // Пустой пакет
    }

    public static void encode(OpenSettingsMenuPacket msg, FriendlyByteBuf buf) {
        // Пустой пакет, ничего не пишем
    }

    public static OpenSettingsMenuPacket decode(FriendlyByteBuf buf) {
        return new OpenSettingsMenuPacket();
    }

    public static void handle(OpenSettingsMenuPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Открываем GUI на клиенте
            SettingsScreen.open();
        });
        ctx.get().setPacketHandled(true);
    }
}