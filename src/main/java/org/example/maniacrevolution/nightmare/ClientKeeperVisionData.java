package org.example.maniacrevolution.nightmare;

import java.util.List;

/** A short-lived mirror, containing neither entities nor world references. */
public final class ClientKeeperVisionData {
    private static List<KeeperSoulState> souls = List.of();
    private static int age;

    private ClientKeeperVisionData() {}

    public static void update(List<KeeperSoulState> next) {
        souls = List.copyOf(next.subList(0, Math.min(next.size(), KeeperSoulState.MAX_SOULS)));
        age = 0;
    }

    public static void tick() {
        if (++age >= KeeperSoulState.TIMEOUT_TICKS) clear();
    }

    public static List<KeeperSoulState> souls() { return souls; }

    public static void clear() {
        souls = List.of();
        age = 0;
    }
}
