package org.example.maniacrevolution.scp173;

/** Match state for one statue. Charge and cooldown durations are measured in ticks. */
public final class Scp173AbilityState {
    public static final int TENSION_TICKS = 200;
    public static final int MOMENTUM_TICKS = 160;
    private static final int MOMENTUM_LOSS_PER_TICK = MOMENTUM_TICKS / 40;
    private double lightCooldown, observation;
    private int tension, momentum;
    private long meleeUntil;
    public boolean tick(int observers, boolean moved) {
        lightCooldown = Math.max(0, lightCooldown - (1 + 0.2 * observers));
        if (observers == 0) {
            if (moved) momentum = Math.max(0, momentum - MOMENTUM_LOSS_PER_TICK);
            return false;
        }
        observation = Math.min(1200, observation + 1 + 0.15 * observers);
        momentum = Math.min(MOMENTUM_TICKS, momentum + 1);
        return tension < TENSION_TICKS && ++tension == TENSION_TICKS;
    }
    public int tension() { return tension; }
    public int momentum() { return momentum; }
    public double movementBonus() { return 0.5 + 0.5 * momentum / MOMENTUM_TICKS; }
    public boolean useTension() {
        if (tension < TENSION_TICKS) return false;
        tension = 0;
        return true;
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
