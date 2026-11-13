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
                pipeline.setFloat("LampStrength", variant.endsWith("lamp") ? 1 : 0);
                pipeline.setVector("LampPosition", 0, 2, -6);
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
                        if (red <= green || green <= blue + 15) throw new AssertionError("Atmosphere lost the warm light: " + variant);
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
            pipeline.setFloat("LampStrength", 0);
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
            Maniacrev.LOGGER.info("Atmosphere render probe PASSED: input, seven variants, shared base, warm light, volumetric fog, 28-block extinction");
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

    /** A white source and black source at the same point must become indistinguishable. */
    private static void visibilityChecks(foundry.veil.api.client.render.post.PostProcessingManager manager,
                                         foundry.veil.api.client.render.post.PostPipeline pipeline) throws java.io.IOException {
        pipeline.setFloat("Active", 1); pipeline.setFloat("Corruption", 0); pipeline.setFloat("Pursuit", 0);
        pipeline.setFloat("Fear", 0); pipeline.setFloat("Hurt", 0); pipeline.setFloat("Plague", 0);
        pipeline.setFloat("LampStrength", 0); pipeline.setFloat("FogDensity", .055F);
        for (int samples : new int[]{8, 12, 24}) for (int indoor = 0; indoor <= 1; indoor++) {
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
        pipeline.setFloat("LampStrength", 1);
        if (planeContrast(manager, pipeline, 30) > 2) throw new AssertionError("Lamp reveals distant source");
        Maniacrev.LOGGER.info("Fog range PASSED: 6 blocks readable, 20 fading, 28/30/100 hidden; 8/12/24 samples, indoors, above low fog, lamp");
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
