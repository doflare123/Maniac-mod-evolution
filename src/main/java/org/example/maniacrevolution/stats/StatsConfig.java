package org.example.maniacrevolution.stats;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.Properties;

/**
 * Читает настройки подключения к БД из файла stats.env.
 *
 * Файл ищется в:
 *   1. Папке run/ (dev-окружение)
 *   2. Корне сервера (production)
 */
public class StatsConfig {

    private static final Logger LOGGER = LogManager.getLogger("ManiacrevStats");
    private static final String ENV_FILE = "stats.env";

    private final String jdbcUrl;
    private final String user;
    private final String pass;
    private final boolean valid;

    private static StatsConfig instance;

    private StatsConfig() {
        Properties props = loadEnvFile();
        if (props == null) {
            this.jdbcUrl = null;
            this.user = null;
            this.pass = null;
            this.valid = false;
            return;
        }

        String host = props.getProperty("DB_HOST", "localhost");
        String port = props.getProperty("DB_PORT", "3306");
        String name = props.getProperty("DB_NAME", "");
        this.user = props.getProperty("DB_USER", "");
        this.pass = props.getProperty("DB_PASS", "");

        this.jdbcUrl = String.format(
                "jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                host, port, name);
        this.valid = !name.isEmpty() && !user.isEmpty();

        if (this.valid) {
            LOGGER.info("[Stats] Конфиг загружен. БД: {}:{}/{}", host, port, name);
        } else {
            LOGGER.warn("[Stats] stats.env найден, но DB_NAME или DB_USER не заполнены.");
        }
    }

    public static synchronized StatsConfig get() {
        if (instance == null) instance = new StatsConfig();
        return instance;
    }

    public String getJdbcUrl() { return jdbcUrl; }
    public String getUser()    { return user; }
    public String getPass()    { return pass; }
    public boolean isValid()   { return valid; }

    /** Сбрасывает кэш — для горячей перезагрузки конфига. */
    public static synchronized void reload() { instance = null; }

    private Properties loadEnvFile() {
        // Ищем в нескольких местах
        File[] candidates = {
                new File(ENV_FILE),            // корень сервера / run/
                new File("run", ENV_FILE),
                new File("config", ENV_FILE),  // config/
        };

        for (File f : candidates) {
            if (f.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
                    Properties props = new Properties();
                    // Читаем вручную чтобы поддерживать # комментарии и пустые строки
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        int eq = line.indexOf('=');
                        if (eq > 0) {
                            props.setProperty(
                                    line.substring(0, eq).trim(),
                                    line.substring(eq + 1).trim()
                            );
                        }
                    }
                    LOGGER.info("[Stats] Загружен конфиг из: {}", f.getAbsolutePath());
                    return props;
                } catch (IOException e) {
                    LOGGER.error("[Stats] Не удалось прочитать {}: {}", f.getAbsolutePath(), e.getMessage());
                }
            }
        }

        LOGGER.warn("[Stats] Файл stats.env не найден. Статистика отключена. " +
                "Создайте файл stats.env рядом с сервером.");
        return null;
    }
}