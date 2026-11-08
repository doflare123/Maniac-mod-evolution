package org.example.maniacrevolution.client.renderer;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.Arrays;

/** Opt-in GPU previews of the runtime hand renderer, without loading or editing a world. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class SyringeClientRenderProbe {
    private static boolean done;
    private static int frames;

    @SubscribeEvent
    public static void render(TickEvent.RenderTickEvent event) {
        var mc = Minecraft.getInstance();
        if (!Boolean.getBoolean("maniacrev.syringe.renderProbe") || done || event.phase != TickEvent.Phase.END
                || mc.screen == null || mc.getOverlay() != null || ++frames < 20) return;
        done = true;
        var target = new TextureTarget(960, 540, true, Minecraft.ON_OSX);
        var projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        var sorting = RenderSystem.getVertexSorting();
        var view = RenderSystem.getModelViewStack();
        view.pushPose();
        try {
            RenderSystem.setProjectionMatrix(new Matrix4f().perspective((float) Math.toRadians(70), 960F / 540, .05F, 100), VertexSorting.DISTANCE_TO_ORIGIN);
            view.setIdentity();
            RenderSystem.applyModelViewMatrix();
            ResourceLocation skin = new ResourceLocation("minecraft", "textures/entity/player/wide/steve.png");
            for (String clip : new String[]{"idle", "run", "inject"}) for (boolean mirror : new boolean[]{false, true}) {
                float time = clip.equals("inject") ? 1.5F : .2F;
                var frame = new SyringeRenderEvents.Frame(clip, time, mirror);
                var model = new PlayerModel<AbstractClientPlayer>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
                model.young = false;
                clear(target);
                SyringeRenderEvents.renderFirstPerson(new PoseStack(), mc.renderBuffers().bufferSource(), LightTexture.FULL_BRIGHT,
                        model, skin, frame, true, true);
                save(target, "first-" + clip + (mirror ? "-left" : "-right"));
                if (clip.equals("idle")) {
                    int[] baseline = null;
                    for (int direction = 0; direction < 2; direction++) {
                        clear(target);
                        float sign = direction == 0 ? 1 : -1;
                        RenderSystem.setShaderLights(new Vector3f(sign, .2F, .3F).normalize(),
                                new Vector3f(.1F, sign, -.3F).normalize());
                        SyringeRenderEvents.renderFirstPerson(new PoseStack(), mc.renderBuffers().bufferSource(), LightTexture.FULL_BRIGHT,
                                model, skin, frame, true, true);
                        int[] pixels = pixels(target);
                        if (baseline == null) baseline = pixels;
                        else if (!Arrays.equals(baseline, pixels)) throw new AssertionError("Directional lights affect first-person syringe/skin");
                    }
                }
                if (clip.equals("run")) {
                    int[] baseline = null;
                    int changed = 0;
                    for (int step = 0; step < 16; step++) {
                        clear(target);
                        var motion = new SyringeRenderEvents.Frame("run", step / 20F, mirror);
                        SyringeRenderEvents.renderFirstPerson(new PoseStack(), mc.renderBuffers().bufferSource(), LightTexture.FULL_BRIGHT,
                                model, skin, motion, true, true);
                        save(target, String.format(java.util.Locale.ROOT, "first-run-%s-frame-%02d", mirror ? "left" : "right", step));
                        int[] pixels = pixels(target);
                        if (baseline == null) baseline = pixels;
                        else for (int pixel = 0; pixel < pixels.length; pixel++) if (pixels[pixel] != baseline[pixel]) changed++;
                    }
                    if (changed < 5000) throw new AssertionError("Running hand is visually stationary");
                    Maniacrev.LOGGER.info("Syringe running motion {}: {} changed pixels; directional-light invariance passed", mirror ? "left" : "right", changed);
                }
                if (clip.equals("inject")) continue;
                for (boolean side : new boolean[]{false, true}) {
                    clear(target);
                    model.rightArm.xRot = (float) Math.toRadians(clip.equals("run") ? -33 : -18);
                    model.leftArm.xRot = (float) Math.toRadians(clip.equals("run") ? 30 : 0);
                    model.rightArm.zRot = .05F;
                    model.leftArm.zRot = -.05F;
                    if (mirror) {
                        float right = model.rightArm.xRot;
                        model.rightArm.xRot = model.leftArm.xRot;
                        model.leftArm.xRot = right;
                    }
                    var pose = new PoseStack();
                    pose.translate(0, .6, -2.3);
                    pose.mulPose(Axis.YP.rotationDegrees(side ? 270 : 180));
                    pose.scale(-1, -1, 1);
                    model.renderToBuffer(pose, mc.renderBuffers().bufferSource().getBuffer(RenderType.entityTranslucent(skin)),
                            LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
                    pose.pushPose();
                    (mirror ? model.leftArm : model.rightArm).translateAndRotate(pose);
                    SyringeItemRenderer.held(pose, mc.renderBuffers().bufferSource(), LightTexture.FULL_BRIGHT, frame);
                    pose.popPose();
                    save(target, "third-" + clip + (mirror ? "-left" : "-right") + (side ? "-side" : "-front"));
                }
            }
            Maniacrev.LOGGER.info("Syringe runtime render probe passed");
        } catch (Exception | AssertionError error) {
            Maniacrev.LOGGER.error("Syringe runtime render probe failed", error);
        } finally {
            view.popPose();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(projection, sorting);
            mc.getMainRenderTarget().bindWrite(true);
            target.destroyBuffers();
            mc.stop();
        }
    }

    private static void clear(TextureTarget target) {
        target.setClearColor(.08F, .10F, .13F, 1);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        RenderSystem.enableDepthTest();
        com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
    }

    private static void save(TextureTarget target, String name) throws java.io.IOException {
        var mc = Minecraft.getInstance();
        mc.renderBuffers().bufferSource().endBatch();
        try (var image = Screenshot.takeScreenshot(target)) {
            image.writeToFile(mc.gameDirectory.toPath().resolve("syringe-" + name + ".png"));
        }
    }

    private static int[] pixels(TextureTarget target) {
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
        try (var image = Screenshot.takeScreenshot(target)) {
            int[] result = new int[image.getWidth() * image.getHeight()];
            for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++)
                result[y * image.getWidth() + x] = image.getPixelRGBA(x, y);
            return result;
        }
    }
}
