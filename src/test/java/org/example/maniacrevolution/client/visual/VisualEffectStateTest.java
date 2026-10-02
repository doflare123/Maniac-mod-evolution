package org.example.maniacrevolution.client.visual;

/** Run with verifyVisualEffects; no window or OpenGL context required. */
public final class VisualEffectStateTest {
    public static void main(String[] args) {
        check(VisualEffectState.injuryTarget(20, 20) == 0, "Full health must remain clear");
        check(VisualEffectState.injuryTarget(8, 20) == 0, "40% is the injury threshold");
        near(VisualEffectState.injuryTarget(5, 20), 0.5f, "25% health gives half intensity");
        near(VisualEffectState.injuryTarget(2, 20), 1, "10% health saturates safely");
        near(VisualEffectState.injuryTarget(25, 100), VisualEffectState.injuryTarget(5, 20),
                "Classes with boosted HP use the same relative threshold");
        check(VisualEffectState.injuryTarget(0, 0) == 0, "Invalid max health must be harmless");
        check(VisualEffectState.injuryTarget(Float.NaN, 20) == 0, "Invalid values cannot reach the shader");

        VisualEffectState state = new VisualEffectState();
        tick(state, 2, true, false);
        check(state.damage(1) == 0, "Joining with low HP is not a hit");
        check(state.injury(0) == 0 && state.injury(1) < 1, "Injury starts with a fade");
        for (int i = 0; i < 80; i++) tick(state, 2, true, false);
        check(state.injury(1) > 0.999f, "Injury reaches its intended strength");
        float old = state.injury(1);
        tick(state, 20, true, false);
        check(state.injury(1) > 0 && state.injury(1) < old, "Healing fades rather than snaps");
        for (int i = 0; i < 80; i++) tick(state, 20, true, false);
        check(state.injury(1) < 0.001f && state.damage(1) == 0, "Healing clears the screen without hit feedback");

        tick(state, 16, true, false);
        check(state.damage(1) > 0.5f, "A hit produces feedback");
        for (int i = 0; i < 40; i++) tick(state, 16, true, false);
        check(state.damage(1) < 0.001f, "Hit feedback does not persist");

        for (int i = 0; i < 100; i++) tick(state, 2, false, true);
        check(state.injury(1) < 0.001f && state.maniac(1) > 0.999f,
                "Maniacs use their own profile even with low HP");
        state.reset();
        check(state.injury(1) == 0 && state.maniac(1) == 0 && state.damage(1) == 0,
                "Logout/role reset clears all visual envelopes");
        tick(state, 1, true, false);
        check(state.damage(1) == 0, "Respawn cannot reuse health from the previous entity");

        state.reset();
        state.tick(20, 20, 24, false, true, false, 0, 0);
        state.tick(20, 20, 22, true, true, false, 0, 0);
        check(state.damage(1) > 0, "Damage absorbed by extra hearts still produces feedback");
        state.reset();
        state.tick(20, 20, 24, false, true, false, 0, 0);
        state.tick(20, 20, 20, false, true, false, 0, 0);
        check(state.damage(1) == 0, "Expiring absorption is not a hit");
        for (int i = 0; i < 50; i++) state.tick(20, 20, 20, false, true, false, 100, 100);
        check(state.roll(1) <= 0.65f && state.landing(1) <= 1, "Camera impulses are bounded");
        for (int i = 0; i < 50; i++) state.tick(20, 20, 20, false, true, false, 0, 0);
        check(state.roll(1) < 0.001f && state.landing(1) < 0.001f,
                "Camera returns to neutral after movement stops");
        System.out.println("VisualEffectState: all checks passed");
    }

    private static void tick(VisualEffectState state, float health, boolean survivor, boolean maniac) {
        state.tick(health, 20, health, true, survivor, maniac, 0, 0);
    }

    private static void near(float actual, float expected, String message) {
        check(Math.abs(actual - expected) < 0.0001f, message);
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
