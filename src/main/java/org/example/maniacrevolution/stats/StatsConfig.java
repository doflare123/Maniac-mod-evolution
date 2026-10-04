package org.example.maniacrevolution.stats;

import org.example.maniacrevolution.Maniacrev;
import java.net.URI;
import java.util.Properties;

/**
 * HTTPS sender defaults bundled in the mod; database credentials stay on the receiver.
 */
public final class StatsConfig {

    public static final String DEFAULT_ENDPOINT = "https://5.83.140.208:25912/v1/matches";
    private static URI endpoint;
    private static String token = "";
    private static boolean enabled;

    public static void load() {
        Properties p = new Properties();
        try {
            try (var in = StatsConfig.class.getResourceAsStream("/maniacrev-stats.properties")) {
                if (in == null) throw new IllegalStateException("Bundled stats sender settings are missing");
                p.load(in);
            }
            endpoint = URI.create(p.getProperty("endpoint", DEFAULT_ENDPOINT));
            token = p.getProperty("token", "").trim();
            enabled = Boolean.parseBoolean(p.getProperty("enabled", "true"));
            if (!"https".equalsIgnoreCase(endpoint.getScheme()) || endpoint.getHost() == null
                    || endpoint.getUserInfo() != null || endpoint.getQuery() != null || endpoint.getFragment() != null) {
                throw new IllegalArgumentException("Stats endpoint must be HTTPS without credentials/query");
            }
            if (token.isEmpty()) enabled = false;
            if (!enabled) Maniacrev.LOGGER.info("[Stats] Upload disabled in this build");
        } catch (Exception e) {
            enabled = false;
            Maniacrev.LOGGER.warn("[Stats] Invalid sender configuration ({})", e.getClass().getSimpleName());
        }
    }
    public static boolean isValid() { return enabled; }
    public static URI endpoint() { return endpoint; }
    public static String token() { return token; }

    private StatsConfig() {}
}
