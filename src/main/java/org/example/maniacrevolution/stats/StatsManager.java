package org.example.maniacrevolution.stats;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class StatsManager {

    private static final Logger LOGGER = LogManager.getLogger("ManiacrevStats");

    private static volatile boolean driverReady = false;

    // ── Маппинги ──────────────────────────────────────────────────────────

    private static final Map<Integer, Integer> SURVIVOR_CLASS_MAP = Map.of(
            8, 10, 5, 11, 10, 4, 9, 3, 1, 2, 6, 1
    );
    private static final Map<Integer, Integer> MANIAC_CLASS_MAP = Map.of(
            7, 8, 5, 6, 6, 9, 12, 12, 10, 7, 4, 5
    );
    private static final Map<String, Integer> PERK_ID_MAP = Map.ofEntries(
            Map.entry("bigmoney", 1), Map.entry("megamind", 2),
            Map.entry("gto_medal", 3), Map.entry("fear_wave", 4),
            Map.entry("str_perk", 5), Map.entry("mana_break", 6),
            Map.entry("hedgehog_skin", 7), Map.entry("mimic", 8),
            Map.entry("wallhack", 9), Map.entry("last_breath", 10),
            Map.entry("quick_reflexes", 11), Map.entry("blindness", 12),
            Map.entry("bloodflow", 13), Map.entry("i_am_speed", 14),
            Map.entry("catch_mistakes", 15), Map.entry("computer_breaker", 16),
            Map.entry("highlight", 17)
    );

    // ── Инициализация драйвера ─────────────────────────────────────────────

    /**
     * Вызывается из FMLCommonSetupEvent в главном классе мода.
     *
     * В момент вызова Forge ещё настраивает classloader-ы,
     * поэтому Class.forName здесь работает корректно.
     *
     * Регистрация в главном классе:
     *   @SubscribeEvent
     *   public void onCommonSetup(FMLCommonSetupEvent event) {
     *       event.enqueueWork(StatsManager::initDriver);
     *   }
     */
    public static void initDriver(FMLCommonSetupEvent event) {
        if (!StatsConfig.isValid()) {
            return;
        }
        // Диагностика: показываем что вообще есть в classpath
        LOGGER.info("[Stats] java.class.path entries with 'mysql':");
        for (String entry : System.getProperty("java.class.path", "").split(java.io.File.pathSeparator)) {
            if (entry.toLowerCase().contains("mysql") || entry.toLowerCase().contains("connector")) {
                LOGGER.info("[Stats]   FOUND: {}", entry);
            }
        }
        LOGGER.info("[Stats] Trying classloaders...");

        // Перебираем все возможные classloader-ы
        ClassLoader[] loaders = {
                StatsManager.class.getClassLoader(),
                Thread.currentThread().getContextClassLoader(),
                ClassLoader.getSystemClassLoader()
        };
        for (ClassLoader cl : loaders) {
            if (cl == null) continue;
            try {
                Class<?> driverClass = Class.forName("com.mysql.cj.jdbc.Driver", true, cl);
                java.sql.Driver driver = (java.sql.Driver) driverClass.getDeclaredConstructor().newInstance();
                java.sql.DriverManager.registerDriver(new DriverShim(driver));
                driverReady = true;
                LOGGER.info("[Stats] MySQL driver загружен через: {}", cl.getClass().getName());
                return;
            } catch (Exception ignored) {}
        }
        LOGGER.error("[Stats] com.mysql.cj.jdbc.Driver не найден ни в одном classloader. " +
                "Убедитесь что mysql-connector-java добавлен через jarJar в build.gradle и gradle refresh выполнен.");
    }

    /** Шим для регистрации драйвера из чужого classloader в DriverManager */
    private static class DriverShim implements java.sql.Driver {
        private final java.sql.Driver d;
        DriverShim(java.sql.Driver d) { this.d = d; }
        public java.sql.Connection connect(String u, java.util.Properties p) throws java.sql.SQLException { return d.connect(u, p); }
        public boolean acceptsURL(String u) throws java.sql.SQLException { return d.acceptsURL(u); }
        public java.sql.DriverPropertyInfo[] getPropertyInfo(String u, java.util.Properties p) throws java.sql.SQLException { return d.getPropertyInfo(u, p); }
        public int getMajorVersion() { return d.getMajorVersion(); }
        public int getMinorVersion() { return d.getMinorVersion(); }
        public boolean jdbcCompliant() { return d.jdbcCompliant(); }
        public java.util.logging.Logger getParentLogger() throws java.sql.SQLFeatureNotSupportedException { return d.getParentLogger(); }
    }

    // ── Публичный API ─────────────────────────────────────────────────────

    public static CompletableFuture<Integer> sendStats(MinecraftServer server, int winner) {
        if (!driverReady) {
            LOGGER.error("[Stats] Драйвер не готов. Проверьте логи загрузки мода.");
            return CompletableFuture.completedFuture(-1);
        }

        GameSnapshot snapshot = collectSnapshot(server, winner);
        CompletableFuture<Integer> future = new CompletableFuture<>();

        Thread t = new Thread(() -> {
            try {
                future.complete(insertSnapshot(snapshot));
            } catch (Exception e) {
                LOGGER.error("[Stats] Ошибка записи: {}", e.getMessage(), e);
                future.complete(-1);
            }
        }, "ManiacrevStats-DB");
        t.setDaemon(true);
        t.start();
        return future;
    }

    public static void optOut(ServerPlayer player) {
        LOGGER.info("[Stats] Opt-out: {} ({})",
                player.getGameProfile().getName(), player.getUUID());
    }

    // ── Сбор данных ───────────────────────────────────────────────────────

    private static GameSnapshot collectSnapshot(MinecraftServer server, int winner) {
        Scoreboard sb    = server.getScoreboard();
        Objective srvObj = sb.getObjective("SurvivorClass");
        Objective mnObj  = sb.getObjective("ManiacClass");

        Map<Integer, Integer> sClasses = new HashMap<>(), mClasses = new HashMap<>();
        Map<Integer, Integer> sPerks   = new HashMap<>(), mPerks   = new HashMap<>();
        int sCount = 0, mCount = 0;

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            String team = p.getTeam() != null ? p.getTeam().getName() : null;
            if (team == null) continue;

            if ("survivors".equalsIgnoreCase(team)) {
                sCount++;
                readClass(sb, srvObj, p, SURVIVOR_CLASS_MAP, sClasses);
                collectPerks(p, sPerks);
            } else if ("maniac".equalsIgnoreCase(team)) {
                mCount++;
                readClass(sb, mnObj, p, MANIAC_CLASS_MAP, mClasses);
                collectPerks(p, mPerks);
            }
        }
        return new GameSnapshot(winner, sCount, mCount, sClasses, mClasses, sPerks, mPerks);
    }

    private static void readClass(Scoreboard sb, Objective obj, ServerPlayer p,
                                  Map<Integer, Integer> mapping, Map<Integer, Integer> target) {
        if (obj == null) return;
        Score score = sb.getOrCreatePlayerScore(p.getScoreboardName(), obj);
        int dbId = mapping.getOrDefault(score.getScore(), -1);
        if (dbId > 0) target.merge(dbId, 1, Integer::sum);
    }

    private static void collectPerks(ServerPlayer player, Map<Integer, Integer> target) {
        org.example.maniacrevolution.data.PlayerData data =
                org.example.maniacrevolution.data.PlayerDataManager.get(player);
        if (data == null) return;
        for (var inst : data.getSelectedPerks()) {
            Integer dbId = PERK_ID_MAP.get(inst.getPerk().getId());
            if (dbId != null) target.merge(dbId, 1, Integer::sum);
        }
    }

    // ── БД ────────────────────────────────────────────────────────────────

    private static Connection openConnection() throws SQLException {
        return DriverManager.getConnection(StatsConfig.JDBC_URL, StatsConfig.USER, StatsConfig.PASS);
    }

    private static int insertSnapshot(GameSnapshot s) throws SQLException {
        try (Connection conn = openConnection()) {
            conn.setAutoCommit(false);
            try {
                int gameId;
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO games (winner, survivors_count, maniacs_count) VALUES (?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, s.winner); ps.setInt(2, s.survivorCount); ps.setInt(3, s.maniacCount);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); gameId = rs.getInt(1); }
                }
                batchInsert(conn, gameId, "game_classes", "class_id", 0, s.survivorClasses);
                batchInsert(conn, gameId, "game_classes", "class_id", 1, s.maniacClasses);
                batchInsert(conn, gameId, "game_perks",   "perk_id",  0, s.survivorPerks);
                batchInsert(conn, gameId, "game_perks",   "perk_id",  1, s.maniacPerks);
                conn.commit();
                LOGGER.info("[Stats] Игра #{} сохранена.", gameId);
                return gameId;
            } catch (SQLException e) { conn.rollback(); throw e; }
        }
    }

    private static void batchInsert(Connection conn, int gameId, String table,
                                    String idCol, int team,
                                    Map<Integer, Integer> map) throws SQLException {
        if (map.isEmpty()) return;
        String sql = "INSERT INTO " + table + " (game_id, team, " + idCol + ", count) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Map.Entry<Integer, Integer> e : map.entrySet()) {
                ps.setInt(1, gameId); ps.setInt(2, team);
                ps.setInt(3, e.getKey()); ps.setInt(4, e.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // ── Snapshot ──────────────────────────────────────────────────────────

    private record GameSnapshot(
            int winner, int survivorCount, int maniacCount,
            Map<Integer, Integer> survivorClasses, Map<Integer, Integer> maniacClasses,
            Map<Integer, Integer> survivorPerks,   Map<Integer, Integer> maniacPerks
    ) {}
}