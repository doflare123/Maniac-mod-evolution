package org.example.maniacrevolution.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.character.CharacterClass;
import org.example.maniacrevolution.character.CharacterRegistry;
import org.example.maniacrevolution.character.CharacterType;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.ReadyStatusPacket;
import org.example.maniacrevolution.network.packets.SelectCharacterPacket;
import org.example.maniacrevolution.readiness.ReadinessManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Экран выбора класса персонажа
 */
public class CharacterSelectionScreen extends Screen {
    private final CharacterType type;
    private final List<CharacterClass> characters;
    private int selectedIndex = 0;

    // Размеры элементов
    private static final int FRESCO_WIDTH = 120;
    private static final int FRESCO_HEIGHT = 280;
    private static final int FRESCO_SPACING = 15;
    private static final int INFO_PANEL_WIDTH = 300;

    // Кнопки
    private Button leftArrowButton;
    private Button rightArrowButton;
    private Button selectButton;

    public CharacterSelectionScreen(CharacterType type) {
        super(Component.literal("Выбор класса: " + type.getDisplayName()));
        this.type = type;
        this.characters = CharacterRegistry.getClassesByType(type);
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Кнопка "Стрелка влево" - стандартная майнкрафтовская
        leftArrowButton = Button.builder(Component.literal("◄"), button -> {
                    previousCharacter();
                })
                .bounds(centerX - 250, centerY, 50, 20)
                .build();

        // Кнопка "Стрелка вправо" - стандартная майнкрафтовская
        rightArrowButton = Button.builder(Component.literal("►"), button -> {
                    nextCharacter();
                })
                .bounds(centerX + 200, centerY, 50, 20)
                .build();

        // Кнопка "Выбрать" - просто выбирает класс и закрывает меню
        selectButton = Button.builder(
                        Component.literal("Выбрать"),
                        button -> selectCharacter()
                )
                .bounds(centerX - 60, this.height - 50, 120, 20)
                .build();

        this.addRenderableWidget(leftArrowButton);
        this.addRenderableWidget(rightArrowButton);
        this.addRenderableWidget(selectButton);

        // Обновляем состояние кнопок
        updateButtonStates();
    }

    /**
     * Предыдущий персонаж
     */
    private void previousCharacter() {
        if (selectedIndex > 0) {
            selectedIndex--;
            updateButtonStates();
        }
    }

    /**
     * Следующий персонаж
     */
    private void nextCharacter() {
        if (selectedIndex < characters.size() - 1) {
            selectedIndex++;
            updateButtonStates();
        }
    }

    /**
     * Обновляет активность кнопок навигации
     */
    private void updateButtonStates() {
        leftArrowButton.active = selectedIndex > 0;
        rightArrowButton.active = selectedIndex < characters.size() - 1;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Рисуем фон
        this.renderBackground(graphics);
        drawGradientBackground(graphics);

        // Рисуем фрески (карточки персонажей)
        drawFrescos(graphics);

        // Рисуем информационную панель
        drawInfoPanel(graphics);

        // Рисуем кнопки (стандартные майнкрафтовские)
        super.render(graphics, mouseX, mouseY, partialTick);

        // Рисуем заголовок
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        // Показываем счётчик классов
        String counterText = (selectedIndex + 1) + " / " + characters.size();
        graphics.drawCenteredString(this.font, counterText, this.width / 2, this.height - 65, 0xAAAAAA);
    }

    /**
     * Рисуем градиентный фон
     */
    private void drawGradientBackground(GuiGraphics graphics) {
        int color1 = type == CharacterType.SURVIVOR ? 0x40004400 : 0x40440000;
        int color2 = type == CharacterType.SURVIVOR ? 0x80006600 : 0x80660000;

        graphics.fillGradient(0, 0, this.width, this.height, color1, color2);
    }

    /**
     * Рисуем фрески персонажей
     */
    private void drawFrescos(GuiGraphics graphics) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Смещаем влево
        int offsetX = -80;

        // Показываем 3 карточки одновременно (текущая по центру)
        int startIndex = Math.max(0, selectedIndex - 1);
        int endIndex = Math.min(characters.size(), startIndex + 3);

        for (int i = startIndex; i < endIndex; i++) {
            CharacterClass character = characters.get(i);

            // Позиция карточки
            int offsetFromCenter = (i - selectedIndex);
            int x = centerX + offsetX - (FRESCO_WIDTH / 2) + (offsetFromCenter * (FRESCO_WIDTH + FRESCO_SPACING));
            int y = centerY - (FRESCO_HEIGHT / 2);

            // Масштаб и прозрачность
            float alpha = (i == selectedIndex) ? 1.0f : 0.6f;

            // Рисуем рамку
            drawFrescoFrame(graphics, x, y, i == selectedIndex);

            // Рисуем текстуру фрески
            drawFrescoTexture(graphics, character, x, y, alpha);

            // Рисуем имя персонажа
            if (i == selectedIndex) {
                graphics.drawCenteredString(this.font, character.getName(),
                        x + FRESCO_WIDTH / 2, y - 15, 0xFFFFFF);
            }
        }
    }

    /**
     * Рисуем рамку фрески
     */
    private void drawFrescoFrame(GuiGraphics graphics, int x, int y, boolean selected) {
        int color = selected ? 0xFFFFD700 : 0xFF808080;
        int thickness = selected ? 3 : 1;

        // Верх
        graphics.fill(x - thickness, y - thickness,
                x + FRESCO_WIDTH + thickness, y, color);
        // Низ
        graphics.fill(x - thickness, y + FRESCO_HEIGHT,
                x + FRESCO_WIDTH + thickness, y + FRESCO_HEIGHT + thickness, color);
        // Лево
        graphics.fill(x - thickness, y,
                x, y + FRESCO_HEIGHT, color);
        // Право
        graphics.fill(x + FRESCO_WIDTH, y,
                x + FRESCO_WIDTH + thickness, y + FRESCO_HEIGHT, color);
    }

    /**
     * Рисуем текстуру фрески
     */
    private void drawFrescoTexture(GuiGraphics graphics, CharacterClass character,
                                   int x, int y, float alpha) {
        ResourceLocation texture = character.getFrescoTexture();

        graphics.pose().pushPose();

        // Применяем альфа-канал
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);

        // Рисуем текстуру (или заглушку)
        try {
            graphics.blit(texture, x, y, 0, 0, FRESCO_WIDTH, FRESCO_HEIGHT,
                    FRESCO_WIDTH, FRESCO_HEIGHT);
        } catch (Exception e) {
            // Если текстуры нет - рисуем заглушку
            graphics.fill(x, y, x + FRESCO_WIDTH, y + FRESCO_HEIGHT, 0xFF333333);
            graphics.drawCenteredString(this.font, "Нет текстуры",
                    x + FRESCO_WIDTH / 2, y + FRESCO_HEIGHT / 2, 0xFFFFFF);
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        graphics.pose().popPose();
    }

    /**
     * Рисуем информационную панель
     */
    private void drawInfoPanel(GuiGraphics graphics) {
        CharacterClass selected = characters.get(selectedIndex);

        // Панель справа
        int panelX = this.width - INFO_PANEL_WIDTH - 30;
        int panelY = 80;
        int panelHeight = this.height - 160;

        // Фон панели
        graphics.fill(panelX, panelY, panelX + INFO_PANEL_WIDTH, panelY + panelHeight, 0xCC000000);

        int textX = panelX + 10;
        int textY = panelY + 10;
        int lineHeight = 9;

        // Имя
        graphics.drawString(this.font, "§e§l" + selected.getName(), textX, textY, 0xFFFFFF);
        textY += 16;

        // Тэги
        if (!selected.getTags().isEmpty()) {
            graphics.drawString(this.font, "§7Тэги: §f" + String.join(", ", selected.getTags()),
                    textX, textY, 0xFFFFFF);
            textY += 12;
        }

        textY += 3;

        // Описание
        graphics.drawString(this.font, "§6Описание:", textX, textY, 0xFFFFFF);
        textY += lineHeight + 2;
        drawWrappedText(graphics, selected.getDescription(), textX, textY, INFO_PANEL_WIDTH - 20, lineHeight);
        textY += getWrappedTextHeight(selected.getDescription(), INFO_PANEL_WIDTH - 20, lineHeight) + 8;

        // Особенности
        if (!selected.getFeatures().isEmpty()) {
            graphics.drawString(this.font, "§6Особенности:", textX, textY, 0xFFFFFF);
            textY += lineHeight + 2;

            for (CharacterClass.Feature feature : selected.getFeatures()) {
                graphics.drawString(this.font, "§a• " + feature.getName(), textX, textY, 0xFFFFFF);
                textY += lineHeight + 1;
                drawWrappedText(graphics, "  " + feature.getDescription(), textX, textY, INFO_PANEL_WIDTH - 30, lineHeight);
                textY += getWrappedTextHeight(feature.getDescription(), INFO_PANEL_WIDTH - 30, lineHeight) + 4;
            }
            textY += 3;
        }

        // Предметы
        if (!selected.getItems().isEmpty()) {
            graphics.drawString(this.font, "§6Предметы:", textX, textY, 0xFFFFFF);
            textY += lineHeight + 2;

            for (CharacterClass.Item item : selected.getItems()) {
                graphics.drawString(this.font, "§b• " + item.getName(), textX, textY, 0xFFFFFF);
                textY += lineHeight + 1;
                drawWrappedText(graphics, "  " + item.getDescription(), textX, textY, INFO_PANEL_WIDTH - 30, lineHeight);
                textY += getWrappedTextHeight(item.getDescription(), INFO_PANEL_WIDTH - 30, lineHeight) + 4;
            }
        }
    }

    /**
     * Рисуем текст с переносом строк
     */
    private void drawWrappedText(GuiGraphics graphics, String text, int x, int y, int maxWidth, int lineHeight) {
        List<String> lines = wrapText(text, maxWidth);
        for (String line : lines) {
            graphics.drawString(this.font, line, x, y, 0xCCCCCC);
            y += lineHeight;
        }
    }

    /**
     * Получить высоту текста с переносом
     */
    private int getWrappedTextHeight(String text, int maxWidth, int lineHeight) {
        return wrapText(text, maxWidth).size() * lineHeight;
    }

    /**
     * Разбить текст на строки по ширине
     */
    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;

            if (this.font.width(testLine) <= maxWidth) {
                if (currentLine.length() > 0) currentLine.append(" ");
                currentLine.append(word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    lines.add(word);
                }
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    /**
     * Выбрать персонажа
     */
    private void selectCharacter() {
        CharacterClass selected = characters.get(selectedIndex);

        // Отправляем пакет на сервер
        ModNetworking.sendToServer(new SelectCharacterPacket(selected.getId()));

        // Закрываем меню
        this.onClose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Проверяем клик по фрескам
        if (button == 0) { // Левая кнопка мыши
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            int offsetX = -80;

            // Проверяем все видимые фрески
            int startIndex = Math.max(0, selectedIndex - 1);
            int endIndex = Math.min(characters.size(), startIndex + 3);

            for (int i = startIndex; i < endIndex; i++) {
                int offsetFromCenter = (i - selectedIndex);
                int x = centerX + offsetX - (FRESCO_WIDTH / 2) + (offsetFromCenter * (FRESCO_WIDTH + FRESCO_SPACING));
                int y = centerY - (FRESCO_HEIGHT / 2);

                // Проверяем попадание в область фрески
                if (mouseX >= x && mouseX <= x + FRESCO_WIDTH &&
                        mouseY >= y && mouseY <= y + FRESCO_HEIGHT) {
                    // Переключаемся на этот класс
                    selectedIndex = i;
                    updateButtonStates();
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESC - закрыть меню
        if (keyCode == 256) {
            this.onClose();
            return true;
        }

        // Стрелки для навигации
        if (keyCode == 263) { // LEFT
            previousCharacter();
            return true;
        }
        if (keyCode == 262) { // RIGHT
            nextCharacter();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}