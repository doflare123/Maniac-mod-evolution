package org.example.maniacrevolution.scp173;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.nio.file.Path;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.RemotePlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.data.ClientKeeperFormData;
import org.example.maniacrevolution.scp173.client.Scp173PlayerForm;
import org.example.maniacrevolution.warden.client.WardenPlayerForm;
import org.joml.Matrix4f;

/** Opt-in engine render check; source is excluded from the shipped mod and normal runClient. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class Scp173ClientRenderProbe {
    private static boolean done;
    @SubscribeEvent public static void frame(TickEvent.RenderTickEvent event) {
        var mc = Minecraft.getInstance();
        if (!Boolean.getBoolean("maniacrev.scp173.renderProbe") || done || event.phase != TickEvent.Phase.END
                || mc.level == null || mc.player == null || mc.player.tickCount < 20) return;
        done = true;
        var player = new RemotePlayer(mc.level, new GameProfile(UUID.randomUUID(), "StatueProbe"));
        ClientKeeperFormData.setPreviewKeeper(player, false);
        WardenPlayerForm.setPreviewWarden(player, false);
        Scp173PlayerForm.setPreviewScp173(player, true);
        try {
            var renderer = Scp173PlayerForm.class.getDeclaredField("renderer"); renderer.setAccessible(true);
            Maniacrev.LOGGER.info("SCP-173 probe: renderer={}, preview={}, alive={}, spectator={}",
                    renderer.get(null), Scp173PlayerForm.isPreview(player), player.isAlive(), player.isSpectator());
        } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
        var target = new TextureTarget(768, 512, true, Minecraft.ON_OSX);
        var projection = new Matrix4f(RenderSystem.getProjectionMatrix()); var sorting = RenderSystem.getVertexSorting();
        var view = RenderSystem.getModelViewStack(); view.pushPose();
        try {
            target.setClearColor(0.05F, 0.07F, 0.10F, 1); target.clear(Minecraft.ON_OSX); target.bindWrite(true);
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, 768, 512, 0, 1000, 21000), VertexSorting.ORTHOGRAPHIC_Z);
            view.setIdentity(); view.translate(0, 0, -10000); RenderSystem.applyModelViewMatrix();
            var graphics = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
            InventoryScreen.renderEntityInInventoryFollowsAngle(graphics, 192, 440, 170, 0, 0, player);
            // Exercise the distinct local-player/world-identity branch used in third person.
            Scp173PlayerForm.update(mc.player.getUUID(), true);
            InventoryScreen.renderEntityInInventoryFollowsAngle(graphics, 576, 440, 170, 0, 0, mc.player);
            graphics.drawCenteredString(mc.font, "Preview", 192, 468, 0xFFFFFF);
            graphics.drawCenteredString(mc.font, "Local player form", 576, 468, 0xFFFFFF);
            graphics.flush();
            try (var image = Screenshot.takeScreenshot(target)) {
                image.writeToFile(Path.of("scp173-preview-probe.png"));
            }
            Maniacrev.LOGGER.info("SCP-173 render probe saved scp173-preview-probe.png");
            var swimmer = new RemotePlayer(mc.level, new GameProfile(UUID.randomUUID(), "SwimProbe")) {
                @Override public float getSwimAmount(float partial) { return 1; }
                @Override public boolean isInWater() { return true; }
                @Override public float getYRot() { return 90; }
            };
            ClientKeeperFormData.setPreviewKeeper(swimmer, false);
            WardenPlayerForm.setPreviewWarden(swimmer, false);
            Scp173PlayerForm.setPreviewScp173(swimmer, true);
            target.clear(Minecraft.ON_OSX); target.bindWrite(true);
            InventoryScreen.renderEntityInInventoryFollowsAngle(graphics, 384, 320, 170, 0, 0, swimmer);
            graphics.drawCenteredString(mc.font, "Rigid swimming pose", 384, 468, 0xFFFFFF);
            graphics.flush();
            try (var image = Screenshot.takeScreenshot(target)) { image.writeToFile(Path.of("scp173-swim-probe.png")); }
            target.clear(Minecraft.ON_OSX); target.bindWrite(true);
            org.example.maniacrevolution.scp173.client.Scp173BlinkOverlay.mask(graphics, 768, 512, 1);
            org.example.maniacrevolution.scp173.client.Scp173BlinkOverlay.panel(graphics, 768, 0.7, 0.4,
                    net.minecraft.network.chat.Component.literal("C"));
            try (var image = Screenshot.takeScreenshot(target)) {
                if ((image.getPixelRGBA(384, 42) & 0xFFFFFF) == 0 || (image.getPixelRGBA(384, 256) & 0xFFFFFF) != 0)
                    throw new AssertionError("Blink panel must remain visible above closed eyelids");
                image.writeToFile(Path.of("scp173-blink-probe.png"));
            }
            Maniacrev.LOGGER.info("SCP-173 swimming and closed-eye panel render probes passed");
        } catch (Exception error) {
            Maniacrev.LOGGER.error("SCP-173 render probe failed", error);
        } finally {
            view.popPose(); RenderSystem.applyModelViewMatrix(); RenderSystem.setProjectionMatrix(projection, sorting);
            mc.getMainRenderTarget().bindWrite(true); target.destroyBuffers();
            mc.stop();
        }
    }
}
