package org.example.maniacrevolution.scp173;

import java.util.ArrayList;
import java.util.List;

/** Pixel geometry and first-person gesture; no changes to the statue's world bones. */
public final class Scp173Presentation {
    public record Pixel(int x, int y, int color) {}
    private Scp173Presentation() {}
    /** Two ticks closing, six closed, two opening: total half a second. */
    public static float blinkClosure(double remaining) {
        if (!Double.isFinite(remaining) || remaining <= 0 || remaining > Scp173BlinkState.DURATION) return 0;
        double amount = Math.max(0, Math.min(1, Math.min((Scp173BlinkState.DURATION - remaining) / 2, remaining / 2)));
        return (float) (amount * amount * (3 - 2 * amount));
    }
    public static List<Pixel> reticle(boolean target, boolean held, boolean cooling) {
        var pixels = new ArrayList<Pixel>();
        int edge = target ? 0xFFE4D09A : 0xFFB6B0A1;
        for (int sign : new int[]{-1, 1}) for (int i = -2; i <= 2; i++) {
            pixels.add(new Pixel(sign * 6, i, edge));
            pixels.add(new Pixel(i, sign * 6, edge));
        }
        pixels.add(new Pixel(0, 0, held ? 0xFFE46666 : cooling ? 0xFF777777 : target ? 0xFF9CE1AE : 0xFFF2E8CF));
        if (held) for (int sign : new int[]{-1, 1}) {
            pixels.add(new Pixel(sign * 2, -2, 0xFFE46666));
            pixels.add(new Pixel(sign * 2, 2, 0xFFE46666));
        }
        return List.copyOf(pixels);
    }
    /** Eight-tick reach and return. Rejected attacks never start this gesture. */
    public static float reach(double age) {
        if (!Double.isFinite(age) || age <= 0 || age >= 8) return 0;
        double progress = age < 2 ? age / 2 : 1 - (age - 2) / 6;
        return (float) (progress * progress * (3 - 2 * progress));
    }
}
