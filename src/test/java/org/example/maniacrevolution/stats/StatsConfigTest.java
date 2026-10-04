package org.example.maniacrevolution.stats;

import java.util.Properties;

/** Runs against the release JAR in an empty working directory, without Forge startup. */
public final class StatsConfigTest {
    public static void main(String[] args) throws Exception {
        var bundled = new Properties();
        try (var input = StatsConfigTest.class.getResourceAsStream("/maniacrev-stats.properties")) {
            if (input == null) throw new AssertionError("Sender settings missing from JAR");
            bundled.load(input);
        }
        StatsConfig.load();
        if (!StatsConfig.isValid()) throw new AssertionError("Automatic stats upload is disabled");
        if (!StatsConfig.endpoint().toString().equals(bundled.getProperty("endpoint")))
            throw new AssertionError("Endpoint does not match bundled settings");
        if (StatsConfig.token().isBlank() || !StatsConfig.token().equals(bundled.getProperty("token")))
            throw new AssertionError("Token does not match bundled settings");
        if (!"https".equals(StatsConfig.endpoint().getScheme())) throw new AssertionError("HTTPS required");
        System.out.println("Bundled sender settings: upload enabled without external files");
    }
}
