package org.example.maniacrevolution.warden;

/** Integer units: 100 running ticks spend the pool; 300 resting ticks refill it. */
public final class WardenStamina {
    public static final int CAPACITY = 300, DRAIN = 3, DELAY = 20;
    private int amount = CAPACITY, delay;
    private boolean exhausted, boosting;
    public void tick(boolean sprint, boolean moving) {
        if (!sprint && amount >= DRAIN) exhausted = false;
        boosting = sprint && !exhausted && amount > 0;
        if (boosting && moving) {
            amount = Math.max(0, amount - DRAIN);
            delay = DELAY;
            if (amount == 0) { exhausted = true; boosting = false; }
        } else if (delay > 0) delay--;
        else amount = Math.min(CAPACITY, amount + 1);
    }
    public int amount() { return amount; }
    public int delay() { return delay; }
    public boolean boosting() { return boosting; }
    public boolean exhausted() { return exhausted; }
}
