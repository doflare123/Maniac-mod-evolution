package org.example.maniacrevolution.stamina;

public final class Stamina {
    public static final int CELLS = 10;
    // Walking recovery restores one point per tick. Sprint drains two.
    public static final int CELL = 5 * 20;
    public static final int MAX = CELLS * CELL;
    private static final int RECOVERY_SCALE = 100;
    private static final int STATIONARY_RECOVERY_BONUS = 15;

    private int amount;
    private int recoveryRemainder;

    public Stamina(int amount) {
        this.amount = Math.max(0, Math.min(MAX, amount));
    }

    public int amount() {
        return amount;
    }

    public void tick(boolean sprinting, boolean recovering, boolean stationary) {
        if (sprinting) {
            amount = Math.max(0, amount - 2);
        } else if (recovering && amount < MAX) {
            int recovery = recoveryRemainder + RECOVERY_SCALE + (stationary ? STATIONARY_RECOVERY_BONUS : 0);
            amount = Math.min(MAX, amount + recovery / RECOVERY_SCALE);
            recoveryRemainder = amount < MAX ? recovery % RECOVERY_SCALE : 0;
        }
    }

    public boolean spendJump() {
        return spend(CELL);
    }

    public boolean spend(int points) {
        if (points <= 0 || amount < points) return false;
        amount -= points;
        return true;
    }
}
