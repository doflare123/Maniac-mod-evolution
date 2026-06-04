package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.gui.Agent47TabletScreen;
import org.example.maniacrevolution.system.Agent47ShopConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Пакет для синхронизации данных планшета с клиентом
 * S -> C
 */
public class Agent47SyncDataPacket {

    private final String targetName;
    private final int money;
    private final List<Agent47ShopConfig.ShopItem> shopItems;

    public Agent47SyncDataPacket(String targetName, int money, List<Agent47ShopConfig.ShopItem> shopItems) {
        this.targetName = targetName;
        this.money = money;
        this.shopItems = shopItems;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(targetName);
        buffer.writeInt(money);

        // Отправляем товары
        buffer.writeInt(shopItems.size());
        for (Agent47ShopConfig.ShopItem item : shopItems) {
            buffer.writeUtf(item.id);
            buffer.writeUtf(item.name);
            buffer.writeUtf(item.description);
            buffer.writeInt(item.price);
            buffer.writeEnum(item.type);
            buffer.writeUtf(item.data != null ? item.data : "");
            buffer.writeInt(item.amount);
            buffer.writeInt(item.duration);
            buffer.writeInt(item.amplifier);
        }
    }

    public static Agent47SyncDataPacket decode(FriendlyByteBuf buffer) {
        String targetName = buffer.readUtf();
        int money = buffer.readInt();

        int itemCount = buffer.readInt();
        List<Agent47ShopConfig.ShopItem> items = new ArrayList<>();

        for (int i = 0; i < itemCount; i++) {
            String id = buffer.readUtf();
            String name = buffer.readUtf();
            String description = buffer.readUtf();
            int price = buffer.readInt();
            Agent47ShopConfig.ShopItemType type = buffer.readEnum(Agent47ShopConfig.ShopItemType.class);
            String data = buffer.readUtf();
            int amount = buffer.readInt();
            int duration = buffer.readInt();
            int amplifier = buffer.readInt();

            Agent47ShopConfig.ShopItem item = new Agent47ShopConfig.ShopItem(id, name, description, price, type);
            item.data = data.isEmpty() ? null : data;
            item.amount = amount;
            item.duration = duration;
            item.amplifier = amplifier;

            items.add(item);
        }

        return new Agent47SyncDataPacket(targetName, money, items);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Обновляем GUI на клиенте
            Agent47TabletScreen.updateData(targetName, money, shopItems);
        });
        context.setPacketHandled(true);
    }
}