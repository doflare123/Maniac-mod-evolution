package org.example.maniacrevolution.atmosphere;

import static org.example.maniacrevolution.atmosphere.AtmosphereRules.Profile.*;

public final class AtmosphereRulesTest {
    public static void main(String[] args) {
        var off = org.example.maniacrevolution.settings.GraphicsMode.OFF;
        var light = org.example.maniacrevolution.settings.GraphicsMode.LIGHT;
        var normal = org.example.maniacrevolution.settings.GraphicsMode.NORMAL;
        var heavy = org.example.maniacrevolution.settings.GraphicsMode.HEAVY;
        check(!off.enabled() && !off.volumetric() && !off.dynamicLights(), "OFF removes atmosphere and range limit");
        check(light.enabled() && !light.volumetric() && !light.dynamicLights(), "LIGHT uses analytic fog without ray-marching");
        check(normal.enabled() && normal.fogSamples() == 12 && !normal.dynamicLights(), "NORMAL quality");
        check(heavy.enabled() && heavy.fogSamples() == 24 && heavy.dynamicLights(), "HEAVY quality");
        check(off.lightSamples() == 0 && light.lightSamples() == 2, "LIGHT uses two integration samples per source");
        check(normal.lightSamples() == 4 && heavy.lightSamples() == 8, "Integration precision is bounded by quality");
        check(off.step(-5) == off && heavy.step(5) == heavy, "Selector stops at boundaries");
        check(off.step(5) == light && heavy.step(-5) == normal, "Shift does not skip modes");
        check(org.example.maniacrevolution.settings.GraphicsMode.fromId(-1) == normal, "Invalid wire id defaults safely");
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
        check(AtmosphereRules.visionRange(0, 0) == 28, "Unimpaired sight stays unchanged");
        check(AtmosphereRules.visionRange(1, 0) == 5, "Blindness has a five-block pocket");
        check(AtmosphereRules.visionRange(0, 1) == 15, "Darkness has its own shorter view");
        check(AtmosphereRules.visionRange(1, 1) == 5, "Overlapping effects cannot extend vision");
        check(AtmosphereRules.visionFadeStart(0, 0) == 18, "Ordinary fog fade stays unchanged");
        for (float strength : new float[]{0, .25F, .5F, .75F, 1}) {
            float range = AtmosphereRules.visionRange(strength, 0);
            check(range >= 5 && range <= 28 && AtmosphereRules.visionFadeStart(strength, 0) < range,
                    "Blindness release preserves valid fog bounds");
        }
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
        for (float distance : new float[]{12, 16, 20, 24, 28})
            check(AtmosphereRules.lightIntersectsView(distance, 9.25), "Light stays active throughout visible space: " + distance);
        check(AtmosphereRules.lightIntersectsView(30, 9.25), "Lamp beyond the fog can illuminate visible space");
        check(AtmosphereRules.lightIntersectsView(38, 10.75), "Row endpoint influence intersects the view");
        check(!AtmosphereRules.lightIntersectsView(38, 9.25), "Cull only after the whole light volume leaves the view");
        check(!AtmosphereRules.lightIntersectsView(Double.NaN, 9.25), "Invalid light bounds are rejected");
        LightTransition transition = new LightTransition();
        LightTransition existingLamp = new LightTransition(1);
        check(existingLamp.value(0) == 1 && existingLamp.value(1) == 1, "An already-lit lamp has no discovery/ignition delay");
        existingLamp.tick(0); existingLamp.tick(0); existingLamp.snap(1);
        check(existingLamp.value(0) == 1 && existingLamp.value(1) == 1, "A lamp coming around a corner is immediately lit");
        transition.tick(1);
        check(transition.value(0) == 0 && transition.value(1) < .5F, "New source ramps in instead of flashing");
        for (int i = 0; i < 100; i++) transition.tick(1);
        check(transition.value(1) > .99F, "A held or rediscovered source stays on");
        transition.tick(0);
        check(transition.value(1) > .7F, "One missed visibility tick does not extinguish the source");
        transition.tick(1);
        check(transition.value(1) > .8F, "Visibility recovery retains source strength");
        for (int i = 0; i < 30; i++) transition.tick(0);
        check(transition.dark(), "An extinguished source eventually releases its slot");

        ImpactPulse impact = new ImpactPulse();
        impact.tick(0);
        check(impact.value(1) == 0, "no damage has no impact pulse");
        impact.tick(10);
        check(impact.value(0) == 0 && impact.value(1) == 1, "damage attacks immediately with interpolated onset");
        for (int tick = 9; tick >= 0; tick--) impact.tick(tick);
        check(impact.value(1) > 0, "damage release outlasts vanilla hurt animation");
        impact.tick(10);
        check(impact.value(1) == 1, "another hit retriggers the pulse");
        for (int i = 0; i < ImpactPulse.DURATION_TICKS + 1; i++) impact.tick(0);
        check(impact.value(1) == 0, "damage pulse ends independently of remaining health");
        impact.trigger(); impact.tick(0);
        check(impact.value(1) == 1, "preview can trigger damage without health changes");
        impact.reset();
        check(impact.value(0) == 0 && impact.value(1) == 0, "world/profile reset clears damage immediately");
        System.out.println("Atmosphere rules checks passed");
    }

    private static AtmosphereRules.Profile profile(boolean alive, boolean survival, int phase,
                                                    String team, boolean keeper, boolean warden) {
        return AtmosphereRules.profile(alive, survival, phase, team, keeper, warden);
    }

    private static void check(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }
}
