package org.example.maniacrevolution.warden.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

/** Short-lived, bounded surface patches around visible sound sources, including wave travel. */
final class WardenNoiseSurfaces {
    static final int CAPACITY = 4, LIFE = 32;
    private static final int RADIUS = 8;
    private static final class Patch {
        final WardenSurfaceCache cache = new WardenSurfaceCache(RADIUS, 1024, 2000, 24000);
        long touched;
        Patch(BlockPos pos, long now) { cache.start(pos); touched = now; }
    }
    private final List<Patch> patches = new ArrayList<>();
    void request(Vec3 position, long now) {
        expire(now);
        var pos = BlockPos.containing(position);
        for (var patch : patches) {
            var center = patch.cache.center();
            if (Math.abs(pos.getX() - center.getX()) <= 4 && Math.abs(pos.getY() - center.getY()) <= 4
                    && Math.abs(pos.getZ() - center.getZ()) <= 4) { patch.touched = now; return; }
        }
        if (patches.size() == CAPACITY) {
            var oldest = patches.stream().min(java.util.Comparator.comparingLong(p -> p.touched)).orElseThrow();
            oldest.cache.clear(); patches.remove(oldest);
        }
        patches.add(new Patch(pos, now));
    }
    void expire(long now) {
        patches.removeIf(p -> {
            if (now - p.touched < LIFE) return false;
            p.cache.clear(); return true;
        });
    }
    void tick(Minecraft mc, long now) { expire(now); for (var patch : patches) patch.cache.tick(mc); }
    List<WardenSurfaceCache> caches() { return patches.stream().map(p -> p.cache).toList(); }
    void clear() { for (var patch : patches) patch.cache.clear(); patches.clear(); }
}
