package org.example.maniacrevolution.downed;

public enum DownedState {
    /** Обычное живое состояние */
    ALIVE,

    /** Игрок лежит — ждёт помощи или таймера смерти */
    DOWNED,

    /** Игрок поднят, но ослаблен — следующая смерть фатальна */
    WEAKENED
}
