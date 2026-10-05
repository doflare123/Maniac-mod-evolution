package org.example.maniacrevolution.warden.client;

import java.util.Locale;

/** CPU wall-time only; no synchronous GPU queries or per-frame logging. */
final class WardenVisionPerformance {
    enum Stage { CACHE, CAPTURE, BUILD, DRAW }
    private static final long[] times = new long[4], calls = new long[4];
    private static int points;
    private WardenVisionPerformance() {}
    static void record(Stage stage, long start) {
        int index = stage.ordinal();
        times[index] += System.nanoTime() - start;
        calls[index]++;
    }
    static void points(int count) { points = count; }
    static void reset() {
        java.util.Arrays.fill(times, 0);
        java.util.Arrays.fill(calls, 0);
        points = 0;
    }
    static String report() {
        return String.format(Locale.ROOT,
                "Варден CPU мс/вызов: cache=%.3f, capture=%.3f, build=%.3f, draw=%.3f; points=%d (не время GPU).",
                average(0), average(1), average(2), average(3), points);
    }
    private static double average(int index) { return calls[index] == 0 ? 0 : times[index] / (calls[index] * 1_000_000.0); }
}
