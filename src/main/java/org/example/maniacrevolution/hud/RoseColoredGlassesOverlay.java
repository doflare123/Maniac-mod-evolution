package org.example.maniacrevolution.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/** Saturated animated filter rendered after the rest of the game HUD. */
public final class RoseColoredGlassesOverlay implements IGuiOverlay {
    public static final RoseColoredGlassesOverlay INSTANCE = new RoseColoredGlassesOverlay();

    private static final int BASE_ALPHA = 54;
    private static final int EDGE_ALPHA = 62;
    private static final int EDGE_LAYERS = 10;
    private static final int GLINT_ALPHA = 18;
    private static final int GLINT_BANDS = 4;

    private RoseColoredGlassesOverlay() {
    }

    @Override
    public void render(ForgeGui forgeGui, GuiGraphics graphics, float partialTick,
                       int screenWidth, int screenHeight) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        RoseColoredGlassesClientEvents.renderFilter(
                graphics, partialTick, screenWidth, screenHeight);
    }

    public static void draw(GuiGraphics graphics, int width, int height, float strength) {
        if (strength <= 0.001F) return;

        long now = Util.getMillis();
        float hueWave = 0.5F + 0.5F * Mth.sin(now / 650.0F);
        int topRgb = lerpRgb(0xFF167E, 0xD629FF, hueWave);
        int bottomRgb = lerpRgb(0xC81491, 0xFF5F9F, 1.0F - hueWave);

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 1200.0F);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        graphics.fillGradient(0, 0, width, height,
                withAlpha(topRgb, Math.round(BASE_ALPHA * strength)),
                withAlpha(bottomRgb, Math.round((BASE_ALPHA + 8) * strength)));
        drawLensEdges(graphics, width, height, strength);
        drawMovingGlints(graphics, width, height, strength, now);

        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        graphics.pose().popPose();
    }

    private static void drawLensEdges(GuiGraphics graphics, int width, int height,
                                      float strength) {
        int step = Math.max(1, Math.min(width, height) / 90);
        for (int layer = 0; layer < EDGE_LAYERS; layer++) {
            int inset = layer * step;
            int alpha = Math.round(EDGE_ALPHA * strength
                    * (EDGE_LAYERS - layer) / (float) EDGE_LAYERS);
            int color = withAlpha(0x5B075F, alpha);
            graphics.fill(inset, inset, width - inset, inset + step, color);
            graphics.fill(inset, height - inset - step, width - inset, height - inset, color);
            graphics.fill(inset, inset, inset + step, height - inset, color);
            graphics.fill(width - inset - step, inset, width - inset, height - inset, color);
        }
    }

    private static void drawMovingGlints(GuiGraphics graphics, int width, int height,
                                         float strength, long now) {
        float cycle = (now % 4200L) / 4200.0F;
        int diagonalLength = Math.max(width, height) * 2;
        int bandWidth = Math.max(5, width / 26);

        graphics.pose().pushPose();
        graphics.pose().translate(width / 2.0F, height / 2.0F, 1.0F);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(-17.0F));
        for (int band = 0; band < GLINT_BANDS; band++) {
            float bandCycle = (cycle + band / (float) GLINT_BANDS) % 1.0F;
            int x = Math.round(-diagonalLength / 2.0F + diagonalLength * bandCycle);
            int alpha = Math.round(GLINT_ALPHA * strength
                    * (0.65F + band * 0.1F));
            int color = withAlpha(band % 2 == 0 ? 0xFFFFFF : 0x67D9FF, alpha);
            graphics.fill(x, -diagonalLength / 2,
                    x + bandWidth, diagonalLength / 2, color);
        }
        graphics.pose().popPose();
    }

    private static int withAlpha(int rgb, int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (rgb & 0x00FFFFFF);
    }

    private static int lerpRgb(int first, int second, float progress) {
        float t = Mth.clamp(progress, 0.0F, 1.0F);
        int red = Math.round(Mth.lerp(t, (first >> 16) & 0xFF, (second >> 16) & 0xFF));
        int green = Math.round(Mth.lerp(t, (first >> 8) & 0xFF, (second >> 8) & 0xFF));
        int blue = Math.round(Mth.lerp(t, first & 0xFF, second & 0xFF));
        return (red << 16) | (green << 8) | blue;
    }
}
