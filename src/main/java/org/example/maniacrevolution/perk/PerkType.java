package org.example.maniacrevolution.perk;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum PerkType {
    PASSIVE("Пассивный", ChatFormatting.BLUE),
    ACTIVE("Активный", ChatFormatting.RED),
    HYBRID("Гибридный", ChatFormatting.LIGHT_PURPLE),
    PASSIVE_COOLDOWN("Пассивный", ChatFormatting.BLUE);

    private final String displayName;
    private final ChatFormatting color;

    PerkType(String displayName, ChatFormatting color) {
        this.displayName = displayName;
        this.color = color;
    }

    public Component getDisplayName() {
        return Component.literal(displayName).withStyle(color);
    }

    public ChatFormatting getColor() { return color; }

    public boolean hasActiveAbility() {
        return this == ACTIVE || this == HYBRID;
    }

    public boolean hasPassiveAbility() {
        return this == PASSIVE || this == HYBRID;
    }
}
