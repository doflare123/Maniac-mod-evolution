package org.example.maniacrevolution.atmosphere.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraftforge.fml.ModList;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.glfw.GLFW;
import oshi.SystemInfo;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

final class BenchmarkReport {
    static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    static final class Frame {
        final double elapsedMs, intervalMs, renderWallMs;
        Double gpuMs;
        Frame(double elapsedMs, double intervalMs, double renderWallMs) {
            this.elapsedMs = elapsedMs; this.intervalMs = intervalMs; this.renderWallMs = renderWallMs;
        }
    }
    static final class Result {
        final BenchmarkPlan.Case test;
        final int actualWidth, actualHeight;
        final int nativeFramebufferWidth, nativeFramebufferHeight, refreshRateHz, fpsLimit;
        final boolean fullscreen;
        final List<String> warnings = new ArrayList<>();
        final transient List<Frame> frames = new ArrayList<>();
        String chunkStatistics;
        boolean completed;
        double measuredDurationMs;
        int minLights = Integer.MAX_VALUE, maxLights, cachedGroups;
        int observedBypassGates;
        long skippedVeilCalls, gcCount, gcTimeMs;
        BenchmarkPlan.Statistics frameIntervals, renderWall, gpu;
        Result(BenchmarkPlan.Case test) {
            this.test = test;
            var mc = Minecraft.getInstance();
            actualWidth = mc.getMainRenderTarget().width; actualHeight = mc.getMainRenderTarget().height;
            fullscreen = mc.getWindow().isFullscreen();
            int[] w = new int[1], h = new int[1]; GLFW.glfwGetFramebufferSize(mc.getWindow().getWindow(), w, h);
            nativeFramebufferWidth = w[0]; nativeFramebufferHeight = h[0];
            refreshRateHz = mc.getWindow().getRefreshRate(); fpsLimit = mc.options.framerateLimit().get();
            chunkStatistics = mc.levelRenderer.getChunkStatistics();
        }
        void summarize() {
            frameIntervals = BenchmarkPlan.statistics(frames.stream().map(f -> f.intervalMs).filter(v -> v > 0).toList());
            renderWall = BenchmarkPlan.statistics(frames.stream().map(f -> f.renderWallMs).filter(v -> v > 0).toList());
            gpu = BenchmarkPlan.statistics(frames.stream().map(f -> f.gpuMs).filter(v -> v != null && v > 0).toList());
            if (minLights == Integer.MAX_VALUE) minLights = 0;
            if (frames.size() < 100) warnings.add("fewer_than_100_frames_percentiles_are_noisy");
            if (actualWidth != test.resolution().width() || actualHeight != test.resolution().height()) warnings.add("actual_resolution_differs_from_requested");
            if (minLights != maxLights) warnings.add("uploaded_light_count_changed_during_measurement");
            if (frameIntervals != null && frameIntervals.medianFps() >= fpsLimit * .96) warnings.add("fps_near_configured_limit");
            if (frameIntervals != null && refreshRateHz > 0 && Math.abs(frameIntervals.medianFps() - refreshRateHz) < refreshRateHz * .025)
                warnings.add("possible_refresh_rate_ceiling");
        }
    }

    static Map<String, Object> settings() {
        var mc = Minecraft.getInstance(); var o = mc.options;
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("framebuffer_width", mc.getMainRenderTarget().width); values.put("framebuffer_height", mc.getMainRenderTarget().height);
        values.put("window_width", mc.getWindow().getScreenWidth()); values.put("window_height", mc.getWindow().getScreenHeight());
        values.put("fullscreen", mc.getWindow().isFullscreen()); values.put("refresh_rate_hz", mc.getWindow().getRefreshRate());
        values.put("vsync", o.enableVsync().get()); values.put("fps_limit", o.framerateLimit().get());
        values.put("render_distance", o.renderDistance().get()); values.put("effective_render_distance", o.getEffectiveRenderDistance());
        values.put("simulation_distance", o.simulationDistance().get()); values.put("vanilla_graphics", o.graphicsMode().get().name());
        values.put("atmosphere_graphics", AtmosphereClient.graphicsMode().name());
        values.put("ambient_occlusion", o.ambientOcclusion().get()); values.put("mipmap_levels", o.mipmapLevels().get());
        values.put("biome_blend_radius", o.biomeBlendRadius().get()); values.put("entity_distance_scaling", o.entityDistanceScaling().get());
        values.put("clouds", o.cloudStatus().get().name()); values.put("particles", o.particles().get().name());
        values.put("fov", o.fov().get()); values.put("gamma", o.gamma().get()); values.put("bob_view", o.bobView().get());
        values.put("camera_type", o.getCameraType().name());
        values.put("fov_effect_scale", o.fovEffectScale().get()); values.put("screen_effect_scale", o.screenEffectScale().get());
        values.put("gui_hidden", o.hideGui); values.put("pause_on_lost_focus", o.pauseOnLostFocus);
        values.put("chat_visibility", o.chatVisibility().get().name());
        return values;
    }

    static Map<String, Object> environment() {
        var mc = Minecraft.getInstance();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("minecraft", SharedConstants.getCurrentVersion().getName());
        values.put("production_runtime", net.minecraftforge.fml.loading.FMLEnvironment.production);
        values.put("build_fingerprint_sha256", fingerprint());
        values.put("os", System.getProperty("os.name")); values.put("os_version", System.getProperty("os.version"));
        values.put("os_arch", System.getProperty("os.arch")); values.put("java_version", System.getProperty("java.version"));
        values.put("java_vendor", System.getProperty("java.vendor")); values.put("jvm_max_heap_bytes", Runtime.getRuntime().maxMemory());
        values.put("jvm_available_processors", Runtime.getRuntime().availableProcessors());
        values.put("gpu_renderer", GL11.glGetString(GL11.GL_RENDERER)); values.put("gpu_vendor", GL11.glGetString(GL11.GL_VENDOR));
        values.put("opengl_version", GL11.glGetString(GL11.GL_VERSION)); values.put("glsl_version", GL11.glGetString(0x8B8C));
        values.put("gpu_timer_supported", GL.getCapabilities().OpenGL33 || GL.getCapabilities().GL_ARB_timer_query);
        try {
            var hardware = new SystemInfo().getHardware();
            var processor = hardware.getProcessor();
            values.put("cpu", processor.getProcessorIdentifier().getName());
            values.put("cpu_physical_cores", processor.getPhysicalProcessorCount()); values.put("cpu_logical_cores", processor.getLogicalProcessorCount());
            values.put("system_ram_bytes", hardware.getMemory().getTotal());
            values.put("graphics_adapters", hardware.getGraphicsCards().stream().map(card -> Map.of(
                    "name", card.getName(), "vendor", card.getVendor(), "vram_bytes", card.getVRam(), "version", card.getVersionInfo())).toList());
        } catch (RuntimeException | LinkageError error) { values.put("hardware_detection_error", error.getClass().getSimpleName()); }
        values.put("mods", ModList.get().getMods().stream().map(mod -> Map.of("id", mod.getModId(), "version", mod.getVersion().toString())).toList());
        values.put("resource_packs", mc.getResourceManager().listPacks().map(pack -> pack.packId()).toList());
        if (mc.player != null) {
            values.put("main_hand_item", BuiltInRegistries.ITEM.getKey(mc.player.getMainHandItem().getItem()).toString());
            values.put("off_hand_item", BuiltInRegistries.ITEM.getKey(mc.player.getOffhandItem().getItem()).toString());
        }
        return values;
    }

    static long gcCount() { return ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(bean -> Math.max(0, bean.getCollectionCount())).sum(); }
    static long gcTime() { return ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(bean -> Math.max(0, bean.getCollectionTime())).sum(); }

    private static String fingerprint() {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            for (String resource : List.of("/org/example/maniacrevolution/atmosphere/client/AtmosphereBenchmark.class",
                    "/org/example/maniacrevolution/atmosphere/client/VeilRenderControl.class",
                    "/assets/maniacrev/pinwheel/shaders/program/atmosphere.fsh",
                    "/assets/maniacrev/pinwheel/shaders/program/atmosphere_volume.fsh",
                    "/assets/maniacrev/pinwheel/shaders/include/atmosphere_lighting.glsl")) {
                digest.update(resource.getBytes(StandardCharsets.UTF_8));
                try (var input = BenchmarkReport.class.getResourceAsStream(resource)) {
                    if (input != null) digest.update(input.readAllBytes());
                }
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (Exception error) { return "unavailable"; }
    }

    static void write(Path output, Map<String, Object> report, List<Result> results) throws IOException {
        Files.createDirectories(output.getParent());
        Path temporary = output.resolveSibling(output.getFileName() + ".part");
        try (var zip = new ZipOutputStream(Files.newOutputStream(temporary), StandardCharsets.UTF_8)) {
            zip.putNextEntry(new ZipEntry("report.json"));
            zip.write(JSON.toJson(report).getBytes(StandardCharsets.UTF_8)); zip.closeEntry();
            zip.putNextEntry(new ZipEntry("frames.csv"));
            zip.write("case_id,frame,elapsed_ms,frame_interval_ms,render_wall_ms,gpu_render_ms\n".getBytes(StandardCharsets.UTF_8));
            for (Result result : results) for (int index = 0; index < result.frames.size(); index++) {
                Frame frame = result.frames.get(index);
                String row = result.test.id() + "," + index + "," + frame.elapsedMs + "," + frame.intervalMs + "," + frame.renderWallMs + "," + (frame.gpuMs == null ? "" : frame.gpuMs) + "\n";
                zip.write(row.getBytes(StandardCharsets.UTF_8));
            }
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("README.txt"));
            zip.write(("Maniac Revolution benchmark, schema 1\nCreated: " + Instant.now() + "\n"
                    + "VEIL_RENDER_BYPASS skips selected rendering services. Veil remains loaded; this is NOT a client without Veil.\n"
                    + "Frame intervals include presentation/waits; GPU timings cover RenderTick START..END, not every external callback.\n"
                    + "onePercentLowFps = 1000 / p99 frame interval. Short samples and frame caps can obscure differences.\n"
                    + "report.json contains hardware, settings, route, validation and summaries. frames.csv contains individual frame samples.\n"
                    + "No player name, UUID, world seed, server address or credentials are collected.\n").getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        try { Files.move(temporary, output, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, output); }
    }
}
