package org.example.maniacrevolution.nightmare;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.Maniacrev;

public final class NightmareHud {
    private static final int PANEL_WIDTH = 104;
    private static final int PANEL_HEIGHT = 32;
    private static final int FACE_SIZE = 24;
    private static final int SEGMENT_COUNT = 5;
    private static final int SEGMENT_WIDTH = 11;
    private static final int SEGMENT_HEIGHT = 6;
    private static final int SEGMENT_GAP = 2;
    private static final int CUSTOM_HUD_MAX_WIDTH = 368;
    private static final int CUSTOM_HUD_HEIGHT = 60;
    private static final int TRIAL_TIMER_Y = 38;
    private static final int PANEL_BG = 0xD0101216;
    private static final int PANEL_BORDER = 0xFF59616C;
    private static final int SLOT_BG = 0xE0181B20;
    private static final int CRITICAL_COLOR = 0xFFE04444;
    private static final ResourceLocation SCREAMER_TEXTURE =
            Maniacrev.loc("textures/gui/abilities/screamer.jpg");

    private NightmareHud() {}

    public static void render(GuiGraphics gui, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (ClientNightmareData.isVisible()) {
            renderSanityVignette(gui, screenWidth, screenHeight);
            int panelX = 6;
            int bottomOffset = 4 + (mc.screen instanceof ChatScreen ? 14 : 0);
            int panelY = screenHeight - PANEL_HEIGHT - bottomOffset;
            int customHudLeft = (screenWidth - CUSTOM_HUD_MAX_WIDTH) / 2;
            if (panelX + PANEL_WIDTH + 4 > customHudLeft) {
                panelY -= CUSTOM_HUD_HEIGHT + 4;
            }
            renderSanityIndicator(gui, mc, panelX, panelY);
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

    private static void renderSanityIndicator(GuiGraphics gui, Minecraft mc, int x, int y) {
        float sanity = ClientNightmareData.getSanityPercent();
        float corruption = 1.0F - sanity;
        long time = System.currentTimeMillis();
        float pulse = (float) (Math.sin(time / 140.0D) * 0.5D + 0.5D);
        int frameColor = lerpColor(PANEL_BORDER, CRITICAL_COLOR, corruption);

        gui.fill(x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, PANEL_BG);
        gui.renderOutline(x, y, PANEL_WIDTH, PANEL_HEIGHT, frameColor);
        gui.fill(x + 1, y + 1, x + PANEL_WIDTH - 1, y + 2, 0x88777F89);

        int faceX = x + 4;
        int faceY = y + 4;
        gui.fill(faceX, faceY, faceX + FACE_SIZE, faceY + FACE_SIZE, SLOT_BG);
        gui.renderOutline(faceX, faceY, FACE_SIZE, FACE_SIZE, frameColor);

        RenderSystem.enableBlend();
        int shakeX = corruption > 0.72F ? (int) (time / 70L % 3L) - 1 : 0;
        PlayerFaceRenderer.draw(gui, mc.player.getSkinTextureLocation(),
                faceX + 2 + shakeX, faceY + 2, 20);

        if (corruption > 0.08F) {
            int faceTintAlpha = Math.min(105, Math.round(corruption * corruption * 120.0F));
            gui.fill(faceX + 1, faceY + 1, faceX + FACE_SIZE - 1, faceY + FACE_SIZE - 1,
                    (faceTintAlpha << 24) | 0x660018);
        }
        if (corruption > 0.72F) {
            int flash = ((int) (65 + 95 * pulse) << 24) | 0xFF2438;
            gui.renderOutline(x - 1, y - 1, PANEL_WIDTH + 2, PANEL_HEIGHT + 2, flash);
        }
        RenderSystem.disableBlend();

        int contentX = x + 34;
        String percent = Math.round(sanity * 100.0F) + "%";
        gui.drawString(mc.font, percent, contentX, y + 6,
                lerpColor(0xFFE2E5E9, 0xFFFF7A83, corruption), true);

        int immunitySeconds = ClientNightmareData.getSanityImmunitySecondsLeft();
        if (immunitySeconds > 0) {
            String immunity = immunitySeconds + "\u0441";
            gui.drawString(mc.font, immunity, x + PANEL_WIDTH - mc.font.width(immunity) - 5,
                    y + 6, 0xFF8ED1FF, true);
        }

        renderSanitySegments(gui, contentX, y + 20, sanity, corruption);
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

    private static void renderSanitySegments(GuiGraphics gui, int x, int y,
                                             float sanity, float corruption) {
        int active = Math.max(0, Math.min(SEGMENT_COUNT,
                (int) Math.ceil(sanity * SEGMENT_COUNT)));
        int activeColor = lerpColor(0xFFC56BE3, CRITICAL_COLOR, corruption);
        for (int i = 0; i < SEGMENT_COUNT; i++) {
            int segmentX = x + i * (SEGMENT_WIDTH + SEGMENT_GAP);
            gui.fill(segmentX, y, segmentX + SEGMENT_WIDTH, y + SEGMENT_HEIGHT,
                    i < active ? activeColor : 0xFF252A30);
            gui.renderOutline(segmentX, y, SEGMENT_WIDTH, SEGMENT_HEIGHT,
                    i < active ? 0xFF8B939D : 0xFF464D56);
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
