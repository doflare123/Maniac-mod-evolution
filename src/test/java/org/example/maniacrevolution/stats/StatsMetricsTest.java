package org.example.maniacrevolution.stats;

public final class StatsMetricsTest {
    public static void main(String[] args) {
        StatsMetrics metrics = new StatsMetrics();
        metrics.add("damage", 2); metrics.add("damage", 3);
        metrics.add("bad", Double.NaN); metrics.add("bad", -1);
        if (metrics.snapshot().get("damage") != 5 || metrics.snapshot().containsKey("bad")) throw new AssertionError("Invalid aggregate");
        for (int i = 0; i < 5000; i++) metrics.add("source_" + i, 1);
        if (metrics.snapshot().size() > 96) throw new AssertionError("Unbounded counters");
        var copy = metrics.snapshot(); copy.put("damage", 0.0);
        if (metrics.snapshot().get("damage") != 5) throw new AssertionError("Snapshot shares mutable storage");
        System.out.println("StatsMetrics: counters, invalid input, bounds and snapshot isolation passed");
    }
}
