package org.example.maniacrevolution.stamina;

public final class Stamina {
    public static final int CELLS = 10;
    // One point per game tick makes one cell equal to five seconds.
    public static final int CELL = 5 * 20;
    public static final int MAX = CELLS * CELL;

    private int amount;

    public Stamina(int amount) {
        this.amount = Math.max(0, Math.min(MAX, amount));
    }

    public int amount() {
        return amount;
    }

    public void tick(boolean sprinting, boolean recovering) {
        if (sprinting) amount = Math.max(0, amount - 1);
        else if (recovering) amount = Math.min(MAX, amount + 1);
    }

    public boolean spendJump() {
        if (amount < CELL) return false;
        amount -= CELL;
        return true;
    }
}
