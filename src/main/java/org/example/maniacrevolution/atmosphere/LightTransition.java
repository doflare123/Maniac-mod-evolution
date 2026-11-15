package org.example.maniacrevolution.atmosphere;

/** Tick-smoothed source strength. Discovery refreshes must retain this state. */
public final class LightTransition {
    private float previous, current;

    public LightTransition() {}

    /** An already-lit world source must not play a delayed ignition animation on discovery. */
    public LightTransition(float initial) { previous = current = AtmosphereRules.clamp(initial); }

    public void snap(float target) { previous = current = AtmosphereRules.clamp(target); }

    public void tick(float target) {
        previous = current;
        current += (AtmosphereRules.clamp(target) - current) * 0.25F;
    }

    public float value(float partialTick) {
        return previous + (current - previous) * AtmosphereRules.clamp(partialTick);
    }

    public boolean dark() { return current < 0.005F; }
}
