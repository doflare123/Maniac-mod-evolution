package org.example.maniacrevolution.warden;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** A bounded per-tick queue coalesces an interaction and its spatial sound. */
public final class WardenNoiseBatch {
    public static final int CAPACITY = 64;
    private final List<WardenNoise> pending = new ArrayList<>();

    public void offer(WardenNoise noise) {
        if (noise == null || !noise.valid()) return;
        for (int i = 0; i < pending.size(); i++) {
            var old = pending.get(i);
            boolean sameSource = Objects.equals(old.source(), noise.source()) || old.source() == null || noise.source() == null;
            boolean sameWaveKind = (old.kind() == WardenNoise.Kind.WAVE) == (noise.kind() == WardenNoise.Kind.WAVE);
            if (sameSource && sameWaveKind && old.dimension().equals(noise.dimension()) && Math.abs(old.tick() - noise.tick()) <= 1
                    && old.position().distanceToSqr(noise.position()) <= 1) {
                var strongest = old.strength() >= noise.strength() ? old : noise;
                var identified = old.source() != null ? old : noise;
                pending.set(i, new WardenNoise(strongest.dimension(), identified.source(), strongest.position(),
                        strongest.source() != null ? strongest.kind() : identified.kind(), strongest.strength(), Math.max(old.hearingRange(), noise.hearingRange()),
                        Math.max(old.tick(), noise.tick())));
                return;
            }
        }
        if (pending.size() < CAPACITY) pending.add(noise);
    }
    public List<WardenNoise> drain() { var result = List.copyOf(pending); pending.clear(); return result; }
    public void clear() { pending.clear(); }
}
