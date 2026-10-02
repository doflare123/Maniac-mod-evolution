package org.example.maniacrevolution.client.visual;

/** Tick-based envelopes, interpolated by the renderer. No Minecraft dependencies. */
public final class VisualEffectState {
    private float injury, previousInjury;
    private float maniac, previousManiac;
    private float damage, previousDamage;
    private float roll, previousRoll;
    private float landing, previousLanding;
    private float lastHealth = Float.NaN;

    public static float injuryTarget(float health, float maxHealth) {
        if (!Float.isFinite(health) || !Float.isFinite(maxHealth) || maxHealth <= 0) return 0;
        return clamp((0.4f - health / maxHealth) / 0.3f, 0, 1);
    }

    public void tick(float health, float maxHealth, float totalHealth, boolean hurt, boolean survivor,
                     boolean killer, float strafe, float landingImpulse) {
        previousInjury = injury;
        previousManiac = maniac;
        previousDamage = damage;
        previousRoll = roll;
        previousLanding = landing;
        injury += ((survivor ? injuryTarget(health, maxHealth) : 0) - injury) * 0.15f;
        maniac += ((killer ? 1 : 0) - maniac) * 0.1f;
        damage *= 0.78f;
        if (hurt && Float.isFinite(lastHealth) && totalHealth < lastHealth) {
            damage = Math.max(damage, clamp((lastHealth - totalHealth) / Math.max(1, maxHealth) * 3, 0, 1));
        }
        lastHealth = totalHealth;
        roll += (clamp(strafe, -1, 1) * 0.65f - roll) * 0.3f;
        landing = Math.max(landing * 0.7f, clamp(landingImpulse, 0, 1));
    }

    public void pulseDamage() { damage = 1; }
    public float injury(float partial) { return interpolate(previousInjury, injury, partial); }
    public float maniac(float partial) { return interpolate(previousManiac, maniac, partial); }
    public float damage(float partial) { return interpolate(previousDamage, damage, partial); }
    public float roll(float partial) { return interpolate(previousRoll, roll, partial); }
    public float landing(float partial) { return interpolate(previousLanding, landing, partial); }

    public void reset() {
        injury = previousInjury = maniac = previousManiac = damage = previousDamage = 0;
        roll = previousRoll = landing = previousLanding = 0;
        lastHealth = Float.NaN;
    }

    private static float interpolate(float old, float current, float partial) {
        return old + (current - old) * clamp(partial, 0, 1);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
