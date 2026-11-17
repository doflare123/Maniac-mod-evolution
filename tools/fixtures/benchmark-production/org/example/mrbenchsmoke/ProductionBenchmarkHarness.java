package org.example.mrbenchsmoke;

import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.atmosphere.client.AtmosphereBenchmark;
import org.example.maniacrevolution.atmosphere.client.VeilRenderControl;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipFile;

/** Separate test mod: validates the obfuscated release JAR in an ordinary Forge installation. */
@Mod("mrbenchsmoke")
@Mod.EventBusSubscriber(modid = "mrbenchsmoke", value = Dist.CLIENT)
public final class ProductionBenchmarkHarness {
    private static boolean loading, running, done;
    private static long started, worldReadyAt;
    private static int frames;

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("maniacrev.benchmark.productionSmoke") || done || event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        try {
            if (!loading) {
                if (mc.screen == null || mc.getOverlay() != null) return;
                if (!Files.exists(mc.gameDirectory.toPath().resolve("saves/perf-map/level.dat"))) throw new AssertionError("Missing copied world");
                loading = true; started = System.nanoTime(); mc.options.pauseOnLostFocus = false;
                mc.createWorldOpenFlows().loadLevel(mc.screen, "perf-map"); return;
            }
            if (System.nanoTime() - started > TimeUnit.MINUTES.toNanos(4)) throw new AssertionError("Production benchmark timed out");
            if (mc.player == null || mc.level == null || frames < 100 || mc.getOverlay() != null) return;
            if (worldReadyAt == 0) worldReadyAt = System.nanoTime();
            if (System.nanoTime() - worldReadyAt < TimeUnit.SECONDS.toNanos(20)) return;
            if (mc.screen != null) mc.setScreen(null);
            if (!running) {
                running = true; AtmosphereBenchmark.startSmoke(false);
                if (!AtmosphereBenchmark.isRunning()) throw new AssertionError("Cannot start production command");
                return;
            }
            if (AtmosphereBenchmark.isRunning()) return;
            try (var zip = new ZipFile(AtmosphereBenchmark.lastReport().toFile()); var input = zip.getInputStream(zip.getEntry("report.json"))) {
                var report = JsonParser.parseString(new String(input.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
                if (!report.get("status").getAsString().equals("complete") || report.getAsJsonArray("results").size() != 5
                        || !report.getAsJsonObject("environment").get("production_runtime").getAsBoolean()
                        || !report.get("server_state_restored").getAsBoolean() || VeilRenderControl.isBypassed())
                    throw new AssertionError("Invalid production report/restoration");
                var bypass = report.getAsJsonArray("results").get(0).getAsJsonObject();
                if (bypass.get("observedBypassGates").getAsInt() != 15) throw new AssertionError("Production mixin gates missing");
            }
            done = true; LoggerFactory.getLogger("MRBenchProduction").info("Portable production benchmark PASSED: release JAR, mixin gates, five modes, ZIP and restoration"); mc.stop();
        } catch (Exception | AssertionError error) {
            done = true; LoggerFactory.getLogger("MRBenchProduction").error("Portable production benchmark FAILED", error); mc.stop();
        }
    }
    @SubscribeEvent
    public static void frame(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.END && loading) frames++;
    }
}
