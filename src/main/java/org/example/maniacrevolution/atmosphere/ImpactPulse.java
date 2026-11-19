package org.example.maniacrevolution.atmosphere;

/** Short damage impulse, independent of health and of the longer sanity state. */
public final class ImpactPulse {
    public static final int DURATION_TICKS = 18;
    private int remaining, lastHurtTime;
    private float previous, current;

    public void trigger() {
        remaining = DURATION_TICKS;
    }

    public void tick(int hurtTime) {
        previous = current;
        if (hurtTime > lastHurtTime) trigger();
        lastHurtTime = hurtTime;
        float t = remaining / (float) DURATION_TICKS;
        current = t * t * (3 - 2 * t);
        if (remaining > 0) remaining--;
    }

    public float value(float partialTick) {
        return previous + (current - previous) * AtmosphereRules.clamp(partialTick);
    }

    public void reset() {
        remaining = lastHurtTime = 0;
        previous = current = 0;
    }
}
