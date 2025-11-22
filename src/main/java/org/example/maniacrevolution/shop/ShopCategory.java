package org.example.maniacrevolution.shop;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum ShopCategory {
    COSMETICS("Косметика", ChatFormatting.LIGHT_PURPLE),
    BOOSTS("Усиления", ChatFormatting.GREEN),
    UPGRADES("Улучшения", ChatFormatting.GOLD);

    private final String displayName;
    private final ChatFormatting color;

    ShopCategory(String name, ChatFormatting color) {
        this.displayName = name;
        this.color = color;
    }

    public Component getDisplayName() {
        return Component.literal(displayName).withStyle(color);
    }
}