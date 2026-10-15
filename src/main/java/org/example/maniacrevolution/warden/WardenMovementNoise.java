package org.example.maniacrevolution.warden;

import net.minecraft.world.phys.Vec3;

/** Server displacement accumulates steps; Shift, gaps and teleports reset the accumulator. */
public final class WardenMovementNoise {
    private Vec3 previous;
    private long lastTick = Long.MIN_VALUE, lastStep = Long.MIN_VALUE;
    private boolean grounded;
    private double distance, descent;

    public WardenNoise.Kind sample(Vec3 position, long tick, boolean ground, boolean sprint, boolean shift) {
        if (previous == null || tick != lastTick + 1 || previous.distanceToSqr(position) > 16) {
            reset(position, tick, ground);
            return null;
        }
        double dy = position.y - previous.y;
        double dx = position.x - previous.x, dz = position.z - previous.z;
        boolean wasGrounded = grounded;
        previous = position; lastTick = tick; grounded = ground;
        if (shift) { distance = descent = 0; lastStep = Long.MIN_VALUE; return null; }
        if (!ground) {
            distance = 0;
            descent += Math.max(0, -dy);
            return wasGrounded && dy > 0.05 ? WardenNoise.Kind.JUMP : null;
        }
        if (!wasGrounded) {
            boolean landed = descent + Math.max(0, -dy) > 0.25;
            descent = distance = 0;
            return landed ? WardenNoise.Kind.LAND : null;
        }
        descent = 0;
        distance += Math.sqrt(dx * dx + dz * dz);
        double stride = sprint ? 1.75 : 1.25;
        if (distance < stride || (lastStep != Long.MIN_VALUE && tick - lastStep < 4)) return null;
        distance %= stride;
        lastStep = tick;
        return sprint ? WardenNoise.Kind.SPRINT : WardenNoise.Kind.STEP;
    }
    private void reset(Vec3 position, long tick, boolean ground) {
        previous = position; lastTick = tick; grounded = ground;
        lastStep = Long.MIN_VALUE; distance = descent = 0;
    }
}
