package org.example.maniacrevolution.warden;

import java.util.*;

/** Per-listener cooldown: one observer cannot consume another observer's player echo. */
public final class WardenNoiseEchoThrottle {
    private record Pair(UUID listener, UUID source) {}
    private final Map<Pair, Long> last = new HashMap<>();
    public boolean allow(UUID listener, UUID source, long now) {
        last.values().removeIf(tick -> now - tick >= WardenNoiseVisibility.ECHO_TICKS);
        var key = new Pair(listener, source);
        if (last.containsKey(key) || last.size() >= 4096) return false;
        last.put(key, now); return true;
    }
    public void clear() { last.clear(); }
}
