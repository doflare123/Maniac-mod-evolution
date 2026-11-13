package org.example.maniacrevolution.lich.client;

/** GUI-independent layout and damped spring, shared with the headless checks. */
public final class LichCorpseLayout {
    public static final int WIDTH = 620, HEIGHT = 370, CARD_WIDTH = 132, CARD_HEIGHT = 222;
    private LichCorpseLayout() {}
    public record Reveal(float opacity, double scale, double offsetY) {}
    public static Reveal reveal(double seconds, double delay, double travel) {
        double time = Math.max(0, seconds - delay);
        double progress = Math.min(1, time / .28);
        double spring = spring(time);
        return new Reveal((float)(progress * progress * (3 - 2 * progress)),
                .82 + .18 * spring, travel * (1 - spring));
    }
    public static double spring(double seconds) {
        double t = Math.max(0, seconds), damping = .64, frequency = 13;
        double damped = frequency * Math.sqrt(1 - damping * damping);
        return 1 - Math.exp(-damping * frequency * t)
                * (Math.cos(damped * t) + damping * frequency / damped * Math.sin(damped * t));
    }
    public static double scale(int width, int height) {
        return Math.max(.01, Math.min(1.25, Math.min((width - 24.0) / WIDTH, (height - 16.0) / HEIGHT)));
    }
    public static double centerX(int index, double seconds) {
        return WIDTH / 2.0 + (index - 1.5) * 144 * spring(seconds - index * .045);
    }
    public static double cardScale(int index, double seconds) {
        return .12 + .88 * spring(seconds - index * .045);
    }
}
