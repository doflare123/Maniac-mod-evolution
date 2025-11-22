package org.example.maniacrevolution.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.example.maniacrevolution.data.ClientGameState;

public class TimerHud {

    public static void render(GuiGraphics gui, int centerX, int y) {
        if (!ClientGameState.isGameRunning()) return;

        Minecraft mc = Minecraft.getInstance();

        // Время
        String time = ClientGameState.getFormattedTime();
        int timeWidth = mc.font.width(time);

        // Фон
        gui.fill(centerX - timeWidth / 2 - 15, y, centerX + timeWidth / 2 + 15, y + 28, 0x80000000);

        // Таймер
        int timeColor = getTimeColor();
        gui.drawString(mc.font, time, centerX - timeWidth / 2, y + 3, timeColor, true);

        // Фаза игры
        String phase = ClientGameState.getPhaseName();
        int phaseColor = ClientGameState.getPhaseColor();
        int phaseWidth = mc.font.width(phase);
        gui.drawString(mc.font, phase, centerX - phaseWidth / 2, y + 15, phaseColor, true);
    }

    private static int getTimeColor() {
        int seconds = ClientGameState.getCurrentTimeSeconds();
        if (seconds <= 30) return 0xFF5555;      // Красный < 30 сек
        if (seconds <= 60) return 0xFFAA00;      // Оранжевый < 1 мин
        return 0xFFFFFF;                          // Белый
    }
}
