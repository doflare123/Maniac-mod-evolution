package org.example.maniacrevolution.perk;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum PerkPhase {
    START("Начало игры", ChatFormatting.AQUA, -1),      // Особая фаза - при вызове /maniacrev start
    HUNT("Охота", ChatFormatting.YELLOW, 1),             // phaseGame = 1
    MIDGAME("Мидгейм", ChatFormatting.GOLD, 2),          // phaseGame = 2
    REVERSAL("Переворот", ChatFormatting.RED, 3),        // phaseGame = 3
    ANY("Любая", ChatFormatting.WHITE, -1);              // Работает всегда (кроме phaseGame = 0)

    private final String displayName;
    private final ChatFormatting color;
    private final int scoreboardValue;

    PerkPhase(String displayName, ChatFormatting color, int scoreboardValue) {
        this.displayName = displayName;
        this.color = color;
        this.scoreboardValue = scoreboardValue;
    }

    public Component getDisplayName() {
        return Component.literal(displayName).withStyle(color);
    }

    public ChatFormatting getColor() { return color; }
    public int getScoreboardValue() { return scoreboardValue; }

    public static PerkPhase fromScoreboardValue(int value) {
        return switch (value) {
            case 1 -> HUNT;
            case 2 -> MIDGAME;
            case 3 -> REVERSAL;
            default -> null;
        };
    }
}
