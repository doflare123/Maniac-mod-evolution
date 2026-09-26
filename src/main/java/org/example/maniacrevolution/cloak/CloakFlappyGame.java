package org.example.maniacrevolution.cloak;

/** Server-owned simulation; input packets never contain a score or a win flag. */
public final class CloakFlappyGame {
    public static final int WIDTH = 240, HEIGHT = 100, BIRD_X = 48, RADIUS = 3;
    public static final int PIPE_WIDTH = 14, GAP = 48, GOAL = 5, DURATION = 140;
    private static final int[] CENTERS = {50, 42, 56, 44, 54};
    private float bird = 50, velocity;
    private int elapsed, runTicks, score, lastFlap = -10;
    private boolean flap;

    public void flap() {
        if (elapsed - lastFlap >= 3) {
            flap = true;
            lastFlap = elapsed;
        }
    }

    public void tick() {
        elapsed++;
        runTicks++;
        if (flap) velocity = -2.3f;
        flap = false;
        velocity += 0.28f;
        bird += velocity;
        boolean crash = bird < RADIUS || bird > HEIGHT - RADIUS;
        for (int i = 0; i < GOAL; i++) {
            float x = pipeX(runTicks, i);
            if (x < BIRD_X + RADIUS && x + PIPE_WIDTH > BIRD_X - RADIUS
                    && (bird - RADIUS < center(i) - GAP / 2f || bird + RADIUS > center(i) + GAP / 2f)) {
                crash = true;
            }
            if (i == score && x + PIPE_WIDTH <= BIRD_X - RADIUS) score++;
        }
        if (crash) {
            bird = 50;
            velocity = 0;
            runTicks = 0;
            score = 0;
        }
    }

    public static float pipeX(int ticks, int index) { return 170 + index * 64 - ticks * 4; }
    public static int center(int index) { return CENTERS[index]; }
    public float bird() { return bird; }
    public int runTicks() { return runTicks; }
    public int score() { return score; }
    public int remaining() { return Math.max(0, DURATION - elapsed); }
    public boolean finished() { return score >= GOAL || remaining() == 0; }
}
