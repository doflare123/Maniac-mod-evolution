package org.example.maniacrevolution.engineer;

/** Balance and input tolerances shared by server, screens and regression checks. */
public final class EngineerRules {
    private EngineerRules() {}
    public static final int CLASS_ID = 17;
    public static final int START_SCRAP = 40, MAX_SCRAP = 60, MATCH_INCOME = 120;
    public static final int BARRICADE_COST = 20, PANEL_COST = 25, PLATE_COST = 25;
    public static final int MAX_BARRICADES = 5, MAX_PANELS = 8;
    public static final int INSTALL_TICKS = 50, BARRICADE_LIFETIME = 2400;
    public static final int BREAK_TICKS = 40, COLLECT_TICKS = 20, PLATE_TICKS = 40;
    public static final int PANEL_USES = 3, PLAY_TICKS = 200, PANEL_COOLDOWN = 300;
    public static final int BUILD_COOLDOWN = 100;
    public static final float ARMOR_PER_SCRAP = 3, MAX_ARMOR = 20;
    public static final double MOVE_TOLERANCE = .25, TURN_TOLERANCE = 12, REACH = 4;
    public static double angleDistance(double a, double b) { return Math.abs(((a - b) % 360 + 540) % 360 - 180); }
    public static boolean interrupted(double distanceSquared, double yaw, double pitch) {
        return distanceSquared > MOVE_TOLERANCE * MOVE_TOLERANCE
                || angleDistance(yaw, 0) > TURN_TOLERANCE || Math.abs(pitch) > TURN_TOLERANCE;
    }
    public static int cost(int type) { return type == 0 ? BARRICADE_COST : type == 1 ? PANEL_COST : PLATE_COST; }
    public static boolean phase(int phase) { return phase >= 1 && phase <= 3; }
    public static float clampArmor(float armor) { return Float.isFinite(armor) ? Math.max(0, Math.min(MAX_ARMOR, armor)) : 0; }
    public static float addArmor(float current) { return Math.min(MAX_ARMOR, clampArmor(current) + ARMOR_PER_SCRAP); }
    public record ArmorHit(float armor, float damage) {}
    public static ArmorHit absorb(float armor, float damage) {
        armor = clampArmor(armor);
        if (!Float.isFinite(damage) || damage <= 0) return new ArmorHit(armor, damage);
        float absorbed = Math.min(armor, damage);
        return new ArmorHit(armor - absorbed, damage - absorbed);
    }
}
