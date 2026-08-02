package org.example.maniacrevolution.perk;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum PerkType {
    PASSIVE("passive", ChatFormatting.GOLD, 0xFFFFC857),
    ACTIVE("active", ChatFormatting.AQUA, 0xFF52D6C7),
    HYBRID("hybrid", ChatFormatting.LIGHT_PURPLE, 0xFFC28BFF),
    PASSIVE_COOLDOWN("passive_cooldown", ChatFormatting.GOLD, 0xFFFFC857),
    CHARGED("charged", ChatFormatting.GREEN, 0xFF70E28A);

    private final String translationSuffix;
    private final ChatFormatting color;
    private final int argbColor;

    PerkType(String translationSuffix, ChatFormatting color, int argbColor) {
        this.translationSuffix = translationSuffix;
        this.color = color;
        this.argbColor = argbColor;
    }

    public Component getDisplayName() {
        return Component.translatable("perk.maniacrev.type." + translationSuffix).withStyle(color);
    }

    public ChatFormatting getColor() { return color; }
    public int getArgbColor() { return argbColor; }

    public boolean hasActiveAbility() {
        return this == ACTIVE || this == HYBRID;
    }

    public boolean hasPassiveAbility() {
        return this == PASSIVE || this == HYBRID || this == CHARGED;
    }
}
