package org.example.maniacrevolution.atmosphere.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.post.PostPipeline;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.data.ClientGameState;
import org.example.maniacrevolution.settings.GraphicsMode;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** Local, reversible benchmark. No server upload, gameplay packet, or persistent graphics override. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class AtmosphereBenchmark {
    private enum State { PREPARING, WARMUP, MEASURING, DRAINING, RESTORING, WRITING }
    private record ServerState(MinecraftServer server, ServerLevel level, UUID player, Vec3 position, float yaw, float pitch,
                               GameType gameType, boolean mayfly, boolean flying, boolean invulnerable, boolean instabuild,
                               boolean mayBuild, boolean noGravity, boolean daylight, boolean weather, boolean spawning, int randomTicks) {}
    private record Pending(int query, BenchmarkReport.Frame frame) {}
    private static final class Session {
        State state = State.PREPARING;
        final ServerState server;
        final List<BenchmarkPlan.Case> plan;
        final List<BenchmarkReport.Result> results = new ArrayList<>();
        final List<String> warnings = new ArrayList<>();
        final Map<String, Object> report = new LinkedHashMap<>();
        final Map<String, Object> originalSettings;
        final Path output;
        final boolean vsync, hiddenGui, pauseOnFocus, bob, fullscreen, fullscreenOption, originalBypass;
        final int limit, nativeWidth, nativeHeight, windowX, windowY;
        final double fovEffects;
        final ChatVisiblity chatVisibility;
        final CameraType cameraType;
        final long warmupNanos, sampleNanos;
        final ArrayDeque<Integer> freeQueries = new ArrayDeque<>();
        final ArrayDeque<Pending> pending = new ArrayDeque<>();
        final boolean gpuSupported;
        BenchmarkReport.Result result;
        BenchmarkPlan.Case current;
        GraphicsMode override;
        int index = -1, activeQuery;
        long changedAt, measuredAt, lastStart, frameStart, caseSkipped, gcCount, gcTime;
        double frameInterval;
        BenchmarkReport.Result activeResult;
        boolean resizing, restored, expectedFullscreen;
        volatile boolean serverStateRestored;
        String status = "complete", reason = "";

        Session(ServerState server, List<BenchmarkPlan.Case> plan, boolean smoke) {
            var mc = Minecraft.getInstance(); var o = mc.options; var window = mc.getWindow();
            this.server = server; this.plan = plan;
            originalSettings = BenchmarkReport.settings();
            vsync = o.enableVsync().get(); limit = o.framerateLimit().get(); hiddenGui = o.hideGui;
            pauseOnFocus = o.pauseOnLostFocus; bob = o.bobView().get(); fovEffects = o.fovEffectScale().get();
            chatVisibility = o.chatVisibility().get();
            cameraType = o.getCameraType();
            fullscreen = window.isFullscreen(); fullscreenOption = o.fullscreen().get(); originalBypass = VeilRenderControl.isBypassed();
            nativeWidth = window.getScreenWidth(); nativeHeight = window.getScreenHeight();
            int[] x = new int[1], y = new int[1]; GLFW.glfwGetWindowPos(window.getWindow(), x, y); windowX = x[0]; windowY = y[0];
            warmupNanos = TimeUnit.MILLISECONDS.toNanos(smoke ? 350 : 3000);
            sampleNanos = TimeUnit.MILLISECONDS.toNanos(smoke ? 700 : 4000);
            gpuSupported = GL.getCapabilities().OpenGL33 || GL.getCapabilities().GL_ARB_timer_query;
            String id = Instant.now().toString().replace(':', '-') + "-" + UUID.randomUUID().toString().substring(0, 8);
            output = mc.gameDirectory.toPath().resolve("benchmark-results/mrbench-" + id + ".zip").toAbsolutePath();
            report.put("schema_version", 1); report.put("suite_version", "mrbench-1"); report.put("started_at", Instant.now().toString());
            report.put("smoke_test", smoke);
            report.put("environment", BenchmarkReport.environment()); report.put("original_settings", originalSettings);
            report.put("plan", plan); report.put("warmup_seconds", warmupNanos / 1e9); report.put("sample_seconds", sampleNanos / 1e9);
            report.put("notes", List.of("VEIL_RENDER_BYPASS is not an unloaded Veil library; resources, other mixins and some callbacks remain.",
                    "GPU timings cover the RenderTick render section; frame intervals also include presentation and frame limiting.",
                    "FPS = 1000 / mean frame interval. onePercentLowFps = 1000 / p99 frame interval.",
                    "Two repeats reverse mode order. Dynamic clocks, short samples and frame caps can obscure small differences."));
            if (!gpuSupported) warnings.add("gpu_timer_unsupported_cpu_and_frame_intervals_only");
        }
        void warn(String value) { if (!warnings.contains(value)) warnings.add(value); }
    }

    private static volatile Session session;
    private static Path lastReport;
    private AtmosphereBenchmark() {}

    public static boolean isRunning() { return session != null; }
    static GraphicsMode graphicsOverride() { return session == null ? null : session.override; }
    static boolean forcingProfile() { return session != null && session.override != null; }
    public static Path lastReport() { return lastReport; }

    static void configureUniforms(PostPipeline pipeline) {
        if (!forcingProfile()) return;
        pipeline.setFloat("Time", 12); pipeline.setFloat("Corruption", 0); pipeline.setFloat("LightCorruption", 0);
        pipeline.setFloat("Comfort", 1); pipeline.setFloat("Pursuit", 0); pipeline.setFloat("Hurt", 0);
        pipeline.setFloat("Fear", 0); pipeline.setFloat("Plague", 0);
        pipeline.setFloat("FogGround", (float) session.current.point().y());
    }

    private static int message(String text) {
        var player = Minecraft.getInstance().player;
        if (player != null) player.displayClientMessage(Component.literal("[MRBench] " + text), false);
        return 1;
    }

    @SubscribeEvent
    public static void commands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mrbench")
                .executes(c -> message("start — около 5 минут; start matrix — несколько разрешений; stop; status; points; veil off|on."))
                .then(Commands.literal("start").executes(c -> start(false, false))
                        .then(Commands.literal("quick").executes(c -> start(false, false)))
                        .then(Commands.literal("matrix").executes(c -> start(true, false))))
                .then(Commands.literal("stop").executes(c -> {
                    if (session == null) return message("Проверка не запущена.");
                    finish("cancelled", "user_cancelled"); return message("Остановка: восстанавливаю настройки и сохраняю собранные данные.");
                }))
                .then(Commands.literal("status").executes(c -> message(session == null ? "Проверка не запущена."
                        : session.state + ", этап " + (session.index + 1) + "/" + session.plan.size())))
                .then(Commands.literal("veil")
                        .then(Commands.literal("off").executes(c -> manualBypass(true)))
                        .then(Commands.literal("on").executes(c -> manualBypass(false))))
                .then(Commands.literal("points")
                        .executes(c -> listPoints())
                        .then(Commands.literal("add").then(Commands.argument("name", StringArgumentType.word()).executes(c -> addPoint(StringArgumentType.getString(c, "name")))))
                        .then(Commands.literal("clear").executes(c -> savePointsCommand(List.of())))
                        .then(Commands.literal("reset").executes(c -> savePointsCommand(BenchmarkPlan.defaults())))));
    }

    private static int manualBypass(boolean value) {
        var mc = Minecraft.getInstance();
        if (session != null) return message("Сначала остановите проверку.");
        if (value && (mc.getSingleplayerServer() == null || ClientGameState.isGameRunning())) return message("Обход доступен в одиночном мире вне матча.");
        try { VeilRenderControl.setBypassed(value); }
        catch (RuntimeException error) { return message(error.getMessage()); }
        return message(value ? "Рендер Veil обойдён. Библиотека остаётся загруженной. Вернуть: /mrbench veil on."
                : "Обычный рендер Veil восстановлен.");
    }

    private static Path pointsFile() { return Minecraft.getInstance().gameDirectory.toPath().resolve("config/maniacrev-benchmark-points.json"); }
    private static List<BenchmarkPlan.Point> points() throws java.io.IOException {
        Path file = pointsFile();
        if (!Files.exists(file)) return BenchmarkPlan.defaults();
        var decoded = BenchmarkReport.JSON.fromJson(Files.readString(file), BenchmarkPlan.Point[].class);
        if (decoded == null || decoded.length > 32) throw new IllegalArgumentException("В маршруте должно быть не больше 32 точек.");
        return List.of(decoded);
    }
    private static void savePoints(List<BenchmarkPlan.Point> points) throws java.io.IOException {
        if (session != null) throw new IllegalStateException("Маршрут нельзя менять во время проверки.");
        Files.createDirectories(pointsFile().getParent()); Files.writeString(pointsFile(), BenchmarkReport.JSON.toJson(points));
    }
    private static int savePointsCommand(List<BenchmarkPlan.Point> points) {
        try { savePoints(points); return message("Маршрут сохранён: " + points.size() + " точек. config/maniacrev-benchmark-points.json"); }
        catch (Exception error) { return message("Не удалось сохранить маршрут: " + error.getMessage()); }
    }
    private static int addPoint(String name) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return message("Сначала откройте мир.");
        try {
            List<BenchmarkPlan.Point> points = new ArrayList<>(points()); points.removeIf(point -> point.name().equals(name));
            if (points.size() >= 32) return message("В маршруте уже 32 точки.");
            var p = mc.player.position();
            points.add(new BenchmarkPlan.Point(name, mc.level.dimension().location().toString(), p.x, p.y, p.z, mc.player.getYRot(), mc.player.getXRot()));
            return savePointsCommand(points);
        } catch (Exception error) { return message("Ошибка маршрута: " + error.getMessage()); }
    }
    private static int listPoints() {
        try { for (var point : points()) message(point.name() + ": " + point.dimension() + " " + point.x() + " / " + point.y() + " / " + point.z() + ", yaw=" + point.yaw()); }
        catch (Exception error) { return message("Ошибка маршрута: " + error.getMessage()); }
        return 1;
    }

    /** Smoke is available only to the automated render harness through a JVM opt-in. */
    public static int startSmoke(boolean matrix) {
        if (!Boolean.getBoolean("maniacrev.benchmark.smoke")) throw new IllegalStateException("Smoke harness is disabled");
        return start(matrix, true);
    }

    private static int start(boolean matrix, boolean smoke) {
        var mc = Minecraft.getInstance();
        if (session != null) return message("Проверка уже запущена.");
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return message("Откройте копию карты в одиночном мире. Проверка не запускается на чужом сервере.");
        if (ClientGameState.isGameRunning()) return message("Проверка доступна вне матча.");
        try {
            String unsupported = VeilRenderControl.unsupportedReason();
            if (unsupported != null) return message(unsupported);
            List<BenchmarkPlan.Point> route = points();
            if (route.isEmpty()) return message("Маршрут пуст: /mrbench points reset или /mrbench points add имя.");
            if (smoke) route = List.of(route.get(0));
            String dimension = mc.level.dimension().location().toString();
            if (route.stream().anyMatch(point -> !point.dimension().equals(dimension))) return message("Все точки маршрута должны находиться в текущем измерении.");
            var manager = VeilRenderSystem.renderer().getPostProcessingManager();
            for (var id : List.of(AtmosphereClient.PIPELINE, AtmosphereClient.LIGHT_PIPELINE))
                if (manager.getPipeline(id) == null || !manager.getPipeline(id).hasUniform("FogDensity")) throw new IllegalStateException("Шейдер атмосферы не загрузился: " + id);
            var resolutions = BenchmarkPlan.resolutions(mc.getMainRenderTarget().width, mc.getMainRenderTarget().height, matrix);
            var plan = BenchmarkPlan.create(route, resolutions, smoke ? 1 : 2);
            Path outputDirectory = mc.gameDirectory.toPath().resolve("benchmark-results");
            Files.createDirectories(outputDirectory);
            Path writeProbe = Files.createTempFile(outputDirectory, ".mrbench-write-test-", ".tmp");
            Files.delete(writeProbe);
            var server = mc.getSingleplayerServer(); var playerId = mc.player.getUUID();
            ServerState saved = server.submit(() -> {
                var player = server.getPlayerList().getPlayer(playerId); var level = player.serverLevel(); var a = player.getAbilities(); var rules = level.getGameRules();
                return new ServerState(server, level, playerId, player.position(), player.getYRot(), player.getXRot(), player.gameMode.getGameModeForPlayer(),
                        a.mayfly, a.flying, a.invulnerable, a.instabuild, a.mayBuild, player.isNoGravity(),
                        rules.getBoolean(GameRules.RULE_DAYLIGHT), rules.getBoolean(GameRules.RULE_WEATHER_CYCLE),
                        rules.getBoolean(GameRules.RULE_DOMOBSPAWNING), rules.getInt(GameRules.RULE_RANDOMTICKING));
            }).get(5, TimeUnit.SECONDS);
            session = new Session(saved, plan, smoke);
            VeilRenderControl.setBypassed(false);
            mc.options.enableVsync().set(false); mc.getWindow().updateVsync(false); mc.options.framerateLimit().set(260);
            mc.options.pauseOnLostFocus = false; mc.options.hideGui = false; mc.options.bobView().set(false); mc.options.fovEffectScale().set(0.0);
            mc.options.chatVisibility().set(ChatVisiblity.HIDDEN);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            KeyMapping.releaseAll();
            server.submit(() -> {
                var rules = saved.level.getGameRules();
                rules.getRule(GameRules.RULE_DAYLIGHT).set(false, server); rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
                rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server); rules.getRule(GameRules.RULE_RANDOMTICKING).set(0, server);
                var player = server.getPlayerList().getPlayer(playerId); player.setGameMode(GameType.SURVIVAL);
                player.setNoGravity(true); player.getAbilities().mayfly = true; player.getAbilities().flying = true;
                player.getAbilities().invulnerable = true; player.onUpdateAbilities();
            }).get(5, TimeUnit.SECONDS);
            if (session.gpuSupported) for (int i = 0; i < 32; i++) session.freeQueries.add(GL15.glGenQueries());
            session.report.put("test_settings", BenchmarkReport.settings());
            session.report.put("day_time", mc.level.getDayTime()); session.report.put("rain_level", mc.level.getRainLevel(1));
            double seconds = plan.size() * (session.warmupNanos + session.sampleNanos) / 1e9;
            message("Начинаю: " + plan.size() + " этапов, примерно " + Math.max(1, (int) Math.ceil(seconds / 60)) + " мин. Не двигайте мышь и не сворачивайте игру. Esc или /mrbench stop — остановить.");
            nextCase();
            return 1;
        } catch (Exception error) {
            Maniacrev.LOGGER.error("Cannot start benchmark", error);
            if (session != null) finish("failed", error.getClass().getSimpleName() + ": " + error.getMessage());
            return message("Не удалось начать проверку: " + error.getMessage());
        }
    }

    private static void nextCase() throws Exception {
        Session s = session;
        if (++s.index == s.plan.size()) { finish("complete", ""); return; }
        s.current = s.plan.get(s.index); s.result = null; s.state = State.PREPARING; s.changedAt = System.nanoTime(); s.lastStart = 0;
        // Release Veil bindings while its methods are still enabled, then select the next mode.
        VeilRenderControl.setBypassed(false);
        s.override = s.current.mode().graphics;
        VeilRenderControl.setBypassed(s.current.mode() == BenchmarkPlan.Mode.VEIL_RENDER_BYPASS);
        var mc = Minecraft.getInstance(); var target = s.current.resolution();
        if (mc.getMainRenderTarget().width != target.width() || mc.getMainRenderTarget().height != target.height()) {
            s.resizing = true;
            if (mc.getWindow().isFullscreen()) mc.getWindow().toggleFullScreen();
            // Convert physical framebuffer pixels to logical window units on HiDPI displays.
            double scaleX = (double) mc.getWindow().getWidth() / Math.max(1, mc.getWindow().getScreenWidth());
            double scaleY = (double) mc.getWindow().getHeight() / Math.max(1, mc.getWindow().getScreenHeight());
            mc.getWindow().setWindowed((int) Math.round(target.width() / scaleX), (int) Math.round(target.height() / scaleY));
        }
        s.expectedFullscreen = mc.getWindow().isFullscreen();
        var point = s.current.point();
        s.server.server.submit(() -> {
            var player = s.server.server.getPlayerList().getPlayer(s.server.player);
            player.setDeltaMovement(Vec3.ZERO);
            player.teleportTo(s.server.level, point.x(), point.y(), point.z(), point.yaw(), point.pitch());
        }).get(5, TimeUnit.SECONDS);
        if (mc.player != null) mc.player.displayClientMessage(Component.literal("[MRBench] " + (s.index + 1) + "/" + s.plan.size()
                + ": " + point.name() + ", " + target.width() + "×" + target.height() + ", " + s.current.mode()), true);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        if (session == null) {
            if (VeilRenderControl.isBypassed() && (mc.level == null || ClientGameState.isGameRunning())) VeilRenderControl.setBypassed(false);
            return;
        }
        Session s = session;
        if (s.state == State.WRITING) return;
        try {
            drain(s);
            long now = System.nanoTime();
            if (s.state == State.DRAINING) {
                if (s.pending.isEmpty() || now - s.changedAt > TimeUnit.SECONDS.toNanos(2)) restore(s);
                return;
            }
            if (s.state == State.RESTORING) {
                if (now - s.changedAt > TimeUnit.MILLISECONDS.toNanos(400)) save(s);
                return;
            }
            if (mc.level == null || mc.player == null || ClientGameState.isGameRunning()) { finish("cancelled", "world_changed_or_match_started"); return; }
            if ((mc.screen != null || mc.getOverlay() != null || !mc.isWindowActive()) && now - s.changedAt > TimeUnit.SECONDS.toNanos(1)) {
                finish("cancelled", "screen_overlay_or_focus_changed"); return;
            }
            var point = s.current.point();
            if ((s.state == State.WARMUP || s.state == State.MEASURING) && mc.getWindow().isFullscreen() != s.expectedFullscreen) {
                s.resizing = true; finish("cancelled", "fullscreen_changed_during_test"); return;
            }
            if (s.state == State.MEASURING && (mc.getMainRenderTarget().width != s.result.actualWidth || mc.getMainRenderTarget().height != s.result.actualHeight)) {
                s.resizing = true; finish("cancelled", "resolution_changed_during_measurement"); return;
            }
            if (mc.options.getCameraType() != CameraType.FIRST_PERSON) { finish("cancelled", "camera_type_changed"); return; }
            if ((s.state == State.WARMUP || s.state == State.MEASURING)
                    && mc.player.position().distanceToSqr(point.x(), point.y(), point.z()) > .25) {
                finish("cancelled", "player_moved"); return;
            }
            if (s.state == State.PREPARING) {
                if (!mc.level.hasChunkAt(net.minecraft.core.BlockPos.containing(point.x(), point.y(), point.z()))
                        || mc.player.position().distanceToSqr(point.x(), point.y(), point.z()) > .25) {
                    if (now - s.changedAt > TimeUnit.SECONDS.toNanos(30)) finish("failed", "teleport_or_chunks_timeout");
                    return;
                }
                if (now - s.changedAt < TimeUnit.MILLISECONDS.toNanos(500)) return;
                s.state = State.WARMUP; s.changedAt = now;
            } else if (s.state == State.WARMUP && now - s.changedAt >= s.warmupNanos) {
                if (s.current.mode().graphics.enabled() && AtmosphereClient.profile() == org.example.maniacrevolution.atmosphere.AtmosphereRules.Profile.OFF)
                    throw new IllegalStateException("Atmosphere inactive for requested mode (check blindness, fluid or camera)");
                s.result = new BenchmarkReport.Result(s.current); s.results.add(s.result);
                s.caseSkipped = VeilRenderControl.skippedCalls(); s.gcCount = BenchmarkReport.gcCount(); s.gcTime = BenchmarkReport.gcTime();
                s.measuredAt = now; s.lastStart = 0; s.state = State.MEASURING;
            } else if (s.state == State.MEASURING && now - s.measuredAt >= s.sampleNanos) {
                s.result.skippedVeilCalls = VeilRenderControl.skippedCalls() - s.caseSkipped;
                s.result.gcCount = BenchmarkReport.gcCount() - s.gcCount; s.result.gcTimeMs = BenchmarkReport.gcTime() - s.gcTime;
                s.result.observedBypassGates = s.current.mode() == BenchmarkPlan.Mode.VEIL_RENDER_BYPASS ? VeilRenderControl.observedGates() : 0;
                s.result.measuredDurationMs = (now - s.measuredAt) / 1e6;
                if (s.current.mode() == BenchmarkPlan.Mode.VEIL_RENDER_BYPASS && (s.result.skippedVeilCalls == 0 || s.result.observedBypassGates != 15))
                    throw new IllegalStateException("Renderer bypass gates did not execute");
                s.result.completed = true;
                nextCase();
            }
        } catch (Exception error) {
            Maniacrev.LOGGER.error("Benchmark failed", error);
            finish("failed", error.getClass().getSimpleName() + ": " + error.getMessage());
        }
    }

    public static void stopForTest() { finish("cancelled", "test_cancelled"); }

    private static void finish(String status, String reason) {
        Session s = session;
        if (s == null || s.state == State.WRITING || s.state == State.RESTORING || s.state == State.DRAINING) return;
        endFrame(s);
        if (s.result != null && !s.result.completed) {
            s.result.skippedVeilCalls = VeilRenderControl.skippedCalls() - s.caseSkipped;
            s.result.gcCount = BenchmarkReport.gcCount() - s.gcCount; s.result.gcTimeMs = BenchmarkReport.gcTime() - s.gcTime;
            s.result.measuredDurationMs = (System.nanoTime() - s.measuredAt) / 1e6;
        }
        s.status = status; s.reason = reason; s.override = null;
        VeilRenderControl.setBypassed(false);
        s.state = State.DRAINING; s.changedAt = System.nanoTime();
    }

    private static void restore(Session s) {
        if (s.restored) return;
        s.restored = true; s.override = null;
        var mc = Minecraft.getInstance(); var o = mc.options;
        try {
            VeilRenderControl.setBypassed(false);
            o.enableVsync().set(s.vsync); mc.getWindow().updateVsync(s.vsync); o.framerateLimit().set(s.limit);
            o.hideGui = s.hiddenGui; o.pauseOnLostFocus = s.pauseOnFocus; o.bobView().set(s.bob); o.fovEffectScale().set(s.fovEffects);
            o.chatVisibility().set(s.chatVisibility);
            o.setCameraType(s.cameraType);
            if (s.resizing) {
                if (mc.getWindow().isFullscreen()) mc.getWindow().toggleFullScreen();
                mc.getWindow().setWindowed(s.nativeWidth, s.nativeHeight);
                GLFW.glfwSetWindowPos(mc.getWindow().getWindow(), s.windowX, s.windowY);
                if (s.fullscreen) mc.getWindow().toggleFullScreen();
                o.fullscreen().set(s.fullscreenOption);
            }
        } catch (Exception error) { s.warn("graphics_restoration_error:" + error.getClass().getSimpleName()); Maniacrev.LOGGER.error("Benchmark graphics restoration failed", error); }
        try {
            s.server.server.submit(() -> {
                if (restoreServer(s.server, s.server.server.getPlayerList().getPlayer(s.server.player))) s.serverStateRestored = true;
            }).get(5, TimeUnit.SECONDS);
            if (!s.serverStateRestored) s.warn("player_missing_during_restoration");
        } catch (Exception error) { s.warn("player_restoration_error:" + error.getClass().getSimpleName()); Maniacrev.LOGGER.error("Benchmark player restoration failed", error); }
        try {
            if (s.originalBypass && mc.level != null && !ClientGameState.isGameRunning()) VeilRenderControl.setBypassed(true);
        } catch (Exception error) { s.warn("bypass_restoration_error:" + error.getClass().getSimpleName()); }
        s.report.put("restored_settings", BenchmarkReport.settings());
        s.state = State.RESTORING; s.changedAt = System.nanoTime();
    }

    private static void save(Session s) {
        drain(s);
        while (!s.pending.isEmpty()) { GL15.glDeleteQueries(s.pending.remove().query); s.warn("gpu_query_timeout_some_samples_missing"); }
        while (!s.freeQueries.isEmpty()) GL15.glDeleteQueries(s.freeQueries.remove());
        for (var result : s.results) result.summarize();
        s.report.put("status", s.status); s.report.put("reason", s.reason); s.report.put("finished_at", Instant.now().toString());
        s.report.put("results", s.results); s.report.put("warnings", s.warnings);
        s.report.put("restored_settings", BenchmarkReport.settings()); s.state = State.WRITING;
        s.report.put("server_state_restored", s.serverStateRestored);
        Util.ioPool().execute(() -> {
            try {
                BenchmarkReport.write(s.output, s.report, s.results);
                Minecraft.getInstance().execute(() -> {
                    lastReport = s.output; session = null;
                    message((s.status.equals("complete") ? "Готово." : "Проверка остановлена: " + s.reason + ".") + " Отправьте ZIP для анализа:");
                    message(s.output.toString());
                    var player = Minecraft.getInstance().player;
                    if (player != null) player.displayClientMessage(Component.literal("[Открыть папку с результатом]").withStyle(style -> style
                            .withColor(ChatFormatting.AQUA).withUnderlined(true)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, s.output.getParent().toString()))), false);
                    Maniacrev.LOGGER.info("MRBench {}: {}", s.status, s.output);
                });
            } catch (Exception error) {
                Maniacrev.LOGGER.error("Cannot save benchmark report", error);
                Minecraft.getInstance().execute(() -> { session = null; message("Не удалось записать отчёт: " + error.getMessage()); });
            }
        });
    }

    private static void drain(Session s) {
        while (!s.pending.isEmpty() && GL15.glGetQueryObjecti(s.pending.peek().query, GL15.GL_QUERY_RESULT_AVAILABLE) != 0) {
            Pending p = s.pending.remove(); double value = GL33.glGetQueryObjecti64(p.query, GL15.GL_QUERY_RESULT) / 1_000_000.0;
            if (value > 0 && Double.isFinite(value)) p.frame.gpuMs = value;
            s.freeQueries.add(p.query);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void beforeFrame(TickEvent.RenderTickEvent event) {
        Session s = session;
        if (s == null || event.phase != TickEvent.Phase.START || s.current == null
                || s.state == State.DRAINING || s.state == State.RESTORING || s.state == State.WRITING) return;
        var mc = Minecraft.getInstance();
        if (mc.player != null) {
            var point = s.current.point(); mc.player.setDeltaMovement(Vec3.ZERO);
            mc.player.setYRot(point.yaw()); mc.player.yRotO = point.yaw(); mc.player.setXRot(point.pitch()); mc.player.xRotO = point.pitch();
        }
        if (s.state != State.MEASURING || mc.screen != null || mc.getOverlay() != null || !mc.isWindowActive()) return;
        drain(s);
        s.frameStart = System.nanoTime(); s.frameInterval = s.lastStart == 0 ? 0 : (s.frameStart - s.lastStart) / 1e6; s.lastStart = s.frameStart;
        s.activeResult = s.result;
        if (s.gpuSupported && !s.freeQueries.isEmpty()) {
            if (GL15.glGetQueryi(GL33.GL_TIME_ELAPSED, GL15.GL_CURRENT_QUERY) == 0) {
                s.activeQuery = s.freeQueries.remove(); GL15.glBeginQuery(GL33.GL_TIME_ELAPSED, s.activeQuery);
            } else s.warn("another_gpu_timer_is_active_some_gpu_samples_skipped");
        }
    }

    private static void endFrame(Session s) {
        if (s.activeResult == null) return;
        var frame = new BenchmarkReport.Frame((s.frameStart - s.measuredAt) / 1e6, s.frameInterval, (System.nanoTime() - s.frameStart) / 1e6);
        s.activeResult.frames.add(frame);
        int lights = AtmosphereLights.sourceCount(); s.activeResult.minLights = Math.min(s.activeResult.minLights, lights);
        s.activeResult.maxLights = Math.max(s.activeResult.maxLights, lights); s.activeResult.cachedGroups = AtmosphereLights.groupCount();
        if (s.activeQuery != 0) {
            GL15.glEndQuery(GL33.GL_TIME_ELAPSED); s.pending.add(new Pending(s.activeQuery, frame)); s.activeQuery = 0;
        }
        s.activeResult = null;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void afterFrame(TickEvent.RenderTickEvent event) {
        if (session != null && event.phase == TickEvent.Phase.END) endFrame(session);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        if (session != null && session.state != State.WRITING) {
            finish("cancelled", "disconnected"); restore(session); save(session);
        }
        if (VeilRenderControl.isBypassed()) VeilRenderControl.setBypassed(false);
    }

    private static boolean restoreServer(ServerState saved, ServerPlayer player) {
        var rules = saved.level.getGameRules(); var server = saved.server;
        rules.getRule(GameRules.RULE_DAYLIGHT).set(saved.daylight, server); rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(saved.weather, server);
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(saved.spawning, server); rules.getRule(GameRules.RULE_RANDOMTICKING).set(saved.randomTicks, server);
        if (player == null) return false;
        player.setGameMode(saved.gameType); player.setNoGravity(saved.noGravity);
        var a = player.getAbilities(); a.mayfly = saved.mayfly; a.flying = saved.flying; a.invulnerable = saved.invulnerable;
        a.instabuild = saved.instabuild; a.mayBuild = saved.mayBuild; player.onUpdateAbilities(); player.setDeltaMovement(Vec3.ZERO);
        player.teleportTo(saved.level, saved.position.x, saved.position.y, saved.position.z, saved.yaw, saved.pitch);
        return true;
    }

    /** Restore before PlayerList saves a departing host, even if its client logout event arrives later. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void serverLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Session s = session;
        if (s == null || s.serverStateRestored || !(event.getEntity() instanceof ServerPlayer player)
                || !player.getUUID().equals(s.server.player) || player.serverLevel().getServer() != s.server.server) return;
        try { s.serverStateRestored = restoreServer(s.server, player); }
        catch (RuntimeException error) { Maniacrev.LOGGER.error("Benchmark logout restoration failed", error); }
    }
}
