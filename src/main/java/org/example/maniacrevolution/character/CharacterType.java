package org.example.maniacrevolution.character;

/**
 * Тип персонажа - Выживший или Маньяк
 */
public enum CharacterType {
    SURVIVOR("Выживший", "SurvivorClass"),
    MANIAC("Маньяк", "ManiacClass");

    private final String displayName;
    private final String scoreboardName;

    CharacterType(String displayName, String scoreboardName) {
        this.displayName = displayName;
        this.scoreboardName = scoreboardName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getScoreboardName() {
        return scoreboardName;
    }
}