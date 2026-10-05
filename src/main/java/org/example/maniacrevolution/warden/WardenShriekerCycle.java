package org.example.maniacrevolution.warden;

/** Persistent cooldown; interrupted screams never resume. */
public final class WardenShriekerCycle {
    private long cooldownUntil, shriekUntil;
    public WardenShriekerCycle(long savedCooldown) { cooldownUntil = Math.max(0, savedCooldown); }
    public boolean cooling(long now) { return now < cooldownUntil; }
    public boolean shrieking(long now) { return now < shriekUntil; }
    public long savedCooldown() { return cooldownUntil; }
    public void start(long now) {
        cooldownUntil = now + WardenShriekerRules.COOLDOWN;
        shriekUntil = now + WardenShriekerRules.SHRIEK_TICKS;
    }
    public void stop() { shriekUntil = 0; }
}
