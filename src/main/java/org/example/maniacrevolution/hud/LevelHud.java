package org.example.maniacrevolution.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.example.maniacrevolution.data.ClientPlayerData;

public class LevelHud {
    private static final int BAR_WIDTH = 100;
    private static final int BAR_HEIGHT = 7;

    public static void render(GuiGraphics gui, int x, int y) {
        Minecraft mc = Minecraft.getInstance();

        int level = ClientPlayerData.getLevel();
        int exp = ClientPlayerData.getExperience();
        int expNext = ClientPlayerData.getExpForNextLevel();
        int coins = ClientPlayerData.getCoins();
        float progress = ClientPlayerData.getExpProgress();

        // Фон панели
        gui.fill(x, y, x + 110, y + 45, 0x90000000);

        // Уровень
        gui.drawString(mc.font, "§6Уровень: " + level, x + 5, y + 3, 0xFFFFFF, true);

        // Полоска опыта - фон
        gui.fill(x + 5, y + 25, x + 5 + BAR_WIDTH, y + 25 + BAR_HEIGHT, 0xFF333333);

        // Полоска опыта - заполнение
        int fillWidth = (int) (BAR_WIDTH * progress);
        gui.fill(x + 5, y + 25, x + 5 + fillWidth, y + 25 + BAR_HEIGHT, 0xFF00AAFF);

        // Рамка
        gui.renderOutline(x + 5, y + 25, BAR_WIDTH, BAR_HEIGHT, 0xFF888888);

        // Текст опыта
        String expText = "§bОпыт: §f" + exp + "/" + expNext;
        gui.drawString(mc.font, expText, x + 5,
                y + 14, 0xFFFFFF, true);

        // Монеты
        gui.drawString(mc.font, "§e\u2B50 " + coins, x + 5, y + 35, 0xFFFFFF, true);
    }
}
