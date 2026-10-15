package org.example.maniacrevolution.scp173;

/** Independent cooldown and next-use discount for each statue. All units are ticks. */
public final class Scp173AbilityState {
    private double lightCooldown, observation;
    private long meleeUntil;
    public void tick(int observers) {
        lightCooldown = Math.max(0, lightCooldown - (1 + 0.2 * observers));
        if (observers > 0) observation = Math.min(1200, observation + 1 + 0.15 * observers);
    }
    public float cost() { return (float) (10 * (1 - observation / 1200)); }
    public double lightCooldown() { return lightCooldown; }
    public double observation() { return observation; }
    public boolean lightReady() { return lightCooldown <= 1.0e-8; }
    public void usedLight() { lightCooldown = 3600; observation = 0; }
    public int meleeCooldown(long now) { return (int) Math.max(0, meleeUntil - now); }
    public boolean melee(long now) { if (meleeCooldown(now) > 0) return false; meleeUntil = now + 100; return true; }
    public void changeClock(long previous, long current) {
        meleeUntil = current + meleeCooldown(previous);
    }
}
