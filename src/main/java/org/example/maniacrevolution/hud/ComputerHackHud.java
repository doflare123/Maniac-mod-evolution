package org.example.maniacrevolution.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.example.maniacrevolution.hack.client.ClientHackData;

/**
 * HUD-блок: прогресс взлома компьютеров.
 * Показывается только когда игра идёт (phase > 0).
 *
 * Использование в CustomHud.render():
 *   ComputerHackHud.render(guiGraphics, x, y);
 */
public class ComputerHackHud {

    public static final int WIDTH  = 90;
    public static final int HEIGHT = 36;
    private static final int BG     = 0xCC000000;
    private static final int BORDER = 0xFF444444;

    /**
     * Рендерит блок. Вызывать только если ClientGameState.isGameRunning().
     *
     * @param x  левый край блока
     * @param y  верхний край блока
     */
    public static void render(GuiGraphics g, int x, int y) {
        int hacked = ClientHackData.getTotalHacked();
        int goal   = ClientHackData.getGoal();

        Minecraft mc = Minecraft.getInstance();

        // Фон + рамка
        g.fill(x, y, x + WIDTH, y + HEIGHT, BG);
        g.renderOutline(x, y, WIDTH, HEIGHT, BORDER);

        // Заголовок
        String title = "§7Компьютеры";
        int titleX = x + (WIDTH - mc.font.width(title)) / 2;
        g.drawString(mc.font, title, titleX, y + 4, 0xFFFFFF, false);

        // Счётчик: X / N  с цветом по прогрессу
        String counter = "§f" + hacked + " §7/ §f" + goal;
        int counterX = x + (WIDTH - mc.font.width(counter)) / 2;
        g.drawString(mc.font, counter, counterX, y + 14, 0xFFFFFF, false);

        // Полоска прогресса
        int barX  = x + 6;
        int barY  = y + HEIGHT - 9;
        int barW  = WIDTH - 12;
        int barH  = 5;
        float pct = goal > 0 ? Math.min(1f, (float) hacked / goal) : 0f;
        int filled = (int)(barW * pct);

        g.fill(barX, barY, barX + barW, barY + barH, 0xFF333333);
        if (filled > 0) {
            int barColor = pct >= 1f ? 0xFF00FF00 : 0xFF00AAFF;
            g.fill(barX, barY, barX + filled, barY + barH, barColor);
        }
        g.renderOutline(barX, barY, barW, barH, BORDER);
    }
}