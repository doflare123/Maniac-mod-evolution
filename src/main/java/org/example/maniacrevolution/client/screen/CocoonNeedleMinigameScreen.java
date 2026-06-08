package org.example.maniacrevolution.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.CocoonNeedleMinigameResultPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class CocoonNeedleMinigameScreen extends Screen {
    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 210;
    private static final int TRACK_THICKNESS = 9;
    private static final double START_RADIUS = 22.0D;
    private static final double TRACK_TOLERANCE = 12.0D;
    private static final int PATH_POINT_COUNT = 8;

    private static final double[][] CRACKS = {
            {0.12D, 0.18D, 0.24D, 0.26D, 0.18D, 0.36D},
            {0.78D, 0.16D, 0.70D, 0.28D, 0.84D, 0.34D},
            {0.16D, 0.82D, 0.30D, 0.76D, 0.38D, 0.88D},
            {0.62D, 0.78D, 0.72D, 0.70D, 0.88D, 0.78D}
    };

    private final BlockPos cocoonPos;
    private final double[][] path;
    private boolean tracing;
    private boolean completed;
    private double progress;
    private int samples;
    private int accurateSamples;
    private final List<TracePoint> trace = new ArrayList<>();

    public CocoonNeedleMinigameScreen(BlockPos cocoonPos) {
        super(Component.empty());
        this.cocoonPos = cocoonPos;
        this.path = generatePath();
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);
        int left = left();
        int top = top();

        gui.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xF0180628);
        gui.fill(left + 5, top + 5, left + PANEL_WIDTH - 5, top + PANEL_HEIGHT - 5, 0xCC2A0A45);
        drawCracks(gui, left, top);
        drawPath(gui, left, top);
        drawTrace(gui);
        drawEndpoint(gui, pathX(left, 0), pathY(top, 0), 0xFFE7D9FF);
        drawEndpoint(gui, pathX(left, path.length - 1), pathY(top, path.length - 1), 0xFFFFEFEF);

        super.render(gui, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || completed) return super.mouseClicked(mouseX, mouseY, button);

        double startX = pathX(left(), 0);
        double startY = pathY(top(), 0);
        if (distance(mouseX, mouseY, startX, startY) > START_RADIUS) {
            return true;
        }

        tracing = true;
        progress = 0.0D;
        samples = 0;
        accurateSamples = 0;
        trace.clear();
        trace.add(new TracePoint(mouseX, mouseY));
        samplePoint(mouseX, mouseY);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && tracing && !completed) {
            addTracePoint(mouseX, mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && tracing && !completed) {
            addTracePoint(mouseX, mouseY);
            finish();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void addTracePoint(double mouseX, double mouseY) {
        TracePoint previous = trace.isEmpty() ? new TracePoint(mouseX, mouseY) : trace.get(trace.size() - 1);
        double dx = mouseX - previous.x;
        double dy = mouseY - previous.y;
        int steps = Math.max(1, (int) Math.ceil(Math.sqrt(dx * dx + dy * dy) / 4.0D));

        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            double x = previous.x + dx * t;
            double y = previous.y + dy * t;
            trace.add(new TracePoint(x, y));
            samplePoint(x, y);
        }
    }

    private void samplePoint(double mouseX, double mouseY) {
        NearestPoint nearest = nearestPoint(mouseX, mouseY);
        samples++;

        boolean onTrack = nearest.distance <= TRACK_TOLERANCE;
        boolean ordered = nearest.progress + 0.06D >= progress;
        if (onTrack && ordered) {
            accurateSamples++;
            progress = Math.max(progress, nearest.progress);
        }

        if (progress >= 0.985D) {
            finish();
        }
    }

    private void finish() {
        if (completed) return;
        completed = true;
        tracing = false;
        float accuracy = samples == 0 ? 0.0F : (float) accurateSamples / samples;
        if (progress < 0.95D) {
            accuracy = 0.0F;
        }
        ModNetworking.sendToServer(new CocoonNeedleMinigameResultPacket(cocoonPos, accuracy));
        if (minecraft != null) {
            minecraft.setScreen(null);
        }
    }

    private void drawPath(GuiGraphics gui, int left, int top) {
        for (int i = 0; i < path.length - 1; i++) {
            drawLine(gui, pathX(left, i), pathY(top, i), pathX(left, i + 1), pathY(top, i + 1),
                    TRACK_THICKNESS, 0xFF49105F);
            drawLine(gui, pathX(left, i), pathY(top, i), pathX(left, i + 1), pathY(top, i + 1),
                    3, 0xFF12051E);
        }
    }

    private void drawTrace(GuiGraphics gui) {
        for (int i = 0; i < trace.size() - 1; i++) {
            TracePoint from = trace.get(i);
            TracePoint to = trace.get(i + 1);
            drawLine(gui, from.x, from.y, to.x, to.y, 5, 0xFFE7D9FF);
        }
    }

    private void drawCracks(GuiGraphics gui, int left, int top) {
        for (double[] crack : CRACKS) {
            double x1 = left + crack[0] * PANEL_WIDTH;
            double y1 = top + crack[1] * PANEL_HEIGHT;
            double x2 = left + crack[2] * PANEL_WIDTH;
            double y2 = top + crack[3] * PANEL_HEIGHT;
            double x3 = left + crack[4] * PANEL_WIDTH;
            double y3 = top + crack[5] * PANEL_HEIGHT;
            drawLine(gui, x1, y1, x2, y2, 2, 0xAA9E6AC8);
            drawLine(gui, x2, y2, x3, y3, 1, 0x889E6AC8);
        }
    }

    private void drawEndpoint(GuiGraphics gui, double x, double y, int color) {
        gui.fill((int) x - 5, (int) y - 5, (int) x + 5, (int) y + 5, color);
    }

    private void drawLine(GuiGraphics gui, double x1, double y1, double x2, double y2,
                          int thickness, int color) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        int steps = Math.max(1, (int) Math.ceil(Math.sqrt(dx * dx + dy * dy) / 2.0D));
        int half = Math.max(1, thickness / 2);
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            int x = (int) Math.round(x1 + dx * t);
            int y = (int) Math.round(y1 + dy * t);
            gui.fill(x - half, y - half, x + half + 1, y + half + 1, color);
        }
    }

    private NearestPoint nearestPoint(double mouseX, double mouseY) {
        NearestPoint best = new NearestPoint(Double.MAX_VALUE, 0.0D);
        for (int i = 0; i < path.length - 1; i++) {
            double x1 = pathX(left(), i);
            double y1 = pathY(top(), i);
            double x2 = pathX(left(), i + 1);
            double y2 = pathY(top(), i + 1);
            double dx = x2 - x1;
            double dy = y2 - y1;
            double lengthSqr = dx * dx + dy * dy;
            double t = lengthSqr == 0.0D ? 0.0D :
                    Math.max(0.0D, Math.min(1.0D, ((mouseX - x1) * dx + (mouseY - y1) * dy) / lengthSqr));
            double px = x1 + dx * t;
            double py = y1 + dy * t;
            double distance = distance(mouseX, mouseY, px, py);
            double pointProgress = (i + t) / (path.length - 1);
            if (distance < best.distance) {
                best = new NearestPoint(distance, pointProgress);
            }
        }
        return best;
    }

    private double pathX(int left, int index) {
        return left + 42.0D + path[index][0] * (PANEL_WIDTH - 84.0D);
    }

    private double pathY(int top, int index) {
        return top + 34.0D + path[index][1] * (PANEL_HEIGHT - 68.0D);
    }

    private static double[][] generatePath() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double[][] generated = new double[PATH_POINT_COUNT][2];

        generated[0][0] = 0.08D;
        generated[0][1] = random.nextDouble(0.42D, 0.69D);
        generated[PATH_POINT_COUNT - 1][0] = 0.92D;
        generated[PATH_POINT_COUNT - 1][1] = random.nextDouble(0.31D, 0.58D);

        double previousY = generated[0][1];
        for (int i = 1; i < PATH_POINT_COUNT - 1; i++) {
            double baseX = 0.08D + (0.84D * i / (PATH_POINT_COUNT - 1));
            generated[i][0] = clamp(baseX + random.nextDouble(-0.025D, 0.025D), 0.12D, 0.88D);

            double y = random.nextDouble(0.28D, 0.72D);
            for (int attempt = 0; attempt < 5 && Math.abs(y - previousY) < 0.10D; attempt++) {
                y = random.nextDouble(0.28D, 0.72D);
            }
            generated[i][1] = y;
            previousY = y;
        }

        return generated;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private int left() {
        return (width - PANEL_WIDTH) / 2;
    }

    private int top() {
        return (height - PANEL_HEIGHT) / 2;
    }

    private double distance(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private record NearestPoint(double distance, double progress) {}

    private record TracePoint(double x, double y) {}
}
