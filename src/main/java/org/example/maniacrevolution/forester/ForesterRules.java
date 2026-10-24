package org.example.maniacrevolution.forester;

/** Shared timing and ballistics. All durations are game ticks. */
public final class ForesterRules {
    public static final int CLASS_ID = 16;
    public static final int TRAP_DELAY = 30, TRAP_GAME = 100, REARM = 50, COLLECT = 30;
    public static final int BLEED = 200, PEPPER_BURN = 300, PEPPER_EFFECT = 100, SNEEZE_INTERVAL = 30;
    public static final int FLASH_DURATION = 60, FLASH_BLIND = 40, DEFAULT_FLASH_USES = 5;
    public static final double PEPPER_RADIUS = 5, FLASH_RANGE = 10;
    public static final double THROW_SPEED = 1.1, GRAVITY = (double) .03F, DRAG = (double) .99F, WATER_DRAG = (double) .8F;
    public static final float ZONE_WIDTH = 0.16F;

    private ForesterRules() {}

    public static float needle(double age) {
        double phase = ((Math.max(0, age - TRAP_DELAY) % 32) + 32) % 32 / 16;
        return (float) (phase <= 1 ? phase : 2 - phase);
    }

    public static boolean hit(double age, float target) {
        return age >= TRAP_DELAY && age < TRAP_DELAY + TRAP_GAME
                && Math.abs(needle(age) - target) <= ZONE_WIDTH / 2;
    }

    public static boolean expired(int age) { return age >= TRAP_DELAY + TRAP_GAME; }
}
