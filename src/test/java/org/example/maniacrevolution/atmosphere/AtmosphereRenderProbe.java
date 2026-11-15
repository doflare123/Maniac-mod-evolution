package org.example.maniacrevolution.atmosphere;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import foundry.veil.api.client.render.VeilRenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.atmosphere.client.AtmosphereClient;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

/** Real OpenGL shader smoke test; uses no player worlds or saves. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class AtmosphereRenderProbe {
    private static boolean done;
    private static int frames;

    @SubscribeEvent
    public static void render(TickEvent.RenderTickEvent event) {
        var mc = Minecraft.getInstance();
        if (!Boolean.getBoolean("maniacrev.atmosphere.renderProbe") || done || event.phase != TickEvent.Phase.END
                || mc.screen == null || mc.getOverlay() != null || ++frames < 30) return;
        done = true;
        var previous = new Matrix4f(RenderSystem.getProjectionMatrix());
        var sorting = RenderSystem.getVertexSorting();
        var view = RenderSystem.getModelViewStack();
        view.pushPose();
        try {
            inputCallbacks(mc.getWindow().getWindow());
            var manager = VeilRenderSystem.renderer().getPostProcessingManager();
            var pipeline = manager.getPipeline(AtmosphereClient.PIPELINE);
            if (pipeline == null || !pipeline.hasUniform("FogDensity")) throw new AssertionError("Atmosphere shader failed to load/compile");
            var projection = new Matrix4f().perspective((float) Math.toRadians(70),
                    (float) mc.getMainRenderTarget().width / mc.getMainRenderTarget().height, 0.05F, 160);
            RenderSystem.setProjectionMatrix(projection, VertexSorting.DISTANCE_TO_ORIGIN);
            view.setIdentity();
            RenderSystem.applyModelViewMatrix();
            var rotation = new Matrix4f();
            VeilRenderSystem.renderer().getCameraMatrices().update(projection, rotation, new Vector3f(0, 2, 4), 0.05F, 160);
            double baseline = 0;
            long commonHash = 0;
            for (String variant : new String[]{"vanilla", "survivor", "maniac", "keeper", "sanity", "chase", "lamp"}) {
                scene();
                pipeline.setFloat("Active", variant.equals("vanilla") ? 0 : 1);
                pipeline.setFloat("Time", 12.0F);
                pipeline.setFloat("Corruption", variant.equals("sanity") ? .95F : 0);
                pipeline.setFloat("LightCorruption", variant.equals("sanity") ? .95F : 0);
                pipeline.setFloat("Comfort", 1);
                pipeline.setFloat("Pursuit", variant.equals("chase") ? 1 : 0);
                pipeline.setFloat("FogGround", 0);
                pipeline.setFloat("FogDensity", .055F);
                pipeline.setFloat("SightFadeStart", AtmosphereRules.FOG_FADE_START);
                pipeline.setFloat("SightRange", AtmosphereRules.FOG_END);
                pipeline.setFloat("Indoor", 0);
                pipeline.setInt("FogSamples", 12);
                pipeline.setFloat("Hurt", 0);
                pipeline.setFloat("Fear", 0);
                pipeline.setFloat("Plague", 0);
                source(pipeline, variant.equals("lamp"));
                pipeline.setFloat("SurfaceLight", variant.equals("lamp") ? 1 : 0);
                manager.runPipeline(pipeline);
                try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                    double sum = 0;
                    long hash = 1;
                    for (int y = 0; y < image.getHeight(); y += 3) for (int x = 0; x < image.getWidth(); x += 3) {
                        int pixel = image.getPixelRGBA(x, y);
                        sum += (pixel & 255) + ((pixel >> 8) & 255) + ((pixel >> 16) & 255);
                        hash = hash * 31 + pixel;
                    }
                    if (sum < 10000) throw new AssertionError("Black output: " + variant);
                    if (variant.equals("vanilla")) baseline = sum;
                    else if (Math.abs(sum - baseline) < 100) throw new AssertionError("Effect missing: " + variant);
                    if (variant.equals("survivor")) commonHash = hash;
                    if ((variant.equals("maniac") || variant.equals("keeper")) && hash != commonHash)
                        throw new AssertionError("Roles do not share the same base: " + variant);
                    if (!variant.equals("vanilla")) {
                        int light = image.getPixelRGBA(image.getWidth() / 2, image.getHeight() / 2);
                        int red = light & 255, green = light >> 8 & 255, blue = light >> 16 & 255;
                        if (red <= green || green <= blue + (variant.equals("sanity") ? 0 : 15))
                            throw new AssertionError("Atmosphere lost the warm light: " + variant);
                    }
                    image.writeToFile(mc.gameDirectory.toPath().resolve("atmosphere-" + variant + ".png"));
                    Maniacrev.LOGGER.info("Atmosphere render probe {}: luminance sum {}", variant, sum);
                }
            }
            // Equal source colors/depth, symmetric screen positions: the lower sample
            // must encounter a fog bank instead of both receiving the same tint.
            var target = mc.getMainRenderTarget();
            target.clear(Minecraft.ON_OSX); target.bindWrite(true);
            RenderSystem.enableDepthTest(); RenderSystem.depthMask(true);
            RenderSystem.disableCull(); RenderSystem.disableBlend();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            var b = Tesselator.getInstance().getBuilder();
            b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            quad(b, -80, -80, -30, 80, -80, -30, 80, 80, -30, -80, 80, -30, .5F, .5F, .5F);
            BufferUploader.drawWithShader(b.end());
            source(pipeline, false);
            pipeline.setFloat("SurfaceLight", 0);
            pipeline.setFloat("FogDensity", .045F);
            manager.runPipeline(pipeline);
            try (var image = Screenshot.takeScreenshot(target)) {
                int upper = image.getPixelRGBA(image.getWidth() / 2, image.getHeight() / 2 - image.getHeight() / 14);
                int lower = image.getPixelRGBA(image.getWidth() / 2, image.getHeight() / 2 + image.getHeight() / 14);
                if (Math.abs((upper & 255) - (lower & 255)) < 4) throw new AssertionError("Volumetric fog became a flat screen tint");
                image.writeToFile(mc.gameDirectory.toPath().resolve("atmosphere-fog-height-check.png"));
                Maniacrev.LOGGER.info("Fog height check PASSED: upper {}, lower {}", upper & 255, lower & 255);
            }
            visibilityChecks(manager, pipeline);
            graphicsModes(manager);
            lightingChecks(manager);
            lighterChecks(manager);
            fogMaskChecks(manager);
            coloredLightsChecks(manager);
            rowLightingChecks(manager);
            ceilingLightChecks(manager);
            haloContinuityChecks(manager);
            org.example.maniacrevolution.settings.SettingsGraphicsProbe.run();
            Maniacrev.LOGGER.info("Atmosphere render probe PASSED: common base, four graphics modes, warm lighting, sanity, depth occlusion, visibility range, settings menu");
        } catch (Exception | AssertionError error) {
            Maniacrev.LOGGER.error("Atmosphere render probe FAILED", error);
        } finally {
            view.popPose();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(previous, sorting);
            mc.getMainRenderTarget().bindWrite(true);
            mc.stop();
        }
    }

    private static void graphicsModes(foundry.veil.api.client.render.post.PostProcessingManager manager) throws Exception {
        var light = manager.getPipeline(AtmosphereClient.LIGHT_PIPELINE);
        if (light == null || !light.hasUniform("FogDensity")) throw new AssertionError("Light shader did not compile");
        for (var mode : org.example.maniacrevolution.settings.GraphicsMode.values()) {
            org.example.maniacrevolution.settings.ClientGameSettings.setGraphicsMode(mode);
            scene();
            var pipeline = manager.getPipeline(mode == org.example.maniacrevolution.settings.GraphicsMode.LIGHT
                    ? AtmosphereClient.LIGHT_PIPELINE : AtmosphereClient.PIPELINE);
            pipeline.setFloat("Active", mode.enabled() ? 1 : 0);
            pipeline.setFloat("Time", 12); pipeline.setFloat("Corruption", 0); pipeline.setFloat("Pursuit", 0);
            pipeline.setFloat("LightCorruption", 0); pipeline.setFloat("Comfort", 1);
            pipeline.setFloat("FogGround", 0); pipeline.setFloat("FogDensity", .055F); pipeline.setFloat("Indoor", 0);
            pipeline.setFloat("SightFadeStart", AtmosphereRules.FOG_FADE_START);
            pipeline.setFloat("SightRange", AtmosphereRules.FOG_END);
            pipeline.setFloat("Hurt", 0); pipeline.setFloat("Fear", 0); pipeline.setFloat("Plague", 0);
            source(pipeline, mode.lightSamples() > 0); pipeline.setInt("FogSamples", mode.fogSamples());
            pipeline.setFloat("SurfaceLight", mode.dynamicLights() ? 1 : 0);
            if (mode.enabled()) manager.runPipeline(pipeline);
            try (var image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
                image.writeToFile(Minecraft.getInstance().gameDirectory.toPath()
                        .resolve("graphics-world-" + mode.name().toLowerCase(java.util.Locale.ROOT) + ".png"));
            }
        }
        visibilityChecks(manager, light);
        org.example.maniacrevolution.settings.ClientGameSettings.resetGraphicsMode();
        Maniacrev.LOGGER.info("Graphics modes render PASSED: OFF without a pass, LIGHT analytic fog, NORMAL 12 steps, HEAVY 24 steps");
    }

    /** A white source and black source at the same point must become indistinguishable. */
    private static void visibilityChecks(foundry.veil.api.client.render.post.PostProcessingManager manager,
                                         foundry.veil.api.client.render.post.PostPipeline pipeline) throws java.io.IOException {
        pipeline.setFloat("Active", 1); pipeline.setFloat("Corruption", 0); pipeline.setFloat("Pursuit", 0);
        pipeline.setFloat("LightCorruption", 0); pipeline.setFloat("Comfort", 1);
        pipeline.setFloat("Fear", 0); pipeline.setFloat("Hurt", 0); pipeline.setFloat("Plague", 0);
        source(pipeline, false); pipeline.setFloat("FogDensity", .055F);
        pipeline.setFloat("SurfaceLight", 0);
        int[] qualities = pipeline.hasUniform("FogSamples") ? new int[]{8, 12, 24} : new int[]{0};
        for (int samples : qualities) for (int indoor = 0; indoor <= 1; indoor++) {
            pipeline.setInt("FogSamples", samples); pipeline.setFloat("Indoor", indoor);
            for (float ground : new float[]{0, -20}) {
                pipeline.setFloat("FogGround", ground);
                int nearContrast = planeContrast(manager, pipeline, 6);
                if (nearContrast < 80) throw new AssertionError("Fog destroys nearby visibility: " + nearContrast);
                int midContrast = planeContrast(manager, pipeline, 20);
                if (midContrast >= nearContrast / 2) throw new AssertionError("20-block objects remain too clear: " + midContrast);
                for (float distance : new float[]{28, 30, 100}) {
                    int farContrast = planeContrast(manager, pipeline, distance);
                    if (farContrast > 2) throw new AssertionError("Fog reveals geometry at " + distance + ": " + farContrast);
                }
            }
        }
        // Bright scattered light also must not restore the hidden source silhouette.
        source(pipeline, true);
        pipeline.setFloat("SurfaceLight", 1);
        if (planeContrast(manager, pipeline, 30) > 2) throw new AssertionError("Lamp reveals distant source");
        Maniacrev.LOGGER.info("Fog range PASSED: 6 blocks readable, 20 fading, 28/30/100 hidden; 8/12/24 samples, indoors, above low fog, lamp");
    }

    private static void source(foundry.veil.api.client.render.post.PostPipeline pipeline, boolean enabled) {
        source(pipeline, enabled ? 1 : 0);
    }

    private static void source(foundry.veil.api.client.render.post.PostPipeline pipeline, int count) {
        var positions = new org.joml.Vector4f[8];
        var colors = new org.joml.Vector4f[8];
        for (int i = 0; i < 8; i++) { positions[i] = new org.joml.Vector4f(); colors[i] = new org.joml.Vector4f(); }
        for (int i = 0; i < count; i++) {
            positions[i].set(.5F + i * .2F, .5F, -4, 8);
            colors[i].set(1, .63F, .28F, 1.1F);
        }
        pipeline.setInt("LightCount", count);
        pipeline.setInt("LightSamples", 8);
        pipeline.setFloat("ContactShadows", 1);
        pipeline.setInt("HandLightCount", 0);
        pipeline.setVectors("LightPositionRadius", positions);
        pipeline.setVectors("LightColorPower", colors);
        var extents = new org.joml.Vector4f[64];
        for (int i = 0; i < extents.length; i++) extents[i] = new org.joml.Vector4f();
        pipeline.setVectors("LightExtent", extents);
    }

    /** Numeric assertions exercise the real shader, not a Java copy of its lighting equations. */
    private static void lightingChecks(foundry.veil.api.client.render.post.PostProcessingManager manager) throws Exception {
        var volume = manager.getPipeline(AtmosphereClient.PIPELINE);
        var light = manager.getPipeline(AtmosphereClient.LIGHT_PIPELINE);
        for (var pipeline : new foundry.veil.api.client.render.post.PostPipeline[]{light, volume}) {
            if (!pipeline.hasUniform("LightCorruption")) throw new AssertionError("Sanity lighting uniform missing");
            pipeline.setFloat("Active", 1); pipeline.setFloat("Time", 12);
            pipeline.setFloat("Corruption", 0); pipeline.setFloat("Pursuit", 0);
            pipeline.setFloat("LightCorruption", 0); pipeline.setFloat("Comfort", 1);
            pipeline.setFloat("Fear", 0); pipeline.setFloat("Hurt", 0); pipeline.setFloat("Plague", 0);
            pipeline.setFloat("FogGround", -20); pipeline.setFloat("Indoor", 1); pipeline.setFloat("FogDensity", .055F);
            pipeline.setFloat("SightFadeStart", 18); pipeline.setFloat("SightRange", 28);
            pipeline.setInt("FogSamples", 24); pipeline.setFloat("SurfaceLight", 0); source(pipeline, false);
            int healthy = lightingPlane(manager, pipeline, 6);
            pipeline.setFloat("LightCorruption", 1);
            int broken = lightingPlane(manager, pipeline, 6);
            if (brightness(broken) >= brightness(healthy) - 10) throw new AssertionError("Sanity does not dim lighting");
            if (warmth(broken) >= warmth(healthy) - 10) throw new AssertionError("Sanity does not cool lighting");
            pipeline.setFloat("LightCorruption", 0);
        }
        light.setFloat("LightCorruption", 1); light.setFloat("Comfort", 0); light.setFloat("Time", 0);
        int stable = lightingPlane(manager, light, 6);
        light.setFloat("Time", 1.5F);
        if (lightingPlane(manager, light, 6) != stable) throw new AssertionError("Comfort zero leaves lighting pulses");
        light.setFloat("Comfort", 1);
        if (brightness(lightingPlane(manager, light, 6)) >= brightness(stable) - 3)
            throw new AssertionError("Critical sanity has no soft light dip");

        source(volume, false);
        int unlit = lightingPlane(manager, volume, 6);
        source(volume, true);
        int scattered = lightingPlane(manager, volume, 6);
        if (brightness(scattered) < brightness(unlit) + 8 || warmth(scattered) < warmth(unlit) + 5)
            throw new AssertionError("Point source does not warm the fog");
        volume.setFloat("SurfaceLight", 1);
        int surface = lightingPlane(manager, volume, 6);
        if (brightness(surface) < brightness(scattered) + 8) throw new AssertionError("Heavy mode does not light surfaces");
        // A foreground plane is a wall between camera and source; heavy scattering must stop at it.
        int blocked = lightingPlane(manager, volume, 2);
        source(volume, false);
        int wall = lightingPlane(manager, volume, 2);
        if (Math.abs(brightness(blocked) - brightness(wall)) > 3) throw new AssertionError("Light leaks through a foreground wall");
        source(volume, true);
        int emitter = lightingPlane(manager, volume, 3.5F);
        source(volume, false);
        if (warmth(emitter) < warmth(lightingPlane(manager, volume, 3.5F)) + 5)
            throw new AssertionError("An emitter's own block face extinguishes its glow");
        source(volume, 8);
        if (brightness(lightingPlane(manager, volume, 6)) < brightness(surface))
            throw new AssertionError("Eight-source uniform upload loses lights");
        source(volume, true);
        scene(); manager.runPipeline(volume);
        try (var image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
            image.writeToFile(Minecraft.getInstance().gameDirectory.toPath().resolve("atmosphere-lighting-heavy.png"));
        }
        volume.setFloat("LightCorruption", 1);
        scene(); manager.runPipeline(volume);
        try (var image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
            image.writeToFile(Minecraft.getInstance().gameDirectory.toPath().resolve("atmosphere-lighting-sanity.png"));
        }
        Maniacrev.LOGGER.info("Atmosphere lighting PASSED: warm fog, heavy surfaces, sanity cooling/dimming, comfort, wall occlusion, solid emitters, eight sources");
    }

    private static int lightingPlane(foundry.veil.api.client.render.post.PostProcessingManager manager,
                                     foundry.veil.api.client.render.post.PostPipeline pipeline, float distance) {
        var target = Minecraft.getInstance().getMainRenderTarget();
        target.clear(Minecraft.ON_OSX); target.bindWrite(true);
        RenderSystem.enableDepthTest(); RenderSystem.depthMask(true);
        RenderSystem.disableCull(); RenderSystem.disableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        var b = Tesselator.getInstance().getBuilder(); b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float size = distance * 2;
        quad(b, -size, -size, -distance, size, -size, -distance, size, size, -distance, -size, size, -distance, .5F, .5F, .5F);
        BufferUploader.drawWithShader(b.end()); manager.runPipeline(pipeline);
        try (var image = Screenshot.takeScreenshot(target)) {
            return image.getPixelRGBA(image.getWidth() / 2, image.getHeight() / 2);
        }
    }

    private static int brightness(int pixel) { return (pixel & 255) + (pixel >> 8 & 255) + (pixel >> 16 & 255); }
    private static int warmth(int pixel) { return (pixel & 255) - (pixel >> 16 & 255); }

    /** Run the actual Lit-tag predicate and measure surface illumination with fog completely disabled. */
    private static void lighterChecks(foundry.veil.api.client.render.post.PostProcessingManager manager) throws Exception {
        var stack = new net.minecraft.world.item.ItemStack(org.example.maniacrevolution.ModItems.NIGHTMARE_LIGHTER.get());
        if (org.example.maniacrevolution.item.NightmareLighterItem.isLit(stack)) throw new AssertionError("Closed lighter emits light");
        stack.getOrCreateTag().putBoolean("Lit", true);
        if (!org.example.maniacrevolution.item.NightmareLighterItem.isLit(stack)) throw new AssertionError("Opened lighter has no light");
        var other = new net.minecraft.world.item.ItemStack(Blocks.STONE);
        other.getOrCreateTag().putBoolean("Lit", true);
        if (org.example.maniacrevolution.item.NightmareLighterItem.isLit(other)) throw new AssertionError("Non-lighter NBT activates hand light");
        for (var mode : new org.example.maniacrevolution.settings.GraphicsMode[]{
                org.example.maniacrevolution.settings.GraphicsMode.LIGHT,
                org.example.maniacrevolution.settings.GraphicsMode.NORMAL,
                org.example.maniacrevolution.settings.GraphicsMode.HEAVY}) {
            var pipeline = manager.getPipeline(mode.volumetric() ? AtmosphereClient.PIPELINE : AtmosphereClient.LIGHT_PIPELINE);
            pipeline.setFloat("Active", 1); pipeline.setFloat("Time", 12);
            pipeline.setFloat("Corruption", 0); pipeline.setFloat("LightCorruption", 0); pipeline.setFloat("Comfort", 1);
            pipeline.setFloat("Pursuit", 0); pipeline.setFloat("Hurt", 0); pipeline.setFloat("Fear", 0); pipeline.setFloat("Plague", 0);
            pipeline.setFloat("FogDensity", 0); pipeline.setFloat("SurfaceLight", 0);
            pipeline.setInt("FogSamples", mode.fogSamples());
            source(pipeline, false);
            int closed = lightingPlane(manager, pipeline, 3);
            handSource(pipeline, true);
            int burning = lightingPlane(manager, pipeline, 3);
            if (brightness(burning) < brightness(closed) + 12 || warmth(burning) < warmth(closed) + 4)
                throw new AssertionError("Lighter only tints fog instead of lighting a surface: " + mode);
            for (float time : new float[]{0, .5F, 1, 2, 5}) {
                pipeline.setFloat("Time", time);
                if (brightness(lightingPlane(manager, pipeline, 3)) < brightness(burning) - 2)
                    throw new AssertionError("Held lighter flashes then decays: " + mode);
            }
            pipeline.setFloat("FogDensity", .055F);
            scene(); manager.runPipeline(pipeline);
            try (var image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
                image.writeToFile(Minecraft.getInstance().gameDirectory.toPath().resolve("atmosphere-lighter-" + mode.name().toLowerCase(java.util.Locale.ROOT) + ".png"));
            }
            stack.getOrCreateTag().putBoolean("Lit", false);
            if (org.example.maniacrevolution.item.NightmareLighterItem.isLit(stack)) throw new AssertionError("Closed state still emits");
            handSource(pipeline, false);
        }
        Maniacrev.LOGGER.info("Lighter lighting PASSED: closed off, Lit tag, non-lighter rejection, surfaces without fog, stable LIGHT/NORMAL/HEAVY");
    }

    private static void handSource(foundry.veil.api.client.render.post.PostPipeline pipeline, boolean enabled) {
        source(pipeline, enabled);
        var position = new org.joml.Vector4f[]{new org.joml.Vector4f(0, -.3F, 0, 10)};
        var color = new org.joml.Vector4f[]{new org.joml.Vector4f(1, .63F, .28F, 1.35F)};
        pipeline.setVectors("LightPositionRadius", position);
        pipeline.setVectors("LightColorPower", color);
        pipeline.setInt("HandLightCount", enabled ? 1 : 0);
    }

    /** A nearby post must not paint a false shadow onto unobstructed fog elsewhere on screen. */
    private static void fogMaskChecks(foundry.veil.api.client.render.post.PostProcessingManager manager) throws Exception {
        var pipeline = manager.getPipeline(AtmosphereClient.PIPELINE);
        pipeline.setFloat("Active", 1); pipeline.setFloat("Time", 12);
        pipeline.setFloat("Corruption", 0); pipeline.setFloat("LightCorruption", 0);
        pipeline.setFloat("Pursuit", 0); pipeline.setFloat("Fear", 0); pipeline.setFloat("Hurt", 0); pipeline.setFloat("Plague", 0);
        pipeline.setFloat("FogDensity", .055F); pipeline.setFloat("FogGround", -20); pipeline.setFloat("Indoor", 1);
        pipeline.setInt("FogSamples", 24); source(pipeline, true);
        var sourcePosition = new org.joml.Vector4f[]{new org.joml.Vector4f(.5F, .5F, -6, 8)};
        pipeline.setVectors("LightPositionRadius", sourcePosition);
        int[] baseline = null;
        for (int heavy = 0; heavy <= 1; heavy++) {
            pipeline.setFloat("SurfaceLight", heavy);
            var target = Minecraft.getInstance().getMainRenderTarget();
            target.clear(Minecraft.ON_OSX); target.bindWrite(true);
            RenderSystem.enableDepthTest(); RenderSystem.depthMask(true);
            RenderSystem.disableCull(); RenderSystem.disableBlend();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            var b = Tesselator.getInstance().getBuilder(); b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            quad(b, -40, -40, -20, 40, -40, -20, 40, 40, -20, -40, 40, -20, .3F, .3F, .3F);
            quad(b, -.6F, -5, -3, -.3F, -5, -3, -.3F, 5, -3, -.6F, 5, -3, .2F, .2F, .2F);
            BufferUploader.drawWithShader(b.end()); manager.runPipeline(pipeline);
            try (var image = Screenshot.takeScreenshot(target)) {
                int[] pixels = new int[24];
                for (int i = 0; i < pixels.length; i++) {
                    float x = i < 12 ? .08F + i * .018F : .65F + (i - 12) * .02F;
                    pixels[i] = image.getPixelRGBA((int) (image.getWidth() * x), image.getHeight() / 2);
                    if (baseline != null) for (int shift : new int[]{0, 8, 16}) {
                        if (Math.abs((pixels[i] >> shift & 255) - (baseline[i] >> shift & 255)) > 2)
                            throw new AssertionError("Foreground geometry casts a fake screen-space fog shadow at sample " + i);
                    }
                }
                baseline = pixels;
                if (heavy == 1) image.writeToFile(Minecraft.getInstance().gameDirectory.toPath().resolve("atmosphere-fog-post-check.png"));
            }
        }
        Maniacrev.LOGGER.info("Fog mask PASSED: nearby post does not shadow unrelated fog rays");
    }

    /** The distant emitter is AFTER fifteen nearer lights: it must still light its own pixel in every quality. */
    private static void coloredLightsChecks(foundry.veil.api.client.render.post.PostProcessingManager manager) throws Exception {
        var red = org.example.maniacrevolution.atmosphere.client.AtmosphereLightPalette.color(Blocks.REDSTONE_TORCH.defaultBlockState());
        var cyan = org.example.maniacrevolution.atmosphere.client.AtmosphereLightPalette.color(Blocks.SOUL_LANTERN.defaultBlockState());
        var sea = org.example.maniacrevolution.atmosphere.client.AtmosphereLightPalette.color(Blocks.SEA_LANTERN.defaultBlockState());
        var flame = org.example.maniacrevolution.atmosphere.client.AtmosphereLightPalette.color(Blocks.TORCH.defaultBlockState());
        checkPalette(red != cyan && cyan != flame && sea != flame, "Different luminous materials share one color");
        for (var mode : org.example.maniacrevolution.settings.GraphicsMode.values()) {
            if (!mode.enabled()) continue;
            var pipeline = manager.getPipeline(mode.volumetric() ? AtmosphereClient.PIPELINE : AtmosphereClient.LIGHT_PIPELINE);
            pipeline.setFloat("Active", 1); pipeline.setFloat("Time", 12);
            pipeline.setFloat("Corruption", 0); pipeline.setFloat("LightCorruption", 0); pipeline.setFloat("Comfort", 1);
            pipeline.setFloat("Pursuit", 0); pipeline.setFloat("Fear", 0); pipeline.setFloat("Hurt", 0); pipeline.setFloat("Plague", 0);
            pipeline.setFloat("FogDensity", 0); pipeline.setInt("FogSamples", mode.fogSamples());
            pipeline.setFloat("SightFadeStart", 18); pipeline.setFloat("SightRange", 28);
            source(pipeline, false);
            pipeline.setFloat("SurfaceLight", 1); pipeline.setFloat("ContactShadows", mode.dynamicLights() ? 1 : 0);
            pipeline.setInt("LightSamples", mode.lightSamples());
            var positions = new org.joml.Vector4f[64];
            var colors = new org.joml.Vector4f[64];
            for (int i = 0; i < 64; i++) {
                positions[i] = new org.joml.Vector4f(); colors[i] = new org.joml.Vector4f();
                if (i < 15) { positions[i].set(2 + i * .05F, 0, -2, 1); colors[i].set(1, .65F, .3F, 1); }
            }
            positions[15].set(0, 0, -22, 8);
            pipeline.setInt("LightCount", 16);
            pipeline.setVectors("LightPositionRadius", positions);
            for (int tint : new int[]{red, cyan}) {
                colors[15].set((tint >> 16 & 255) / 255F, (tint >> 8 & 255) / 255F, (tint & 255) / 255F, 1.35F);
                pipeline.setVectors("LightColorPower", colors);
                int pixel = lightingPlane(manager, pipeline, 24);
                int r = pixel & 255, g = pixel >> 8 & 255, b = pixel >> 16 & 255;
                if (tint == red) checkPalette(r > g + 8 && r > b + 8, "Red source is missing/orange: " + mode);
                else checkPalette(g > r + 6 && b > r + 6, "Soul source is missing/orange: " + mode);
            }
        }
        Maniacrev.LOGGER.info("Colored local lighting PASSED: red/cyan at 22 blocks after 15 near lights, LIGHT/NORMAL/HEAVY");
    }

    /** Compare a four-lamp row to its two-point representation using pixels from the real shader. */
    private static void rowLightingChecks(foundry.veil.api.client.render.post.PostProcessingManager manager) throws Exception {
        for (var name : new net.minecraft.resources.ResourceLocation[]{AtmosphereClient.LIGHT_PIPELINE, AtmosphereClient.PIPELINE}) {
            var pipeline = manager.getPipeline(name);
            source(pipeline, false);
            pipeline.setFloat("Active", 1); pipeline.setFloat("Time", 12);
            pipeline.setFloat("Corruption", 0); pipeline.setFloat("LightCorruption", 0); pipeline.setFloat("Comfort", 1);
            pipeline.setFloat("Pursuit", 0); pipeline.setFloat("Fear", 0); pipeline.setFloat("Hurt", 0); pipeline.setFloat("Plague", 0);
            pipeline.setFloat("FogDensity", 0); pipeline.setFloat("SurfaceLight", 1); pipeline.setFloat("ContactShadows", 0);
            int[] unlit = rowPixels(manager, pipeline);
            var points = new org.joml.Vector4f[4];
            var powers = new org.joml.Vector4f[4];
            for (int i = 0; i < 4; i++) {
                points[i] = new org.joml.Vector4f(i - 1.5F, .5F, -4, 8);
                powers[i] = new org.joml.Vector4f(1, .63F, .28F, 1);
            }
            pipeline.setInt("LightCount", 4);
            pipeline.setVectors("LightPositionRadius", points); pipeline.setVectors("LightColorPower", powers);
            int[] individual = rowPixels(manager, pipeline);
            pipeline.setInt("LightCount", 1);
            pipeline.setVectors("LightPositionRadius", new org.joml.Vector4f[]{new org.joml.Vector4f(0, .5F, -4, 8)});
            pipeline.setVectors("LightColorPower", new org.joml.Vector4f[]{new org.joml.Vector4f(1, .63F, .28F, 4)});
            pipeline.setVectors("LightExtent", new org.joml.Vector4f[]{new org.joml.Vector4f(1.5F, 0, 0, 4)});
            int[] grouped = rowPixels(manager, pipeline);
            for (int i = 0; i < grouped.length; i++) {
                if (brightness(grouped[i]) <= brightness(unlit[i]) + 10)
                    throw new AssertionError("Grouped row lost illumination at its center/end: " + i);
                if (Math.abs(brightness(grouped[i]) - brightness(individual[i])) > 24)
                    throw new AssertionError("Grouped row changed brightness excessively: " + i);
            }
            source(pipeline, false);
        }
        Maniacrev.LOGGER.info("Lamp rows render PASSED: four blocks in one slot retain center/end illumination in both shaders");
    }

    private static int[] rowPixels(foundry.veil.api.client.render.post.PostProcessingManager manager,
                                   foundry.veil.api.client.render.post.PostPipeline pipeline) {
        lightingPlane(manager, pipeline, 6);
        try (var image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
            return new int[]{image.getPixelRGBA(image.getWidth() / 2, image.getHeight() / 2),
                    image.getPixelRGBA(image.getWidth() / 3, image.getHeight() / 2),
                    image.getPixelRGBA(image.getWidth() * 2 / 3, image.getHeight() / 2)};
        }
    }

    /** Grazing view of adjacent luminous blocks: their depth must not project boxes into open fog. */
    private static void ceilingLightChecks(foundry.veil.api.client.render.post.PostProcessingManager manager) throws Exception {
        var pipeline = manager.getPipeline(AtmosphereClient.PIPELINE);
        var rotation = new Matrix4f().rotationX((float) Math.toRadians(-22));
        var camera = VeilRenderSystem.renderer().getCameraMatrices();
        camera.update(RenderSystem.getProjectionMatrix(), rotation, new Vector3f(0, 2, 4), .05F, 160);
        pipeline.setFloat("Active", 1); pipeline.setFloat("Time", 12);
        pipeline.setFloat("Corruption", 0); pipeline.setFloat("LightCorruption", 0); pipeline.setFloat("Comfort", 1);
        pipeline.setFloat("Pursuit", 0); pipeline.setFloat("Fear", 0); pipeline.setFloat("Hurt", 0); pipeline.setFloat("Plague", 0);
        pipeline.setFloat("FogDensity", .055F); pipeline.setFloat("FogGround", -20); pipeline.setFloat("Indoor", 1);
        pipeline.setInt("FogSamples", 24);
        source(pipeline, false);
        pipeline.setInt("LightCount", 4); pipeline.setInt("LightSamples", 8);
        var positions = new org.joml.Vector4f[4];
        var colors = new org.joml.Vector4f[4];
        var extents = new org.joml.Vector4f[4];
        for (int i = 0; i < 4; i++) {
            var p = new Vector3f(i < 2 ? -2 : 3, 3.3F, -6.5F - i % 2 - (i < 2 ? 0 : 5));
            positions[i] = new org.joml.Vector4f(p.x, p.y, p.z, 9.25F);
            colors[i] = new org.joml.Vector4f(1, .76F, .45F, 2);
            // Each 2x2 fixture consists of two touching rows; use the same packed metadata as the uploader.
            extents[i] = new org.joml.Vector4f(.5F, 0, 0, 2 | (((i ^ 1) + 1) << 3));
        }
        pipeline.setVectors("LightPositionRadius", positions); pipeline.setVectors("LightColorPower", colors);
        pipeline.setVectors("LightExtent", extents);
        int[] clearFog = null;
        int changed = 0, maximum = 0;
        for (int surface = 0; surface <= 1; surface++) for (int shadows = 0; shadows <= 1; shadows++) {
            ceilingScene(rotation);
            pipeline.setFloat("SurfaceLight", surface); pipeline.setFloat("ContactShadows", shadows);
            manager.runPipeline(pipeline);
            try (var image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
                int width = image.getWidth(), height = image.getHeight();
                if (surface == 0 && shadows == 0) clearFog = new int[width * height];
                for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
                    int pixel = image.getPixelRGBA(x, y), at = y * width + x;
                    if (surface == 0 && shadows == 0) clearFog[at] = pixel;
                    if (surface == 0 && shadows == 1) {
                        int difference = 0;
                        for (int shift : new int[]{0, 8, 16}) difference = Math.max(difference,
                                Math.abs((pixel >> shift & 255) - (clearFog[at] >> shift & 255)));
                        maximum = Math.max(maximum, difference);
                        if (difference > 8) changed++;
                    }
                }
                image.writeToFile(Minecraft.getInstance().gameDirectory.toPath().resolve("atmosphere-ceiling-"
                        + (surface == 0 ? "fog" : "surface") + (shadows == 0 ? "-clear.png" : "-shadow.png")));
            }
        }
        camera.update(RenderSystem.getProjectionMatrix(), new Matrix4f(), new Vector3f(0, 2, 4), .05F, 160);
        Maniacrev.LOGGER.info("Ceiling emitter diagnostics: {} fog pixels differ by >8, maximum {}", changed, maximum);
        if (changed > 200) throw new AssertionError("Adjacent ceiling emitters project rectangular shadows into fog: " + changed + " pixels, max=" + maximum);
        source(pipeline, false);
        Maniacrev.LOGGER.info("Ceiling emitters PASSED: two 2x2 lamps at a grazing angle do not project boxes into fog");
    }

    private static void ceilingScene(Matrix4f rotation) {
        var target = Minecraft.getInstance().getMainRenderTarget();
        target.clear(Minecraft.ON_OSX); target.bindWrite(true);
        RenderSystem.enableDepthTest(); RenderSystem.depthMask(true);
        RenderSystem.disableCull(); RenderSystem.disableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        var b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        ceilingBox(b, rotation, -12, 3.8F, -28, 24, 1, 30, .26F, .28F, .25F);
        ceilingBox(b, rotation, -12, -3, -28, 24, .2F, 30, .25F, .27F, .24F);
        ceilingBox(b, rotation, -12, -3, -28, 24, 7, .2F, .22F, .24F, .22F);
        ceilingBox(b, rotation, -8, -3, -28, .2F, 7, 30, .24F, .26F, .23F);
        ceilingBox(b, rotation, 8, -3, -28, .2F, 7, 30, .24F, .26F, .23F);
        for (int fixture = 0; fixture < 2; fixture++) for (int x = 0; x < 2; x++) for (int z = 0; z < 2; z++)
            ceilingBox(b, rotation, (fixture == 0 ? -3 : 2) + x, 2.8F, -7 - z - fixture * 5,
                    1, 1, 1, .95F, .78F, .42F);
        BufferUploader.drawWithShader(b.end());
    }

    private static void ceilingBox(BufferBuilder b, Matrix4f rotation, float x, float y, float z,
                                   float sx, float sy, float sz, float r, float g, float blue) {
        float[][] points = {{x,y,z}, {x+sx,y,z}, {x+sx,y+sy,z}, {x,y+sy,z},
                {x,y,z+sz}, {x+sx,y,z+sz}, {x+sx,y+sy,z+sz}, {x,y+sy,z+sz}};
        for (int[] face : new int[][]{{0,1,2,3}, {5,4,7,6}, {4,0,3,7}, {1,5,6,2}, {3,2,6,7}, {4,5,1,0}})
            for (int vertex : face) b.vertex(rotation, points[vertex][0], points[vertex][1], points[vertex][2]).color(r,g,blue,1F).endVertex();
    }

    private static void checkPalette(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }

    /** Intersecting colored halos must not acquire circles/seams from a changing top-N selection. */
    private static void haloContinuityChecks(foundry.veil.api.client.render.post.PostProcessingManager manager) throws Exception {
        for (var mode : org.example.maniacrevolution.settings.GraphicsMode.values()) {
            if (!mode.enabled()) continue;
            var pipeline = manager.getPipeline(mode.volumetric() ? AtmosphereClient.PIPELINE : AtmosphereClient.LIGHT_PIPELINE);
            source(pipeline, false);
            pipeline.setFloat("Active", 1); pipeline.setFloat("Time", 12);
            pipeline.setFloat("Corruption", 0); pipeline.setFloat("LightCorruption", 0);
            pipeline.setFloat("Fear", 0); pipeline.setFloat("Pursuit", 0); pipeline.setFloat("Hurt", 0); pipeline.setFloat("Plague", 0);
            pipeline.setFloat("FogDensity", .055F); pipeline.setFloat("FogGround", -20); pipeline.setFloat("Indoor", 1);
            pipeline.setFloat("SurfaceLight", 1); pipeline.setFloat("ContactShadows", 0);
            pipeline.setInt("FogSamples", mode.fogSamples()); pipeline.setInt("LightSamples", mode.lightSamples());
            var positions = new org.joml.Vector4f[64];
            var colors = new org.joml.Vector4f[64];
            for (int i = 0; i < 64; i++) {
                positions[i] = new org.joml.Vector4f(); colors[i] = new org.joml.Vector4f();
                if (i < 12) {
                    positions[i].set(i % 2 == 0 ? -4 : 4, 0, -4 - (i / 2) * 2, 9);
                    colors[i].set(i % 2 == 0 ? 1 : .3F, i % 2 == 0 ? .15F : .9F, i % 2 == 0 ? .2F : 1, .8F);
                }
            }
            pipeline.setInt("LightCount", 12);
            pipeline.setVectors("LightPositionRadius", positions); pipeline.setVectors("LightColorPower", colors);
            lightingPlane(manager, pipeline, 30);
            try (var image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
                int maximum = 0;
                for (int y = image.getHeight() / 3; y <= image.getHeight() * 2 / 3; y += image.getHeight() / 12) {
                    for (int x = image.getWidth() / 10 + 1; x < image.getWidth() * 9 / 10; x++) {
                        int a = image.getPixelRGBA(x - 1, y), b = image.getPixelRGBA(x, y);
                        for (int shift : new int[]{0, 8, 16}) maximum = Math.max(maximum,
                                Math.abs((a >> shift & 255) - (b >> shift & 255)));
                    }
                }
                if (maximum > 4) throw new AssertionError("Intersecting halos have a hard edge: " + mode + ", jump=" + maximum);
                image.writeToFile(Minecraft.getInstance().gameDirectory.toPath().resolve("atmosphere-colored-halos-"
                        + mode.name().toLowerCase(java.util.Locale.ROOT) + ".png"));
            }
        }
        Maniacrev.LOGGER.info("Colored halo continuity PASSED: twelve intersecting red/cyan lights have no hard seams in three qualities");
    }

    private static int planeContrast(foundry.veil.api.client.render.post.PostProcessingManager manager,
                                     foundry.veil.api.client.render.post.PostPipeline pipeline, float distance) {
        int[] pixels = new int[2];
        var mc = Minecraft.getInstance(); var target = mc.getMainRenderTarget();
        for (int source = 0; source <= 1; source++) {
            target.clear(Minecraft.ON_OSX); target.bindWrite(true);
            RenderSystem.enableDepthTest(); RenderSystem.depthMask(true);
            RenderSystem.disableCull(); RenderSystem.disableBlend();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            var b = Tesselator.getInstance().getBuilder(); b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            float size = distance * 2;
            quad(b, -size, -size, -distance, size, -size, -distance, size, size, -distance, -size, size, -distance, source, source, source);
            BufferUploader.drawWithShader(b.end()); manager.runPipeline(pipeline);
            try (var image = Screenshot.takeScreenshot(target)) {
                pixels[source] = image.getPixelRGBA(image.getWidth() / 2, image.getHeight() / 2);
            }
        }
        int contrast = 0;
        for (int shift : new int[]{0, 8, 16}) contrast = Math.max(contrast,
                Math.abs((pixels[0] >> shift & 255) - (pixels[1] >> shift & 255)));
        return contrast;
    }

    /** Exercise the installed GLFW callbacks that previously called ImGui without a context. */
    private static void inputCallbacks(long window) {
        if (foundry.veil.Veil.IMGUI) throw new AssertionError("Veil debug editor must be disabled for the prototype");
        var scroll = GLFW.glfwSetScrollCallback(window, null);
        GLFW.glfwSetScrollCallback(window, scroll);
        var mouse = GLFW.glfwSetMouseButtonCallback(window, null);
        GLFW.glfwSetMouseButtonCallback(window, mouse);
        var key = GLFW.glfwSetKeyCallback(window, null);
        GLFW.glfwSetKeyCallback(window, key);
        if (scroll == null || mouse == null || key == null) throw new AssertionError("Missing Minecraft input callbacks");
        scroll.invoke(window, 0, 1);
        scroll.invoke(window, 0, -1);
        mouse.invoke(window, GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_RELEASE, 0);
        key.invoke(window, GLFW.GLFW_KEY_F12, 0, GLFW.GLFW_RELEASE, 0);
        Maniacrev.LOGGER.info("Atmosphere input probe PASSED: scroll up/down, mouse release, key release; ImGui editor disabled");
    }

    private static void scene() {
        var mc = Minecraft.getInstance();
        var target = mc.getMainRenderTarget();
        target.setClearColor(.19F, .23F, .28F, 1);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableCull();
        RenderSystem.disableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        var b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        quad(b, -35, -2, -100, 35, -2, -100, 35, -2, 2, -35, -2, 2, .27F, .29F, .27F);
        quad(b, -9, -2, -100, -9, 7, -100, -9, 7, 2, -9, -2, 2, .26F, .30F, .34F);
        quad(b, 9, -2, 2, 9, 7, 2, 9, 7, -100, 9, -2, -100, .30F, .28F, .26F);
        for (int z = -8; z >= -80; z -= 12) {
            quad(b, -6, -2, z, -4, -2, z, -4, 4, z, -6, 4, z, .44F, .40F, .33F);
            quad(b, 4, -2, z, 6, -2, z, 6, 4, z, 4, 4, z, .33F, .40F, .44F);
            quad(b, -7, 4, z, 7, 4, z, 7, 4.6F, z, -7, 4.6F, z, .22F, .24F, .28F);
        }
        quad(b, -.15F, -.2F, -6, .15F, -.2F, -6, .15F, .2F, -6, -.15F, .2F, -6, 1, .66F, .16F);
        BufferUploader.drawWithShader(b.end());
        // Use actual Minecraft materials so desaturation/detail loss is visible in QA.
        var buffers = mc.renderBuffers().bufferSource();
        var pose = new PoseStack();
        for (int z : new int[]{-5, -13, -25, -41}) for (int side : new int[]{-1, 1}) {
            for (int y = 0; y < 5; y++) {
                pose.pushPose();
                pose.translate(side == -1 ? -4 : 3, y - 2, z);
                mc.getBlockRenderer().renderSingleBlock((y == 0 || y == 4 ? Blocks.CHISELED_STONE_BRICKS : Blocks.DEEPSLATE_BRICKS)
                                .defaultBlockState(), pose, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                pose.popPose();
            }
        }
        for (int z = -3; z >= -28; z--) for (int x = -3; x < 3; x++) {
            pose.pushPose(); pose.translate(x, -3, z);
            mc.getBlockRenderer().renderSingleBlock(((x + z) % 2 == 0 ? Blocks.POLISHED_DEEPSLATE : Blocks.STONE_BRICKS)
                            .defaultBlockState(), pose, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        buffers.endBatch();
    }

    private static void quad(BufferBuilder b, float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4, float r, float g, float blue) {
        b.vertex(x1, y1, z1).color(r, g, blue, 1F).endVertex();
        b.vertex(x2, y2, z2).color(r, g, blue, 1F).endVertex();
        b.vertex(x3, y3, z3).color(r, g, blue, 1F).endVertex();
        b.vertex(x4, y4, z4).color(r, g, blue, 1F).endVertex();
    }
}
