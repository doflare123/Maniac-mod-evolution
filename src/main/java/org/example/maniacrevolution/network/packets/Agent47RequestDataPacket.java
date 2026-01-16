package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.system.Agent47MoneyManager;
import org.example.maniacrevolution.system.Agent47ShopConfig;
import org.example.maniacrevolution.system.Agent47TargetManager;

import java.util.function.Supplier;

/**
 * Пакет для запроса данных планшета Агента 47
 * C -> S: Клиент запрашивает данные
 * S -> C: Сервер отправляет Agent47SyncDataPacket
 */
public class Agent47RequestDataPacket {

    public Agent47RequestDataPacket() {
    }

    public void encode(FriendlyByteBuf buffer) {
        // Пустой пакет
    }

    public static Agent47RequestDataPacket decode(FriendlyByteBuf buffer) {
        return new Agent47RequestDataPacket();
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            // Получаем данные
            String targetName = "Нет цели";
            ServerPlayer target = Agent47TargetManager.getCurrentTarget(player);
            if (target != null) {
                targetName = target.getName().getString();
            }

            int money = Agent47MoneyManager.getMoney(player);
            var shopItems = Agent47ShopConfig.getShopItems();

            // Отправляем обратно клиенту
            ModNetworking.sendToPlayer(
                    new Agent47SyncDataPacket(targetName, money, shopItems),
                    player
            );
        });
        context.setPacketHandled(true);
    }
}