package org.example.maniacrevolution.nightmare;

import java.util.UUID;

/** Only the server decides whether a keeper is actually draining a soul. */
public record KeeperSoulState(UUID playerId, float sanity, boolean feeding) {
    public static final int MAX_SOULS = 16;
    public static final int REFRESH_TICKS = 4;
    public static final int TIMEOUT_TICKS = 40;

    public KeeperSoulState {
        sanity = Float.isFinite(sanity) ? Math.max(0, Math.min(1, sanity)) : 1;
    }
}
