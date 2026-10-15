package org.example.maniacrevolution.warden;

/** Tick-based, per-Warden ability timing. Cooldown begins when preparation begins. */
public final class WardenSniffCycle {
    public static final int PREPARATION = 10, DURATION = 100, COOLDOWN = 200;
    private long begin = -1;
    public boolean activate(long now) {
        if (cooldown(now) > 0) return false;
        begin = now; return true;
    }
    public int preparation(long now) { return begin < 0 ? 0 : remaining(begin + PREPARATION, now); }
    public int duration(long now) { return begin < 0 || preparation(now) > 0 ? 0 : remaining(begin + PREPARATION + DURATION, now); }
    public int cooldown(long now) { return begin < 0 ? 0 : remaining(begin + COOLDOWN, now); }
    private static int remaining(long end, long now) { return (int) Math.max(0, Math.min(COOLDOWN, end - now)); }
}
