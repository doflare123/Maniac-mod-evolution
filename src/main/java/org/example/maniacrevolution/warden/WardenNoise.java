package org.example.maniacrevolution.warden;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

/** A frozen sound origin, never a frozen player model. Values are provisional visual parameters. */
public record WardenNoise(ResourceLocation dimension, UUID source, Vec3 position, Kind kind,
                          float strength, double hearingRange, long tick) {
    public enum Kind { STEP, SPRINT, JUMP, LAND, ATTACK, DAMAGE, INTERACTION, QTE, SOUND, WAVE }

    public boolean valid() {
        return dimension != null && position != null && kind != null && tick >= 0
                && Double.isFinite(position.x) && Double.isFinite(position.y) && Double.isFinite(position.z)
                && Float.isFinite(strength) && strength > 0 && strength <= 1
                && Double.isFinite(hearingRange) && hearingRange > 0 && hearingRange <= 64;
    }
    public double radius() { return 3 + 9 * Math.sqrt(strength); }
    public double duration() { return 8 + 20 * Math.sqrt(strength); }
    public boolean audible(ResourceLocation listenerDimension, Vec3 listener) {
        return valid() && dimension.equals(listenerDimension) && listener != null
                && position.distanceToSqr(listener) < hearingRange * hearingRange;
    }
    public boolean fresh(ResourceLocation listenerDimension, long now) {
        return valid() && dimension.equals(listenerDimension) && now - tick < duration() && tick - now <= 20;
    }
}
