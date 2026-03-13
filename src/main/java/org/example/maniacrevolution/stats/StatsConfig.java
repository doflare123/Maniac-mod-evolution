package org.example.maniacrevolution.stats;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Настройки подключения к БД.
 * Значения вшиты при компиляции из gradle.properties через BuildConfig.
 * Никаких файлов конфигурации на сервере не нужно.
 */
public final class StatsConfig {

    private static final Logger LOGGER = LogManager.getLogger("ManiacrevStats");

    public static final String JDBC_URL = String.format(
            "jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
            BuildConfig.DB_HOST, BuildConfig.DB_PORT, BuildConfig.DB_NAME);

    public static final String USER = BuildConfig.DB_USER;
    public static final String PASS = BuildConfig.DB_PASS;

    public static boolean isValid() {
        boolean ok = !BuildConfig.DB_HOST.isEmpty()
                && !BuildConfig.DB_NAME.isEmpty()
                && !BuildConfig.DB_USER.isEmpty()
                // Проверяем что Gradle подставил реальные значения, а не плейсхолдер
                && !BuildConfig.DB_HOST.startsWith("${");
        if (!ok) {
            LOGGER.warn("[Stats] DB credentials не заданы. " +
                    "Создайте gradle.properties с stats_db_host/user/pass и пересоберите мод.");
        }
        return ok;
    }

    private StatsConfig() {}
}