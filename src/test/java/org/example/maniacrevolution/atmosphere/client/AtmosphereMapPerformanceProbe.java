package org.example.maniacrevolution.atmosphere.client;

import foundry.veil.forge.event.ForgeVeilPostProcessingEvent;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.settings.ClientGameSettings;
import org.example.maniacrevolution.settings.GraphicsMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/** Loads only the explicitly prepared copy named perf-map; never opens the user's original save. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class AtmosphereMapPerformanceProbe {
    private static final String LABEL = System.getProperty("maniacrev.atmosphere.mapBenchmark", "");
    private static final GraphicsMode MODE = GraphicsMode.valueOf(System.getProperty("maniacrev.atmosphere.mapBenchmarkMode", "HEAVY"));
    private static final boolean LIGHT_BREAKDOWN = Boolean.getBoolean("maniacrev.atmosphere.mapLightBreakdown");
    private static final Vec3 DENSE_HALL = new Vec3(-217.5, 41, -8.5);
    private static final float DENSE_YAW = 180;
    private static final int BREAKDOWN_REPEATS = 3, BREAKDOWN_FRAMES = 80;
    private static final int WARMUP_TICKS = 40, MAX_DENSE_WARMUP_TICKS = 300;
    private record Variant(GraphicsMode mode, boolean surface, boolean shadows) {}
    private static final List<Variant> VARIANTS = List.of(
            new Variant(GraphicsMode.LIGHT, true, false), new Variant(GraphicsMode.NORMAL, true, false),
            new Variant(GraphicsMode.HEAVY, false, false), new Variant(GraphicsMode.HEAVY, true, false),
            new Variant(GraphicsMode.HEAVY, false, true), new Variant(GraphicsMode.HEAVY, true, true));
    private static boolean loading, initialized, done, measuring, denseReady;
    private static int ticks, view = -1, query;
    private static long started;
    private static Vec3 recent;
    private static final ArrayList<Double> samples = new ArrayList<>();
    private static final StringBuilder report = new StringBuilder("view,x,y,z,yaw,width,height,groups,median_ms,p95_ms\n");
    private static final StringBuilder breakdown = new StringBuilder("repeat,mode,surface_light,contact_shadows,width,height,groups,frames,median_ms,p95_ms\n");

    private static Variant variant() {
        int row = view % VARIANTS.size();
        return VARIANTS.get(view / VARIANTS.size() % 2 == 0 ? row : VARIANTS.size() - 1 - row);
    }

    private static boolean measuredPipeline(ResourceLocation name) {
        GraphicsMode mode = LIGHT_BREAKDOWN ? variant().mode() : MODE;
        return mode == GraphicsMode.LIGHT ? name.equals(AtmosphereClient.LIGHT_PIPELINE) || name.equals(AtmosphereClient.REDUCED_LIGHT_PIPELINE)
                : name.equals(AtmosphereClient.PIPELINE);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (LABEL.isEmpty() || done || event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        try {
            if (!loading) {
                if (mc.screen == null || mc.getOverlay() != null) return;
                if (!Files.exists(mc.gameDirectory.toPath().resolve("saves/perf-map/level.dat")))
                    throw new AssertionError("Prepare a copy of the map as saves/perf-map first");
                loading = true; started = System.nanoTime();
                mc.options.pauseOnLostFocus = false;
                mc.options.enableVsync().set(false);
                mc.options.framerateLimit().set(260);
                mc.createWorldOpenFlows().loadLevel(mc.screen, "perf-map");
                return;
            }
            if (System.nanoTime() - started > TimeUnit.MINUTES.toNanos(5)) throw new AssertionError("Map benchmark timed out");
            if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null || mc.player.tickCount < 40) return;
            if (mc.screen != null) mc.setScreen(null);
            if (!initialized) {
                initialized = true; recent = new Vec3(-244.5, 41, -22.92);
                mc.getSingleplayerServer().submit(() -> {
                    var level = mc.getSingleplayerServer().overworld();
                    var rules = level.getGameRules();
                    rules.getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false, mc.getSingleplayerServer());
                    rules.getRule(net.minecraft.world.level.GameRules.RULE_WEATHER_CYCLE).set(false, mc.getSingleplayerServer());
                    rules.getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false, mc.getSingleplayerServer());
                    rules.getRule(net.minecraft.world.level.GameRules.RULE_RANDOMTICKING).set(0, mc.getSingleplayerServer());
                    level.setDayTime(6000); level.setWeatherParameters(6000, 0, false, false);
                }).get(10, TimeUnit.SECONDS);
                int width = Integer.getInteger("maniacrev.atmosphere.mapBenchmarkWidth", 3840);
                mc.getWindow().setWidth(width); mc.getWindow().setHeight(width * 9 / 16); mc.resizeDisplay();
                ClientCommandHandler.runCommand("mrvisual preview survivor");
                query = GL15.glGenQueries();
                if (LIGHT_BREAKDOWN) {
                    mc.options.setCameraType(CameraType.FIRST_PERSON);
                    teleport(mc, DENSE_HALL, DENSE_YAW);
                    view = 0;
                    Maniacrev.LOGGER.info("Atmosphere map GPU: {}, {}", GL11.glGetString(GL11.GL_RENDERER), GL11.glGetString(GL11.GL_VERSION));
                }
            }
            if (LIGHT_BREAKDOWN) {
                tickBreakdown(mc);
                return;
            }
            ClientGameSettings.setGraphicsMode(MODE);
            if (mc.isPaused()) return;
            if (++ticks < 60) return;
            if (view >= 0 && samples.size() < 40) return;
            if (view >= 0) {
                if (MODE == GraphicsMode.LIGHT) verifyPipeline(AtmosphereClient.lightPipelineFor(
                        mc.getMainRenderTarget().height, AtmosphereLights.sourceCount()));
                samples.sort(Double::compareTo);
                Vec3 p = mc.player.position();
                report.append(String.format(Locale.ROOT, "%d,%.2f,%.2f,%.2f,%.1f,%d,%d,%d,%.3f,%.3f%n", view,
                        p.x, p.y, p.z, mc.player.getYRot(), mc.getMainRenderTarget().width, mc.getMainRenderTarget().height,
                        AtmosphereLights.sourceCount(), samples.get(samples.size() / 2), samples.get((samples.size() - 1) * 95 / 100)));
                var output = mc.gameDirectory.toPath().resolve("benchmark-" + LABEL);
                Files.createDirectories(output);
                try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(output.resolve("view-" + view + ".png")); }
                Files.writeString(output.resolve("gpu.csv"), report);
                Maniacrev.LOGGER.info("Map GPU benchmark view {}: groups={}, median={} ms", view,
                        AtmosphereLights.sourceCount(), samples.get(samples.size() / 2));
            }
            samples.clear(); ticks = 0;
            if (++view == 12) { finish(mc); return; }
            Vec3 position = switch (view / 4) {
                case 0 -> recent;
                case 1 -> DENSE_HALL;
                default -> new Vec3(-494.5, 44, 18.5);
            };
            teleport(mc, position, (view % 4) * 90);
        } catch (Exception | AssertionError error) {
            done = true; Maniacrev.LOGGER.error("Atmosphere map benchmark FAILED", error); mc.stop();
        }
    }

    private static void tickBreakdown(Minecraft mc) throws Exception {
        Variant current = variant();
        ClientGameSettings.setGraphicsMode(current.mode());
        if (mc.isPaused() || !denseReady && mc.player.position().distanceToSqr(DENSE_HALL) > .0001) return;
        ticks++;
        if (!denseReady) {
            if (ticks < WARMUP_TICKS || ticks < MAX_DENSE_WARMUP_TICKS
                    && AtmosphereLights.sourceCount() != AtmosphereLights.MAX_SOURCES) return;
            denseReady = true;
            if (view == 0) Maniacrev.LOGGER.info("Dense hall ready after {} ticks", ticks);
        }
        verifyDenseFrame(mc);
        if (samples.size() < BREAKDOWN_FRAMES) return;
        var framebuffer = mc.getMainRenderTarget();
        int width = Integer.getInteger("maniacrev.atmosphere.mapBenchmarkWidth", 3840);
        if (framebuffer.width != width || framebuffer.height != width * 9 / 16)
            throw new AssertionError("Unexpected benchmark resolution: " + framebuffer.width + "x" + framebuffer.height);
        if (current.mode() == GraphicsMode.LIGHT) verifyPipeline(AtmosphereClient.lightPipelineFor(framebuffer.height, AtmosphereLights.MAX_SOURCES));
        else verifyPipeline(AtmosphereClient.PIPELINE);
        var stats = BenchmarkPlan.statistics(samples);
        int repeat = view / VARIANTS.size() + 1;
        String row = String.format(Locale.ROOT, "%d,%s,%b,%b,%d,%d,%d,%d,%.3f,%.3f%n",
                repeat, current.mode(), current.surface(), current.shadows(), framebuffer.width, framebuffer.height,
                AtmosphereLights.MAX_SOURCES, stats.count(), stats.medianMs(), stats.p95Ms());
        breakdown.append(row);
        var output = mc.gameDirectory.toPath().resolve("benchmark-" + LABEL);
        Files.createDirectories(output);
        Files.writeString(output.resolve("light-breakdown.csv"), breakdown);
        Maniacrev.LOGGER.info("Map light breakdown: {}", row.strip());
        samples.clear(); ticks = 0; denseReady = false;
        if (++view == BREAKDOWN_REPEATS * VARIANTS.size()) finish(mc);
    }

    private static void verifyDenseFrame(Minecraft mc) {
        if (mc.player.position().distanceToSqr(DENSE_HALL) > .0001
                || Math.abs(Mth.wrapDegrees(mc.player.getYRot() - DENSE_YAW)) > .01 || Math.abs(mc.player.getXRot()) > .01
                || AtmosphereLights.sourceCount() != AtmosphereLights.MAX_SOURCES || AtmosphereLights.handLightCount() != 0)
            throw new AssertionError("Unexpected dense hall frame at " + mc.player.position() + ", yaw=" + mc.player.getYRot()
                    + ", pitch=" + mc.player.getXRot() + ", lights=" + AtmosphereLights.sourceCount()
                    + ", indexed groups=" + AtmosphereLights.groupCount()
                    + ", hand lights=" + AtmosphereLights.handLightCount());
    }

    private static void verifyPipeline(ResourceLocation expected) {
        var active = foundry.veil.api.client.render.VeilRenderSystem.renderer().getPostProcessingManager().getActivePipelines()
                .stream().map(entry -> entry.getPipeline()).filter(AtmosphereClient.PIPELINES::contains).toList();
        if (!active.equals(List.of(expected))) throw new AssertionError("Expected only " + expected + ", found " + active);
    }

    private static void teleport(Minecraft mc, Vec3 position, float yaw) throws Exception {
        mc.getSingleplayerServer().submit(() -> {
            var player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
            player.setGameMode(GameType.SURVIVAL);
            player.setNoGravity(true); player.setInvulnerable(true);
            player.getAbilities().mayfly = true; player.getAbilities().flying = true; player.onUpdateAbilities();
            player.teleportTo(player.serverLevel(), position.x, position.y, position.z, yaw, 0);
        }).get(10, TimeUnit.SECONDS);
    }

    private static void finish(Minecraft mc) {
        done = true; GL15.glDeleteQueries(query);
        Maniacrev.LOGGER.info("Atmosphere map benchmark PASSED: {}", LABEL);
        mc.stop();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void before(ForgeVeilPostProcessingEvent.Pre event) {
        if (LABEL.isEmpty() || done || view < 0 || !measuredPipeline(event.getName())) return;
        if (!LIGHT_BREAKDOWN && ticks < WARMUP_TICKS) return;
        event.getPipeline().setFloat("Time", 12);
        if (LIGHT_BREAKDOWN) {
            Variant current = variant();
            event.getPipeline().setFloat("FogGround", (float) DENSE_HALL.y);
            event.getPipeline().setFloat("SurfaceLight", current.surface() ? 1 : 0);
            event.getPipeline().setFloat("ContactShadows", current.shadows() ? 1 : 0);
            if (!denseReady || samples.size() == BREAKDOWN_FRAMES) return;
        }
        GL15.glBeginQuery(GL33.GL_TIME_ELAPSED, query); measuring = true;
    }

    @SubscribeEvent
    public static void after(ForgeVeilPostProcessingEvent.Post event) {
        if (!measuring || !measuredPipeline(event.getName())) return;
        GL15.glEndQuery(GL33.GL_TIME_ELAPSED); measuring = false;
        samples.add(GL33.glGetQueryObjecti64(query, GL15.GL_QUERY_RESULT) / 1_000_000.0);
    }
}
