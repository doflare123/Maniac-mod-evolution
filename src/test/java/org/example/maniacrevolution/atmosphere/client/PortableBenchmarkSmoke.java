package org.example.maniacrevolution.atmosphere.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipFile;

/** Exercises the shipping command, native resizing, bypass, ZIP export and three restore paths. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class PortableBenchmarkSmoke {
    private static boolean loading, done;
    private static int stage, ticks, baselinePixel, renderedFrames, stageFrame;
    private static long started;
    private static Path previousReport;
    private static Vec3 originalPosition;
    private static GameType originalMode;
    private static boolean vsync, hidden, bob, pause, fullscreen;
    private static int limit;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("maniacrev.benchmark.smoke") || done || event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        try {
            if (!loading) {
                if (mc.screen == null || mc.getOverlay() != null) return;
                check(Files.exists(mc.gameDirectory.toPath().resolve("saves/perf-map/level.dat")), "Missing isolated save copy");
                loading = true; started = System.nanoTime();
                mc.options.pauseOnLostFocus = false;
                mc.createWorldOpenFlows().loadLevel(mc.screen, "perf-map"); return;
            }
            if (System.nanoTime() - started > TimeUnit.MINUTES.toNanos(4)) throw new AssertionError("Portable benchmark smoke timed out at stage " + stage);
            if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null || mc.player.tickCount < 45) return;
            ticks++;
            if (stage == 0) {
                if (mc.screen != null) mc.setScreen(null);
                mc.getWindow().setWindowed(1600, 900);
                stageFrame = renderedFrames;
                stage = 1; ticks = 0; return;
            }
            if (stage == 1) {
                if (ticks < 20 || renderedFrames - stageFrame < 10 || mc.getOverlay() != null
                        || mc.getMainRenderTarget().width != 1600 || mc.getMainRenderTarget().height != 900) return;
                ClientCommandHandler.runCommand("mrbench points reset");
                vsync = mc.options.enableVsync().get(); limit = mc.options.framerateLimit().get(); hidden = mc.options.hideGui;
                bob = mc.options.bobView().get(); pause = mc.options.pauseOnLostFocus; fullscreen = mc.getWindow().isFullscreen();
                originalPosition = mc.player.position(); originalMode = mc.gameMode.getPlayerMode();
                try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) { baselinePixel = image.getPixelRGBA(800, 450); }
                ClientCommandHandler.runCommand("mrbench veil off");
                check(VeilRenderControl.isBypassed(), "Manual bypass command did not switch");
                stageFrame = renderedFrames;
                stage = 2; ticks = 0; return;
            }
            if (stage == 2) {
                if (renderedFrames - stageFrame < 5) return;
                try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                    int pixel = image.getPixelRGBA(800, 450);
                    int delta = 0;
                    for (int shift : new int[]{0, 8, 16}) delta = Math.max(delta, Math.abs((pixel >> shift & 255) - (baselinePixel >> shift & 255)));
                    check(delta <= 4, "Bypass changed the center pixel: " + delta);
                }
                check(VeilRenderControl.skippedCalls() > 0, "Mixin gates never executed");
                ClientCommandHandler.runCommand("mrbench veil on");
                check(!VeilRenderControl.isBypassed(), "Manual resume failed");
                AtmosphereBenchmark.startSmoke(true);
                check(AtmosphereBenchmark.isRunning(), "Cannot start matrix smoke");
                stage = 3; ticks = 0; return;
            }
            if (stage == 3) {
                if (AtmosphereBenchmark.isRunning()) return;
                JsonObject report = report("complete");
                check(report.getAsJsonArray("results").size() == 10, "Expected five modes at two native resolutions");
                HashSet<String> modes = new HashSet<>(), resolutions = new HashSet<>();
                for (var value : report.getAsJsonArray("results")) {
                    var result = value.getAsJsonObject(); var test = result.getAsJsonObject("test");
                    String mode = test.get("mode").getAsString(); modes.add(mode);
                    var requested = test.getAsJsonObject("resolution");
                    int width = result.get("actualWidth").getAsInt(), height = result.get("actualHeight").getAsInt();
                    resolutions.add(width + "x" + height);
                    check(width == requested.get("width").getAsInt() && height == requested.get("height").getAsInt(), "Native resize did not settle");
                    check(result.has("frameIntervals") && result.has("gpu"), "Missing measured samples");
                    if (mode.equals("VEIL_RENDER_BYPASS")) check(result.get("skippedVeilCalls").getAsLong() > 0, "Bypass is falsely labelled");
                    else check(result.get("skippedVeilCalls").getAsLong() == 0, "Bypass leaked into normal modes");
                }
                check(modes.size() == 5 && resolutions.size() == 2, "Incomplete mode/resolution coverage");
                restored();
                AtmosphereBenchmark.startSmoke(false); check(AtmosphereBenchmark.isRunning(), "Cannot start cancellation smoke");
                stage = 4; ticks = 0; return;
            }
            if (stage == 4) {
                if (ticks < 42) return;
                ClientCommandHandler.runCommand("mrbench stop");
                stage = 5; ticks = 0; return;
            }
            if (stage == 5) {
                if (AtmosphereBenchmark.isRunning()) return;
                report("cancelled"); restored();
                AtmosphereBenchmark.startSmoke(false); check(AtmosphereBenchmark.isRunning(), "Cannot start GUI-interruption smoke");
                stage = 6; ticks = 0; return;
            }
            if (stage == 6) {
                if (ticks < 42) return;
                mc.setScreen(new PauseScreen(true)); stage = 7; ticks = 0; return;
            }
            if (stage == 7) {
                if (AtmosphereBenchmark.isRunning()) return;
                if (mc.screen != null) mc.setScreen(null);
                report("cancelled"); restored();
                done = true;
                Maniacrev.LOGGER.info("Portable benchmark smoke PASSED: native resize, five modes, reversible bypass, hardware ZIP, stop, GUI interruption, state restoration");
                mc.stop();
            }
        } catch (Exception | AssertionError error) {
            done = true; Maniacrev.LOGGER.error("Portable benchmark smoke FAILED at stage " + stage, error);
            if (AtmosphereBenchmark.isRunning()) AtmosphereBenchmark.stopForTest();
            mc.stop();
        }
    }

    private static JsonObject report(String status) throws Exception {
        Path file = AtmosphereBenchmark.lastReport();
        check(file != null && Files.isRegularFile(file) && !file.equals(previousReport), "Missing/new report file"); previousReport = file;
        try (ZipFile zip = new ZipFile(file.toFile())) {
            check(zip.getEntry("frames.csv") != null && zip.getEntry("README.txt") != null, "Incomplete ZIP");
            String text;
            try (var input = zip.getInputStream(zip.getEntry("report.json"))) { text = new String(input.readAllBytes(), StandardCharsets.UTF_8); }
            var report = JsonParser.parseString(text).getAsJsonObject();
            check(report.get("status").getAsString().equals(status), "Unexpected report status: " + report.get("status"));
            var environment = report.getAsJsonObject("environment");
            check(environment.has("cpu") && environment.has("gpu_renderer") && environment.has("system_ram_bytes") && environment.has("mods"), "Missing hardware configuration");
            check(!text.contains("ABOBUS") && !text.contains("java.class.path") && !text.contains("world_seed"), "Report contains unnecessary private metadata");
            return report;
        }
    }

    private static void restored() {
        var mc = Minecraft.getInstance();
        check(!VeilRenderControl.isBypassed() && AtmosphereBenchmark.graphicsOverride() == null, "Leaked diagnostic override");
        check(mc.options.enableVsync().get() == vsync && mc.options.framerateLimit().get() == limit && mc.options.hideGui == hidden
                && mc.options.bobView().get() == bob && mc.options.pauseOnLostFocus == pause, "Graphics options were not restored");
        check(mc.getWindow().isFullscreen() == fullscreen && mc.getMainRenderTarget().width == 1600 && mc.getMainRenderTarget().height == 900, "Window not restored");
        check(mc.gameMode.getPlayerMode() == originalMode && mc.player.position().distanceToSqr(originalPosition) < .25, "Player state not restored");
    }
    private static void check(boolean value, String label) { if (!value) throw new AssertionError(label); }

    @SubscribeEvent
    public static void frames(TickEvent.RenderTickEvent event) {
        if (Boolean.getBoolean("maniacrev.benchmark.smoke") && event.phase == TickEvent.Phase.END) renderedFrames++;
    }
}
