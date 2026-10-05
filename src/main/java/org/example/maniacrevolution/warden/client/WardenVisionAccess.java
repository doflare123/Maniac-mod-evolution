package org.example.maniacrevolution.warden.client;

import org.example.maniacrevolution.warden.WardenMatchRules;

/** Shared access decision for rendering, sound intake and transition cleanup. */
final class WardenVisionAccess {
    enum Mode { OFF, MATCH, TEST }

    private WardenVisionAccess() {}

    static Mode mode(boolean test, boolean alive, boolean survivalOrAdventure,
                     int phase, int maniacClass, String team) {
        return mode(test, false, alive, survivalOrAdventure, phase, maniacClass, team);
    }
    static Mode mode(boolean test, boolean normalForTests, boolean alive, boolean survivalOrAdventure,
                     int phase, int maniacClass, String team) {
        if (normalForTests) return Mode.OFF;
        if (!alive || !survivalOrAdventure) return Mode.OFF;
        if (test) return Mode.TEST;
        return WardenMatchRules.active(alive, survivalOrAdventure, phase, maniacClass, team) ? Mode.MATCH : Mode.OFF;
    }
}
