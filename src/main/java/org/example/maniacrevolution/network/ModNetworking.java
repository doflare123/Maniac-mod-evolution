package org.example.maniacrevolution.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.network.packets.*;

public class ModNetworking {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Maniacrev.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        // Server -> Client
        CHANNEL.messageBuilder(SyncPlayerDataPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncPlayerDataPacket::encode)
                .decoder(SyncPlayerDataPacket::decode)
                .consumerMainThread(SyncPlayerDataPacket::handle)
                .add();

        CHANNEL.messageBuilder(GameStatePacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(GameStatePacket::encode)
                .decoder(GameStatePacket::decode)
                .consumerMainThread(GameStatePacket::handle)
                .add();

        CHANNEL.messageBuilder(OpenGuiPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenGuiPacket::encode)
                .decoder(OpenGuiPacket::decode)
                .consumerMainThread(OpenGuiPacket::handle)
                .add();

        // Client -> Server
        CHANNEL.messageBuilder(ActivatePerkPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ActivatePerkPacket::encode)
                .decoder(ActivatePerkPacket::decode)
                .consumerMainThread(ActivatePerkPacket::handle)
                .add();

        CHANNEL.messageBuilder(SwitchPerkPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SwitchPerkPacket::encode)
                .decoder(SwitchPerkPacket::decode)
                .consumerMainThread(SwitchPerkPacket::handle)
                .add();

        CHANNEL.messageBuilder(SelectPerkPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SelectPerkPacket::encode)
                .decoder(SelectPerkPacket::decode)
                .consumerMainThread(SelectPerkPacket::handle)
                .add();

        // ФИКС: Добавлена регистрация PurchaseItemPacket
        CHANNEL.messageBuilder(PurchaseItemPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PurchaseItemPacket::encode)
                .decoder(PurchaseItemPacket::decode)
                .consumerMainThread(PurchaseItemPacket::handle)
                .add();

        // Пакеты для пресетов
        CHANNEL.messageBuilder(SavePresetPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SavePresetPacket::encode)
                .decoder(SavePresetPacket::decode)
                .consumerMainThread(SavePresetPacket::handle)
                .add();

        CHANNEL.messageBuilder(ApplyPresetPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ApplyPresetPacket::encode)
                .decoder(ApplyPresetPacket::decode)
                .consumerMainThread(ApplyPresetPacket::handle)
                .add();

        CHANNEL.messageBuilder(DeletePresetPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DeletePresetPacket::encode)
                .decoder(DeletePresetPacket::decode)
                .consumerMainThread(DeletePresetPacket::handle)
                .add();

        // Пакет для переключения косметики
        CHANNEL.messageBuilder(ToggleCosmeticPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ToggleCosmeticPacket::encode)
                .decoder(ToggleCosmeticPacket::decode)
                .consumerMainThread(ToggleCosmeticPacket::handle)
                .add();

        Maniacrev.LOGGER.info("Network packets registered: {} packets", packetId);
    }
}