package org.example.maniacrevolution.stats;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.maniacrevolution.downed.DownedCapability;
import org.example.maniacrevolution.downed.DownedData;
import org.example.maniacrevolution.downed.DownedState;
import org.example.maniacrevolution.hack.HackConfig;
import org.example.maniacrevolution.hack.HackManager;
import org.example.maniacrevolution.map.MapData;
import org.example.maniacrevolution.map.MapRegistry;
import org.example.maniacrevolution.settings.GameSettings;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class StatsManager {

    private static final Logger LOGGER = LogManager.getLogger("ManiacrevStats");

    public static final int RESULT_ERROR = -1;
    public static final int RESULT_SKIPPED = -2;

    private static final int MIN_MATCH_SECONDS = 60;
    private static final String SESSION_SOURCE_SERVER_START = "server_start";
    private static final String SESSION_SOURCE_FIRST_JOIN = "first_world_join";
    private static final String SESSION_KEY_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int SESSION_KEY_LENGTH = 6;
    private static final SecureRandom SESSION_KEY_RANDOM = new SecureRandom();

    private static volatile boolean driverReady = false;
    private static volatile int activeSessionId = -1;
    private static volatile String activeSessionKey = null;
    private static volatile String lastSkipReason = "";

    private static final Object MATCH_LOCK = new Object();
    private static long activeMatchStartMillis = -1L;
    private static Timestamp activeMatchStartedAt = null;
    private static boolean activeMatchInvalidated = false;
    private static final Set<UUID> activeMatchParticipants = new HashSet<>();

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

    public static void initDriver(FMLCommonSetupEvent event) {
        if (!StatsConfig.isValid()) {
            return;
        }

        LOGGER.info("[Stats] java.class.path entries with 'mysql':");
        for (String entry : System.getProperty("java.class.path", "").split(java.io.File.pathSeparator)) {
            if (entry.toLowerCase().contains("mysql") || entry.toLowerCase().contains("connector")) {
                LOGGER.info("[Stats]   FOUND: {}", entry);
            }
        }
        LOGGER.info("[Stats] Trying classloaders...");

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
                LOGGER.info("[Stats] MySQL driver loaded via: {}", cl.getClass().getName());
                return;
            } catch (Exception ignored) {
            }
        }
        LOGGER.error("[Stats] com.mysql.cj.jdbc.Driver not found. Check mysql connector jarJar setup.");
    }

    private static class DriverShim implements java.sql.Driver {
        private final java.sql.Driver d;

        DriverShim(java.sql.Driver d) {
            this.d = d;
        }

        public java.sql.Connection connect(String u, java.util.Properties p) throws java.sql.SQLException {
            return d.connect(u, p);
        }

        public boolean acceptsURL(String u) throws java.sql.SQLException {
            return d.acceptsURL(u);
        }

        public java.sql.DriverPropertyInfo[] getPropertyInfo(String u, java.util.Properties p) throws java.sql.SQLException {
            return d.getPropertyInfo(u, p);
        }

        public int getMajorVersion() {
            return d.getMajorVersion();
        }

        public int getMinorVersion() {
            return d.getMinorVersion();
        }

        public boolean jdbcCompliant() {
            return d.jdbcCompliant();
        }

        public java.util.logging.Logger getParentLogger() throws java.sql.SQLFeatureNotSupportedException {
            return d.getParentLogger();
        }
    }

    public static CompletableFuture<Integer> sendStats(MinecraftServer server, int winner) {
        if (!driverReady) {
            LOGGER.error("[Stats] Driver is not ready.");
            return CompletableFuture.completedFuture(RESULT_ERROR);
        }

        GameSnapshot snapshot = collectSnapshot(server, winner);
        if (snapshot.skipReason != null) {
            lastSkipReason = snapshot.skipReason;
            clearActiveMatch();
            LOGGER.info("[Stats] Match skipped: {}", snapshot.skipReason);
            return CompletableFuture.completedFuture(RESULT_SKIPPED);
        }

        CompletableFuture<Integer> future = new CompletableFuture<>();
        Thread t = new Thread(() -> {
            try {
                int result = insertSnapshot(snapshot);
                clearActiveMatch();
                future.complete(result);
            } catch (Exception e) {
                LOGGER.error("[Stats] Write error: {}", e.getMessage(), e);
                future.complete(RESULT_ERROR);
            }
        }, "ManiacrevStats-DB");
        t.setDaemon(true);
        t.start();
        return future;
    }

    public static void onServerStarted(MinecraftServer server) {
        ensureSessionAsync(SESSION_SOURCE_SERVER_START, server);
    }

    public static void onServerStopping() {
        if (!driverReady || activeSessionId <= 0) return;
        int sessionId = activeSessionId;
        String sessionKey = activeSessionKey;
        Thread t = new Thread(() -> {
            try (Connection conn = openConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "UPDATE game_sessions SET ended_at = current_timestamp() WHERE id = ? AND session_key = ? AND ended_at IS NULL")) {
                ps.setInt(1, sessionId);
                ps.setString(2, sessionKey);
                ps.executeUpdate();
            } catch (SQLException e) {
                LOGGER.warn("[Stats] Failed to close session {}: {}", sessionKey, e.getMessage());
            }
        }, "ManiacrevStats-EndSession");
        t.setDaemon(true);
        t.start();
    }

    public static void onPlayerLoggedIn(ServerPlayer player) {
        if (!driverReady) return;
        Thread t = new Thread(() -> {
            try (Connection conn = openConnection()) {
                SessionRef session = ensureSession(conn, SESSION_SOURCE_FIRST_JOIN);
                upsertSessionPlayer(conn, session, player);
            } catch (SQLException e) {
                LOGGER.warn("[Stats] Failed to record session player {}: {}",
                        player.getGameProfile().getName(), e.getMessage());
            }
        }, "ManiacrevStats-SessionPlayer");
        t.setDaemon(true);
        t.start();
    }

    public static void onPlayerLoggedOut(ServerPlayer player) {
        if (!isActiveMatchParticipant(player)) return;
        synchronized (MATCH_LOCK) {
            activeMatchInvalidated = true;
        }
        LOGGER.info("[Stats] Match marked irrelevant: {} left during the game.",
                player.getGameProfile().getName());
    }

    public static void onGameStarted(MinecraftServer server) {
        synchronized (MATCH_LOCK) {
            activeMatchStartMillis = System.currentTimeMillis();
            activeMatchStartedAt = new Timestamp(activeMatchStartMillis);
            activeMatchInvalidated = false;
            activeMatchParticipants.clear();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (isTrackedTeam(player)) {
                    activeMatchParticipants.add(player.getUUID());
                }
            }
        }
        ensureSessionAsync(SESSION_SOURCE_FIRST_JOIN, server);
        LOGGER.info("[Stats] Started match tracking. Participants: {}", activeMatchParticipants.size());
    }

    public static String getLastSkipReason() {
        return lastSkipReason;
    }

    public static void optOut(ServerPlayer player) {
        LOGGER.info("[Stats] Opt-out: {} ({})",
                player.getGameProfile().getName(), player.getUUID());
    }

    private static GameSnapshot collectSnapshot(MinecraftServer server, int winner) {
        Scoreboard sb = server.getScoreboard();
        Objective srvObj = sb.getObjective("SurvivorClass");
        Objective mnObj = sb.getObjective("ManiacClass");

        Map<Integer, Integer> sClasses = new HashMap<>(), mClasses = new HashMap<>();
        Map<Integer, Integer> sPerks = new HashMap<>(), mPerks = new HashMap<>();
        int sCount = 0, mCount = 0, activeSurvivors = 0;

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            String team = p.getTeam() != null ? p.getTeam().getName() : null;
            if (team == null) continue;

            if ("survivors".equalsIgnoreCase(team)) {
                sCount++;
                if (isActiveSurvivor(p)) activeSurvivors++;
                readClass(sb, srvObj, p, SURVIVOR_CLASS_MAP, sClasses);
                collectPerks(p, sPerks);
            } else if ("maniac".equalsIgnoreCase(team)) {
                mCount++;
                readClass(sb, mnObj, p, MANIAC_CLASS_MAP, mClasses);
                collectPerks(p, mPerks);
            }
        }

        MatchState matchState = readMatchState();
        if (matchState.startMillis < 0L) {
            return GameSnapshot.skipped("no active match state; use /maniacrev start before sending stats");
        }
        if (matchState.invalidated) {
            return GameSnapshot.skipped("a match participant left the game");
        }

        Timestamp endedAt = new Timestamp(System.currentTimeMillis());
        int durationSeconds = (int) Math.max(0L, (endedAt.getTime() - matchState.startMillis) / 1000L);
        if (durationSeconds < MIN_MATCH_SECONDS) {
            return GameSnapshot.skipped("match shorter than " + MIN_MATCH_SECONDS + " seconds (" + durationSeconds + " sec)");
        }

        int totalPlayers = sCount + mCount;
        if (totalPlayers < 2) {
            return GameSnapshot.skipped("too few players for stats: " + totalPlayers);
        }
        if (totalPlayers == 2 && (sCount != 1 || mCount != 1)) {
            return GameSnapshot.skipped("two-player match is not 1v1: survivors=" + sCount + ", maniacs=" + mCount);
        }

        boolean lastSurvivorStanding = sCount > 1 && activeSurvivors == 1;
        boolean lastSurvivorStandingWon = lastSurvivorStanding && winner == 0;
        MapSnapshot map = collectMapSnapshot(server);
        HackSettingsSnapshot hackSettings = collectHackSettingsSnapshot();

        return new GameSnapshot(
                winner, sCount, mCount, matchState.startedAt, endedAt, durationSeconds,
                lastSurvivorStanding, lastSurvivorStandingWon,
                map.numericId, map.id, map.name,
                HackConfig.COMPUTERS_NEEDED_FOR_WIN, HackManager.get().getTotalHacked(), hackSettings,
                sClasses, mClasses, sPerks, mPerks, null
        );
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

    private static MapSnapshot collectMapSnapshot(MinecraftServer server) {
        int numericId = 0;
        Scoreboard sb = server.getScoreboard();
        Objective mapObj = sb.getObjective("map");
        if (mapObj != null) {
            numericId = sb.getOrCreatePlayerScore("Game", mapObj).getScore();
        }
        if (numericId <= 0) {
            numericId = GameSettings.get(server).getSelectedMap();
        }

        MapData data = numericId > 0 ? MapRegistry.getMapByNumericId(numericId) : null;
        return new MapSnapshot(
                numericId > 0 ? numericId : null,
                data != null ? data.getId() : null,
                data != null ? data.getName() : null
        );
    }

    private static HackSettingsSnapshot collectHackSettingsSnapshot() {
        return new HackSettingsSnapshot(
                HackConfig.HACK_POINTS_REQUIRED,
                HackConfig.POINTS_PER_PLAYER_PER_SECOND,
                HackConfig.POINTS_PER_SPECIALIST_PER_SECOND,
                HackConfig.MAX_BONUS_PLAYERS,
                HackConfig.HACKER_RADIUS,
                HackConfig.SUPPORT_RADIUS,
                HackConfig.QTE_INTERVAL_MIN_SECONDS,
                HackConfig.QTE_INTERVAL_MAX_SECONDS,
                HackConfig.QTE_SUCCESS_BONUS,
                HackConfig.QTE_CRIT_BONUS,
                HackConfig.COMPUTERS_NEEDED_FOR_WIN
        );
    }

    private static Connection openConnection() throws SQLException {
        return DriverManager.getConnection(StatsConfig.JDBC_URL, StatsConfig.USER, StatsConfig.PASS);
    }

    private static int insertSnapshot(GameSnapshot s) throws SQLException {
        try (Connection conn = openConnection()) {
            conn.setAutoCommit(false);
            try {
                SessionRef session = ensureSession(conn, SESSION_SOURCE_FIRST_JOIN);
                int gameId;
                boolean duel = s.survivorCount + s.maniacCount == 2;

                if (duel) {
                    gameId = insertDuelGame(conn, session, s);
                    batchInsert(conn, gameId, "duel_game_classes", "duel_game_id", "class_id", 0, s.survivorClasses);
                    batchInsert(conn, gameId, "duel_game_classes", "duel_game_id", "class_id", 1, s.maniacClasses);
                    batchInsert(conn, gameId, "duel_game_perks", "duel_game_id", "perk_id", 0, s.survivorPerks);
                    batchInsert(conn, gameId, "duel_game_perks", "duel_game_id", "perk_id", 1, s.maniacPerks);
                    insertHackSettingsIfCustom(conn, gameId, "duel_game_hack_settings", "duel_game_id", s.hackSettings);
                } else {
                    gameId = insertMainGame(conn, session, s);
                    batchInsert(conn, gameId, "game_classes", "game_id", "class_id", 0, s.survivorClasses);
                    batchInsert(conn, gameId, "game_classes", "game_id", "class_id", 1, s.maniacClasses);
                    batchInsert(conn, gameId, "game_perks", "game_id", "perk_id", 0, s.survivorPerks);
                    batchInsert(conn, gameId, "game_perks", "game_id", "perk_id", 1, s.maniacPerks);
                    insertHackSettingsIfCustom(conn, gameId, "game_hack_settings", "game_id", s.hackSettings);
                }

                conn.commit();
                LOGGER.info("[Stats] {} #{} saved. Session {}, duration {} sec.",
                        duel ? "Duel game" : "Game", gameId, session.key, s.durationSeconds);
                return gameId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private static int insertMainGame(Connection conn, SessionRef session, GameSnapshot s) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO games (session_id, session_key, started_at, ended_at, duration_seconds, winner, survivors_count, maniacs_count, last_survivor_standing, last_survivor_standing_won, map_numeric_id, map_id, map_name, computers_target, computers_charged) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, session.id);
            ps.setString(2, session.key);
            ps.setTimestamp(3, s.startedAt);
            ps.setTimestamp(4, s.endedAt);
            ps.setInt(5, s.durationSeconds);
            ps.setInt(6, s.winner);
            ps.setInt(7, s.survivorCount);
            ps.setInt(8, s.maniacCount);
            ps.setBoolean(9, s.lastSurvivorStanding);
            ps.setBoolean(10, s.lastSurvivorStandingWon);
            setNullableInt(ps, 11, s.mapNumericId);
            ps.setString(12, s.mapId);
            ps.setString(13, s.mapName);
            ps.setInt(14, s.computersTarget);
            ps.setInt(15, s.computersCharged);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static int insertDuelGame(Connection conn, SessionRef session, GameSnapshot s) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO duel_games (session_id, session_key, started_at, ended_at, duration_seconds, winner, survivors_count, maniacs_count, map_numeric_id, map_id, map_name, computers_target, computers_charged) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, session.id);
            ps.setString(2, session.key);
            ps.setTimestamp(3, s.startedAt);
            ps.setTimestamp(4, s.endedAt);
            ps.setInt(5, s.durationSeconds);
            ps.setInt(6, s.winner);
            ps.setInt(7, s.survivorCount);
            ps.setInt(8, s.maniacCount);
            setNullableInt(ps, 9, s.mapNumericId);
            ps.setString(10, s.mapId);
            ps.setString(11, s.mapName);
            ps.setInt(12, s.computersTarget);
            ps.setInt(13, s.computersCharged);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static void batchInsert(Connection conn, int gameId, String table,
                                    String gameIdCol, String idCol, int team,
                                    Map<Integer, Integer> map) throws SQLException {
        if (map.isEmpty()) return;
        String sql = "INSERT INTO " + table + " (" + gameIdCol + ", team, " + idCol + ", count) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Map.Entry<Integer, Integer> e : map.entrySet()) {
                ps.setInt(1, gameId);
                ps.setInt(2, team);
                ps.setInt(3, e.getKey());
                ps.setInt(4, e.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void insertHackSettingsIfCustom(Connection conn, int gameId, String table,
                                                   String gameIdCol, HackSettingsSnapshot s) throws SQLException {
        if (!s.custom()) return;
        String sql = "INSERT INTO " + table + " (" + gameIdCol + ", hack_points_required, points_per_player_per_second, " +
                "points_per_specialist_per_second, max_bonus_players, hacker_radius, support_radius, qte_interval_min_seconds, " +
                "qte_interval_max_seconds, qte_success_bonus, qte_crit_bonus, computers_needed_for_win) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, gameId);
            ps.setFloat(2, s.hackPointsRequired);
            ps.setFloat(3, s.pointsPerPlayer);
            ps.setFloat(4, s.pointsPerSpecialist);
            ps.setInt(5, s.maxBonusPlayers);
            ps.setDouble(6, s.hackerRadius);
            ps.setDouble(7, s.supportRadius);
            ps.setInt(8, s.qteIntervalMin);
            ps.setInt(9, s.qteIntervalMax);
            ps.setFloat(10, s.qteSuccessBonus);
            ps.setFloat(11, s.qteCritBonus);
            ps.setInt(12, s.computersNeededForWin);
            ps.executeUpdate();
        }
    }

    private static void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    private static synchronized SessionRef ensureSession(Connection conn, String source) throws SQLException {
        if (activeSessionId > 0 && activeSessionKey != null) {
            return new SessionRef(activeSessionId, activeSessionKey);
        }

        SQLException lastError = null;
        for (int attempt = 0; attempt < 12; attempt++) {
            String key = generateSessionKey();
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO game_sessions (session_key, source) VALUES (?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, key);
                ps.setString(2, source);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    activeSessionId = rs.getInt(1);
                    activeSessionKey = key;
                    LOGGER.info("[Stats] Created game session {} ({})", activeSessionKey, source);
                    return new SessionRef(activeSessionId, activeSessionKey);
                }
            } catch (SQLException e) {
                lastError = e;
                if (!isDuplicateKeyError(e)) {
                    throw e;
                }
            }
        }
        throw new SQLException("Could not generate unique session_key", lastError);
    }

    private static void ensureSessionAsync(String source, MinecraftServer server) {
        if (!driverReady || (activeSessionId > 0 && activeSessionKey != null)) return;
        Thread t = new Thread(() -> {
            try (Connection conn = openConnection()) {
                SessionRef session = ensureSession(conn, source);
                if (server != null) {
                    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                        upsertSessionPlayer(conn, session, player);
                    }
                }
            } catch (SQLException e) {
                LOGGER.warn("[Stats] Failed to create game session: {}", e.getMessage());
            }
        }, "ManiacrevStats-Session");
        t.setDaemon(true);
        t.start();
    }

    private static void upsertSessionPlayer(Connection conn, SessionRef session, ServerPlayer player) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO game_session_players (session_id, session_key, player_uuid, player_name, last_seen_at) VALUES (?,?,?,?,current_timestamp()) " +
                        "ON DUPLICATE KEY UPDATE session_key = VALUES(session_key), player_name = VALUES(player_name), last_seen_at = current_timestamp()")) {
            ps.setInt(1, session.id);
            ps.setString(2, session.key);
            ps.setString(3, player.getUUID().toString());
            ps.setString(4, player.getGameProfile().getName());
            ps.executeUpdate();
        }
    }

    private static String generateSessionKey() {
        StringBuilder key = new StringBuilder(SESSION_KEY_LENGTH);
        for (int i = 0; i < SESSION_KEY_LENGTH; i++) {
            key.append(SESSION_KEY_ALPHABET.charAt(SESSION_KEY_RANDOM.nextInt(SESSION_KEY_ALPHABET.length())));
        }
        return key.toString();
    }

    private static boolean isDuplicateKeyError(SQLException e) {
        return "23000".equals(e.getSQLState()) || e.getErrorCode() == 1062;
    }

    private static boolean isActiveMatchParticipant(ServerPlayer player) {
        synchronized (MATCH_LOCK) {
            if (activeMatchStartMillis < 0L) return false;
            return activeMatchParticipants.contains(player.getUUID()) || isTrackedTeam(player);
        }
    }

    private static boolean isTrackedTeam(ServerPlayer player) {
        String team = player.getTeam() != null ? player.getTeam().getName() : null;
        return "survivors".equalsIgnoreCase(team) || "maniac".equalsIgnoreCase(team);
    }

    private static boolean isActiveSurvivor(ServerPlayer player) {
        if (player.isSpectator() || !player.isAlive()) return false;
        DownedData downed = DownedCapability.get(player);
        return downed == null || downed.getState() != DownedState.DOWNED;
    }

    private static MatchState readMatchState() {
        synchronized (MATCH_LOCK) {
            return new MatchState(activeMatchStartMillis, activeMatchStartedAt, activeMatchInvalidated);
        }
    }

    private static void clearActiveMatch() {
        synchronized (MATCH_LOCK) {
            activeMatchStartMillis = -1L;
            activeMatchStartedAt = null;
            activeMatchInvalidated = false;
            activeMatchParticipants.clear();
        }
    }

    private record GameSnapshot(
            int winner, int survivorCount, int maniacCount,
            Timestamp startedAt, Timestamp endedAt, int durationSeconds,
            boolean lastSurvivorStanding, boolean lastSurvivorStandingWon,
            Integer mapNumericId, String mapId, String mapName,
            int computersTarget, int computersCharged, HackSettingsSnapshot hackSettings,
            Map<Integer, Integer> survivorClasses, Map<Integer, Integer> maniacClasses,
            Map<Integer, Integer> survivorPerks, Map<Integer, Integer> maniacPerks,
            String skipReason
    ) {
        static GameSnapshot skipped(String reason) {
            return new GameSnapshot(
                    -1, 0, 0, null, null, 0, false, false,
                    null, null, null, 0, 0, HackSettingsSnapshot.defaults(),
                    Map.of(), Map.of(), Map.of(), Map.of(), reason
            );
        }
    }

    private record MatchState(long startMillis, Timestamp startedAt, boolean invalidated) {
    }

    private record SessionRef(int id, String key) {
    }

    private record MapSnapshot(Integer numericId, String id, String name) {
    }

    private record HackSettingsSnapshot(
            float hackPointsRequired,
            float pointsPerPlayer,
            float pointsPerSpecialist,
            int maxBonusPlayers,
            double hackerRadius,
            double supportRadius,
            int qteIntervalMin,
            int qteIntervalMax,
            float qteSuccessBonus,
            float qteCritBonus,
            int computersNeededForWin
    ) {
        static HackSettingsSnapshot defaults() {
            return new HackSettingsSnapshot(
                    GameSettings.DEFAULT_HACK_POINTS_REQUIRED,
                    GameSettings.DEFAULT_POINTS_PER_PLAYER,
                    GameSettings.DEFAULT_POINTS_PER_SPECIALIST,
                    GameSettings.DEFAULT_MAX_BONUS_PLAYERS,
                    GameSettings.DEFAULT_HACKER_RADIUS,
                    GameSettings.DEFAULT_SUPPORT_RADIUS,
                    GameSettings.DEFAULT_QTE_INTERVAL_MIN,
                    GameSettings.DEFAULT_QTE_INTERVAL_MAX,
                    GameSettings.DEFAULT_QTE_SUCCESS_BONUS,
                    GameSettings.DEFAULT_QTE_CRIT_BONUS,
                    GameSettings.DEFAULT_COMPUTERS_NEEDED
            );
        }

        boolean custom() {
            HackSettingsSnapshot d = defaults();
            return !same(hackPointsRequired, d.hackPointsRequired)
                    || !same(pointsPerPlayer, d.pointsPerPlayer)
                    || !same(pointsPerSpecialist, d.pointsPerSpecialist)
                    || maxBonusPlayers != d.maxBonusPlayers
                    || !same(hackerRadius, d.hackerRadius)
                    || !same(supportRadius, d.supportRadius)
                    || qteIntervalMin != d.qteIntervalMin
                    || qteIntervalMax != d.qteIntervalMax
                    || !same(qteSuccessBonus, d.qteSuccessBonus)
                    || !same(qteCritBonus, d.qteCritBonus)
                    || computersNeededForWin != d.computersNeededForWin;
        }

        private static boolean same(double a, double b) {
            return Math.abs(a - b) < 0.0001D;
        }
    }
}
