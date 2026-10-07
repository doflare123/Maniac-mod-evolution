package org.example.maniacrevolution.strange;

/** Camera arm motion uses the same wind-up, impact and recovery ticks as the server. */
public final class WhipMotion {
    private WhipMotion() {}
    public static float raise(float ticks) {
        if (ticks <= 10) return smooth(ticks / 10);
        if (ticks <= StrangeRules.WINDUP_TICKS) return 1 - smooth((ticks - 10) / 3);
        return 0;
    }
    public static float thrust(float ticks) {
        if (ticks <= 10) return 0;
        if (ticks <= StrangeRules.WINDUP_TICKS) return smooth((ticks - 10) / 3);
        return 1 - smooth((ticks - StrangeRules.WINDUP_TICKS) / (StrangeRules.WHIP_TICKS - StrangeRules.WINDUP_TICKS));
    }
    private static float smooth(float t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }
}
