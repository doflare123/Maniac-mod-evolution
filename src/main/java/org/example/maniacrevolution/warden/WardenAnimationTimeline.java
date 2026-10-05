package org.example.maniacrevolution.warden;

/** Common, server-timed visual state; does not change ability timing or damage. */
public final class WardenAnimationTimeline {
    public enum Action { NONE, ATTACK, CHARGE, RELEASE, RECOVER }
    public static final int ATTACK = 8, RELEASE = 26, RECOVER = 6, SNIFF = 84, LEASE = 60;
    public record Frame(Action action, double elapsed, int charge, double sniff) {
        public static final Frame IDLE = new Frame(Action.NONE, 0, 0, -1);
    }
    public record State(Action action, long begin, int charge, long sniff, long tick) {
        public boolean valid() {
            return action != null && tick >= 0 && begin >= -1 && begin <= tick && sniff >= -1 && sniff <= tick
                    && charge >= 0 && charge <= 20 && (action == Action.NONE ? begin == -1 : begin >= 0);
        }
        public Frame frame(double now) {
            if (!valid() || now - tick > LEASE || tick - now > 20) return Frame.IDLE;
            double elapsed = begin < 0 ? 0 : Math.max(0, now - begin);
            var current = action;
            int duration = switch (action) { case ATTACK -> ATTACK; case RELEASE -> RELEASE; case RECOVER -> RECOVER; default -> Integer.MAX_VALUE; };
            if (elapsed >= duration) current = Action.NONE;
            double scent = sniff < 0 || now - sniff >= SNIFF ? -1 : Math.max(0, now - sniff);
            return new Frame(current, elapsed, charge, scent);
        }
    }
    private WardenAnimationTimeline() {}
}
