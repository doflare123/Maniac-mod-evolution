package org.example.maniacrevolution.warden;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** Source visibility decides whether to build surfaces, or send an isolated short player echo. */
public final class WardenNoiseVisibility {
    public static final int ECHO_TICKS = 20;
    public static final double ECHO_RANGE = 10;
    public enum Reveal { PULSE, ECHO, NONE }
    private WardenNoiseVisibility() {}
    public static Reveal reveal(boolean visible, boolean playerSource, WardenNoise.Kind kind) {
        return visible ? Reveal.PULSE : playerSource && kind != WardenNoise.Kind.WAVE ? Reveal.ECHO : Reveal.NONE;
    }
    public static boolean echoInRange(Vec3 listener, Vec3 source) {
        return listener.distanceToSqr(source) <= ECHO_RANGE * ECHO_RANGE;
    }
    public static boolean visible(Level level, Entity viewer, Vec3 from, Vec3 origin) {
        var start = BlockPos.containing(from); var end = BlockPos.containing(origin);
        for (int x = Math.min(start.getX() >> 4, end.getX() >> 4); x <= Math.max(start.getX() >> 4, end.getX() >> 4); x++)
            for (int z = Math.min(start.getZ() >> 4, end.getZ() >> 4); z <= Math.max(start.getZ() >> 4, end.getZ() >> 4); z++)
                if (!level.hasChunk(x, z)) return false;
        var hit = level.clip(new ClipContext(from, origin.add(0, 0.08, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, viewer));
        // Interactions/sounds may originate inside a chest/door: its front face is a visible source.
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(end);
    }
}
