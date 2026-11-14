package org.example.maniacrevolution.settings;

/** Host-selected mode. Stable ids are stored in the world and sent to every client. */
public enum GraphicsMode {
    OFF(0, "Отключить", 0, false),
    LIGHT(1, "Лёгкий", 0, false),
    NORMAL(2, "Обычный", 12, false),
    HEAVY(3, "Тяжёлый", 24, true);

    private final int id;
    private final String displayName;
    private final int fogSamples;
    private final boolean dynamicLights;

    GraphicsMode(int id, String displayName, int fogSamples, boolean dynamicLights) {
        this.id = id;
        this.displayName = displayName;
        this.fogSamples = fogSamples;
        this.dynamicLights = dynamicLights;
    }

    public int id() { return id; }
    public String displayName() { return displayName; }
    public int fogSamples() { return fogSamples; }
    public boolean enabled() { return this != OFF; }
    public boolean volumetric() { return fogSamples > 0; }
    public boolean dynamicLights() { return dynamicLights; }

    public String description() {
        return switch (this) {
            case OFF -> "Без атмосферы и ограничения обзора. Подходит для самых слабых ПК.";
            case LIGHT -> "Простой туман и приглушённые цвета. Без объёмного расчёта и дополнительного света.";
            case NORMAL -> "Объёмный туман средней детализации. Обзор ограничен 28 блоками.";
            case HEAVY -> "Подробный объёмный туман. Дополнительный свет при поддержке Veil Deferred.";
        };
    }

    public GraphicsMode step(int direction) {
        return fromId(Math.max(0, Math.min(HEAVY.id, id + Integer.signum(direction))));
    }

    public static GraphicsMode fromId(int id) {
        for (GraphicsMode mode : values()) if (mode.id == id) return mode;
        return NORMAL;
    }
}
