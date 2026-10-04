package org.example.maniacrevolution.stats;

import java.util.LinkedHashMap;
import java.util.Map;

/** Fixed-size aggregates; never retains players, damage events or world entities. */
public final class StatsMetrics {
    private final Map<String, Double> counters = new LinkedHashMap<>();
    public void add(String key, double amount) {
        if (!Double.isFinite(amount) || amount < 0) return;
        if (!counters.containsKey(key) && counters.size() >= 95) key = "other";
        counters.merge(key, amount, Double::sum);
    }
    public Map<String, Double> snapshot() { return new LinkedHashMap<>(counters); }
}
