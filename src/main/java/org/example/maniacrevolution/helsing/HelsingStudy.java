package org.example.maniacrevolution.helsing;

import java.util.UUID;

/** An interrupted acquisition restarts, while acquired information has a short grace period. */
final class HelsingStudy<T> {
    UUID target;
    int held;
    long lastSeen;
    T intel;
    void observe(UUID id, long tick) {
        if (!id.equals(target)) { target = id; held = 0; intel = null; }
        held++; lastSeen = tick;
    }
    void lose(long tick, int memoryTicks) {
        held = 0;
        if (intel == null || tick - lastSeen > memoryTicks) clear();
    }
    void clear() { target = null; held = 0; intel = null; }
}
