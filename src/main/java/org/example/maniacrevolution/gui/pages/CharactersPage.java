package org.example.maniacrevolution.gui.pages;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.character.CharacterClass;
import org.example.maniacrevolution.character.CharacterRegistry;
import org.example.maniacrevolution.character.CharacterType;
import org.example.maniacrevolution.character.TagRegistry;
import org.example.maniacrevolution.gui.GuideScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CharactersPage extends GuidePage {
    private CharacterType currentFilter = null; // null = все
    private String selectedTag = null;
    private int scrollOffset = 0;
    private CharacterClass hoveredCharacter = null;
    private CharacterClass selectedCharacter = null;
    private int detailScrollOffset = 0;

    // Размеры фрески
    private static final int FRESCO_WIDTH = 200;
    private static final int FRESCO_HEIGHT = 500;
    private static final float FRESCO_SCALE = 0.3f; // Масштаб для списка
    private static final int SCALED_FRESCO_WIDTH = (int) (FRESCO_WIDTH * FRESCO_SCALE);
    private static final int SCALED_FRESCO_HEIGHT = (int) (FRESCO_HEIGHT * FRESCO_SCALE);

    public CharactersPage(GuideScreen parent) {
        super(parent);
    }

    @Override
    public void init(int guiLeft, int guiTop, int guiWidth, int guiHeight) {
        super.init(guiLeft, guiTop, guiWidth, guiHeight);
        scrollOffset = 0;
        detailScrollOffset = 0;
        selectedCharacter = null;
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackButton(gui, mouseX, mouseY);

        if (selectedCharacter != null) {
            renderCharacterDetails(gui, mouseX, mouseY);
        } else {
            renderCharacterList(gui, mouseX, mouseY);
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

    private void renderCharacterList(GuiGraphics gui, int mouseX, int mouseY) {
        // Заголовок
        gui.drawCenteredString(font, "§6§lПерсонажи режима",
                guiLeft + guiWidth / 2, guiTop + 28, 0xFFFFFF);

        // Фильтры по типу
        renderTypeFilters(gui, mouseX, mouseY);

        // Фильтр по тегам (если выбран тип)
        int tagFilterY = guiTop + 70;
        int listStartY = tagFilterY + 5;

        if (currentFilter != null) {
            renderTagFilter(gui, mouseX, mouseY, tagFilterY);
            listStartY = tagFilterY + 35;
        }

        // Список персонажей
        List<CharacterClass> characters = getFilteredCharacters();
        hoveredCharacter = null;

        int y = listStartY - scrollOffset;
        int entryHeight = SCALED_FRESCO_HEIGHT + 10;

        // ИСПРАВЛЕНО: Уменьшена видимая область снизу для индикатора прокрутки
        int bottomMargin = guiTop + guiHeight - 5; // Вместо -10

        gui.enableScissor(guiLeft + 10, listStartY, guiLeft + guiWidth, bottomMargin);

        for (CharacterClass character : characters) {
            if (y + entryHeight > listStartY && y < bottomMargin) {
                boolean hovered = isMouseOverCharacter(mouseX, mouseY, guiLeft + 10, y, guiWidth - 20, entryHeight, listStartY);

                if (hovered) {
                    hoveredCharacter = character;
                }

                renderCharacterEntry(gui, character, guiLeft + 10, y, hovered);
            }
            y += entryHeight;
        }

        gui.disableScissor();

        // ИСПРАВЛЕНО: Индикатор прокрутки размещён в зарезервированной области
//        if (characters.size() > 2) {
//            gui.drawString(font, "§8↑↓ Прокрутка", guiLeft + guiWidth - 85,
//                    guiTop + guiHeight - 30, 0xAAAAAA, false);
//        }
    }

    private void renderTypeFilters(GuiGraphics gui, int mouseX, int mouseY) {
        int btnY = guiTop + 45;
        int btnWidth = 85;
        int btnHeight = 18;
        int spacing = 5;
        int startX = guiLeft + (guiWidth - (btnWidth * 3 + spacing * 2)) / 2;

        renderFilterButton(gui, mouseX, mouseY, startX, btnY, btnWidth, btnHeight, "§fВсе", null);
        renderFilterButton(gui, mouseX, mouseY, startX + btnWidth + spacing, btnY, btnWidth, btnHeight, "§aВыжившие", CharacterType.SURVIVOR);
        renderFilterButton(gui, mouseX, mouseY, startX + (btnWidth + spacing) * 2, btnY, btnWidth, btnHeight, "§cМаньяки", CharacterType.MANIAC);
    }

    private void renderFilterButton(GuiGraphics gui, int mouseX, int mouseY, int x, int y, int width, int height, String text, CharacterType type) {
        boolean selected = (currentFilter == type);
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;

        int bgColor = selected ? 0xFF444444 : (hovered ? 0xFF3a3a3a : 0xFF2a2a2a);
        gui.fill(x, y, x + width, y + height, bgColor);
        gui.renderOutline(x, y, width, height, selected ? 0xFFFFAA00 : 0xFF666666);
        gui.drawCenteredString(font, text, x + width / 2, y + 5, 0xFFFFFF);
    }

    private void renderTagFilter(GuiGraphics gui, int mouseX, int mouseY, int y) {
        List<String> availableTags = getAvailableTags();

        if (availableTags.isEmpty()) return;

        int tagX = guiLeft + 10;
        int tagY = y;
        int maxTagY = y + 36; // ИСПРАВЛЕНО: Максимум 2 строки тегов

        gui.drawString(font, "§7Фильтр:", tagX, tagY + 4, 0xAAAAAA, false);
        tagX += font.width("Фильтр: ") + 5;

        int displayedTags = 0;
        int maxTags = 12; // ИСПРАВЛЕНО: Максимум 12 тегов

        for (String tag : availableTags) {
            if (displayedTags >= maxTags) break; // ИСПРАВЛЕНО: Ограничение

            boolean selected = tag.equals(selectedTag);
            int tagWidth = font.width(tag) + 8;

            boolean hovered = mouseX >= tagX && mouseX < tagX + tagWidth && mouseY >= tagY && mouseY < tagY + 16;

            int bgColor = selected ? 0xFF555555 : (hovered ? 0xFF3a3a3a : 0xFF2a2a2a);
            gui.fill(tagX, tagY, tagX + tagWidth, tagY + 16, bgColor);
            gui.renderOutline(tagX, tagY, tagWidth, 16, selected ? 0xFFFFAA00 : 0xFF666666);
            gui.drawCenteredString(font, "§f" + tag, tagX + tagWidth / 2, tagY + 4, 0xFFFFFF);

            tagX += tagWidth + 3;
            displayedTags++;

            // Переносим на новую строку если не влезает
            if (tagX > guiLeft + guiWidth - 50) {
                tagX = guiLeft + 10;
                tagY += 18;

                // ИСПРАВЛЕНО: Проверяем, не вышли ли за пределы
                if (tagY >= maxTagY) break;
            }
        }
    }

    private void renderCharacterEntry(GuiGraphics gui, CharacterClass character, int x, int y, boolean hovered) {
        int width = guiWidth - 20;
        int height = SCALED_FRESCO_HEIGHT + 5;

        // Фон
        gui.fill(x, y, x + width, y + height, hovered ? 0xAA444444 : 0x80333333);
        if (hovered) {
            gui.renderOutline(x, y, width, height, 0xFFFFAA00);
        }

        // Фреска
        renderFresco(gui, character, x + 5, y + 3, FRESCO_SCALE);

        // Информация справа от фрески
        int infoX = x + SCALED_FRESCO_WIDTH + 15;
        int infoY = y + 10;

        // Имя
        String typeColor = character.getType() == CharacterType.SURVIVOR ? "§a" : "§c";
        gui.drawString(font, typeColor + "§l" + character.getName(), infoX, infoY, 0xFFFFFF, false);
        infoY += 12;

        // Тип
        String combatType = getCombatType(character);
        if (combatType != null) {
            gui.drawString(font, "§7" + combatType, infoX, infoY, 0xAAAAAA, false);
            infoY += 11;
        }

        // Сложность
        gui.drawString(font, "§7Сложность: " + character.getDifficultyStars(), infoX, infoY, 0xFFFFFF, false);
        infoY += 11;

        // Теги (первые 3)
        List<String> tags = character.getTags();
        if (!tags.isEmpty()) {
            String tagText = tags.stream().limit(3).collect(Collectors.joining("§7, §e"));
            gui.drawString(font, "§e" + tagText, infoX, infoY, 0xFFFFFF, false);
        }

        // Подсказка при наведении
        if (hovered) {
            gui.drawString(font, "§e§oКлик для подробностей →", infoX, y + height - 15, 0xFFAA00, false);
        }
    }

    private void renderFresco(GuiGraphics gui, CharacterClass character, int x, int y, float scale) {
        ResourceLocation texture = character.getFrescoTexture();

        try {
            gui.pose().pushPose();
            gui.pose().translate(x, y, 0);
            gui.pose().scale(scale, scale, 1.0f);

            RenderSystem.setShaderTexture(0, texture);
            RenderSystem.enableBlend();
            gui.blit(texture, 0, 0, 0, 0, FRESCO_WIDTH, FRESCO_HEIGHT, FRESCO_WIDTH, FRESCO_HEIGHT);
            RenderSystem.disableBlend();

            gui.pose().popPose();
        } catch (Exception e) {
            // Placeholder
            int scaledW = (int) (FRESCO_WIDTH * scale);
            int scaledH = (int) (FRESCO_HEIGHT * scale);
            gui.fill(x, y, x + scaledW, y + scaledH, 0xFF555555);
            gui.renderOutline(x, y, scaledW, scaledH, 0xFF888888);
            gui.drawCenteredString(font, "§8?", x + scaledW / 2, y + scaledH / 2 - 5, 0xFFFFFF);
        }
    }

    private void renderCharacterDetails(GuiGraphics gui, int mouseX, int mouseY) {
        // Кнопка назад
        int btnX = guiLeft + 5;
        int btnY = guiTop + guiHeight - 25;
        boolean hovered = mouseX >= btnX && mouseX < btnX + 70 && mouseY >= btnY && mouseY < btnY + 20;

        gui.fill(btnX, btnY, btnX + 70, btnY + 20, hovered ? 0xFF555555 : 0xFF333333);
        gui.renderOutline(btnX, btnY, 70, 20, 0xFF888888);
        gui.drawCenteredString(font, "← Назад", btnX + 35, btnY + 6, 0xFFFFFF);

        // ИСПРАВЛЕНО: Рассчитываем точную высоту контента
        int contentStartY = guiTop + 35;
        int maxWidth = guiWidth - 30;

        // Временный Y для расчёта высоты
        int calculatedY = 0;
//
//        // Фреска
//        calculatedY += (int)(FRESCO_HEIGHT * 0.5f) + 10;

        // Имя
        calculatedY += 15;

        // Сложность
        calculatedY += 15;

        // Описание
        calculatedY += wrapText(selectedCharacter.getDescription(), maxWidth).size() * 11 + 10;

        // Теги
        if (!selectedCharacter.getTags().isEmpty()) {
            calculatedY += 12; // Заголовок
            for (String tag : selectedCharacter.getTags()) {
                String desc = TagRegistry.getTagDescription(tag);
                String fullText = "● " + tag + ": " + desc;
                // ИСПРАВЛЕНО: Учитываем перенос длинных тегов
                calculatedY += wrapText(fullText, maxWidth - 5).size() * 11;
            }
            calculatedY += 10;
        }

        // Особенности
        if (!selectedCharacter.getFeatures().isEmpty()) {
            calculatedY += 12; // Заголовок
            for (CharacterClass.Feature feature : selectedCharacter.getFeatures()) {
                calculatedY += 11; // Название
                calculatedY += wrapText(feature.getDescription(), maxWidth - 30).size() * 11 + 5;
            }
            calculatedY += 10;
        }

        // Предметы
        if (!selectedCharacter.getItems().isEmpty()) {
            calculatedY += 12; // Заголовок
            for (CharacterClass.Item item : selectedCharacter.getItems()) {
                calculatedY += 11; // Название
                calculatedY += wrapText(item.getDescription(), maxWidth - 30).size() * 11 + 5;
            }
            calculatedY += 10;
        }

        // ИСПРАВЛЕНО: Рассчитываем максимальный скролл
        int visibleHeight = guiHeight - 65;
        int totalContentHeight = calculatedY;
        int maxScroll = Math.max(0, totalContentHeight - visibleHeight);

        // ИСПРАВЛЕНО: Ограничиваем скролл
        detailScrollOffset = Math.min(detailScrollOffset, maxScroll);

        // Область скролла
        gui.enableScissor(guiLeft + 5, guiTop + 30, guiLeft + guiWidth - 5, guiTop + guiHeight - 30);

        int y = contentStartY - detailScrollOffset;

        // Фреска (большая)
//        int frescoX = guiLeft + (guiWidth - (int)(FRESCO_WIDTH * 0.5f)) / 2;
//        renderFresco(gui, selectedCharacter, frescoX, y, 0.5f);
//        y += (int)(FRESCO_HEIGHT * 0.5f) + 10;

        // Имя
        String typeColor = selectedCharacter.getType() == CharacterType.SURVIVOR ? "§a" : "§c";
        gui.drawCenteredString(font, typeColor + "§l" + selectedCharacter.getName(),
                guiLeft + guiWidth / 2, y, 0xFFFFFF);
        y += 15;

        // Сложность
        gui.drawCenteredString(font, "§7Сложность: " + selectedCharacter.getDifficultyStars(),
                guiLeft + guiWidth / 2, y, 0xFFFFFF);
        y += 15;

        // Описание
        List<String> descLines = wrapText(selectedCharacter.getDescription(), maxWidth);
        for (String line : descLines) {
            gui.drawString(font, "§7" + line, guiLeft + 15, y, 0xFFFFFF, false);
            y += 11;
        }
        y += 10;

        // Теги
        if (!selectedCharacter.getTags().isEmpty()) {
            gui.drawString(font, "§e§lТеги:", guiLeft + 15, y, 0xFFFFFF, false);
            y += 12;

            for (String tag : selectedCharacter.getTags()) {
                String desc = TagRegistry.getTagDescription(tag);
                String fullText = "§e● " + tag + "§7: " + desc;

                // ИСПРАВЛЕНО: Переносим длинные теги
                List<String> tagLines = wrapText(fullText, maxWidth - 5);
                for (int i = 0; i < tagLines.size(); i++) {
                    gui.drawString(font, "§7" + tagLines.get(i), guiLeft + 20, y, 0xFFFFFF, false);
                    y += 11;
                }
            }
            y += 10;
        }

        // Особенности
        if (!selectedCharacter.getFeatures().isEmpty()) {
            gui.drawString(font, "§e§lОсобенности:", guiLeft + 15, y, 0xFFFFFF, false);
            y += 12;

            for (CharacterClass.Feature feature : selectedCharacter.getFeatures()) {
                gui.drawString(font, "§6● " + feature.getName(), guiLeft + 20, y, 0xFFFFFF, false);
                y += 11;

                List<String> featureLines = wrapText(feature.getDescription(), maxWidth - 30);
                for (String line : featureLines) {
                    gui.drawString(font, "§7  " + line, guiLeft + 25, y, 0xFFFFFF, false);
                    y += 11;
                }
                y += 5;
            }
            y += 10;
        }

        // Предметы
        if (!selectedCharacter.getItems().isEmpty()) {
            gui.drawString(font, "§e§lПредметы:", guiLeft + 15, y, 0xFFFFFF, false);
            y += 12;

            for (CharacterClass.Item item : selectedCharacter.getItems()) {
                gui.drawString(font, "§b● " + item.getName(), guiLeft + 20, y, 0xFFFFFF, false);
                y += 11;

                List<String> itemLines = wrapText(item.getDescription(), maxWidth - 30);
                for (String line : itemLines) {
                    gui.drawString(font, "§7  " + line, guiLeft + 25, y, 0xFFFFFF, false);
                    y += 11;
                }
                y += 5;
            }
        }

        gui.disableScissor();

        // ИСПРАВЛЕНО: Показываем индикатор только если есть что прокручивать
        if (totalContentHeight > visibleHeight) {
            gui.drawString(font, "§8↑↓ Прокрутка", guiLeft + guiWidth - 85,
                    guiTop + guiHeight - 32, 0xAAAAAA, false);
        }
    }

    @Override
    public void renderTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        if (selectedCharacter == null && hoveredCharacter != null) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal("§e" + hoveredCharacter.getName()));

            String combatType = getCombatType(hoveredCharacter);
            if (combatType != null) {
                tooltip.add(Component.literal("§7" + combatType));
            }

            tooltip.add(Component.literal("§7Клик для подробностей"));
            gui.renderComponentTooltip(font, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Кнопка "Назад на главную"
            if (mouseX >= guiLeft + 5 && mouseX < guiLeft + 85 && mouseY >= guiTop + 5 && mouseY < guiTop + 23) {
                parent.switchPage(PageType.MAIN);
                return true;
            }

            // Кнопка "Назад к списку"
            if (selectedCharacter != null) {
                if (mouseX >= guiLeft + 5 && mouseX < guiLeft + 75 &&
                        mouseY >= guiTop + guiHeight - 25 && mouseY < guiTop + guiHeight - 5) {
                    selectedCharacter = null;
                    detailScrollOffset = 0;
                    return true;
                }
            }

            // Фильтры типа
            if (selectedCharacter == null) {
                int btnY = guiTop + 45;
                int btnWidth = 85;
                int spacing = 5;
                int startX = guiLeft + (guiWidth - (btnWidth * 3 + spacing * 2)) / 2;

                if (mouseY >= btnY && mouseY < btnY + 18) {
                    if (mouseX >= startX && mouseX < startX + btnWidth) {
                        currentFilter = null;
                        selectedTag = null;
                        scrollOffset = 0;
                        return true;
                    } else if (mouseX >= startX + btnWidth + spacing && mouseX < startX + (btnWidth + spacing) * 2) {
                        currentFilter = CharacterType.SURVIVOR;
                        selectedTag = null;
                        scrollOffset = 0;
                        return true;
                    } else if (mouseX >= startX + (btnWidth + spacing) * 2 && mouseX < startX + (btnWidth + spacing) * 3) {
                        currentFilter = CharacterType.MANIAC;
                        selectedTag = null;
                        scrollOffset = 0;
                        return true;
                    }
                }

                // Фильтр по тегам
                if (currentFilter != null) {
                    int tagY = guiTop + 70;
                    int tagX = guiLeft + 10 + font.width("Фильтр: ") + 5;

                    for (String tag : getAvailableTags()) {
                        int tagWidth = font.width(tag) + 8;

                        if (mouseX >= tagX && mouseX < tagX + tagWidth && mouseY >= tagY && mouseY < tagY + 16) {
                            selectedTag = selectedTag != null && selectedTag.equals(tag) ? null : tag;
                            scrollOffset = 0;
                            return true;
                        }

                        tagX += tagWidth + 3;
                        if (tagX > guiLeft + guiWidth - 50) {
                            tagX = guiLeft + 10;
                            tagY += 18;
                        }
                    }
                }
            }

            // Клик на персонажа
            if (hoveredCharacter != null && selectedCharacter == null) {
                selectedCharacter = hoveredCharacter;
                detailScrollOffset = 0;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (selectedCharacter != null) {
            int maxScroll = Math.max(0, 800 - (guiHeight - 65));
            detailScrollOffset = (int) Math.max(0, Math.min(maxScroll, detailScrollOffset - delta * 30));
        } else {
            List<CharacterClass> characters = getFilteredCharacters();
            int maxScroll = Math.max(0, characters.size() * (SCALED_FRESCO_HEIGHT + 10) - 150);
            scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - delta * 30));
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (selectedCharacter != null && keyCode == 256) {
            selectedCharacter = null;
            detailScrollOffset = 0;
            return true;
        }
        return false;
    }

    private List<CharacterClass> getFilteredCharacters() {
        List<CharacterClass> characters = currentFilter != null
                ? CharacterRegistry.getClassesByType(currentFilter)
                : new ArrayList<>(CharacterRegistry.getAllClasses());

        if (selectedTag != null) {
            characters = characters.stream()
                    .filter(c -> c.getTags().contains(selectedTag))
                    .collect(Collectors.toList());
        }

        return characters;
    }

    private List<String> getAvailableTags() {
        return getFilteredCharacters().stream()
                .flatMap(c -> c.getTags().stream())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private String getCombatType(CharacterClass character) {
        if (character.getTags().contains("Ближний бой")) return "Ближний бой";
        if (character.getTags().contains("Дальний бой")) return "Дальний бой";
        return null;
    }

    private boolean isMouseOverCharacter(int mouseX, int mouseY, int x, int y, int width, int height, int minY) {
        return mouseX >= x && mouseX < x + width
                && mouseY >= y && mouseY < y + height
                && mouseY >= minY && mouseY < guiTop + guiHeight - 10;
    }
}