package org.example.maniacrevolution.gui.pages;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.gui.GuideScreen;

import java.util.ArrayList;
import java.util.List;

public class TutorialPage extends GuidePage {
    private int scrollOffset = 0;
    private List<Section> sections = new ArrayList<>();
    private Section hoveredLink = null;

    public TutorialPage(GuideScreen parent) {
        super(parent);
        initContent();
    }

    private void initContent() {
        sections.clear();

        // Заголовок
        sections.add(new TitleSection("§6§l✦ ПОЛНЫЙ ГАЙД ПО РЕЖИМУ ✦"));
        sections.add(new SpacerSection(10));

        // Введение
        sections.add(new HeaderSection("§e§l📖 Введение"));
        sections.add(new TextSection(
                "Добро пожаловать в режим Maniac! Это асимметричный PvP режим, вдохновленный игрой DBD, " +
                        "где команда выживших противостоит безжалостным маньякам. " +
                        "Выживание требует командной работы, стратегии и умения использовать перки."
        ));
        sections.add(new SpacerSection(15));

        // Правила игры
        sections.add(new HeaderSection("§e§l⚔ Основные правила"));
        sections.add(new TextSection(
                "§7Команды:§r Игроки делятся на две команды - §aВыжившие§r и §cМаньяки§r."
        ));
        sections.add(new TextSection(
                "§7Цель выживших:§r Хакнуть все компьютеры и убить маньяка до окончания времени игры"
        ));
        sections.add(new TextSection(
                "§7Цель маньяков:§r Устранить всех выживших до окончания времени."
        ));
        sections.add(new SpacerSection(10));

        // Фазы игры
        sections.add(new HeaderSection("§e§l⏱ Фазы игры"));
        sections.add(new TextSection(
                "§6Фаза 1 - Охота:§r Маньяки ищут выживших. Некоторые перки недоступны."
        ));
        sections.add(new TextSection(
                "§6Фаза 2 - Мидгейм (прошла половина времени от таймера):§r Открываются дополнительные перки. Игра становится интенсивнее."
        ));
        sections.add(new TextSection(
                "§6Фаза 3 - Переворот:§r Если выжившие хакнули все компы, то они получают карточку для открытия сейфов"
        ));
        sections.add(new SpacerSection(15));

        // Перки
        sections.add(new HeaderSection("§e§l⚡ Система перков"));
        sections.add(new TextSection(
                "Перки - это уникальные способности, которые дают преимущества в бою. " +
                        "Существует три типа перков:"
        ));
        sections.add(new TextSection("§9● Пассивные§r - работают автоматически"));
        sections.add(new TextSection("§c● Активные§r - требуют активации клавишей"));
        sections.add(new TextSection("§d● Гибридные§r - имеют пассивный эффект и активацию"));
        sections.add(new SpacerSection(5));

        // Ссылка на страницу перков
        sections.add(new LinkSection("➤ Полный список перков", PageType.PERKS));
        sections.add(new SpacerSection(15));

        // Карты
        sections.add(new HeaderSection("§e§l🗺 Карты"));
        sections.add(new TextSection(
                "Каждая карта имеет уникальные особенности, которые влияют на геймплей. " +
                        "Изучите карту перед игрой, чтобы использовать её преимущества."
        ));
        sections.add(new SpacerSection(5));

        // Ссылка на карты
        sections.add(new LinkSection("➤ Подробнее о картах", PageType.MAPS));
        sections.add(new SpacerSection(15));

        // Советы
        sections.add(new HeaderSection("§e§l💡 Советы для новичков"));
        sections.add(new TextSection("§a1.§r Всегда двигайтесь в команде - одиночки умирают первыми"));
        sections.add(new TextSection("§a2.§r Изучите перки своей команды - синергия решает"));
        sections.add(new TextSection("§a3.§r Следите за таймером - фазы меняют баланс сил"));
        sections.add(new TextSection("§a4.§r Используйте особенности карты - укрытия и ловушки"));
        sections.add(new TextSection("§a5.§r Общайтесь с командой - координация важна"));
        sections.add(new SpacerSection(15));

        // Механики
        sections.add(new HeaderSection("§e§l🔧 Особые механики"));
        sections.add(new TextSection(
                "§7Flesh Heap:§r Маньяки получают постоянное увеличение здоровья за каждое убийство. " +
                        "Стаки сбрасываются при смерти маньяка."
        ));
        sections.add(new TextSection(
                "§7Система маны:§r Некоторые способности требуют ману. Мана восстанавливается со временем."
        ));
        sections.add(new TextSection(
                "§7Воскрешение:§r Некромант может воскресить павшего союзника один раз за игру."
        ));
        sections.add(new SpacerSection(20));

        // Футер
        sections.add(new TextSection("§7§oУдачи в игре! Да победит сильнейшая команда!"));
    }

    @Override
    public void init(int guiLeft, int guiTop, int guiWidth, int guiHeight) {
        super.init(guiLeft, guiTop, guiWidth, guiHeight);
        scrollOffset = 0;
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        // Кнопка "Назад на главную"
        renderBackButton(gui, mouseX, mouseY);

        hoveredLink = null;

        // Область скролла
        gui.enableScissor(guiLeft + 5, guiTop + 30, guiLeft + guiWidth - 5, guiTop + guiHeight - 15);

        int y = guiTop + 35 - scrollOffset;
        int maxWidth = guiWidth - 30;

        for (Section section : sections) {
            int sectionHeight = section.getHeight(maxWidth);

            if (y + sectionHeight > guiTop + 30 && y < guiTop + guiHeight - 15) {
                section.render(gui, guiLeft + 15, y, maxWidth, mouseX, mouseY);

                // Проверяем наведение на ссылки
                if (section instanceof LinkSection link) {
                    if (link.isHovered(mouseX, mouseY, guiLeft + 15, y, maxWidth)) {
                        hoveredLink = link;
                    }
                }
            }

            y += sectionHeight;
        }

        gui.disableScissor();

        // Индикатор скролла
        int totalHeight = sections.stream().mapToInt(s -> s.getHeight(maxWidth)).sum();
        if (totalHeight > guiHeight - 50) {
            gui.drawString(font, "§8↑↓ Прокрутка", guiLeft + guiWidth - 80,
                    guiTop + guiHeight - 12, 0x666666, false);
        }
    }

    private void renderBackButton(GuiGraphics gui, int mouseX, int mouseY) {
        int btnX = guiLeft + 5;
        int btnY = guiTop + 5;
        int btnW = 80;
        int btnH = 18;

        boolean hovered = mouseX >= btnX && mouseX < btnX + btnW && mouseY >= btnY && mouseY < btnY + btnH;

        gui.fill(btnX, btnY, btnX + btnW, btnY + btnH, hovered ? 0xFF444444 : 0xFF333333);
        gui.renderOutline(btnX, btnY, btnW, btnH, 0xFF666666);
        gui.drawCenteredString(font, "← Главная", btnX + btnW / 2, btnY + 5, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Кнопка "Назад"
            if (mouseX >= guiLeft + 5 && mouseX < guiLeft + 85 && mouseY >= guiTop + 5 && mouseY < guiTop + 23) {
                parent.switchPage(PageType.MAIN);
                return true;
            }

            // Клик на ссылку
            if (hoveredLink instanceof LinkSection link) {
                parent.switchPage(link.targetPage);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int maxWidth = guiWidth - 30;
        int totalHeight = sections.stream().mapToInt(s -> s.getHeight(maxWidth)).sum();
        int maxScroll = Math.max(0, totalHeight - (guiHeight - 50));

        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - delta * 30));
        return true;
    }

    // === Секции контента ===

    private abstract class Section {
        abstract int getHeight(int maxWidth);
        abstract void render(GuiGraphics gui, int x, int y, int maxWidth, int mouseX, int mouseY);
    }

    private class TitleSection extends Section {
        String text;

        TitleSection(String text) {
            this.text = text;
        }

        @Override
        int getHeight(int maxWidth) {
            return 15;
        }

        @Override
        void render(GuiGraphics gui, int x, int y, int maxWidth, int mouseX, int mouseY) {
            gui.drawCenteredString(font, text, x + maxWidth / 2, y, 0xFFFFFF);
        }
    }

    private class HeaderSection extends Section {
        String text;

        HeaderSection(String text) {
            this.text = text;
        }

        @Override
        int getHeight(int maxWidth) {
            return 14;
        }

        @Override
        void render(GuiGraphics gui, int x, int y, int maxWidth, int mouseX, int mouseY) {
            gui.drawString(font, text, x, y, 0xFFFFFF, false);
        }
    }

    private class TextSection extends Section {
        String text;

        TextSection(String text) {
            this.text = text;
        }

        @Override
        int getHeight(int maxWidth) {
            List<String> lines = wrapText(text, maxWidth);
            return lines.size() * 11 + 2;
        }

        @Override
        void render(GuiGraphics gui, int x, int y, int maxWidth, int mouseX, int mouseY) {
            List<String> lines = wrapText(text, maxWidth);
            for (int i = 0; i < lines.size(); i++) {
                gui.drawString(font, lines.get(i), x, y + i * 11, 0xFFFFFF, false);
            }
        }
    }

    private class LinkSection extends Section {
        String text;
        PageType targetPage;

        LinkSection(String text, PageType targetPage) {
            this.text = text;
            this.targetPage = targetPage;
        }

        @Override
        int getHeight(int maxWidth) {
            return 14;
        }

        @Override
        void render(GuiGraphics gui, int x, int y, int maxWidth, int mouseX, int mouseY) {
            boolean hovered = isHovered(mouseX, mouseY, x, y, maxWidth);
            String displayText = hovered ? "§n§e" + text : "§e" + text;

            gui.drawString(font, displayText, x, y, 0xFFFFFF, false);

            if (hovered) {
                gui.drawString(font, "§7(клик для перехода)", x + font.width(text) + 5, y, 0xAAAAAA, false);
            }
        }

        boolean isHovered(int mouseX, int mouseY, int x, int y, int maxWidth) {
            int textWidth = font.width(text);
            return mouseX >= x && mouseX < x + textWidth && mouseY >= y && mouseY < y + 11;
        }
    }

    private class SpacerSection extends Section {
        int height;

        SpacerSection(int height) {
            this.height = height;
        }

        @Override
        int getHeight(int maxWidth) {
            return height;
        }

        @Override
        void render(GuiGraphics gui, int x, int y, int maxWidth, int mouseX, int mouseY) {
            // Пустое пространство
        }
    }
}