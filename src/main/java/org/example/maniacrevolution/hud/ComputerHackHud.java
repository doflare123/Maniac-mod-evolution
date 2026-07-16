package org.example.maniacrevolution.hud;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.example.maniacrevolution.hack.client.ClientHackData;

public final class ComputerHackHud {
    public static final int WIDTH = 104;
    public static final int HEIGHT = 19;
    private static int lastHacked = -1;
    private static long flashUntil;
    private static float displayedProgress = -1.0f;

    private ComputerHackHud() {
    }

    public static void render(GuiGraphics gui, int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        int hacked = ClientHackData.getTotalHacked();
        int goal = ClientHackData.getGoal();
        float target = goal > 0 ? Mth.clamp((float) hacked / goal, 0.0f, 1.0f) : 0.0f;
        displayedProgress = displayedProgress < 0.0f
                ? target
                : Mth.lerp(mc.isPaused() ? 0.0f : 0.16f, displayedProgress, target);

        if (lastHacked >= 0 && hacked > lastHacked) flashUntil = Util.getMillis() + 450L;
        lastHacked = hacked;
        boolean flashing = Util.getMillis() < flashUntil;

        gui.fill(x, y, x + WIDTH, y + HEIGHT, flashing ? 0xC5243B30 : 0xB5101216);
        gui.renderOutline(x, y, WIDTH, HEIGHT, flashing ? 0xFF70E28A : 0xCC59616C);

        String title = "Компьютеры";
        String count = hacked + "/" + goal;
        gui.drawString(mc.font, title, x + 5, y + 3, 0xFFC7CDD4, false);
        gui.drawString(mc.font, count, x + WIDTH - mc.font.width(count) - 5,
                y + 3, target >= 1.0f ? 0xFF70E28A : 0xFFFFFFFF, true);

        gui.fill(x + 1, y + HEIGHT - 3, x + WIDTH - 1, y + HEIGHT - 1, 0xFF272C32);
        gui.fill(x + 1, y + HEIGHT - 3,
                x + 1 + Math.round((WIDTH - 2) * displayedProgress), y + HEIGHT - 1,
                target >= 1.0f ? 0xFF70E28A : 0xFF4CA8E8);
    }
}
