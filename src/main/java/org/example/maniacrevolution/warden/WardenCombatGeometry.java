package org.example.maniacrevolution.warden;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.Optional;

public final class WardenCombatGeometry {
    private WardenCombatGeometry() {}
    /** Contact on the actual target bounds, so projectile thickness cannot reach through a wall. */
    public static Optional<Vec3> contact(AABB box, Vec3 start, Vec3 end, double radius) {
        var expanded = box.inflate(radius);
        var entry = expanded.contains(start) ? Optional.of(start) : expanded.clip(start, end);
        return entry.map(p -> new Vec3(Mth.clamp(p.x, box.minX, box.maxX), Mth.clamp(p.y, box.minY, box.maxY), Mth.clamp(p.z, box.minZ, box.maxZ)));
    }
    public static boolean unobstructed(Vec3 contact, Vec3 clipped) { return contact.distanceToSqr(clipped) < 1.0e-8; }
    /** Aim tolerance does not extend reach: the actual body contact must stay within range. */
    public static Optional<Vec3> meleeContact(AABB box, Vec3 start, Vec3 end, double pickRadius) {
        return contact(box, start, end, Math.max(WardenCombatRules.MELEE_MARGIN, pickRadius))
                .filter(p -> start.distanceToSqr(p) <= WardenCombatRules.MELEE_RANGE * WardenCombatRules.MELEE_RANGE);
    }
}
