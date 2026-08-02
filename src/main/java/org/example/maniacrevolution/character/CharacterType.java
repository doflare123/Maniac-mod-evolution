package org.example.maniacrevolution.character;

import net.minecraft.network.chat.Component;

/**
 * Тип персонажа - Выживший или Маньяк
 */
public enum CharacterType {
    SURVIVOR("survivor", "SurvivorClass"),
    MANIAC("maniac", "ManiacClass");

    private final String translationSuffix;
    private final String scoreboardName;

    CharacterType(String translationSuffix, String scoreboardName) {
        this.translationSuffix = translationSuffix;
        this.scoreboardName = scoreboardName;
    }

    public String getDisplayName() {
        return Component.translatable("character.maniacrev.type." + translationSuffix).getString();
    }

    public String getScoreboardName() {
        return scoreboardName;
    }
}
