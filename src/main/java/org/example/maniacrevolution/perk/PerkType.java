package org.example.maniacrevolution.perk;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum PerkType {
    PASSIVE("Пассивный", ChatFormatting.GOLD, 0xFFFFC857),
    ACTIVE("Активный", ChatFormatting.AQUA, 0xFF52D6C7),
    HYBRID("Гибридный", ChatFormatting.LIGHT_PURPLE, 0xFFC28BFF),
    PASSIVE_COOLDOWN("Пассивный", ChatFormatting.GOLD, 0xFFFFC857);

    private final String displayName;
    private final ChatFormatting color;
    private final int argbColor;

    PerkType(String displayName, ChatFormatting color, int argbColor) {
        this.displayName = displayName;
        this.color = color;
        this.argbColor = argbColor;
    }

    public Component getDisplayName() {
        return Component.literal(displayName).withStyle(color);
    }

    public ChatFormatting getColor() { return color; }
    public int getArgbColor() { return argbColor; }

    public boolean hasActiveAbility() {
        return this == ACTIVE || this == HYBRID;
    }

    public boolean hasPassiveAbility() {
        return this == PASSIVE || this == HYBRID;
    }
}
