package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.system.Agent47MoneyManager;
import org.example.maniacrevolution.system.Agent47ShopConfig;

import java.util.function.Supplier;

/**
 * Пакет для покупки товара в магазине
 * C -> S
 */
public class Agent47PurchasePacket {

    private final String itemId;

    public Agent47PurchasePacket(String itemId) {
        this.itemId = itemId;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(itemId);
    }

    public static Agent47PurchasePacket decode(FriendlyByteBuf buffer) {
        return new Agent47PurchasePacket(buffer.readUtf());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            // Пытаемся купить товар
            boolean success = Agent47ShopConfig.purchaseItem(player, itemId);

            // Отправляем обновленный баланс обратно
            if (success) {
                ModNetworking.sendToPlayer(
                        new Agent47UpdateMoneyPacket(Agent47MoneyManager.getMoney(player)),
                        player
                );
            }
        });
        context.setPacketHandled(true);
    }
}