package org.example.maniacrevolution.atmosphere.client;

import org.example.maniacrevolution.settings.GraphicsMode;

import java.util.ArrayList;
import java.util.List;

/** Pure benchmark schedule and statistics, shared by the client runner and regression checks. */
public final class BenchmarkPlan {
    public enum Mode {
        VEIL_RENDER_BYPASS(GraphicsMode.OFF), OFF(GraphicsMode.OFF), LIGHT(GraphicsMode.LIGHT), NORMAL(GraphicsMode.NORMAL), HEAVY(GraphicsMode.HEAVY);
        public final GraphicsMode graphics;
        Mode(GraphicsMode graphics) { this.graphics = graphics; }
    }
    public record Point(String name, String dimension, double x, double y, double z, float yaw, float pitch) {
        public Point {
            if (name == null || name.isBlank() || name.length() > 64 || dimension == null
                    || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                    || Math.abs(x) > 29_999_984 || Math.abs(z) > 29_999_984 || Math.abs(y) > 2048
                    || !Float.isFinite(yaw) || !Float.isFinite(pitch) || Math.abs(pitch) > 90)
                throw new IllegalArgumentException("Invalid benchmark point");
        }
    }
    public record Resolution(int width, int height) {
        public Resolution { if (width < 320 || height < 240) throw new IllegalArgumentException("Resolution too small"); }
    }
    public record Case(int id, Point point, Resolution resolution, Mode mode, int repeat) {}
    public record Statistics(int count, double meanMs, double medianMs, double p95Ms, double p99Ms, double maxMs,
                             double averageFps, double medianFps, double onePercentLowFps) {}

    private BenchmarkPlan() {}

    public static List<Point> defaults() {
        return List.of(new Point("room", "minecraft:overworld", -244.5, 41, -22.92, 90, 0),
                new Point("hall-north", "minecraft:overworld", -217.5, 41, -8.5, 0, 0),
                new Point("hall-south", "minecraft:overworld", -217.5, 41, -8.5, 180, 0),
                new Point("outside", "minecraft:overworld", -494.5, 44, 18.5, 180, 0));
    }

    public static List<Resolution> resolutions(int width, int height, boolean matrix) {
        List<Resolution> result = new ArrayList<>();
        result.add(new Resolution(width, height));
        if (matrix) for (Resolution candidate : List.of(new Resolution(1280, 720), new Resolution(1920, 1080))) {
            if (candidate.width <= width && candidate.height <= height && !result.contains(candidate)) result.add(candidate);
        }
        return List.copyOf(result);
    }

    public static List<Case> create(List<Point> points, List<Resolution> resolutions, int repeats) {
        if (points.isEmpty() || points.size() > 32 || repeats < 1 || repeats > 4) throw new IllegalArgumentException("Invalid benchmark schedule");
        List<Case> result = new ArrayList<>();
        for (Resolution resolution : resolutions) for (Point point : points) for (int repeat = 0; repeat < repeats; repeat++) {
            Mode[] modes = Mode.values();
            for (int i = 0; i < modes.length; i++) {
                Mode mode = modes[repeat % 2 == 0 ? i : modes.length - i - 1];
                result.add(new Case(result.size(), point, resolution, mode, repeat + 1));
            }
        }
        return List.copyOf(result);
    }

    public static Statistics statistics(List<Double> values) {
        if (values.isEmpty()) return null;
        double[] sorted = values.stream().mapToDouble(Double::doubleValue).sorted().toArray();
        double sum = 0;
        for (double value : sorted) {
            if (!Double.isFinite(value) || value <= 0) throw new IllegalArgumentException("Invalid timing sample");
            sum += value;
        }
        double median = quantile(sorted, .5), p99 = quantile(sorted, .99);
        return new Statistics(sorted.length, sum / sorted.length, median, quantile(sorted, .95), p99,
                sorted[sorted.length - 1], 1000 * sorted.length / sum, 1000 / median, 1000 / p99);
    }

    private static double quantile(double[] values, double q) {
        double position = (values.length - 1) * q;
        int lower = (int) position, upper = Math.min(lower + 1, values.length - 1);
        return values[lower] + (values[upper] - values[lower]) * (position - lower);
    }
}
