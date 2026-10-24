package org.example.maniacrevolution.forester;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.*;
import org.joml.Matrix4f;

/** Opt-in actual Minecraft model/texture/hand-transform render, without opening a world. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class ForesterClientRenderProbe {
    private static boolean done;
    private static int frames;
    @SubscribeEvent public static void render(TickEvent.RenderTickEvent event) {
        var mc = Minecraft.getInstance();
        if (!Boolean.getBoolean("maniacrev.forester.renderProbe") || done || event.phase != TickEvent.Phase.END
                || mc.screen == null || mc.getOverlay() != null || ++frames < 20) return;
        done = true;
        var target = new TextureTarget(960, 540, true, Minecraft.ON_OSX);
        var previousProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        var sorting = RenderSystem.getVertexSorting(); var view = RenderSystem.getModelViewStack(); view.pushPose();
        try {
            RenderSystem.setProjectionMatrix(new Matrix4f().perspective((float) Math.toRadians(70), 960F / 540, .05F, 100), VertexSorting.DISTANCE_TO_ORIGIN);
            view.setIdentity(); RenderSystem.applyModelViewMatrix();
            for (String item : new String[]{"trap", "flashlight"}) for (HumanoidArm arm : HumanoidArm.values()) {
                target.setClearColor(0, 0, 0, 1); target.clear(Minecraft.ON_OSX); target.bindWrite(true);
                RenderSystem.enableDepthTest(); com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
                var stack = new ItemStack(item.equals("trap") ? ModItems.FORESTER_TRAP.get() : ModItems.FORESTER_FLASHLIGHT.get());
                if (mc.getItemRenderer().getModel(stack, null, null, 0) == mc.getModelManager().getMissingModel()) throw new AssertionError("Missing item model " + item);
                var pose = new PoseStack();
                boolean applied = IClientItemExtensions.of(stack).applyForgeHandTransform(pose, null, arm, stack, 0, 0, 0);
                if (!applied) throw new AssertionError("Missing forester hand transform");
                boolean left = arm == HumanoidArm.LEFT;
                mc.getItemRenderer().renderStatic(null, stack, left ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                        left, pose, mc.renderBuffers().bufferSource(), null, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
                mc.renderBuffers().bufferSource().endBatch();
                try (var image = Screenshot.takeScreenshot(target)) {
                    int visible = 0;
                    for (int x = 0; x < image.getWidth(); x++) for (int y = 0; y < image.getHeight(); y++)
                        if ((image.getPixelRGBA(x, y) & 0xFFFFFF) != 0) visible++;
                    if (visible < 500) throw new AssertionError(item + " invisible in " + arm + ": " + visible + " pixels");
                    image.writeToFile(mc.gameDirectory.toPath().resolve("forester-" + item + "-" + arm.name().toLowerCase(java.util.Locale.ROOT) + ".png"));
                    Maniacrev.LOGGER.info("Forester render probe {} {}: {} visible pixels", item, arm, visible);
                }
            }
            Maniacrev.LOGGER.info("Forester hand render checks passed");
        } catch (Exception | AssertionError error) {
            Maniacrev.LOGGER.error("Forester render probe failed", error);
        } finally {
            view.popPose(); RenderSystem.applyModelViewMatrix(); RenderSystem.setProjectionMatrix(previousProjection, sorting);
            mc.getMainRenderTarget().bindWrite(true); target.destroyBuffers(); mc.stop();
        }
    }
}
