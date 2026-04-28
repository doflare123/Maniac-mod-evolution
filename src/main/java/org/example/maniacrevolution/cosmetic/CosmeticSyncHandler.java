package org.example.maniacrevolution.cosmetic;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.data.PlayerData;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.SyncPlayerCosmeticsPacket;

/**
 * Синхронизация косметики игроков
 * Отправляет информацию о косметике всем игрокам поблизости
 */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CosmeticSyncHandler {

    /**
     * При входе игрока синхронизируем его косметику всем остальным
     * И синхронизируем косметику других игроков ему
     */
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer joiningPlayer)) return;

        PlayerData joiningData = PlayerDataManager.get(joiningPlayer);
        if (joiningData == null) return;

        // 1. Отправляем косметику входящего игрока всем остальным
        for (ServerPlayer otherPlayer : joiningPlayer.serverLevel().players()) {
            if (otherPlayer != joiningPlayer) {
                ModNetworking.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> otherPlayer),
                        new SyncPlayerCosmeticsPacket(
                                joiningPlayer.getUUID(),
                                joiningData.getCosmeticData().getEnabledCosmetics()
                        )
                );
            }
        }

        // 2. Отправляем косметику всех остальных игроков входящему
        for (ServerPlayer otherPlayer : joiningPlayer.serverLevel().players()) {
            if (otherPlayer != joiningPlayer) {
                PlayerData otherData = PlayerDataManager.get(otherPlayer);
                if (otherData != null) {
                    ModNetworking.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> joiningPlayer),
                            new SyncPlayerCosmeticsPacket(
                                    otherPlayer.getUUID(),
                                    otherData.getCosmeticData().getEnabledCosmetics()
                            )
                    );
                }
            }
        }
    }

    /**
     * Синхронизировать косметику игрока всем игрокам на сервере
     * Вызывается при изменении косметики (покупка/включение/выключение)
     */
    public static void syncCosmeticsToAll(ServerPlayer player) {
        PlayerData data = PlayerDataManager.get(player);
        if (data == null) return;

        SyncPlayerCosmeticsPacket packet = new SyncPlayerCosmeticsPacket(
                player.getUUID(),
                data.getCosmeticData().getEnabledCosmetics()
        );

        // Отправляем всем игрокам на сервере
        for (ServerPlayer otherPlayer : player.serverLevel().players()) {
            ModNetworking.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> otherPlayer),
                    packet
            );
        }
    }
}