package org.example.maniacrevolution.atmosphere;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import foundry.veil.api.client.render.VeilRenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.GameRenderer;
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
            for (String variant : new String[]{"vanilla", "survivor", "sanity", "keeper", "chase", "lamp"}) {
                scene();
                pipeline.setFloat("Active", variant.equals("vanilla") ? 0 : 1);
                pipeline.setFloat("Time", 12.0F);
                pipeline.setFloat("Corruption", variant.equals("sanity") ? .95F : 0);
                pipeline.setFloat("Keeper", variant.equals("keeper") ? 1 : 0);
                pipeline.setFloat("Hunter", variant.equals("keeper") ? 1 : 0);
                pipeline.setFloat("Pursuit", variant.equals("chase") ? 1 : 0);
                pipeline.setFloat("FogGround", 0);
                pipeline.setFloat("FogDensity", .024F);
                pipeline.setInt("FogSamples", 12);
                pipeline.setFloat("Hurt", 0);
                pipeline.setFloat("Fear", 0);
                pipeline.setFloat("Plague", 0);
                pipeline.setFloat("LampStrength", variant.equals("lamp") ? 1 : 0);
                pipeline.setVector("LampPosition", 0, 2, -6);
                manager.runPipeline(pipeline);
                try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                    double sum = 0;
                    for (int y = 0; y < image.getHeight(); y += 3) for (int x = 0; x < image.getWidth(); x += 3) {
                        int pixel = image.getPixelRGBA(x, y);
                        sum += (pixel & 255) + ((pixel >> 8) & 255) + ((pixel >> 16) & 255);
                    }
                    if (sum < 10000) throw new AssertionError("Black output: " + variant);
                    if (variant.equals("vanilla")) baseline = sum;
                    else if (Math.abs(sum - baseline) < 100) throw new AssertionError("Effect missing: " + variant);
                    image.writeToFile(mc.gameDirectory.toPath().resolve("atmosphere-" + variant + ".png"));
                    Maniacrev.LOGGER.info("Atmosphere render probe {}: luminance sum {}", variant, sum);
                }
            }
            Maniacrev.LOGGER.info("Atmosphere render probe PASSED: native mouse/keyboard callbacks, six variants, real shader compiled");
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
    }

    private static void quad(BufferBuilder b, float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4, float r, float g, float blue) {
        b.vertex(x1, y1, z1).color(r, g, blue, 1F).endVertex();
        b.vertex(x2, y2, z2).color(r, g, blue, 1F).endVertex();
        b.vertex(x3, y3, z3).color(r, g, blue, 1F).endVertex();
        b.vertex(x4, y4, z4).color(r, g, blue, 1F).endVertex();
    }
}
