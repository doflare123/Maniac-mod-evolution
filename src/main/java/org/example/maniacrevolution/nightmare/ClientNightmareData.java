package org.example.maniacrevolution.nightmare;

public final class ClientNightmareData {
    private static boolean visible;
    private static float sanity = NightmareConfig.MAX_SANITY;
    private static float maxSanity = NightmareConfig.MAX_SANITY;
    private static NightmareTrialType trialType = NightmareTrialType.NONE;
    private static int trialSecondsLeft;
    private static long screamerUntilMillis;

    private ClientNightmareData() {}

    public static void update(boolean hudVisible, float sanityValue, float maxValue,
                              NightmareTrialType activeTrial, int secondsLeft) {
        visible = hudVisible;
        sanity = sanityValue;
        maxSanity = maxValue;
        trialType = activeTrial;
        trialSecondsLeft = secondsLeft;
    }

    public static boolean isVisible() {
        return visible;
    }

    public static float getSanityPercent() {
        return maxSanity <= 0.0F ? 0.0F : Math.max(0.0F, Math.min(1.0F, sanity / maxSanity));
    }

    public static NightmareTrialType getTrialType() {
        return trialType;
    }

    public static int getTrialSecondsLeft() {
        return trialSecondsLeft;
    }

    public static void showScreamer(int durationTicks) {
        screamerUntilMillis = System.currentTimeMillis() + durationTicks * 50L;
    }

    public static boolean shouldShowScreamer() {
        return System.currentTimeMillis() < screamerUntilMillis;
    }
}
