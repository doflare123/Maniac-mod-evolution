package org.example.maniacrevolution.warden;

import org.example.maniacrevolution.character.maniac.WardenClass;

/** The same match eligibility for the player's vision and visible form. */
public final class WardenMatchRules {
    public static final double VISION_RANGE = 50;
    private WardenMatchRules() {}

    public static boolean participant(boolean alive, boolean survivalOrAdventure, int phase, String team) {
        return alive && survivalOrAdventure && phase >= 1 && phase <= 3
                && ("maniac".equalsIgnoreCase(team) || "survivors".equalsIgnoreCase(team));
    }

    public static boolean active(boolean alive, boolean survivalOrAdventure, int phase, int classId, String team) {
        return alive && survivalOrAdventure && phase >= 1 && phase <= 3
                && classId == WardenClass.SCOREBOARD_ID && "maniac".equalsIgnoreCase(team);
    }
}
