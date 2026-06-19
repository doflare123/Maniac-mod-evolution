package org.example.maniacrevolution.nightmare;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.Maniacrev;

public final class NightmareHud {
    private static final int PORTRAIT_WIDTH = 72;
    private static final int PORTRAIT_HEIGHT = 88;
    private static final int TRIAL_TIMER_Y = 38;
    private static final ResourceLocation SCREAMER_TEXTURE =
            Maniacrev.loc("textures/gui/abilities/screamer.jpg");

    private NightmareHud() {}

    public static void render(GuiGraphics gui, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (ClientNightmareData.isVisible()) {
            renderSanityVignette(gui, screenWidth, screenHeight);
            int portraitX = 8;
            int portraitY = screenHeight - 128;
            renderSanityPortrait(gui, mc, portraitX, portraitY);
            renderSanityResistance(gui, mc, portraitX, portraitY + PORTRAIT_HEIGHT + 5);
        }

        if (ClientNightmareData.getTrialType() != NightmareTrialType.NONE) {
            String text = trialName(ClientNightmareData.getTrialType()) + ": "
                    + ClientNightmareData.getTrialSecondsLeft() + "\u0441";
            int x = screenWidth / 2 - mc.font.width(text) / 2;
            gui.drawString(mc.font, text, x, TRIAL_TIMER_Y, 0xFFFFD5FF, true);
        }

        if (ClientNightmareData.shouldShowScreamer()) {
            RenderSystem.enableBlend();
            RenderSystem.setShaderTexture(0, SCREAMER_TEXTURE);
            gui.blit(SCREAMER_TEXTURE, 0, 0, 0, 0, screenWidth, screenHeight, screenWidth, screenHeight);
            RenderSystem.disableBlend();
        }
    }

    private static void renderSanityPortrait(GuiGraphics gui, Minecraft mc, int x, int y) {
        float sanity = ClientNightmareData.getSanityPercent();
        float corruption = 1.0F - sanity;
        long time = System.currentTimeMillis();
        float pulse = (float) (Math.sin(time / 140.0D) * 0.5D + 0.5D);

        int px = x;
        int py = y;
        int frameColor = lerpColor(0xFF6F5A8A, 0xFFFF2D55, corruption);
        int glowAlpha = (int) ((35 + 95 * pulse) * corruption);
        int glowColor = (glowAlpha << 24) | 0xAA0038;

        gui.fill(px, py, px + PORTRAIT_WIDTH, py + PORTRAIT_HEIGHT, 0xCC07050D);
        gui.fill(px + 2, py + 2, px + PORTRAIT_WIDTH - 2, py + PORTRAIT_HEIGHT - 14, 0xEE100818);
        gui.renderOutline(px, py, PORTRAIT_WIDTH, PORTRAIT_HEIGHT, frameColor);
        gui.renderOutline(px + 3, py + 3, PORTRAIT_WIDTH - 6, PORTRAIT_HEIGHT - 18, 0x882C1838);

        RenderSystem.enableBlend();
        gui.pose().pushPose();
        gui.pose().translate(0.0F, 0.0F, 60.0F);
        float lookX = (float) Math.sin(time / 420.0D) * (8.0F + corruption * 18.0F);
        float lookY = -6.0F + corruption * 10.0F;
        InventoryScreen.renderEntityInInventoryFollowsMouse(gui, px + PORTRAIT_WIDTH / 2,
                py + PORTRAIT_HEIGHT - 18, 32, lookX, lookY, mc.player);
        gui.pose().popPose();

        if (corruption > 0.02F) {
            gui.fill(px + 2, py + 2, px + PORTRAIT_WIDTH - 2, py + PORTRAIT_HEIGHT - 14, glowColor);
        }
        if (corruption > 0.72F) {
            int flash = ((int) (70 + 80 * pulse) << 24) | 0xFF0000;
            gui.renderOutline(px - 1, py - 1, PORTRAIT_WIDTH + 2, PORTRAIT_HEIGHT + 2, flash);
        }
        RenderSystem.disableBlend();

        renderSanityMarks(gui, px + 9, py + PORTRAIT_HEIGHT - 10, sanity);
    }

    private static void renderSanityVignette(GuiGraphics gui, int screenWidth, int screenHeight) {
        float corruption = 1.0F - ClientNightmareData.getSanityPercent();
        if (corruption <= 0.04F) return;

        int alpha = (int) (Math.min(0.82F, corruption * corruption * 0.95F) * 255.0F);
        int color = (alpha << 24) | 0x050009;
        int sideWidth = Math.max(12, (int) (screenWidth * (0.08F + corruption * 0.20F)));
        int verticalHeight = Math.max(8, (int) (screenHeight * (0.04F + corruption * 0.10F)));

        RenderSystem.enableBlend();
        gui.fill(0, 0, sideWidth, screenHeight, color);
        gui.fill(screenWidth - sideWidth, 0, screenWidth, screenHeight, color);
        gui.fill(0, 0, screenWidth, verticalHeight, color);
        gui.fill(0, screenHeight - verticalHeight, screenWidth, screenHeight, color);
        RenderSystem.disableBlend();
    }

    private static void renderSanityResistance(GuiGraphics gui, Minecraft mc, int x, int y) {
        int secondsLeft = ClientNightmareData.getSanityImmunitySecondsLeft();
        if (secondsLeft <= 0) {
            return;
        }

        String text = "\u0417\u0430\u0449\u0438\u0442\u0430: " + secondsLeft + "\u0441";
        int width = Math.max(PORTRAIT_WIDTH, mc.font.width(text) + 12);
        gui.fill(x, y, x + width, y + 14, 0xCC090511);
        gui.renderOutline(x, y, width, 14, 0xFF7A4D96);
        gui.fill(x + 3, y + 11, x + width - 3, y + 12, 0xFFD65BFF);
        gui.drawString(mc.font, text, x + 6, y + 3, 0xFFE9C7FF, true);
    }

    private static void renderSanityMarks(GuiGraphics gui, int x, int y, float sanity) {
        int active = Math.max(0, Math.min(5, (int) Math.ceil(sanity * 5.0F)));
        for (int i = 0; i < 5; i++) {
            int markX = x + i * 11;
            int color = i < active ? 0xFFD65BFF : 0xFF26152E;
            gui.fill(markX + 2, y, markX + 7, y + 5, color);
            gui.renderOutline(markX + 1, y - 1, 7, 7, 0xFF4B245F);
        }
    }

    private static int lerpColor(int colorA, int colorB, float t) {
        float clamped = Math.max(0.0F, Math.min(1.0F, t));
        int aA = (colorA >> 24) & 0xFF;
        int rA = (colorA >> 16) & 0xFF;
        int gA = (colorA >> 8) & 0xFF;
        int bA = colorA & 0xFF;
        int aB = (colorB >> 24) & 0xFF;
        int rB = (colorB >> 16) & 0xFF;
        int gB = (colorB >> 8) & 0xFF;
        int bB = colorB & 0xFF;
        int a = (int) (aA + (aB - aA) * clamped);
        int r = (int) (rA + (rB - rA) * clamped);
        int g = (int) (gA + (gB - gA) * clamped);
        int b = (int) (bA + (bB - bA) * clamped);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static String trialName(NightmareTrialType type) {
        return switch (type) {
            case MAZE -> "\u041b\u0430\u0431\u0438\u0440\u0438\u043d\u0442";
            case ARENA -> "\u0410\u0440\u0435\u043d\u0430";
            case FEAR_RACE -> "\u0413\u043e\u043d\u043a\u0430";
            case NONE -> "";
        };
    }
}
