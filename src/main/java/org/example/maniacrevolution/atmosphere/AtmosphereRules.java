package org.example.maniacrevolution.atmosphere;

/** Decisions shared by the prototype and its dependency-free regression checks. */
public final class AtmosphereRules {
    public enum Profile { OFF, SURVIVOR, MANIAC, KEEPER }

    private AtmosphereRules() {}

    public static Profile profile(boolean alive, boolean survivalOrAdventure, int phase,
                                  String team, boolean keeper, boolean warden) {
        if (!alive || !survivalOrAdventure || phase < 1 || phase > 3) return Profile.OFF;
        if ("survivors".equals(team)) return Profile.SURVIVOR;
        if (!"maniac".equals(team) || warden) return Profile.OFF;
        return keeper ? Profile.KEEPER : Profile.MANIAC;
    }

    public static float corruption(Profile profile, boolean sanityActive, float sanityPercent) {
        return profile == Profile.SURVIVOR && sanityActive ? 1 - clamp(sanityPercent) : 0;
    }

    public static float clamp(float value) {
        return Float.isFinite(value) ? Math.max(0, Math.min(1, value)) : 0;
    }
}
