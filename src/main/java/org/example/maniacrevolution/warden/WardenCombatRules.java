package org.example.maniacrevolution.warden;

public final class WardenCombatRules {
    public static final int MELEE_CD = 100, WAVE_CD = 600, MIN_CHARGE = 4, MAX_CHARGE = 20, INPUT_LEASE = 60;
    public static final float MELEE_DAMAGE = 9;
    public static final double MELEE_RANGE = 3, MELEE_MARGIN = 0.15, WAVE_SPEED = 1, WAVE_RADIUS = 0.35;
    private WardenCombatRules() {}
    public record Shot(int charge, float damage, double range, float strength) {}
    public static Shot shot(long charge) {
        if (charge < MIN_CHARGE) return null;
        int ticks = (int) Math.min(MAX_CHARGE, charge);
        float damage = ticks <= 10 ? 3 + (ticks - 4) * 0.5F : 6 + (ticks - 10) * 0.6F;
        return new Shot(ticks, damage, ticks * 2, 0.25F + 0.75F * (ticks - 4) / 16F);
    }
}
