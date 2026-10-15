package org.example.maniacrevolution.techies;

/** Pure boundaries shared by production and regression checks. */
public final class TechiesRules {
    private TechiesRules() {}
    public static boolean reversible(long elapsed) { return elapsed >= 0 && elapsed < 4; }
    public static boolean spaced(double squaredDistance) { return squaredDistance >= 4; }
    public static float drain(float mana) { return Math.max(0, Math.min(15, mana)); }
    /** Ignore sub-packet standing jitter; real movement and teleports still cancel. */
    public static boolean moved(double squaredDistance) { return squaredDistance > 0.003 * 0.003; }
    public static boolean active(boolean alive, boolean gameMode, int phase, int id, String team) {
        return alive && gameMode && phase >= 1 && phase <= 3
                && id == org.example.maniacrevolution.character.survivor.TechiesClass.SCOREBOARD_ID && "survivors".equals(team);
    }
}
