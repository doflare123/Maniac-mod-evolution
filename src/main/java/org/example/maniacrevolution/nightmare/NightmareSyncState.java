package org.example.maniacrevolution.nightmare;

/** The HUD snapshot last delivered to a player, without retaining player or level objects. */
record NightmareSyncState(boolean visible, float sanity, NightmareTrialType trialType,
                          int trialSecondsLeft, int immunitySecondsLeft) {
    private static final int SANITY_SYNC_INTERVAL = 5;

    boolean shouldSend(NightmareSyncState previous, int elapsedTicks) {
        if (previous == null) return true;
        if (equals(previous)) return false;
        // Visibility, trial transitions and displayed seconds must change immediately.
        if (visible != previous.visible || trialType != previous.trialType
                || trialSecondsLeft != previous.trialSecondsLeft
                || immunitySecondsLeft != previous.immunitySecondsLeft) return true;
        return elapsedTicks >= SANITY_SYNC_INTERVAL;
    }
}
