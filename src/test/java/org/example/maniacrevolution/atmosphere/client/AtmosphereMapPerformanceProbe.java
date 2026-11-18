package org.example.maniacrevolution.atmosphere.client;

import foundry.veil.forge.event.ForgeVeilPostProcessingEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
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
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/** Loads only the explicitly prepared copy named perf-map; never opens the user's original save. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class AtmosphereMapPerformanceProbe {
    private static final String LABEL = System.getProperty("maniacrev.atmosphere.mapBenchmark", "");
    private static final GraphicsMode MODE = GraphicsMode.valueOf(System.getProperty("maniacrev.atmosphere.mapBenchmarkMode", "HEAVY"));
    private static boolean loading, initialized, done, measuring;
    private static int ticks, view = -1, query;
    private static long started;
    private static Vec3 recent;
    private static final ArrayList<Double> samples = new ArrayList<>();
    private static final StringBuilder report = new StringBuilder("view,x,y,z,yaw,width,height,groups,median_ms,p95_ms\n");

    private static boolean measuredPipeline(net.minecraft.resources.ResourceLocation name) {
        return MODE == GraphicsMode.LIGHT ? name.equals(AtmosphereClient.LIGHT_PIPELINE) || name.equals(AtmosphereClient.REDUCED_LIGHT_PIPELINE)
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
            }
            ClientGameSettings.setGraphicsMode(MODE);
            if (mc.isPaused()) return;
            if (++ticks < 60) return;
            if (view >= 0 && samples.size() < 40) return;
            if (view >= 0) {
                if (MODE == GraphicsMode.LIGHT) {
                    var expected = AtmosphereClient.lightPipelineFor(mc.getMainRenderTarget().height, AtmosphereLights.sourceCount());
                    int active = 0;
                    for (var entry : foundry.veil.api.client.render.VeilRenderSystem.renderer().getPostProcessingManager().getActivePipelines()) {
                        if (!measuredPipeline(entry.getPipeline())) continue;
                        active++;
                        if (!entry.getPipeline().equals(expected)) throw new AssertionError("Wrong LIGHT pipeline: " + entry.getPipeline());
                    }
                    if (active != 1) throw new AssertionError("Expected exactly one LIGHT pipeline, got " + active);
                }
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
            if (++view == 12) {
                done = true; GL15.glDeleteQueries(query);
                Maniacrev.LOGGER.info("Atmosphere map benchmark PASSED: {}", LABEL);
                mc.stop(); return;
            }
            Vec3 position = switch (view / 4) {
                case 0 -> recent;
                case 1 -> new Vec3(-217.5, 41, -8.5);
                default -> new Vec3(-494.5, 44, 18.5);
            };
            float yaw = (view % 4) * 90;
            mc.getSingleplayerServer().submit(() -> {
                var player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                player.setGameMode(GameType.SURVIVAL);
                player.setNoGravity(true); player.setInvulnerable(true);
                player.getAbilities().mayfly = true; player.getAbilities().flying = true; player.onUpdateAbilities();
                player.teleportTo(player.serverLevel(), position.x, position.y, position.z, yaw, 0);
            }).get(10, TimeUnit.SECONDS);
        } catch (Exception | AssertionError error) {
            done = true; Maniacrev.LOGGER.error("Atmosphere map benchmark FAILED", error); mc.stop();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void before(ForgeVeilPostProcessingEvent.Pre event) {
        if (LABEL.isEmpty() || done || view < 0 || ticks < 40 || !measuredPipeline(event.getName())) return;
        event.getPipeline().setFloat("Time", 12);
        GL15.glBeginQuery(GL33.GL_TIME_ELAPSED, query); measuring = true;
    }

    @SubscribeEvent
    public static void after(ForgeVeilPostProcessingEvent.Post event) {
        if (!measuring || !measuredPipeline(event.getName())) return;
        GL15.glEndQuery(GL33.GL_TIME_ELAPSED); measuring = false;
        samples.add(GL33.glGetQueryObjecti64(query, GL15.GL_QUERY_RESULT) / 1_000_000.0);
    }
}
