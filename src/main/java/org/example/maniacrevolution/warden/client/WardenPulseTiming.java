package org.example.maniacrevolution.warden.client;

/** Prototype timing in client ticks, independent of rendering and key mappings. */
final class WardenPulseTiming {
    static final double DURATION = 80;
    static final double SPEED = 2.4; // 48 blocks/second: 12 blocks in 0.25 seconds.
    static final double ATTACK = 0.7; // Smooth reveal within 35 ms, interpolated every rendered frame.

    private WardenPulseTiming() {}

    static float brightness(double age, double distance) {
        return brightness(age, distance, 12, DURATION);
    }

    static float brightness(double age, double distance, double radius, double duration) {
        double localAge = age - distance / SPEED;
        if (localAge <= 0 || distance > radius || age >= duration) return 0;
        double attack = Math.min(1, localAge / ATTACK);
        attack = attack * attack * (3 - 2 * attack);
        // Fade begins at arrival, so distant points also reveal at full strength.
        double fade = Math.max(0, 1 - localAge / (duration - distance / SPEED));
        return (float) (attack * fade * fade);
    }
}
