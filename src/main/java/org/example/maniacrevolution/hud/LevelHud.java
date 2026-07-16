package org.example.maniacrevolution.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.example.maniacrevolution.data.ClientPlayerData;

public final class LevelHud {
    private static final int WIDTH = 94;
    private static final int HEIGHT = 17;
    private static float displayedProgress = -1.0f;

    private LevelHud() {
    }

    public static void render(GuiGraphics gui, int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        float target = Mth.clamp(ClientPlayerData.getExpProgress(), 0.0f, 1.0f);
        displayedProgress = displayedProgress < 0.0f
                ? target
                : Mth.lerp(mc.isPaused() ? 0.0f : 0.14f, displayedProgress, target);

        gui.fill(x, y, x + WIDTH, y + HEIGHT, 0xB5101216);
        gui.renderOutline(x, y, WIDTH, HEIGHT, 0xCC59616C);

        String level = "Lv." + ClientPlayerData.getLevel();
        String coins = "★ " + ClientPlayerData.getCoins();
        gui.drawString(mc.font, level, x + 4, y + 3, 0xFFFFC857, true);
        gui.drawString(mc.font, coins, x + WIDTH - mc.font.width(coins) - 4, y + 3, 0xFFFFD966, true);

        int barWidth = WIDTH - 2;
        gui.fill(x + 1, y + HEIGHT - 3, x + WIDTH - 1, y + HEIGHT - 1, 0xFF272C32);
        gui.fill(x + 1, y + HEIGHT - 3,
                x + 1 + Math.round(barWidth * displayedProgress), y + HEIGHT - 1, 0xFF4CA8E8);
    }
}
