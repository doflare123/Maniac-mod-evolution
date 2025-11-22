package org.example.maniacrevolution.shop.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.data.PlayerData;
import org.example.maniacrevolution.shop.ShopCategory;
import org.example.maniacrevolution.shop.ShopItem;

import java.util.ArrayList;
import java.util.List;

public class PerkSkinItem extends ShopItem {
    private final String perkId;

    public PerkSkinItem(String id, String perkId, int price) {
        super(id, price, ShopCategory.COSMETICS, true);
        this.perkId = perkId;
    }

    @Override
    protected void onPurchase(ServerPlayer player, PlayerData data) {
        player.displayClientMessage(
                Component.literal("§dВы приобрели новый скин для перка!"), false);
    }

    @Override
    public List<Component> getTooltip(PlayerData data) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(getName().copy().withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(getDescription().copy().withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Для перка: " + perkId).withStyle(ChatFormatting.DARK_GRAY));

        if (data.hasPurchased(id)) {
            tooltip.add(Component.literal("✓ Куплено").withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.literal("Цена: " + price + " монет").withStyle(ChatFormatting.GOLD));
        }
        return tooltip;
    }
}
