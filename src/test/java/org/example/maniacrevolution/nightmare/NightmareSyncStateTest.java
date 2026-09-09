package org.example.maniacrevolution.nightmare;

/** Dependency-free regression tests, executed by Gradle's verifyNightmareSync task. */
public final class NightmareSyncStateTest {
    public static void main(String[] args) {
        NightmareSyncState disabled = state(false, 100, NightmareTrialType.NONE, 0, 0);
        check(disabled.shouldSend(null, 0), "A new connection needs an initial snapshot");
        check(!disabled.shouldSend(disabled, 1200), "An idle server must not repeat disabled snapshots");

        NightmareSyncState active = state(true, 100, NightmareTrialType.NONE, 0, 0);
        check(active.shouldSend(disabled, 0), "HUD activation must bypass throttling");
        check(disabled.shouldSend(active, 0), "HUD deactivation must bypass throttling");
        check(!active.shouldSend(active, 1200), "Unchanged active state must not be repeated");

        NightmareSyncState lowerSanity = state(true, 99, NightmareTrialType.NONE, 0, 0);
        check(!lowerSanity.shouldSend(active, 4), "Sanity updates must wait for the interval");
        check(lowerSanity.shouldSend(active, 5), "The latest sanity must be sent at the interval");
        check(active.shouldSend(lowerSanity, 5), "The final regeneration update must not be lost");

        NightmareSyncState maze = state(true, 100, NightmareTrialType.MAZE, 60, 0);
        check(maze.shouldSend(active, 0), "Entering a trial must be immediate");
        check(active.shouldSend(maze, 0), "Leaving a trial must be immediate");
        check(state(true, 100, NightmareTrialType.MAZE, 59, 0).shouldSend(maze, 1),
                "Displayed countdown changes must be immediate");
        check(state(true, 100, NightmareTrialType.NONE, 0, 10).shouldSend(active, 0),
                "Immunity activation must be immediate");
        check(active.shouldSend(state(true, 100, NightmareTrialType.NONE, 0, 1), 0),
                "Immunity expiration must be immediate");

        NightmareSyncState previous = null;
        int lastSentTick = 0;
        int sends = 0;
        for (int tick = 0; tick < 1200; tick++) {
            if (disabled.shouldSend(previous, tick - lastSentTick)) {
                previous = disabled;
                lastSentTick = tick;
                sends++;
            }
        }
        check(sends == 1, "One minute without a keeper should send exactly one initial packet per player");
        check(disabled.shouldSend(null, 0), "Cache invalidation must force a snapshot after reconnect/respawn");
        System.out.println("NightmareSyncState: 15 checks passed");
    }

    private static NightmareSyncState state(boolean visible, float sanity, NightmareTrialType trial,
                                            int seconds, int immunity) {
        return new NightmareSyncState(visible, sanity, trial, seconds, immunity);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
