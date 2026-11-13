package org.example.maniacrevolution.atmosphere;

import static org.example.maniacrevolution.atmosphere.AtmosphereRules.Profile.*;

public final class AtmosphereRulesTest {
    public static void main(String[] args) {
        check(profile(true, true, 1, "survivors", false, false) == SURVIVOR, "survivor match");
        check(profile(true, true, 3, "maniac", true, false) == KEEPER, "keeper match");
        check(profile(true, true, 2, "maniac", false, false) == MANIAC, "maniac match");
        check(profile(true, true, 1, "maniac", false, true) == OFF, "warden owns his vision");
        check(profile(true, false, 1, "survivors", false, false) == OFF, "creative/spectator");
        check(profile(false, true, 1, "survivors", false, false) == OFF, "dead player");
        check(profile(true, true, 0, "survivors", false, false) == OFF, "lobby");
        check(profile(true, true, 4, "survivors", false, false) == OFF, "unknown phase");
        check(profile(true, true, 1, null, true, false) == OFF, "selected class is not team membership");
        check(AtmosphereRules.corruption(KEEPER, true, 0) == 0, "keeper has no sanity distortion");
        check(AtmosphereRules.corruption(SURVIVOR, false, 0) == 0, "disabled sanity mirror");
        check(AtmosphereRules.corruption(SURVIVOR, true, .2F) == .8F, "survivor corruption");
        check(AtmosphereRules.fogVisibility(8) == 1, "near space stays clear of range cutoff");
        check(AtmosphereRules.fogVisibility(18) == 1, "range cutoff starts at 18 blocks");
        check(AtmosphereRules.fogVisibility(20) > 0 && AtmosphereRules.fogVisibility(20) < 1, "20-block transition");
        check(AtmosphereRules.fogVisibility(25) < .25F, "25-block objects strongly fade");
        check(AtmosphereRules.fogVisibility(28) == 0, "28-block cutoff");
        check(AtmosphereRules.fogVisibility(100000) == 0, "far geometry cannot return");
        check(!AtmosphereRules.beyondFog(27 * 27), "near living entity remains renderable");
        check(AtmosphereRules.beyondFog(28 * 28), "hide living entity and name at cutoff");
        check(AtmosphereRules.beyondFog(100 * 100), "hide distant silhouettes");

        ChaseContact chase = new ChaseContact();
        chase.update(0, true, true);
        chase.update(5, true, true);
        check(chase.strength(5) == 0, "passing glance is not a chase");
        chase.update(10, false, false);
        chase.update(15, true, true);
        chase.update(20, true, true);
        check(chase.strength(20) == 0, "acquisition restarts after lost sight");
        chase.update(25, true, true);
        check(chase.strength(25) == 1, "sustained acquisition");
        chase.update(30, false, false);
        check(chase.strength(85) == 1, "corner grace");
        check(chase.strength(105) == .5F, "gradual release");
        check(chase.strength(125) == 0, "escape");
        chase.hit(140);
        check(chase.strength(140) == 1, "attack starts chase immediately");
        chase.update(220, false, true);
        check(chase.strength(220) == 1, "visible reacquisition during release");
        check(chase.strength(320) == 0, "contact eventually expires");
        ChaseContact other = new ChaseContact();
        check(other.strength(220) == 0, "each killer/victim pair is independent");
        System.out.println("Atmosphere: 31 checks passed");
    }

    private static AtmosphereRules.Profile profile(boolean alive, boolean survival, int phase,
                                                    String team, boolean keeper, boolean warden) {
        return AtmosphereRules.profile(alive, survival, phase, team, keeper, warden);
    }

    private static void check(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }
}
