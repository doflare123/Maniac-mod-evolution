package org.example.maniacrevolution.warden;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Source limits never merge different players. QTE input cannot flood pulses while a session stays open. */
public final class WardenNoiseThrottle {
    private final Map<UUID, EnumMap<WardenNoise.Kind, Long>> ticks = new HashMap<>();
    public boolean allow(UUID source, WardenNoise.Kind kind, long tick) {
        if (source == null) return true;
        var entries = ticks.computeIfAbsent(source, id -> new EnumMap<>(WardenNoise.Kind.class));
        Long last = entries.get(kind);
        int interval = kind == WardenNoise.Kind.QTE ? 20 : kind == WardenNoise.Kind.INTERACTION ? 4 : 2;
        if (last != null && tick - last < interval) return false;
        entries.put(kind, tick);
        return true;
    }
    public void remove(UUID source) { ticks.remove(source); }
    public void clear() { ticks.clear(); }
}
