package org.example.maniacrevolution.warden.client;

import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.function.Predicate;

/** Wave visuals have their own bounded visibility checks, independent of the near-world cache. */
public final class WardenWavePoints {
    public static final int MAX_TESTS = 512, POINTS_PER_RING = 48;
    public static final double MAX_DISTANCE = 48;
    public record Ring(Vec3 position, Vec3 direction, double born, float strength) {}
    public record Point(Vec3 position, float alpha) {}
    private WardenWavePoints() {}
    public static List<Point> visible(List<Ring> rings, double now, Vec3 camera, Predicate<Vec3> unobstructed) {
        var result = new ArrayList<Point>(); int tests = 0;
        var ordered = rings.stream().sorted(Comparator.comparingDouble(r -> r.position.distanceToSqr(camera))).toList();
        for (var ring : ordered) {
            double age = Math.max(0, now - ring.born);
            if (age >= 8 || ring.position.distanceToSqr(camera) > MAX_DISTANCE * MAX_DISTANCE) continue;
            var axis = Math.abs(ring.direction.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
            var a = ring.direction.cross(axis).normalize(); var b = ring.direction.cross(a).normalize();
            double radius = 0.12 + ring.strength * 0.3 + age * 0.035;
            for (int i = 0; i < POINTS_PER_RING; i++) {
                if (tests++ >= MAX_TESTS) return List.copyOf(result);
                double angle = i * Math.PI * 2 / POINTS_PER_RING;
                var p = ring.position.add(a.scale(Math.cos(angle) * radius)).add(b.scale(Math.sin(angle) * radius));
                if (unobstructed.test(p)) result.add(new Point(p, (float) (1 - age / 8)));
            }
        }
        return List.copyOf(result);
    }
}
