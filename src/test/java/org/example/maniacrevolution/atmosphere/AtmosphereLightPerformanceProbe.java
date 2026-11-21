package org.example.maniacrevolution.atmosphere;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import foundry.veil.api.client.render.VeilRenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.atmosphere.client.AtmosphereClient;
import org.example.maniacrevolution.settings.GraphicsMode;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;

import java.nio.file.Files;
import java.util.Arrays;
import java.util.Locale;

/** Same geometry, lamps and view in all modes; times the complete post pipeline including its copies. */
final class AtmosphereLightPerformanceProbe {
    static void run(Runnable scene) throws Exception {
        var mc = Minecraft.getInstance();
        var renderer = VeilRenderSystem.renderer();
        var manager = renderer.getPostProcessingManager();
        var output = mc.gameDirectory.toPath().resolve("benchmark-" + System.getProperty("maniacrev.atmosphere.benchmark"));
        Files.createDirectories(output);
        StringBuilder report = new StringBuilder("width,height,mode,groups,median_ms,p95_ms\n");
        int query = GL15.glGenQueries();
        try {
            for (int width : new int[]{1280, 1920, 2560, 3840}) {
                int height = width * 9 / 16;
                mc.getMainRenderTarget().resize(width, height, Minecraft.ON_OSX);
                renderer.getFramebufferManager().resizeFramebuffers(width, height);
                var projection = new Matrix4f().perspective((float) Math.toRadians(70), 16F / 9, .05F, 160);
                RenderSystem.setProjectionMatrix(projection, VertexSorting.DISTANCE_TO_ORIGIN);
                renderer.getCameraMatrices().update(projection, new Matrix4f(), new Vector3f(0, 2, 4), .05F, 160);
                for (int count : new int[]{0, 40, 64}) for (var mode : new GraphicsMode[]{GraphicsMode.LIGHT, GraphicsMode.NORMAL, GraphicsMode.HEAVY}) {
                    var pipeline = manager.getPipeline(mode == GraphicsMode.LIGHT ? AtmosphereClient.lightPipelineFor(height, count) : AtmosphereClient.PIPELINE);
                    if (pipeline == null || !pipeline.hasUniform("FogDensity")) throw new AssertionError("Shader failed to compile: " + mode);
                    pipeline.setFloat("Active", 1); pipeline.setFloat("Time", 12);
                    pipeline.setFloat("Corruption", 0); pipeline.setFloat("LightCorruption", 0);
                    pipeline.setFloat("Comfort", 1);
                    pipeline.setFloat("FogGround", 0); pipeline.setFloat("FogDensity", .055F);
                    pipeline.setFloat("SightFadeStart", AtmosphereRules.FOG_FADE_START);
                    pipeline.setFloat("SightRange", AtmosphereRules.FOG_END);
                    pipeline.setFloat("Indoor", 0); pipeline.setFloat("Hurt", 0);
                    pipeline.setFloat("Fear", 0); pipeline.setFloat("Plague", 0);
                    pipeline.setInt("FogSamples", mode.fogSamples()); pipeline.setInt("LightSamples", mode.lightSamples());
                    pipeline.setInt("HandLightCount", 0); pipeline.setInt("LightCount", count);
                    pipeline.setFloat("SurfaceLight", 1); pipeline.setFloat("ContactShadows", mode.dynamicLights() ? 1 : 0);
                    Vector4f[] positions = new Vector4f[64], colors = new Vector4f[64], extents = new Vector4f[64];
                    for (int i = 0; i < 64; i++) {
                        positions[i] = new Vector4f((i % 5 - 2) * 3F, .5F, -4 - (i / 5) * 3F, 8);
                        // Keep the comparison image below clipping so brightness drift remains visible.
                        colors[i] = new Vector4f(1, .63F, .28F, .15F);
                        extents[i] = new Vector4f(1.5F, 0, 0, 4);
                    }
                    pipeline.setVectors("LightPositionRadius", positions);
                    pipeline.setVectors("LightColorPower", colors); pipeline.setVectors("LightExtent", extents);
                    double[] times = new double[31];
                    for (int i = -30; i < times.length; i++) {
                        scene.run();
                        GL15.glBeginQuery(GL33.GL_TIME_ELAPSED, query);
                        manager.runPipeline(pipeline);
                        GL15.glEndQuery(GL33.GL_TIME_ELAPSED);
                        double elapsed = GL33.glGetQueryObjecti64(query, GL15.GL_QUERY_RESULT) / 1_000_000.0;
                        if (i >= 0) times[i] = elapsed;
                    }
                    Arrays.sort(times);
                    report.append(String.format(Locale.ROOT, "%d,%d,%s,%d,%.3f,%.3f%n", width, height, mode, count, times[15], times[29]));
                    String filename = width + "-" + mode + "-groups" + count + ".png";
                    try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                        image.writeToFile(output.resolve(filename));
                        var reference = output.getParent().resolve("benchmark-light-before").resolve(filename);
                        if (!output.equals(reference.getParent()) && Files.exists(reference)) {
                            try (var input = Files.newInputStream(reference);
                                 var before = com.mojang.blaze3d.platform.NativeImage.read(input)) {
                                long sum = 0, changed = 0;
                                int[] histogram = new int[256];
                                for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
                                    int a = image.getPixelRGBA(x, y), b = before.getPixelRGBA(x, y), worst = 0;
                                    for (int shift = 0; shift < 24; shift += 8) {
                                        int delta = Math.abs((a >> shift & 255) - (b >> shift & 255));
                                        sum += delta; worst = Math.max(worst, delta);
                                    }
                                    histogram[worst]++;
                                    if (worst > 8) changed++;
                                }
                                int p99 = 0, cumulative = 0;
                                while ((cumulative += histogram[p99]) < width * height * .99 && p99 < 255) p99++;
                                double mean = sum / (width * height * 3.0);
                                Maniacrev.LOGGER.info("Light image comparison {}: meanRGB={}, p99Max={}, above8={}%", filename, mean, p99, changed * 100.0 / (width * height));
                                if (mode != GraphicsMode.LIGHT && sum != 0) throw new AssertionError("Other mode changed: " + filename);
                                if (mode == GraphicsMode.LIGHT && (mean > 1 || p99 > 8)) throw new AssertionError("Light reconstruction drift: " + filename);
                            }
                        }
                    }
                    Maniacrev.LOGGER.info("Light GPU benchmark: {}x{}, {}, {} groups: {} ms", width, height, mode, count, times[15]);
                }
            }
            Files.writeString(output.resolve("gpu.csv"), report);
            Maniacrev.LOGGER.info("Atmosphere light benchmark PASSED: {}", output);
        } finally { GL15.glDeleteQueries(query); }
    }
}
