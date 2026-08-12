package org.example.maniacrevolution.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.pinkorchid.client.PinkOrchidClientHandler;
import org.example.maniacrevolution.util.PlayerModeUtil;

/** Миниатюрная орхидея над HUD: лепестки опадают по мере шестисекундной записи. */
public final class PinkOrchidRecordingOverlay implements IGuiOverlay {
    public static final PinkOrchidRecordingOverlay INSTANCE =
            new PinkOrchidRecordingOverlay();

    private static final ResourceLocation ORCHID_BASE = new ResourceLocation(
            Maniacrev.MODID, "textures/gui/pink_orchid_base.png");
    private static final ResourceLocation ORCHID_PETAL = new ResourceLocation(
            Maniacrev.MODID, "textures/gui/pink_orchid_petal.png");
    private static final int WIDGET_WIDTH = 38;
    private static final int WIDGET_HEIGHT = 34;
    private static final int ORCHID_SIZE = 24;
    private static final int PETAL_SIZE = 7;
    private static final int WIDGET_GAP_ABOVE_HUD = 5;
    private static final int PETAL_COUNT = 6;
    private static final float[] PETAL_X = {0.0F, 3.5F, 3.0F, -0.5F, -4.0F, -3.5F};
    private static final float[] PETAL_Y = {-4.0F, -2.0F, 2.0F, 3.0F, 2.0F, -2.0F};
    private static final float[] PETAL_ROTATION = {0.0F, 58.0F, 118.0F,
            180.0F, 238.0F, 302.0F};

    private PinkOrchidRecordingOverlay() {
    }

    @Override
    public void render(ForgeGui forgeGui, GuiGraphics graphics, float partialTick,
                       int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null
                || minecraft.screen != null
                || !PlayerModeUtil.isSurvivalOrAdventure(minecraft.player)) return;

        if (!PinkOrchidClientHandler.isRecording()) return;

        int hudTop = screenHeight - CustomHud.getBottomContentTopOffset(
                minecraft.player, false);
        int x = screenWidth / 2 - WIDGET_WIDTH / 2;
        int y = hudTop - WIDGET_HEIGHT - WIDGET_GAP_ABOVE_HUD;

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 900.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        renderOrchidBase(graphics, x, y);
        renderPetals(graphics, x, y, partialTick);
        RenderSystem.disableBlend();
        graphics.pose().popPose();
    }

    private static void renderOrchidBase(GuiGraphics graphics, int x, int y) {
        int baseX = x + (WIDGET_WIDTH - ORCHID_SIZE) / 2;
        int baseY = y + WIDGET_HEIGHT - ORCHID_SIZE;
        graphics.blit(ORCHID_BASE, baseX, baseY, 0, 0,
                ORCHID_SIZE, ORCHID_SIZE, ORCHID_SIZE, ORCHID_SIZE);
    }

    private static void renderPetals(GuiGraphics graphics, int x, int y,
                                     float partialTick) {
        int duration = Math.max(1, PinkOrchidClientHandler.getRecordingDurationTicks());
        float elapsed = PinkOrchidClientHandler.getRecordingElapsedTicks() + partialTick;
        float petalDuration = duration / (float) PETAL_COUNT;
        float flowerCenterX = x + WIDGET_WIDTH / 2.0F;
        float flowerCenterY = y + WIDGET_HEIGHT - ORCHID_SIZE + 6.5F;
        for (int index = 0; index < PETAL_COUNT; index++) {
            float local = Mth.clamp((elapsed - index * petalDuration)
                    / petalDuration, 0.0F, 1.0F);
            if (local >= 1.0F) continue;

            float fall = local * local;
            float sway = Mth.sin(local * (float) Math.PI * 2.0F + index * 1.37F)
                    * (2.0F + index % 2) * local;
            float petalX = flowerCenterX + PETAL_X[index] + sway;
            float petalY = flowerCenterY + PETAL_Y[index]
                    + fall * (12.0F + index % 3 * 2.0F);
            float rotation = PETAL_ROTATION[index] + local * (90.0F + index * 13.0F);
            renderPetal(graphics, petalX, petalY, rotation);
        }
    }

    private static void renderPetal(GuiGraphics graphics, float centerX, float centerY,
                                    float rotation) {
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 1.0F);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(rotation));
        graphics.blit(ORCHID_PETAL, -PETAL_SIZE / 2, -PETAL_SIZE / 2,
                0, 0, PETAL_SIZE, PETAL_SIZE, PETAL_SIZE, PETAL_SIZE);
        graphics.pose().popPose();
    }
}
