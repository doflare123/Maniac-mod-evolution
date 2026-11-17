package org.example.maniacrevolution.atmosphere.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.settings.ClientGameSettings;
import org.example.maniacrevolution.settings.GraphicsMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;

import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/** Asynchronous render-section GPU timing plus actual start-to-start frame pacing. No Veil types. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class VeilOverheadProbe {
    private static final String VARIANT = System.getProperty("maniacrev.veilOverhead", "");
    private static final String RUN = System.getProperty("maniacrev.veilOverheadRun", VARIANT);
    private static final Vec3[] POINTS = {new Vec3(-244.5, 41, -22.92), new Vec3(-217.5, 41, -8.5),
            new Vec3(-217.5, 41, -8.5), new Vec3(-494.5, 44, 18.5)};
    private static final float[] YAWS = {90, 0, 180, 180};
    private record Pending(int query, int view, double cpu, double interval) {}
    private static final ArrayDeque<Pending> pending = new ArrayDeque<>();
    private static final ArrayDeque<Integer> free = new ArrayDeque<>();
    private static final ArrayList<Double> gpu = new ArrayList<>(), cpu = new ArrayList<>(), intervals = new ArrayList<>();
    private static final StringBuilder report = new StringBuilder("variant,run,view,x,y,z,yaw,width,height,gui_visible,frames,gpu_median_ms,gpu_p95_ms,cpu_median_ms,cpu_p95_ms,interval_median_ms,interval_p95_ms,fps\n");
    private static boolean loading, initialized, done;
    private static int view = -1, ticks, activeQuery;
    private static long started, frameStart, lastStart, measureStart;
    private static double frameInterval;

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (VARIANT.isEmpty() || done || event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        try {
            ClientGameSettings.setGraphicsMode(GraphicsMode.OFF);
            if (!loading) {
                if (mc.screen == null || mc.getOverlay() != null) return;
                if (!Files.isRegularFile(mc.gameDirectory.toPath().resolve("saves/perf-map/level.dat"))) throw new AssertionError("Missing test save copy");
                if (ModList.get().isLoaded("veil") != VARIANT.equals("with")) throw new AssertionError("Wrong Veil mod presence for " + VARIANT);
                boolean classPresent;
                try { Class.forName("foundry.veil.Veil", false, VeilOverheadProbe.class.getClassLoader()); classPresent = true; }
                catch (ClassNotFoundException expected) { classPresent = false; }
                if (classPresent != VARIANT.equals("with")) throw new AssertionError("Wrong Veil class presence for " + VARIANT);
                Maniacrev.LOGGER.info("Veil overhead identity: variant={}, modLoaded={}, classPresent={}, renderer={}",
                        VARIANT, ModList.get().isLoaded("veil"), classPresent, GL11.glGetString(GL11.GL_RENDERER));
                loading = true; started = System.nanoTime();
                mc.options.pauseOnLostFocus = false;
                mc.options.enableVsync().set(false); mc.options.framerateLimit().set(260);
                mc.options.renderDistance().set(8); mc.options.simulationDistance().set(8);
                mc.options.hideGui = false;
                mc.createWorldOpenFlows().loadLevel(mc.screen, "perf-map");
                return;
            }
            if (System.nanoTime() - started > TimeUnit.MINUTES.toNanos(5)) throw new AssertionError("Veil overhead probe timed out");
            if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null || mc.player.tickCount < 60) return;
            if (mc.screen != null) mc.setScreen(null);
            if (!initialized) {
                initialized = true;
                mc.getWindow().setWidth(3840); mc.getWindow().setHeight(2160); mc.resizeDisplay();
                mc.getWindow().updateVsync(false);
                org.lwjgl.glfw.GLFW.glfwSwapInterval(0);
                if (VARIANT.equals("with")) {
                    var renderer = Class.forName("foundry.veil.api.client.render.VeilRenderSystem").getMethod("renderer").invoke(null);
                    var manager = renderer.getClass().getMethod("getPostProcessingManager").invoke(renderer);
                    var active = (java.util.List<?>) manager.getClass().getMethod("getActivePipelines").invoke(manager);
                    if (!active.isEmpty()) throw new AssertionError("Effects still active in OFF: " + active);
                    Maniacrev.LOGGER.info("Veil overhead OFF verified: active pipelines={}", active.size());
                }
                Maniacrev.LOGGER.info("Veil overhead settings: vsync={}, fpsLimit={}, active={}, renderDistance={}, resolution={}x{}", mc.options.enableVsync().get(),
                        mc.options.framerateLimit().get(), mc.isWindowActive(), mc.options.renderDistance().get(), mc.getMainRenderTarget().width, mc.getMainRenderTarget().height);
                for (int i = 0; i < 32; i++) free.add(GL15.glGenQueries());
                mc.getSingleplayerServer().submit(() -> {
                    var server = mc.getSingleplayerServer(); var level = server.overworld();
                    level.setDayTime(18000); level.setWeatherParameters(100000, 0, false, false);
                    level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                    level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
                    level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                    level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, server);
                }).get(10, TimeUnit.SECONDS);
            }
            if (mc.isPaused() || ++ticks < 60) return;
            if (view >= 0 && (measureStart == 0 || System.nanoTime() - measureStart < TimeUnit.SECONDS.toNanos(4))) return;
            drain();
            if (view >= 0) {
                if (gpu.size() < 80) throw new AssertionError("Insufficient timing samples: " + gpu.size());
                gpu.sort(Double::compareTo); cpu.sort(Double::compareTo); intervals.sort(Double::compareTo);
                Vec3 p = mc.player.position();
                report.append(String.format(Locale.ROOT, "%s,%s,%d,%.2f,%.2f,%.2f,%.1f,%d,%d,%s,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.2f%n", VARIANT, RUN, view,
                        p.x, p.y, p.z, mc.player.getYRot(), mc.getMainRenderTarget().width, mc.getMainRenderTarget().height, !mc.options.hideGui, gpu.size(),
                        percentile(gpu, .5), percentile(gpu, .95), percentile(cpu, .5), percentile(cpu, .95), percentile(intervals, .5), percentile(intervals, .95), 1000 / percentile(intervals, .5)));
                var output = mc.gameDirectory.toPath().resolve("benchmark-" + RUN); Files.createDirectories(output);
                Files.writeString(output.resolve("frames.csv"), report);
                try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(output.resolve("view-" + view + ".png")); }
                Maniacrev.LOGGER.info("Veil overhead view {}: variant={}, GPU={} ms, interval={} ms, FPS={}", view, VARIANT,
                        percentile(gpu, .5), percentile(intervals, .5), 1000 / percentile(intervals, .5));
            }
            gpu.clear(); cpu.clear(); intervals.clear(); ticks = 0; measureStart = lastStart = 0;
            if (++view == POINTS.length) {
                done = true;
                while (!pending.isEmpty()) GL15.glDeleteQueries(pending.remove().query);
                while (!free.isEmpty()) GL15.glDeleteQueries(free.remove());
                Maniacrev.LOGGER.info("Veil overhead probe PASSED: {} {}", VARIANT, RUN); mc.stop(); return;
            }
            Vec3 p = POINTS[view]; float yaw = YAWS[view];
            mc.getSingleplayerServer().submit(() -> {
                var player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                player.setGameMode(GameType.SURVIVAL); player.setNoGravity(true); player.setInvulnerable(true);
                player.getAbilities().mayfly = true; player.getAbilities().flying = true; player.onUpdateAbilities();
                player.teleportTo(player.serverLevel(), p.x, p.y, p.z, yaw, 0);
            }).get(10, TimeUnit.SECONDS);
        } catch (Exception | AssertionError error) {
            done = true; Maniacrev.LOGGER.error("Veil overhead probe FAILED", error); mc.stop();
        }
    }

    private static double percentile(ArrayList<Double> values, double percentile) { return values.get((int) ((values.size() - 1) * percentile)); }

    private static void drain() {
        while (!pending.isEmpty() && GL15.glGetQueryObjecti(pending.peek().query, GL15.GL_QUERY_RESULT_AVAILABLE) != 0) {
            var sample = pending.remove();
            double elapsed = GL33.glGetQueryObjecti64(sample.query, GL15.GL_QUERY_RESULT) / 1_000_000.0;
            if (sample.view == view) { gpu.add(elapsed); cpu.add(sample.cpu); if (sample.interval > 0) intervals.add(sample.interval); }
            free.add(sample.query);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void before(TickEvent.RenderTickEvent event) {
        if (VARIANT.isEmpty() || done || event.phase != TickEvent.Phase.START || view < 0 || ticks < 60) return;
        drain();
        if (free.isEmpty()) return;
        frameStart = System.nanoTime();
        if (measureStart == 0) measureStart = frameStart;
        frameInterval = lastStart == 0 ? 0 : (frameStart - lastStart) / 1_000_000.0; lastStart = frameStart;
        activeQuery = free.remove(); GL15.glBeginQuery(GL33.GL_TIME_ELAPSED, activeQuery);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void after(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || activeQuery == 0) return;
        GL15.glEndQuery(GL33.GL_TIME_ELAPSED);
        pending.add(new Pending(activeQuery, view, (System.nanoTime() - frameStart) / 1_000_000.0, frameInterval)); activeQuery = 0;
    }
}
