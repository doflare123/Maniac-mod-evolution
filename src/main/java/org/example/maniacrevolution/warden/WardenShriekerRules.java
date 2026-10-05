package org.example.maniacrevolution.warden;

public final class WardenShriekerRules {
    public static final int COOLDOWN = 100, SHRIEK_TICKS = 90, ECHO_TICKS = 160;
    public static final int PULSE_TICKS = 60;
    public static final double RADIUS = 12;
    public static final int LISTENER_RADIUS = 8;
    private WardenShriekerRules() {}
    public static boolean trigger(boolean enabled, boolean alive, boolean survivalAdventure, int phase,
                                  String team, boolean cooling) {
        return enabled && !cooling && WardenMatchRules.participant(alive, survivalAdventure, phase, team)
                && "survivors".equalsIgnoreCase(team);
    }
    public static boolean fresh(long tick, long now) { return tick >= 0 && now - tick < ECHO_TICKS && tick - now <= 20; }
}
