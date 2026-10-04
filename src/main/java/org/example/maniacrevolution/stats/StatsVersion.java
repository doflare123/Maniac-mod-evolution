package org.example.maniacrevolution.stats;

import java.io.IOException;
import java.util.Properties;

/** Immutable build label; runtime sender configuration cannot change it. */
public final class StatsVersion {
    public static final String VALUE = load();

    private StatsVersion() {}

    private static String load() {
        try (var input = StatsVersion.class.getResourceAsStream("/mod-stats-version.properties")) {
            if (input != null) {
                var properties = new Properties();
                properties.load(input);
                var value = properties.getProperty("mod_stats_version", "unknown");
                if (value.matches("[A-Za-z0-9][A-Za-z0-9_.-]{0,95}")) return value;
            }
        } catch (IOException ignored) {
            // Development launches without processed resources are labelled explicitly.
        }
        return "unknown";
    }
}
