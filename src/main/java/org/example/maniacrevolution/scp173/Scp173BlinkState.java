package org.example.maniacrevolution.scp173;

/** Server tick clock; closing the eyes never erases accumulated pressure. */
public final class Scp173BlinkState {
    public static final int DURATION = 10;
    private double meter = 1, pressure;
    private long until;
    private boolean forced;
    public double meter() { return meter; }
    public double pressure() { return pressure; }
    public int remaining(long now) { return (int) Math.max(0, until - now); }
    public boolean blink(long now) {
        if (remaining(now) > 0) return false;
        forced = false;
        meter = 1; until = now + DURATION; return true;
    }
    public void forceBlink(long now) {
        forced = true;
        meter = 1;
        until = now + DURATION;
    }
    public boolean forcedBlinking(long now) { return forced && remaining(now) > 0; }
    public void tick(long now, boolean looking) {
        pressure = Math.max(0, Math.min(1, pressure + (looking ? 1 : -1) / 300.0));
        if (remaining(now) > 0) return;
        meter = Math.max(0, meter - (1 + 5 * pressure) / 120.0);
        if (meter < 1.0e-9) blink(now);
    }
}
