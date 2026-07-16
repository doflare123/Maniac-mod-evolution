package org.example.maniacrevolution.hud;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.example.maniacrevolution.data.ClientGameState;

public final class TimerHud {
    private static final int HEIGHT = 17;
    private static final int TIME_WIDTH = 50;
    private static final int PHASE_WIDTH = 78;
    private static final int GAP = 3;
    private static final long FLIP_DURATION_MS = 520L;

    private static int previousPhase = -1;
    private static int targetPhase = -1;
    private static long flipStartedAt;

    private TimerHud() {
    }

    public static void render(GuiGraphics gui, int centerX, int y) {
        if (!ClientGameState.isGameRunning()) {
            previousPhase = -1;
            targetPhase = -1;
            return;
        }

        int currentPhase = ClientGameState.getPhase();
        long now = Util.getMillis();
        if (targetPhase < 0) {
            previousPhase = currentPhase;
            targetPhase = currentPhase;
        } else if (currentPhase != targetPhase) {
            previousPhase = targetPhase;
            targetPhase = currentPhase;
            flipStartedAt = now;
        }

        int totalWidth = TIME_WIDTH + GAP + PHASE_WIDTH;
        int x = centerX - totalWidth / 2;
        renderTimeCard(gui, x, y);
        renderPhaseCard(gui, x + TIME_WIDTH + GAP, y, now);
    }

    private static void renderTimeCard(GuiGraphics gui, int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        gui.fill(x, y, x + TIME_WIDTH, y + HEIGHT, 0xB5101216);
        gui.renderOutline(x, y, TIME_WIDTH, HEIGHT, 0xCC59616C);
        String time = ClientGameState.getFormattedTime();
        gui.drawString(mc.font, time, x + (TIME_WIDTH - mc.font.width(time)) / 2,
                y + 4, getTimeColor(), true);
    }

    private static void renderPhaseCard(GuiGraphics gui, int x, int y, long now) {
        float progress = flipStartedAt == 0L
                ? 1.0f
                : Mth.clamp((now - flipStartedAt) / (float) FLIP_DURATION_MS, 0.0f, 1.0f);
        if (progress >= 1.0f) {
            previousPhase = targetPhase;
            flipStartedAt = 0L;
            renderPhaseFace(gui, x, y, targetPhase, 1.0f, 0.0f);
            return;
        }

        float angle = progress * (float) Math.PI;
        float faceScale = Math.abs((float) Math.cos(angle));
        boolean incomingFace = progress >= 0.5f;
        int phase = incomingFace ? targetPhase : previousPhase;
        float darkness = 1.0f - faceScale;

        if (faceScale < 0.08f) {
            int edgeY = y + HEIGHT / 2;
            gui.fill(x, edgeY - 1, x + PHASE_WIDTH, edgeY + 2, 0xFF343A42);
            gui.fill(x + 1, edgeY - 1, x + PHASE_WIDTH - 1, edgeY, phaseColor(targetPhase));
            return;
        }

        renderPhaseFace(gui, x, y, phase, faceScale, darkness);
    }

    private static void renderPhaseFace(GuiGraphics gui, int x, int y, int phase,
                                        float scaleY, float darkness) {
        Minecraft mc = Minecraft.getInstance();
        gui.pose().pushPose();
        gui.pose().translate(x, y + HEIGHT / 2.0f, 0.0f);
        gui.pose().scale(1.0f, scaleY, 1.0f);

        int top = -HEIGHT / 2;
        gui.fill(0, top, PHASE_WIDTH, top + HEIGHT, 0xC5101216);
        gui.renderOutline(0, top, PHASE_WIDTH, HEIGHT, 0xCC59616C);
        if (darkness > 0.01f) {
            int shadeAlpha = Math.round(darkness * 150.0f);
            gui.fill(1, top + 1, PHASE_WIDTH - 1, top + HEIGHT - 1, shadeAlpha << 24);
        }

        String phaseName = phaseName(phase);
        gui.drawString(mc.font, phaseName, (PHASE_WIDTH - mc.font.width(phaseName)) / 2,
                top + 4, phaseColor(phase), true);
        gui.pose().popPose();
    }

    private static String phaseName(int phase) {
        return switch (phase) {
            case 1 -> "Охота";
            case 2 -> "Мидгейм";
            case 3 -> "Переворот";
            default -> "???";
        };
    }

    private static int phaseColor(int phase) {
        return switch (phase) {
            case 1 -> 0xFFFFFF55;
            case 2 -> 0xFFFFAA00;
            case 3 -> 0xFFFF5555;
            default -> 0xFFFFFFFF;
        };
    }

    private static int getTimeColor() {
        int seconds = ClientGameState.getCurrentTimeSeconds();
        if (seconds <= 30) {
            int pulse = 185 + (int) (Math.sin(Util.getMillis() / 180.0) * 35.0);
            return 0xFF000000 | (pulse << 16) | 0x004646;
        }
        if (seconds <= 60) return 0xFFFFB347;
        return 0xFFF2F4F7;
    }
}
