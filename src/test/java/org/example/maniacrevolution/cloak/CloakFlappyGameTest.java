package org.example.maniacrevolution.cloak;

/** Standalone regression checks, like the existing nightmare synchronization checks. */
public final class CloakFlappyGameTest {
    public static void main(String[] args) {
        CloakFlappyGame idle = new CloakFlappyGame();
        for (int i = 0; i < 139; i++) {
            idle.tick();
            check(!idle.finished(), "A failed attempt must not shorten the seven-second timer");
        }
        idle.tick();
        check(idle.finished() && idle.remaining() == 0 && idle.score() == 0, "Timeout must free a non-playing victim");

        CloakFlappyGame winner = new CloakFlappyGame();
        int ticks = 0;
        while (!winner.finished() && ticks++ < 140) {
            if (winner.bird() > 53) winner.flap();
            winner.tick();
        }
        check(winner.score() == 5 && winner.remaining() > 0, "Five obstacles must be beatable before timeout");

        CloakFlappyGame single = new CloakFlappyGame(), spam = new CloakFlappyGame();
        for (int i = 0; i < 140; i++) {
            single.flap();
            for (int j = 0; j < 1000; j++) spam.flap();
            single.tick(); spam.tick();
            check(single.bird() == spam.bird() && single.score() == spam.score(), "Packet spam must not accelerate flaps");
        }
        System.out.println("Cloak checks passed: fixed timeout, attainable five-point escape, flap rate limiting.");
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
