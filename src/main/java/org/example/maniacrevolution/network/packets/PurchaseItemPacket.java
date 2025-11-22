package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.data.PlayerData;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.game.GameManager;
import org.example.maniacrevolution.shop.ShopItem;
import org.example.maniacrevolution.shop.ShopRegistry;

import java.util.function.Supplier;

public class PurchaseItemPacket {
    private final String itemId;

    public PurchaseItemPacket(String itemId) {
        this.itemId = itemId;
    }

    // ВАЖНО: Конструктор для декодирования должен принимать FriendlyByteBuf
    public PurchaseItemPacket(FriendlyByteBuf buf) {
        this.itemId = buf.readUtf(256);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(itemId, 256);
    }

    public static PurchaseItemPacket decode(FriendlyByteBuf buf) {
        return new PurchaseItemPacket(buf);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // Проверяем что игра не идёт
            if (GameManager.getPhaseValue() != 0) {
                player.displayClientMessage(
                        Component.literal("§cМагазин доступен только вне игры!"), true);
                return;
            }

            ShopItem item = ShopRegistry.getItem(itemId);
            if (item == null) {
                player.displayClientMessage(
                        Component.literal("§cТовар не найден!"), true);
                return;
            }

            PlayerData data = PlayerDataManager.get(player);
            if (item.purchase(player, data)) {
                player.displayClientMessage(
                        Component.literal("§aПокупка успешна: " + item.getName().getString()), false);
                PlayerDataManager.syncToClient(player);
            } else {
                player.displayClientMessage(
                        Component.literal("§cНе удалось совершить покупку!"), true);
            }
        });
        ctx.get().setPacketHandled(true);
        return true;
    }
}
