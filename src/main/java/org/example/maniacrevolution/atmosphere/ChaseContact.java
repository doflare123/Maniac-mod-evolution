package org.example.maniacrevolution.atmosphere;

/** Server-tick clock: sustained acquisition, contact grace and a gradual release. */
public final class ChaseContact {
    public static final int ACQUIRE_TICKS = 10;
    public static final int GRACE_TICKS = 60;
    public static final int RELEASE_TICKS = 40;
    private long acquisition = -1;
    private long lastContact = -1;

    public void update(long tick, boolean acquiring, boolean visible) {
        if (strength(tick) > 0) {
            if (visible) lastContact = tick;
            return;
        }
        if (!acquiring) {
            acquisition = -1;
            return;
        }
        if (acquisition < 0) acquisition = tick;
        if (tick - acquisition >= ACQUIRE_TICKS) hit(tick);
    }

    public void hit(long tick) {
        lastContact = tick;
        acquisition = -1;
    }

    public float strength(long tick) {
        if (lastContact < 0) return 0;
        return AtmosphereRules.clamp(1 - (tick - lastContact - GRACE_TICKS) / (float) RELEASE_TICKS);
    }
}
