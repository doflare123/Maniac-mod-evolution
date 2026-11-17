package org.example.maniacrevolution.atmosphere.client;

import java.util.List;

public final class BenchmarkPlanTest {
    public static void main(String[] args) {
        var resolutions = BenchmarkPlan.resolutions(3840, 2160, true);
        check(resolutions.size() == 3, "current/720p/1080p");
        check(BenchmarkPlan.resolutions(1280, 720, true).size() == 1, "no duplicates or automatic upscaling");
        check(BenchmarkPlan.resolutions(1920, 1080, true).size() == 2, "1080p does not repeat native resolution");
        var plan = BenchmarkPlan.create(List.of(BenchmarkPlan.defaults().get(0)), resolutions, 2);
        check(plan.size() == 30, "all resolutions, five modes and both repeats");
        check(plan.get(0).mode() == BenchmarkPlan.Mode.VEIL_RENDER_BYPASS && plan.get(4).mode() == BenchmarkPlan.Mode.HEAVY, "forward order");
        check(plan.get(5).mode() == BenchmarkPlan.Mode.HEAVY && plan.get(9).mode() == BenchmarkPlan.Mode.VEIL_RENDER_BYPASS, "reverse order");
        for (int i = 0; i < plan.size(); i++) check(plan.get(i).id() == i, "stable unique case IDs");
        var stats = BenchmarkPlan.statistics(List.of(10.0, 10.0, 10.0, 70.0));
        check(stats.count() == 4 && stats.meanMs() == 25 && stats.medianMs() == 10, "mean differs from median under stutter");
        check(stats.averageFps() == 40 && stats.medianFps() == 100 && stats.onePercentLowFps() < 15, "FPS and slow-tail definitions");
        check(BenchmarkPlan.statistics(List.of()) == null, "missing GPU samples remain missing");
        boolean invalid = false;
        try { BenchmarkPlan.statistics(List.of(Double.NaN)); } catch (IllegalArgumentException expected) { invalid = true; }
        check(invalid, "reject invalid timing values");
        invalid = false;
        try { new BenchmarkPlan.Point("bad", "minecraft:overworld", Double.NaN, 0, 0, 0, 0); } catch (IllegalArgumentException expected) { invalid = true; }
        check(invalid, "reject malformed route coordinates");
        System.out.println("Portable benchmark plan and statistics checks passed");
    }
    private static void check(boolean value, String label) { if (!value) throw new AssertionError(label); }
}
