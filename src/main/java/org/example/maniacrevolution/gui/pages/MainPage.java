package org.example.maniacrevolution.gui.pages;

import net.minecraft.client.gui.GuiGraphics;
import org.example.maniacrevolution.gui.GuideScreen;

import java.util.ArrayList;
import java.util.List;

public class MainPage extends GuidePage {
    private List<MenuButton> buttons = new ArrayList<>();

    public MainPage(GuideScreen parent) {
        super(parent);
    }

    @Override
    public void init(int guiLeft, int guiTop, int guiWidth, int guiHeight) {
        super.init(guiLeft, guiTop, guiWidth, guiHeight);

        buttons.clear();

        int centerX = guiLeft + guiWidth / 2;
        int startY = guiTop + 70;
        int buttonWidth = 250;
        int buttonHeight = 35;
        int spacing = 10;

        // Кнопки навигации
        buttons.add(new MenuButton(
                centerX - buttonWidth / 2, startY,
                buttonWidth, buttonHeight,
                "§6§l📖 Полный гайд по режиму",
                "Узнайте все правила и механики",
                PageType.TUTORIAL
        ));

        buttons.add(new MenuButton(
                centerX - buttonWidth / 2, startY + (buttonHeight + spacing) * 3,
                buttonWidth, buttonHeight,
                "§d§l👤 Персонажи",
                "Все выжившие и маньяки режима",
                PageType.CHARACTERS // НОВОЕ
        ));

        buttons.add(new MenuButton(
                centerX - buttonWidth / 2, startY + buttonHeight + spacing,
                buttonWidth, buttonHeight,
                "§a§l⚡ Перки и способности",
                "Список всех перков для выживших и маньяков",
                PageType.PERKS
        ));

        buttons.add(new MenuButton(
                centerX - buttonWidth / 2, startY + (buttonHeight + spacing) * 2,
                buttonWidth, buttonHeight,
                "§c§l🗺 Карты и их особенности",
                "Изучите карты и их уникальные механики",
                PageType.MAPS
        ));
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        // Заголовок
        gui.drawCenteredString(font, "§6§l✦ ГАЙД ПО РЕЖИМУ ✦",
                guiLeft + guiWidth / 2, guiTop + 15, 0xFFFFFF);

        gui.drawCenteredString(font, "§7Добро пожаловать! Выберите раздел:",
                guiLeft + guiWidth / 2, guiTop + 35, 0xAAAAAA);

        // Рендерим кнопки
        for (MenuButton button : buttons) {
            button.render(gui, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (MenuButton menuButton : buttons) {
                if (menuButton.isHovered(mouseX, mouseY)) {
                    parent.switchPage(menuButton.targetPage);
                    return true;
                }
            }
        }
        return false;
    }

    private class MenuButton {
        int x, y, width, height;
        String title, subtitle;
        PageType targetPage;

        MenuButton(int x, int y, int width, int height, String title, String subtitle, PageType targetPage) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.title = title;
            this.subtitle = subtitle;
            this.targetPage = targetPage;
        }

        boolean isHovered(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }

        void render(GuiGraphics gui, int mouseX, int mouseY) {
            boolean hovered = isHovered(mouseX, mouseY);

            // Фон
            int bgColor = hovered ? 0xFF3a3a3a : 0xFF2a2a2a;
            gui.fill(x, y, x + width, y + height, bgColor);

            // Рамка
            int borderColor = hovered ? 0xFFFFAA00 : 0xFF555555;
            gui.renderOutline(x, y, width, height, borderColor);

            // Текст
            gui.drawCenteredString(font, title, x + width / 2, y + 8, 0xFFFFFF);
            gui.drawCenteredString(font, "§7" + subtitle, x + width / 2, y + 22, 0xAAAAAA);
        }
    }
}