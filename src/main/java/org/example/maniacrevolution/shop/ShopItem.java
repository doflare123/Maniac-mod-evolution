package org.example.maniacrevolution.shop;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.data.PlayerData;

import java.util.List;

public abstract class ShopItem {
    protected final String id;
    protected final String nameKey;
    protected final String descKey;
    protected final int price;
    protected final ShopCategory category;
    protected final boolean oneTimePurchase;

    protected ShopItem(String id, int price, ShopCategory category, boolean oneTime) {
        this.id = id;
        this.nameKey = "shop.maniacrev." + id + ".name";
        this.descKey = "shop.maniacrev." + id + ".desc";
        this.price = price;
        this.category = category;
        this.oneTimePurchase = oneTime;
    }

    public String getId() { return id; }
    public int getPrice() { return price; }
    public ShopCategory getCategory() { return category; }
    public boolean isOneTimePurchase() { return oneTimePurchase; }

    public Component getName() { return Component.translatable(nameKey); }
    public Component getDescription() { return Component.translatable(descKey); }

    public boolean canPurchase(PlayerData data) {
        if (data.getCoins() < price) return false;
        if (oneTimePurchase && data.hasPurchased(id)) return false;
        return true;
    }

    public boolean purchase(ServerPlayer player, PlayerData data) {
        if (!canPurchase(data)) return false;
        if (!data.spendCoins(price)) return false;

        if (oneTimePurchase) {
            data.addPurchase(id);
        }

        onPurchase(player, data);
        return true;
    }

    protected abstract void onPurchase(ServerPlayer player, PlayerData data);

    public abstract List<Component> getTooltip(PlayerData data);
}