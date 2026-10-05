package org.example.maniacrevolution.warden.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/** Pixel geometry shared by the live HUD and its standalone preview. */
public final class WardenReticleLayout {
    public record Rect(int x, int y, int width, int height, int color) {}
    public record Label(String text, int x, boolean wave) {}
    private static final int DARK = 0xCC061C23, EDGE = 0xFF24505A, TEAL = 0xFF39CFC3, LIGHT = 0xFFD4FFF7;
    private WardenReticleLayout() {}
    public static List<Label> cooldowns(int melee, int wave, ToIntFunction<String> width) {
        String a = melee > 0 ? "Удар " + (melee + 19) / 20 + "с" : null;
        String b = wave > 0 ? "Волна " + (wave + 19) / 20 + "с" : null;
        if (a == null && b == null) return List.of();
        if (a == null) return List.of(new Label(b, -width.applyAsInt(b) / 2, true));
        if (b == null) return List.of(new Label(a, -width.applyAsInt(a) / 2, false));
        int aw = width.applyAsInt(a), bw = width.applyAsInt(b), x = -(aw + 8 + bw) / 2;
        return List.of(new Label(a, x, false), new Label(b, x + aw + 8, true));
    }
    public static List<Rect> strike(float remaining, boolean hit) {
        if (!Float.isFinite(remaining) || remaining <= 0) return List.of();
        remaining = Math.min(1, remaining); int distance = 4 + Math.round((1 - remaining) * 5);
        int color = (Math.round(remaining * remaining * 255) << 24) | ((hit ? LIGHT : TEAL) & 0xFFFFFF);
        var rects = new ArrayList<Rect>();
        for (int x : new int[]{-1, 1}) for (int y : new int[]{-1, 1}) for (int i = 0; i < (hit ? 3 : 2); i++)
            rects.add(new Rect(x * (distance + i), y * (distance + i), 1, 1, color));
        return List.copyOf(rects);
    }
    public static List<Rect> build(float charge, double time) {
        float q = Float.isFinite(charge) ? Math.max(0, Math.min(1, charge)) : 0;
        var out = new ArrayList<Rect>();
        // Open center: one bright aiming point surrounded by a small sculk diamond.
        out.add(new Rect(-1, -1, 3, 3, DARK)); out.add(new Rect(0, 0, 1, 1, LIGHT));
        for (int side : new int[]{-1, 1}) {
            for (int i = 0; i < 3; i++) {
                out.add(new Rect(side < 0 ? -7 + i : 6 - i, -2 - i, 1, 2, TEAL));
                out.add(new Rect(side < 0 ? -7 + i : 6 - i, 1 + i, 1, 2, TEAL));
            }
            // Horns and sound spectrum rise towards the outer bars.
            int[] heights = {3, 5, 8, 12, 17, 23, 17, 11};
            for (int i = 0; i < heights.length; i++) {
                int x = side < 0 ? -16 - i * 4 : 15 + i * 4, h = heights[i];
                out.add(new Rect(x - 1, -h / 2 - 1, 4, h + 2, DARK));
                out.add(new Rect(x, -h / 2, 2, h, EDGE));
                int fill = Math.round(h * Math.max(0, Math.min(1, q * heights.length - i)));
                if (fill > 0) out.add(new Rect(x, (h - fill) / 2 - h / 2, 2, fill,
                        q >= 1 && Math.sin(time * Math.PI / 5) > 0 ? LIGHT : TEAL));
            }
            out.add(new Rect(side < 0 ? -11 : 10, -8, 1, 5, EDGE));
            out.add(new Rect(side < 0 ? -12 : 11, -10, 1, 3, TEAL));
        }
        return List.copyOf(out);
    }
}
