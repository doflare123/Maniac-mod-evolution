package org.example.maniacrevolution.warden;

import net.minecraft.world.phys.Vec3;
import java.util.ArrayDeque;
import java.util.List;

/** Bounded sampling from actual server displacement, including quieter Shift movement. */
public final class WardenScentTrail {
    public static final int MAX_POINTS = 128;
    private final ArrayDeque<WardenScentPoint> points = new ArrayDeque<>();
    private Vec3 previous;
    private long lastTick = -1, lastSample = -1;
    private double distance;
    public void sample(Vec3 position, long now, boolean shift) {
        points.removeIf(point -> point.brightness(now) <= 0);
        if (previous == null || now - lastTick != 1 || previous.distanceToSqr(position) > 16) {
            // Teleport/gap cannot draw a connecting trail, or leave old traces in the new place.
            if (previous != null) points.clear();
            previous = position; lastTick = now; distance = 0; lastSample = -1; return;
        }
        distance += previous.distanceTo(position);
        previous = position; lastTick = now;
        if (distance >= 0.5 && (lastSample < 0 || now - lastSample >= 4)) {
            if (points.size() == MAX_POINTS) points.removeFirst();
            points.addLast(new WardenScentPoint(position.add(0, 0.06, 0), shift ? 0.25F : 1, now));
            lastSample = now; distance = 0;
        }
    }
    public List<WardenScentPoint> nearby(Vec3 listener, long now) {
        return points.stream().filter(p -> p.brightness(now) > 0 && p.keep(now)
                && p.position().distanceToSqr(listener) < WardenScentPoint.RANGE * WardenScentPoint.RANGE).toList();
    }
}
