package org.example.maniacrevolution.warden.client;

import net.minecraft.world.phys.Vec3;

/** Thin copper ripples follow the captured fluid quad, including sloped and vertical faces. */
final class WardenWaterPattern {
    static final int SEGMENTS = 8, BANDS = 2, COUNT = SEGMENTS * BANDS;
    static final float RED = 1, GREEN = 0.38F, BLUE = 0.08F;
    private WardenWaterPattern() {}
    record Ribbon(Vec3 a, Vec3 b, Vec3 c, Vec3 d) {
        Vec3 center() { return new Vec3((a.x + b.x + c.x + d.x) * 0.25,
                (a.y + b.y + c.y + d.y) * 0.25, (a.z + b.z + c.z + d.z) * 0.25); }
    }
    static Ribbon ribbon(WardenSurfaceCache.Face face, int index, double time) {
        return ribbon(face.a(), face.b(), face.c(), face.d(), face.normal(), index, time);
    }
    static java.util.List<Vec3> samples(Vec3 a, Vec3 b, Vec3 c, Vec3 d, Vec3 normal) {
        return java.util.stream.IntStream.range(0, COUNT).mapToObj(i -> ribbon(a, b, c, d, normal, i, 0).center()).toList();
    }
    private static Ribbon ribbon(Vec3 a, Vec3 b, Vec3 c, Vec3 d, Vec3 normal, int index, double time) {
        double u0 = (index % SEGMENTS) / (double) SEGMENTS, u1 = u0 + 1.0 / SEGMENTS;
        double band = index / SEGMENTS == 0 ? 0.27 : 0.73;
        double v0 = band + 0.055 * Math.sin(u0 * Math.PI * 2 - time * 0.06);
        double v1 = band + 0.055 * Math.sin(u1 * Math.PI * 2 - time * 0.06);
        double width = 0.018;
        return new Ribbon(point(a, b, c, d, normal, u0, v0 - width), point(a, b, c, d, normal, u1, v1 - width),
                point(a, b, c, d, normal, u1, v1 + width), point(a, b, c, d, normal, u0, v0 + width));
    }
    private static Vec3 point(Vec3 a, Vec3 b, Vec3 c, Vec3 d, Vec3 normal, double u, double v) {
        double wa = (1 - u) * (1 - v), wb = u * (1 - v), wc = u * v, wd = (1 - u) * v;
        return new Vec3(a.x * wa + b.x * wb + c.x * wc + d.x * wd + normal.x,
                a.y * wa + b.y * wb + c.y * wc + d.y * wd + normal.y,
                a.z * wa + b.z * wb + c.z * wc + d.z * wd + normal.z);
    }
}
