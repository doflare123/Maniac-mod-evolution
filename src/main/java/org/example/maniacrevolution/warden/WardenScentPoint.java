package org.example.maniacrevolution.warden;

import net.minecraft.world.phys.Vec3;

/** A past position, not a current player marker. Packets carry no entity identity. */
public record WardenScentPoint(Vec3 position, float strength, long tick) {
    public static final int LIFE = 400;
    public static final double RANGE = 16;
    public boolean valid() {
        return position != null && Double.isFinite(position.x) && Double.isFinite(position.y) && Double.isFinite(position.z)
                && Float.isFinite(strength) && strength > 0 && strength <= 1 && tick >= 0;
    }
    public float brightness(double now) {
        double age = now - tick;
        if (!valid() || age < -20 || age >= LIFE) return 0;
        double fade = Math.max(0, 1 - Math.max(0, age) / LIFE); return (float) (strength * fade);
    }
    public boolean keep(long now) {
        long age = now - tick;
        return age < 200 || (age < 300 ? (tick / 4) % 2 == 0 : (tick / 4) % 4 == 0);
    }
}
