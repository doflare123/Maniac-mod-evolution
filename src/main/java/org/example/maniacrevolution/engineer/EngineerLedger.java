package org.example.maniacrevolution.engineer;

import java.util.HashMap;
import java.util.Map;

/** Shared high-water marks use the same computer IDs as HackManager. */
public final class EngineerLedger {
    private final Map<Integer, Float> highWater = new HashMap<>();
    public void observe(int computer, float progress) {
        if (Float.isFinite(progress)) highWater.merge(computer, Math.max(0, progress), Math::max);
    }
    public float freshFraction(int computer, float before, float after) {
        if (!Float.isFinite(before) || !Float.isFinite(after) || after <= before) return 0;
        float previous = Math.max(before, highWater.getOrDefault(computer, 0f));
        observe(computer, after);
        return Math.max(0, after - previous) / (after - before);
    }
    public void clear() { highWater.clear(); }
}
