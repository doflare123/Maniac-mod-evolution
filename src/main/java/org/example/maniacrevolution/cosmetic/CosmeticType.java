package org.example.maniacrevolution.cosmetic;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum CosmeticType {
    PARTICLE("Частицы", ChatFormatting.LIGHT_PURPLE, "Визуальные эффекты вокруг игрока"),
    WEAPON_EFFECT("Эффект оружия", ChatFormatting.RED, "Визуальные эффекты на оружии"),
    PERK_SKIN("Скин перка", ChatFormatting.GOLD, "Изменяет внешний вид иконки перка"),
    TRAIL("След", ChatFormatting.AQUA, "Оставляет след за игроком");

    private final String displayName;
    private final ChatFormatting color;
    private final String description;

    CosmeticType(String name, ChatFormatting color, String desc) {
        this.displayName = name;
        this.color = color;
        this.description = desc;
    }

    public Component getDisplayName() {
        return Component.literal(displayName).withStyle(color);
    }

    public String getDescription() { return description; }
    public ChatFormatting getColor() { return color; }
}