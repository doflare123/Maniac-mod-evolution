package org.example.maniacrevolution.warden.client;

import java.util.ArrayDeque;
import java.util.List;
import java.util.function.DoubleSupplier;

/** Audio callbacks never mutate renderer state. A bounded inbox bridges to the client tick. */
final class WardenSoundPulseInbox {
    static final int CAPACITY = 64;
    record PendingSound(WardenSoundPulseFilter.HeardSound position, DoubleSupplier volume) {
        WardenSoundPulseFilter.HeardSound sample() {
            return position.withVolume((float) volume.getAsDouble());
        }
    }
    private final ArrayDeque<PendingSound> sounds = new ArrayDeque<>();
    private boolean enabled;
    private long generation;

    synchronized long generation() { return generation; }
    synchronized long ticket() { return enabled ? generation : -1; }

    synchronized void enable() { enabled = true; }

    synchronized void clear() {
        enabled = false;
        generation++;
        sounds.clear();
    }

    synchronized void offer(long expectedGeneration, PendingSound sound) {
        if (!enabled || generation != expectedGeneration || sounds.size() == CAPACITY) return;
        sounds.addLast(sound);
    }

    synchronized List<PendingSound> drain() {
        var result = List.copyOf(sounds);
        sounds.clear();
        return result;
    }
}
