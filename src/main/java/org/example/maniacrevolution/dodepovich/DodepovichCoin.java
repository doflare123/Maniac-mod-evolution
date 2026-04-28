package org.example.maniacrevolution.dodepovich;

public enum DodepovichCoin {
    ELUSIVENESS("coin_elusiveness", "Монетка Неуловимости"),
    INSIGHT("coin_insight", "Монетка Прозрения"),
    SHACKLES("coin_shackles", "Монетка Оков"),
    HEALTH("coin_health", "Монетка ЗОЖ"),
    EAGLE("coin_eagle", "Монетка Орла"),
    DEBT("coin_debt", "Монетка Долга"),
    REROLL("coin_reroll", "Монетка Реролла"),
    FATE("coin_fate", "Монеточка Судьбы");

    private final String id;
    private final String displayName;

    DodepovichCoin(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }
}
