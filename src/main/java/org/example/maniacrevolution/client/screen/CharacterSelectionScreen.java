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
import org.example.maniacrevolution.character.TagRegistry;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.SelectCharacterPacket;

import java.util.*;
import java.util.stream.Collectors;

public class CharacterSelectionScreen extends Screen {
    private final CharacterType type;
    private final List<CharacterClass> allCharacters;
    private List<CharacterClass> filteredCharacters;
    private int selectedIndex = 0;
    private Set<String> activeFilters = new HashSet<>();

    // Адаптивные размеры
    private int frescoWidth;
    private int frescoHeight;
    private int infoPanelWidth;

    private Button leftArrowButton;
    private Button rightArrowButton;
    private Button selectButton;
    private List<Button> filterButtons = new ArrayList<>();

    public CharacterSelectionScreen(CharacterType type) {
        super(Component.literal("Выбор класса: " + type.getDisplayName()));
        this.type = type;
        this.allCharacters = CharacterRegistry.getClassesByType(type);
        this.filteredCharacters = new ArrayList<>(allCharacters);
    }

    @Override
    protected void init() {
        super.init();

        // Вычисляем адаптивные размеры
        double guiScale = this.minecraft.getWindow().getGuiScale();
        calculateSizes(guiScale);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int arrowWidth = 50;
        int arrowHeight = 20;
        int gap = 10;

        // Левая стрелка - слева от фрески
        leftArrowButton = Button.builder(Component.literal("◄"), b -> previousCharacter())
                .bounds(centerX - frescoWidth / 2 - arrowWidth - gap, centerY, arrowWidth, arrowHeight)
                .build();

        // Правая стрелка - справа от фрески
        rightArrowButton = Button.builder(Component.literal("►"), b -> nextCharacter())
                .bounds(centerX + frescoWidth / 2 + gap, centerY, arrowWidth, arrowHeight)
                .build();

        // Кнопка выбора внизу
        selectButton = Button.builder(Component.literal("Выбрать"), b -> selectCharacter())
                .bounds(centerX - 60, this.height - 50, 120, 20)
                .build();

        this.addRenderableWidget(leftArrowButton);
        this.addRenderableWidget(rightArrowButton);
        this.addRenderableWidget(selectButton);

        // Создаём кнопки фильтров
        createFilterButtons();

        updateButtonStates();
    }

    private void calculateSizes(double guiScale) {
        // Базовые размеры для scale 2
        if (guiScale <= 2.0) {
            frescoWidth = 120;
            frescoHeight = 280;
            infoPanelWidth = 350;
        } else if (guiScale <= 3.0) {
            // Для scale 3 уменьшаем
            frescoWidth = 100;
            frescoHeight = 233;
            infoPanelWidth = 280;
        } else {
            // Для больших scale ещё меньше
            frescoWidth = 80;
            frescoHeight = 187;
            infoPanelWidth = 220;
        }
    }

    private void createFilterButtons() {
        // Очищаем старые кнопки
        filterButtons.forEach(this::removeWidget);
        filterButtons.clear();

        // Собираем все уникальные тэги
        Set<String> allTags = new HashSet<>();
        for (CharacterClass character : allCharacters) {
            allTags.addAll(character.getTags());
        }

        List<String> sortedTags = new ArrayList<>(allTags);
        Collections.sort(sortedTags);

        // Создаём кнопки фильтров
        int startX = 10;
        int startY = 50;
        int buttonWidth = 100;
        int buttonHeight = 20;
        int spacing = 5;
        int x = startX;
        int y = startY;

        for (String tag : sortedTags) {
            boolean isActive = activeFilters.contains(tag);

            Button filterButton = Button.builder(
                    Component.literal((isActive ? "§a✓ " : "§7") + tag),
                    b -> toggleFilter(tag)
            ).bounds(x, y, buttonWidth, buttonHeight).build();

            filterButtons.add(filterButton);
            this.addRenderableWidget(filterButton);

            y += buttonHeight + spacing;

            // Если вышли за пределы экрана - новый столбец
            if (y > this.height - 100) {
                y = startY;
                x += buttonWidth + spacing;
            }
        }
    }

    private void toggleFilter(String tag) {
        if (activeFilters.contains(tag)) {
            activeFilters.remove(tag);
        } else {
            activeFilters.add(tag);
        }

        applyFilters();
        createFilterButtons();

        // Сброс выбранного индекса если вышли за пределы
        if (selectedIndex >= filteredCharacters.size()) {
            selectedIndex = Math.max(0, filteredCharacters.size() - 1);
        }

        updateButtonStates();
    }

    private void applyFilters() {
        if (activeFilters.isEmpty()) {
            filteredCharacters = new ArrayList<>(allCharacters);
        } else {
            filteredCharacters = allCharacters.stream()
                    .filter(c -> c.getTags().stream().anyMatch(activeFilters::contains))
                    .collect(Collectors.toList());
        }
    }

    private void previousCharacter() {
        if (selectedIndex > 0) {
            selectedIndex--;
            updateButtonStates();
        }
    }

    private void nextCharacter() {
        if (selectedIndex < filteredCharacters.size() - 1) {
            selectedIndex++;
            updateButtonStates();
        }
    }

    private void updateButtonStates() {
        leftArrowButton.active = selectedIndex > 0;
        rightArrowButton.active = selectedIndex < filteredCharacters.size() - 1;
    }

    private void selectCharacter() {
        if (filteredCharacters.isEmpty()) return;

        CharacterClass selected = filteredCharacters.get(selectedIndex);
        ModNetworking.sendToServer(new SelectCharacterPacket(selected.getId()));
        this.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        drawGradientBackground(graphics);

        if (!filteredCharacters.isEmpty()) {
            drawFresco(graphics);
            drawInfoPanel(graphics, mouseX, mouseY);
        } else {
            graphics.drawCenteredString(this.font, "§cНет персонажей с выбранными тэгами",
                    this.width / 2, this.height / 2, 0xFFFFFF);
        }

        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        if (!filteredCharacters.isEmpty()) {
            String counter = (selectedIndex + 1) + " / " + filteredCharacters.size();
            graphics.drawCenteredString(this.font, counter, this.width / 2, this.height - 65, 0xAAAAAA);
        }

        // Рендерим подсказки для тэгов
        renderTagTooltips(graphics, mouseX, mouseY);
    }

    private void drawGradientBackground(GuiGraphics graphics) {
        int color1 = type == CharacterType.SURVIVOR ? 0x40003344 : 0x40440000;
        int color2 = type == CharacterType.SURVIVOR ? 0x80004466 : 0x80660000;
        graphics.fillGradient(0, 0, this.width, this.height, color1, color2);
    }

    private void drawFresco(GuiGraphics graphics) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Показываем 3 фрески (предыдущая, текущая, следующая)
        int startIndex = Math.max(0, selectedIndex - 1);
        int endIndex = Math.min(filteredCharacters.size(), selectedIndex + 2);

        for (int i = startIndex; i < endIndex; i++) {
            CharacterClass character = filteredCharacters.get(i);

            // Позиция карточки относительно центра
            int offsetFromCenter = (i - selectedIndex);
            int spacing = 15;
            int x = centerX - frescoWidth / 2 + (offsetFromCenter * (frescoWidth + spacing));
            int y = centerY - frescoHeight / 2;

            // Прозрачность для боковых карточек
            float alpha = (i == selectedIndex) ? 1.0f : 0.6f;

            // Рамка
            if (i == selectedIndex) {
                int color = type == CharacterType.SURVIVOR ? 0xFF00CCFF : 0xFFFFD700;
                graphics.fill(x - 3, y - 3, x + frescoWidth + 3, y, color);
                graphics.fill(x - 3, y + frescoHeight, x + frescoWidth + 3, y + frescoHeight + 3, color);
                graphics.fill(x - 3, y, x, y + frescoHeight, color);
                graphics.fill(x + frescoWidth, y, x + frescoWidth + 3, y + frescoHeight, color);
            } else {
                // Тонкая серая рамка для боковых
                graphics.fill(x - 1, y - 1, x + frescoWidth + 1, y, 0xFF808080);
                graphics.fill(x - 1, y + frescoHeight, x + frescoWidth + 1, y + frescoHeight + 1, 0xFF808080);
                graphics.fill(x - 1, y, x, y + frescoHeight, 0xFF808080);
                graphics.fill(x + frescoWidth, y, x + frescoWidth + 1, y + frescoHeight, 0xFF808080);
            }

            // Текстура
            ResourceLocation texture = character.getFrescoTexture();
            graphics.pose().pushPose();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);

            try {
                graphics.blit(texture, x, y, 0, 0, frescoWidth, frescoHeight,
                        frescoWidth, frescoHeight);
            } catch (Exception e) {
                graphics.fill(x, y, x + frescoWidth, y + frescoHeight, 0xFF333333);
                if (i == selectedIndex) {
                    graphics.drawCenteredString(this.font, "Нет текстуры",
                            x + frescoWidth / 2, y + frescoHeight / 2, 0xFFFFFF);
                }
            }

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            graphics.pose().popPose();

            // Имя только для выбранной
            if (i == selectedIndex) {
                graphics.drawCenteredString(this.font, character.getName(),
                        x + frescoWidth / 2, y - 15, 0xFFFFFF);
            }
        }
    }

    private void drawInfoPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        CharacterClass selected = filteredCharacters.get(selectedIndex);

        int panelX = this.width - infoPanelWidth - 20;
        int panelY = 60;
        int panelHeight = this.height - 140;

        graphics.fill(panelX, panelY, panelX + infoPanelWidth, panelY + panelHeight, 0xCC000000);

        int textX = panelX + 10;
        int textY = panelY + 10;
        int lineHeight = 9;
        int maxWidth = infoPanelWidth - 20;

        // Имя
        String titleColor = type == CharacterType.SURVIVOR ? "§b§l" : "§e§l";
        graphics.drawString(this.font, titleColor + selected.getName(), textX, textY, 0xFFFFFF);
        textY += 16;

        // Тэги с переносом
        if (!selected.getTags().isEmpty()) {
            graphics.drawString(this.font, "§7Тэги:", textX, textY, 0xFFFFFF);
            textY += 10;

            StringBuilder currentLine = new StringBuilder();
            for (int i = 0; i < selected.getTags().size(); i++) {
                String tag = selected.getTags().get(i);
                String tagText = tag + (i < selected.getTags().size() - 1 ? ", " : "");

                if (this.font.width(currentLine + tagText) > maxWidth) {
                    graphics.drawString(this.font, "§f" + currentLine.toString(), textX, textY, 0xFFFFFF);
                    textY += lineHeight;
                    currentLine = new StringBuilder(tagText);
                } else {
                    currentLine.append(tagText);
                }
            }
            if (currentLine.length() > 0) {
                graphics.drawString(this.font, "§f" + currentLine.toString(), textX, textY, 0xFFFFFF);
                textY += lineHeight;
            }
            textY += 3;
        }

        textY += 5;

        // Описание
        graphics.drawString(this.font, "§6Описание:", textX, textY, 0xFFFFFF);
        textY += lineHeight + 2;
        textY += drawWrappedText(graphics, selected.getDescription(), textX, textY, maxWidth, lineHeight);
        textY += 8;

        // Особенности
        if (!selected.getFeatures().isEmpty()) {
            graphics.drawString(this.font, "§6Особенности:", textX, textY, 0xFFFFFF);
            textY += lineHeight + 2;

            for (CharacterClass.Feature feature : selected.getFeatures()) {
                graphics.drawString(this.font, "§a• " + feature.getName(), textX, textY, 0xFFFFFF);
                textY += lineHeight + 1;
                textY += drawWrappedText(graphics, "  " + feature.getDescription(), textX, textY, maxWidth - 10, lineHeight);
                textY += 4;
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
                textY += drawWrappedText(graphics, "  " + item.getDescription(), textX, textY, maxWidth - 10, lineHeight);
                textY += 4;
            }
        }
    }

    private int drawWrappedText(GuiGraphics graphics, String text, int x, int y, int maxWidth, int lineHeight) {
        List<String> lines = wrapText(text, maxWidth);
        for (String line : lines) {
            graphics.drawString(this.font, line, x, y, 0xCCCCCC);
            y += lineHeight;
        }
        return lines.size() * lineHeight;
    }

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

    private void renderTagTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        for (Button button : filterButtons) {
            if (button.isMouseOver(mouseX, mouseY)) {
                String tagText = button.getMessage().getString().replace("§a✓ ", "").replace("§7", "");
                String description = TagRegistry.getTagDescription(tagText);

                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.literal("§e" + tagText));
                tooltip.add(Component.literal("§7" + description));

                graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
                break;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && !filteredCharacters.isEmpty()) {
            int centerX = this.width / 2;
            int centerY = this.height / 2;

            // Проверяем клик по всем видимым фрескам
            int startIndex = Math.max(0, selectedIndex - 1);
            int endIndex = Math.min(filteredCharacters.size(), selectedIndex + 2);

            for (int i = startIndex; i < endIndex; i++) {
                int offsetFromCenter = (i - selectedIndex);
                int spacing = 15;
                int x = centerX - frescoWidth / 2 + (offsetFromCenter * (frescoWidth + spacing));
                int y = centerY - frescoHeight / 2;

                if (mouseX >= x && mouseX <= x + frescoWidth &&
                        mouseY >= y && mouseY <= y + frescoHeight) {
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
        if (keyCode == 256) {
            this.onClose();
            return true;
        }

        if (keyCode == 263) {
            previousCharacter();
            return true;
        }
        if (keyCode == 262) {
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