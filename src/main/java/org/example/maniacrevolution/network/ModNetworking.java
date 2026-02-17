package org.example.maniacrevolution.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.network.packets.*;
import org.example.maniacrevolution.network.packets.MapVotingPacket;
import org.example.maniacrevolution.network.packets.MapVotingResultPacket;
import org.example.maniacrevolution.network.packets.PlayerVotePacket;
import org.example.maniacrevolution.network.packets.OpenVotingMenuPacket;
import org.example.maniacrevolution.network.packets.OpenSettingsMenuPacket;
import org.example.maniacrevolution.network.packets.SyncSettingsPacket;
import org.example.maniacrevolution.network.packets.UpdateSettingsPacket;
import org.example.maniacrevolution.network.packets.GiveSettingsToAllPacket;

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

        CHANNEL.messageBuilder(ClosePerkScreenPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClosePerkScreenPacket::encode)
                .decoder(ClosePerkScreenPacket::decode)
                .consumerMainThread(ClosePerkScreenPacket::handle)
                .add();

        CHANNEL.messageBuilder(StartQTEPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(StartQTEPacket::encode)
                .decoder(StartQTEPacket::decode)
                .consumerMainThread(StartQTEPacket::handle)
                .add();

        CHANNEL.messageBuilder(StopQTEPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(StopQTEPacket::encode)
                .decoder(StopQTEPacket::decode)
                .consumerMainThread(StopQTEPacket::handle)
                .add();

        CHANNEL.messageBuilder(FearDirectionPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FearDirectionPacket::encode)
                .decoder(FearDirectionPacket::decode)
                .consumerMainThread(FearDirectionPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncPlayerCosmeticsPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncPlayerCosmeticsPacket::encode)
                .decoder(SyncPlayerCosmeticsPacket::decode)
                .consumerMainThread(SyncPlayerCosmeticsPacket::handle)
                .add();

        CHANNEL.messageBuilder(WallhackGlowPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(WallhackGlowPacket::encode)
                .decoder(WallhackGlowPacket::decode)
                .consumerMainThread(WallhackGlowPacket::handle)
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

        CHANNEL.messageBuilder(PurchaseItemPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PurchaseItemPacket::encode)
                .decoder(PurchaseItemPacket::decode)
                .consumerMainThread(PurchaseItemPacket::handle)
                .add();

        CHANNEL.messageBuilder(QTEKeyPressPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(QTEKeyPressPacket::encode)
                .decoder(QTEKeyPressPacket::decode)
                .consumerMainThread(QTEKeyPressPacket::handle)
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

        CHANNEL.messageBuilder(ToggleCosmeticPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ToggleCosmeticPacket::encode)
                .decoder(ToggleCosmeticPacket::decode)
                .consumerMainThread(ToggleCosmeticPacket::handle)
                .add();

        // Пакет синхронизации маны
        CHANNEL.messageBuilder(SyncManaPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncManaPacket::encode)
                .decoder(SyncManaPacket::decode)
                .consumerMainThread(SyncManaPacket::handle)
                .add();

        CHANNEL.messageBuilder(FleshHeapSyncPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(FleshHeapSyncPacket::decode)
                .encoder(FleshHeapSyncPacket::encode)
                .consumerMainThread(FleshHeapSyncPacket::handle)
                .add();

        // Регистрация пакета для открытия GUI (сервер -> клиент)
        CHANNEL.messageBuilder(OpenResurrectionGuiPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(OpenResurrectionGuiPacket::new)
                .encoder(OpenResurrectionGuiPacket::toBytes)
                .consumerMainThread(OpenResurrectionGuiPacket::handle)
                .add();

        // Регистрация пакета для воскрешения (клиент -> сервер)
        CHANNEL.messageBuilder(ResurrectPlayerPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(ResurrectPlayerPacket::new)
                .encoder(ResurrectPlayerPacket::toBytes)
                .consumerMainThread(ResurrectPlayerPacket::handle)
                .add();

        // Запрос списка мёртвых игроков (клиент -> сервер)
        CHANNEL.messageBuilder(RequestDeadPlayersPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(RequestDeadPlayersPacket::decode)
                .encoder(RequestDeadPlayersPacket::encode)
                .consumerMainThread(RequestDeadPlayersPacket::handle)
                .add();

    // Синхронизация списка мёртвых игроков (сервер -> клиент)
        CHANNEL.messageBuilder(SyncDeadPlayersPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(SyncDeadPlayersPacket::decode)
                .encoder(SyncDeadPlayersPacket::encode)
                .consumerMainThread(SyncDeadPlayersPacket::handle)
                .add();

        CHANNEL.messageBuilder(OpenCharacterMenuPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenCharacterMenuPacket::encode)
                .decoder(OpenCharacterMenuPacket::decode)
                .consumerMainThread(OpenCharacterMenuPacket::handle)
                .add();

        // Пакеты для выбора персонажей (Client -> Server)
        CHANNEL.messageBuilder(SelectCharacterPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SelectCharacterPacket::encode)
                .decoder(SelectCharacterPacket::decode)
                .consumerMainThread(SelectCharacterPacket::handle)
                .add();

        CHANNEL.messageBuilder(ReadyStatusPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ReadyStatusPacket::encode)
                .decoder(ReadyStatusPacket::decode)
                .consumerMainThread(ReadyStatusPacket::handle)
                .add();

        CHANNEL.messageBuilder(StartTrackingPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(StartTrackingPacket::decode)
                .encoder(StartTrackingPacket::encode)
                .consumerMainThread(StartTrackingPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncTabletCooldownPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(SyncTabletCooldownPacket::decode)
                .encoder(SyncTabletCooldownPacket::encode)
                .consumerMainThread(SyncTabletCooldownPacket::handle)
                .add();

        // Пакеты Агента 47
        CHANNEL.messageBuilder(Agent47RequestDataPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(Agent47RequestDataPacket::decode)
                .encoder(Agent47RequestDataPacket::encode)
                .consumerMainThread(Agent47RequestDataPacket::handle)
                .add();

        CHANNEL.messageBuilder(Agent47SyncDataPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(Agent47SyncDataPacket::decode)
                .encoder(Agent47SyncDataPacket::encode)
                .consumerMainThread(Agent47SyncDataPacket::handle)
                .add();

        CHANNEL.messageBuilder(Agent47PurchasePacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(Agent47PurchasePacket::decode)
                .encoder(Agent47PurchasePacket::encode)
                .consumerMainThread(Agent47PurchasePacket::handle)
                .add();

        CHANNEL.messageBuilder(Agent47UpdateMoneyPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(Agent47UpdateMoneyPacket::decode)
                .encoder(Agent47UpdateMoneyPacket::encode)
                .consumerMainThread(Agent47UpdateMoneyPacket::handle)
                .add();

        CHANNEL.messageBuilder(OpenMedicTabletPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(OpenMedicTabletPacket::decode)
                .encoder(OpenMedicTabletPacket::encode)
                .consumerMainThread(OpenMedicTabletPacket::handle)
                .add();

        // Пакеты для голосования за карту
        CHANNEL.messageBuilder(MapVotingPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MapVotingPacket::encode)
                .decoder(MapVotingPacket::new)
                .consumerMainThread(MapVotingPacket::handle)
                .add();

        CHANNEL.messageBuilder(MapVotingResultPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MapVotingResultPacket::encode)
                .decoder(MapVotingResultPacket::new)
                .consumerMainThread(MapVotingResultPacket::handle)
                .add();

        CHANNEL.messageBuilder(PlayerVotePacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PlayerVotePacket::encode)
                .decoder(PlayerVotePacket::new)
                .consumerMainThread(PlayerVotePacket::handle)
                .add();

        CHANNEL.messageBuilder(OpenVotingMenuPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(OpenVotingMenuPacket::encode)
                .decoder(OpenVotingMenuPacket::new)
                .consumerMainThread(OpenVotingMenuPacket::handle)
                .add();

        CHANNEL.messageBuilder(OpenGuidePacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(OpenGuidePacket::decode)
                .encoder(OpenGuidePacket::encode)
                .consumerMainThread(OpenGuidePacket::handle)
                .add();

        CHANNEL.messageBuilder(ActivateArmorAbilityPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(ActivateArmorAbilityPacket::decode)
                .encoder(ActivateArmorAbilityPacket::encode)
                .consumerMainThread(ActivateArmorAbilityPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncAbilityCooldownPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(SyncAbilityCooldownPacket::decode)
                .encoder(SyncAbilityCooldownPacket::encode)
                .consumerMainThread(SyncAbilityCooldownPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncGeneratorPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(SyncGeneratorPacket::new)
                .encoder(SyncGeneratorPacket::encode)
                .consumerMainThread(SyncGeneratorPacket::handle)
                .add();

        CHANNEL.messageBuilder(OpenSettingsMenuPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenSettingsMenuPacket::encode)
                .decoder(OpenSettingsMenuPacket::decode)
                .consumerMainThread(OpenSettingsMenuPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncSettingsPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncSettingsPacket::encode)
                .decoder(SyncSettingsPacket::decode)
                .consumerMainThread(SyncSettingsPacket::handle)
                .add();

        CHANNEL.messageBuilder(UpdateSettingsPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UpdateSettingsPacket::encode)
                .decoder(UpdateSettingsPacket::decode)
                .consumerMainThread(UpdateSettingsPacket::handle)
                .add();

        CHANNEL.messageBuilder(GiveSettingsToAllPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(GiveSettingsToAllPacket::encode)
                .decoder(GiveSettingsToAllPacket::decode)
                .consumerMainThread(GiveSettingsToAllPacket::handle)
                .add();

        Maniacrev.LOGGER.info("Network packets registered: {} packets", packetId);
    }

    // Утилитные методы для отправки пакетов

    /**
     * Отправляет пакет конкретному игроку (Server -> Client)
     */
    public static <MSG> void send(MSG message, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    /**
     * Отправляет пакет конкретному игроку
     */
    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    /**
     * Отправляет пакет всем игрокам на сервере
     */
    public static <MSG> void sendToAllPlayers(MSG message) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), message);
    }

    /**
     * Отправляет пакет на сервер (используется на клиенте)
     */
    public static <MSG> void sendToServer(MSG message) {
        CHANNEL.sendToServer(message);
    }

    /**
     * Отправляет пакет всем игрокам рядом с точкой
     */
    public static <MSG> void sendToNearby(MSG message, ServerPlayer player, double radius) {
        CHANNEL.send(
                PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        radius,
                        player.level().dimension()
                )),
                message
        );
    }
}