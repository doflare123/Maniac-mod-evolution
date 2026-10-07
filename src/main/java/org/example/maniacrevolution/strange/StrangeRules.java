package org.example.maniacrevolution.strange;

/** Values in health points, mana points, blocks, and server ticks. */
public final class StrangeRules {
    public static final int PORTAL_TICKS = 300, WINDUP_TICKS = 13, WHIP_TICKS = 24;
    public static final int WHIP_COOLDOWN = 40, SLOW_TICKS = 60;
    public static final double WHIP_RANGE = 6;
    public static final float WHIP_MANA = 8;
    public static final int COLLAPSE_TICKS = 20;
    public static final int OPEN_CAST_TICKS = 100, CLOSE_CAST_TICKS = 30;
    public static float openingScale(float ticks) { return smooth(ticks / OPEN_CAST_TICKS); }
    public static float closingScale(float ticks, int duration) { return 1 - smooth(ticks / Math.max(1, duration)); }
    public static float gestureAngle(float ticks, boolean closing) {
        float progress=Math.max(0,Math.min(1,ticks/(closing?CLOSE_CAST_TICKS:OPEN_CAST_TICKS)));
        return (float)(progress*Math.PI*2*(closing?-1:3));
    }
    private static float smooth(float t) { t=Math.max(0,Math.min(1,t)); return t*t*(3-2*t); }
    public static float collapseDamage(float maxHealth) { return Math.max(0, maxHealth) * .25f; }
    public static float portalCost(float mana) { return Math.min(Math.max(0, mana) * .5f, 30); }
    public static float shieldCost(float damage) { return Math.max(0, damage) * 2; }
    /** Swept test in the portal's local coordinates: avoids skipping a thin plane at high speed. */
    public static boolean entersPortal(double side, double height, double depth,
                                      double previousSide, double previousHeight, double previousDepth) {
        if (Math.abs(side) <= .9 && height >= -.35 && height <= 2.3 && Math.abs(depth) <= .45) return true;
        if (depth * previousDepth >= 0 || Math.abs(depth - previousDepth) < 1e-9) return false;
        double t = -previousDepth / (depth - previousDepth);
        double crossingSide = previousSide + t * (side - previousSide);
        double crossingHeight = previousHeight + t * (height - previousHeight);
        return Math.abs(crossingSide) <= .9 && crossingHeight >= -.35 && crossingHeight <= 2.3;
    }
    private StrangeRules() {}
}
