package org.example.maniacrevolution.shop.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.data.PlayerData;
import org.example.maniacrevolution.shop.ShopCategory;
import org.example.maniacrevolution.shop.ShopItem;

import java.util.ArrayList;
import java.util.List;

public class PresetSlotItem extends ShopItem {

    public PresetSlotItem(String id, int price) {
        super(id, price, ShopCategory.UPGRADES, false); // Можно покупать несколько раз
    }

    @Override
    public Component getName() {
        return Component.literal("Доп. слот пресета");
    }

    @Override
    public Component getDescription() {
        return Component.literal("Добавляет +1 слот для сохранённых наборов перков");
    }

    @Override
    public boolean canPurchase(PlayerData data) {
        if (data.getCoins() < price) return false;
        // Максимум 10 слотов
        return data.getMaxPresets() < 10;
    }

    @Override
    protected void onPurchase(ServerPlayer player, PlayerData data) {
        data.increaseMaxPresets();
        player.displayClientMessage(
                Component.literal("§6+1 слот для пресетов! Теперь у вас: " + data.getMaxPresets()), false);
    }

    @Override
    public List<Component> getTooltip(PlayerData data) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(getName().copy().withStyle(ChatFormatting.GOLD));
        tooltip.add(getDescription().copy().withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Текущих слотов: " + data.getMaxPresets() + "/10")
                .withStyle(ChatFormatting.DARK_GRAY));

        if (data.getMaxPresets() >= 10) {
            tooltip.add(Component.literal("Максимум достигнут!").withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.literal("Цена: " + price + " монет").withStyle(ChatFormatting.GOLD));
        }
        return tooltip;
    }
}
