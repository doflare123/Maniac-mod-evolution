package org.example.maniacrevolution.shop.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.data.PlayerData;
import org.example.maniacrevolution.shop.ShopCategory;
import org.example.maniacrevolution.shop.ShopItem;

import java.util.ArrayList;
import java.util.List;

public class CosmeticShopItem extends ShopItem {
    private final String displayName;
    private final String description;

    public CosmeticShopItem(String cosmeticId, int price, String displayName, String description) {
        super(cosmeticId, price, ShopCategory.COSMETICS, true);
        this.displayName = displayName;
        this.description = description;
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
    public boolean canPurchase(PlayerData data) {
        if (data.getCoins() < price) return false;
        // Проверяем через косметику, а не через purchasedItems
        return !data.getCosmeticData().hasPurchased(id);
    }

    @Override
    protected void onPurchase(ServerPlayer player, PlayerData data) {
        // Добавляем в косметику
        data.getCosmeticData().addPurchase(id);
        // Автоматически включаем
        data.getCosmeticData().setEnabled(id, true);

        player.displayClientMessage(
                Component.literal("§dВы приобрели: " + displayName), false);
        player.displayClientMessage(
                Component.literal("§7Управляйте эффектами в меню косметики!"), false);
    }

    @Override
    public List<Component> getTooltip(PlayerData data) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.literal(displayName).withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal(description).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.empty());

        if (data.getCosmeticData().hasPurchased(id)) {
            tooltip.add(Component.literal("✓ Куплено").withStyle(ChatFormatting.GREEN));
            boolean enabled = data.getCosmeticData().isEnabled(id);
            tooltip.add(Component.literal(enabled ? "Включено" : "Выключено")
                    .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED));
        } else {
            tooltip.add(Component.literal("Цена: " + price + " монет").withStyle(ChatFormatting.GOLD));
        }
        return tooltip;
    }
}
