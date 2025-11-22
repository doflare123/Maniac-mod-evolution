package org.example.maniacrevolution.shop.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.data.PlayerData;
import org.example.maniacrevolution.shop.ShopCategory;
import org.example.maniacrevolution.shop.ShopItem;

import java.util.ArrayList;
import java.util.List;

public class InfoBoostItem extends ShopItem {
    private final String displayName;
    private final String description;

    public InfoBoostItem(String id, int price, String name, String desc) {
        super(id, price, ShopCategory.BOOSTS, false);
        this.displayName = name;
        this.description = desc;
    }

    @Override
    public Component getName() {
        return Component.literal(displayName);
    }

    @Override
    public Component getDescription() {
        return Component.literal(description);
    }

    @Override
    protected void onPurchase(ServerPlayer player, PlayerData data) {
        // Помечаем для использования в следующей игре
        player.addTag("maniacrev_boost_" + id);
        player.displayClientMessage(
                Component.literal("§aУсиление будет активно в начале следующей игры!"), false);
    }

    @Override
    public List<Component> getTooltip(PlayerData data) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.literal(displayName).withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal(description).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("§7Активируется в начале игры"));
        tooltip.add(Component.literal("§7Одноразовое использование"));
        tooltip.add(Component.literal("Цена: " + price + " монет").withStyle(ChatFormatting.GOLD));
        return tooltip;
    }
}
