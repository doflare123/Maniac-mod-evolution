package org.example.maniacrevolution.helsing;

/** Arithmetic shared by the mod's existing wall-clock and game-tick cooldowns. */
public final class HelsingTiming {
    public static long pauseStart(long start, long durationMillis, long now, long elapsedMillis) {
        long intervalStart = now - Math.max(0, elapsedMillis);
        if (intervalStart - start >= durationMillis) return start; // already ready before this frozen interval
        return start + Math.min(Math.max(0, elapsedMillis), Math.max(0, now - start));
    }
    public static long pauseDeadline(long until, long tick) { return until >= tick && until > 0 ? until + 1 : until; }
    private HelsingTiming() {}
}
