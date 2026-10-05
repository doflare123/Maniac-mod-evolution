package org.example.maniacrevolution.warden;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Connection-local form flags; false removes the entry rather than retaining departed players. */
public final class WardenFormState {
    private final Set<UUID> active = new HashSet<>();

    public boolean active(UUID player) { return active.contains(player); }
    public boolean update(UUID player, boolean value) { return value ? active.add(player) : active.remove(player); }
    public void clear() { active.clear(); }
}
