package org.example.maniacrevolution.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.example.maniacrevolution.map.MapData;
import org.example.maniacrevolution.map.MapRegistry;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packet.PlayerVotePacket;

import java.util.*;

public class MapVotingScreen extends Screen {
    private static final int CARD_WIDTH = 160;
    private static final int CARD_HEIGHT = 220; // Увеличено с 200 до 220
    private static final int CARD_SPACING = 20;
    private static final int LOGO_HEIGHT = 100;
    private static final int DESCRIPTION_HEIGHT = 60; // Высота области описания
    private static final int MAX_CARDS_VISIBLE = 4; // Максимум карт на экране одновременно

    private final List<MapData> maps;
    private int selectedIndex = -1; // Текущая выбранная карта (жёлтая обводка)
    private int votedIndex = -1; // Карта за которую проголосовали (зелёная обводка)
    private int scrollOffset = 0; // Смещение для прокрутки
    private int timeRemaining;
    private Map<String, Integer> voteCount;

    // Для прокрутки описания карт
    private final Map<Integer, Integer> descriptionScrolls = new HashMap<>();
    private int hoveredCardIndex = -1;

    private Button selectButton;
    private Button leftArrow;
    private Button rightArrow;

    // Для анимации результата
    private boolean showingResult = false;
    private String winnerMapId;
    private List<String> tiedMaps = new ArrayList<>();
    private float animationTime = 0;
    private static final float ANIMATION_DURATION = 5.0f;
    private int resultDisplayTime = 0;
    private static final int RESULT_DISPLAY_DURATION = 100; // 5 секунд (100 тиков)

    public MapVotingScreen(int timeRemaining, Map<String, Integer> voteCount) {
        super(Component.literal("Голосование за карту"));
        this.maps = new ArrayList<>(MapRegistry.getAllMaps());
        this.timeRemaining = timeRemaining;
        this.voteCount = new HashMap<>(voteCount);
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Кнопка выбора - меняет текст в зависимости от состояния
        String buttonText = votedIndex >= 0 ? "Перевыбрать" : "Выбрать";
        this.selectButton = Button.builder(
                        Component.literal(buttonText),
                        btn -> onSelectMap())
                .bounds(centerX - 60, this.height - 40, 120, 20)
                .build();
        this.selectButton.active = selectedIndex >= 0;
        addRenderableWidget(selectButton);

        // Показываем стрелки только если карт больше чем влезает
        if (maps.size() > MAX_CARDS_VISIBLE) {
            this.leftArrow = Button.builder(
                            Component.literal("<"),
                            btn -> scrollLeft())
                    .bounds(20, centerY - 20, 30, 40)
                    .build();
            addRenderableWidget(leftArrow);

            this.rightArrow = Button.builder(
                            Component.literal(">"),
                            btn -> scrollRight())
                    .bounds(this.width - 50, centerY - 20, 30, 40)
                    .build();
            addRenderableWidget(rightArrow);

            updateArrowButtons();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Полупрозрачный фон
        graphics.fill(0, 0, this.width, this.height, 0xAA000000);

        if (showingResult) {
            renderResultAnimation(graphics, partialTick);
        } else {
            renderVoting(graphics, mouseX, mouseY);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void tick() {
        super.tick();

        // Отсчет времени показа результата
        if (showingResult) {
            resultDisplayTime++;
            if (resultDisplayTime >= RESULT_DISPLAY_DURATION) {
                this.onClose();
            }
        }
    }

    private void renderVoting(GuiGraphics graphics, int mouseX, int mouseY) {
        int centerX = this.width / 2;

        // Заголовок
        graphics.drawCenteredString(this.font, "Голосование за карту",
                centerX, 30, 0xFFFFFF);

        // Таймер
        int minutes = timeRemaining / 60;
        int seconds = timeRemaining % 60;
        String timerText = String.format("%02d:%02d", minutes, seconds);
        graphics.drawCenteredString(this.font, timerText, centerX, 50, 0xFF6666);

        // Отрисовка всех карточек
        renderAllCards(graphics, mouseX, mouseY);
    }

    private void renderAllCards(GuiGraphics graphics, int mouseX, int mouseY) {
        int visibleCards = Math.min(maps.size(), MAX_CARDS_VISIBLE);
        int totalWidth = visibleCards * CARD_WIDTH + (visibleCards - 1) * CARD_SPACING;
        int startX = (this.width - totalWidth) / 2;
        int startY = (this.height - CARD_HEIGHT) / 2;

        // Определяем диапазон видимых карт
        int endIndex = Math.min(scrollOffset + MAX_CARDS_VISIBLE, maps.size());

        // Сбрасываем hoveredCardIndex
        hoveredCardIndex = -1;

        for (int i = scrollOffset; i < endIndex; i++) {
            MapData map = maps.get(i);
            int cardX = startX + (i - scrollOffset) * (CARD_WIDTH + CARD_SPACING);
            int cardY = startY;

            boolean isHovered = mouseX >= cardX && mouseX <= cardX + CARD_WIDTH &&
                    mouseY >= cardY && mouseY <= cardY + CARD_HEIGHT;

            if (isHovered) {
                hoveredCardIndex = i;
            }

            boolean isSelectedNow = i == selectedIndex; // Жёлтая обводка
            boolean isVoted = i == votedIndex; // Зелёная обводка

            renderCard(graphics, map, cardX, cardY, isHovered, isSelectedNow, isVoted, i);
        }
    }

    private void renderCard(GuiGraphics graphics, MapData map, int x, int y, boolean hovered, boolean selected, boolean voted, int cardIndex) {
        // Определяем цвет обводки
        int borderColor;
        if (voted) {
            // Зелёная обводка - уже проголосовали за эту карту
            borderColor = 0xFF00FF00;
        } else if (selected) {
            // Жёлтая обводка - выбрана, но еще не подтверждена
            borderColor = 0xFFFFFF00;
        } else if (hovered) {
            // Белая обводка при наведении
            borderColor = 0xFFFFFFFF;
        } else {
            // Серая обводка по умолчанию
            borderColor = 0xFF666666;
        }

        // Фон карточки
        graphics.fill(x - 2, y - 2, x + CARD_WIDTH + 2, y + CARD_HEIGHT + 2, borderColor);
        graphics.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, 0xFF333333);

        // Логотип карты
        try {
            RenderSystem.setShaderTexture(0, map.getLogoTexture());
            graphics.blit(map.getLogoTexture(), x + 10, y + 10, 0, 0,
                    CARD_WIDTH - 20, LOGO_HEIGHT, CARD_WIDTH - 20, LOGO_HEIGHT);
        } catch (Exception e) {
            // Если текстура не загрузилась - рисуем заглушку
            graphics.fill(x + 10, y + 10, x + CARD_WIDTH - 10, y + LOGO_HEIGHT + 10, 0xFF555555);
        }

        // Название
        graphics.drawCenteredString(this.font, map.getName(),
                x + CARD_WIDTH / 2, y + LOGO_HEIGHT + 15, 0xFFFFFF);

        // Описание с прокруткой
        int descY = y + LOGO_HEIGHT + 30;
        renderScrollableDescription(graphics, map.getDescription(), x + 10, descY, CARD_WIDTH - 20, DESCRIPTION_HEIGHT, cardIndex);

        // Количество голосов
        int votes = voteCount.getOrDefault(map.getId(), 0);
        String voteText = "Голосов: " + votes;
        graphics.drawCenteredString(this.font, voteText,
                x + CARD_WIDTH / 2, y + CARD_HEIGHT - 20, 0xFFFF00);

        // Показываем статус если проголосовали
        if (voted) {
            graphics.drawCenteredString(this.font, "✓ Ваш голос",
                    x + CARD_WIDTH / 2, y + CARD_HEIGHT - 35, 0x00FF00);
        }
    }

    private void renderScrollableDescription(GuiGraphics graphics, String text, int x, int y, int maxWidth, int maxHeight, int cardIndex) {
        // Разбиваем текст на строки
        List<String> lines = wrapText(text, maxWidth);

        // Получаем текущее смещение прокрутки для этой карты
        int scrollOffset = descriptionScrolls.getOrDefault(cardIndex, 0);

        // Включаем scissors для ограничения области отрисовки
        graphics.enableScissor(x, y, x + maxWidth, y + maxHeight);

        // Отрисовываем строки с учётом прокрутки
        int lineHeight = 10;
        int totalLines = lines.size();
        int visibleLines = maxHeight / lineHeight;

        for (int i = 0; i < totalLines; i++) {
            int lineY = y + (i * lineHeight) - scrollOffset;

            // Отрисовываем только видимые строки
            if (lineY >= y - lineHeight && lineY < y + maxHeight) {
                graphics.drawString(this.font, lines.get(i), x, lineY, 0xAAAAAA);
            }
        }

        // Отключаем scissors
        graphics.disableScissor();

        // Рисуем индикатор прокрутки если нужно
        if (totalLines > visibleLines) {
            int scrollbarHeight = Math.max(10, (visibleLines * maxHeight) / totalLines);
            int maxScroll = (totalLines - visibleLines) * lineHeight;
            int scrollbarY = y + (maxScroll > 0 ? (scrollOffset * (maxHeight - scrollbarHeight)) / maxScroll : 0);

            graphics.fill(x + maxWidth + 2, y, x + maxWidth + 4, y + maxHeight, 0xFF444444);
            graphics.fill(x + maxWidth + 2, scrollbarY, x + maxWidth + 4, scrollbarY + scrollbarHeight, 0xFFAAAAAA);
        }
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
                    // Слово слишком длинное - добавляем как есть
                    lines.add(word);
                }
            }
        }
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    private void renderResultAnimation(GuiGraphics graphics, float partialTick) {
        animationTime += partialTick / 20.0f;

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        MapData winner = MapRegistry.getMapById(winnerMapId);

        // Анимация только если есть несколько карт с равными голосами
        if (tiedMaps.size() > 1) {
            graphics.drawCenteredString(this.font, "Определяем победителя...",
                    centerX, 50, 0xFFFFFF);

            float progress = Math.min(animationTime / ANIMATION_DURATION, 1.0f);

            for (int i = 0; i < tiedMaps.size(); i++) {
                MapData map = MapRegistry.getMapById(tiedMaps.get(i));
                if (map == null) continue;

                float angle = (animationTime * 2.0f + i * (360.0f / tiedMaps.size())) % 360.0f;
                float radius = 100 * (1.0f - progress * 0.8f);

                float x = centerX + (float)Math.cos(Math.toRadians(angle)) * radius;
                float y = centerY + (float)Math.sin(Math.toRadians(angle)) * radius;

                // Прозрачность
                float alpha = 1.0f;
                if (progress > 0.6f && !map.getId().equals(winnerMapId)) {
                    alpha = Math.max(0, 1.0f - (progress - 0.6f) * 2.5f);
                }

                if (alpha > 0.01f) {
                    graphics.pose().pushPose();
                    graphics.pose().translate(x - CARD_WIDTH/4, y - CARD_HEIGHT/4, 0);
                    graphics.pose().scale(0.5f, 0.5f, 1.0f);

                    renderCard(graphics, map, 0, 0, false, false, map.getId().equals(winnerMapId), i);

                    graphics.pose().popPose();
                }
            }

            // Показываем победителя после анимации
            if (winner != null && animationTime >= ANIMATION_DURATION) {
                graphics.drawCenteredString(this.font, "Победитель: " + winner.getName(),
                        centerX, centerY + 120, 0x00FF00);

                String closingText = "Закрытие через " + ((RESULT_DISPLAY_DURATION - resultDisplayTime) / 20) + "...";
                graphics.drawCenteredString(this.font, closingText,
                        centerX, centerY + 140, 0xAAAAAA);
            }
        } else {
            // Если победитель один - сразу показываем его карточку (без анимации)
            graphics.drawCenteredString(this.font, "Голосование завершено!",
                    centerX, 50, 0xFFFFFF);

            if (winner != null) {
                // Показываем большую карточку победителя в центре
                int cardX = centerX - CARD_WIDTH / 2;
                int cardY = centerY - CARD_HEIGHT / 2;

                int winnerIndex = maps.indexOf(winner);
                renderCard(graphics, winner, cardX, cardY, false, false, true, winnerIndex);

                graphics.drawCenteredString(this.font, "Победитель: " + winner.getName(),
                        centerX, cardY + CARD_HEIGHT + 20, 0x00FF00);

                String closingText = "Закрытие через " + ((RESULT_DISPLAY_DURATION - resultDisplayTime) / 20) + "...";
                graphics.drawCenteredString(this.font, closingText,
                        centerX, cardY + CARD_HEIGHT + 40, 0xAAAAAA);
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (showingResult) return super.mouseScrolled(mouseX, mouseY, delta);

        // Прокручиваем описание карты если мышь над ней
        if (hoveredCardIndex >= 0) {
            int currentScroll = descriptionScrolls.getOrDefault(hoveredCardIndex, 0);
            int scrollAmount = (int)(delta * 10); // 10 пикселей за клик колеса

            // Вычисляем максимальную прокрутку
            MapData map = maps.get(hoveredCardIndex);
            List<String> lines = wrapText(map.getDescription(), CARD_WIDTH - 20);
            int lineHeight = 10;
            int visibleLines = DESCRIPTION_HEIGHT / lineHeight;
            int maxScroll = Math.max(0, (lines.size() - visibleLines) * lineHeight);

            // Применяем прокрутку с ограничениями
            int newScroll = Math.max(0, Math.min(maxScroll, currentScroll - scrollAmount));
            descriptionScrolls.put(hoveredCardIndex, newScroll);

            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showingResult) return super.mouseClicked(mouseX, mouseY, button);

        // Проверяем клик по карточкам
        int visibleCards = Math.min(maps.size(), MAX_CARDS_VISIBLE);
        int totalWidth = visibleCards * CARD_WIDTH + (visibleCards - 1) * CARD_SPACING;
        int startX = (this.width - totalWidth) / 2;
        int startY = (this.height - CARD_HEIGHT) / 2;

        int endIndex = Math.min(scrollOffset + MAX_CARDS_VISIBLE, maps.size());

        for (int i = scrollOffset; i < endIndex; i++) {
            int cardX = startX + (i - scrollOffset) * (CARD_WIDTH + CARD_SPACING);
            int cardY = startY;

            if (mouseX >= cardX && mouseX <= cardX + CARD_WIDTH &&
                    mouseY >= cardY && mouseY <= cardY + CARD_HEIGHT) {

                // Если кликнули на ту же карту что уже выбрана - снимаем выделение
                if (selectedIndex == i && votedIndex != i) {
                    selectedIndex = -1;
                    selectButton.active = false;
                } else {
                    // Иначе выбираем новую карту
                    selectedIndex = i;
                    selectButton.active = true;
                }

                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void scrollLeft() {
        if (scrollOffset > 0) {
            scrollOffset--;
            updateArrowButtons();
        }
    }

    private void scrollRight() {
        if (scrollOffset + MAX_CARDS_VISIBLE < maps.size()) {
            scrollOffset++;
            updateArrowButtons();
        }
    }

    private void updateArrowButtons() {
        if (leftArrow != null) leftArrow.active = scrollOffset > 0;
        if (rightArrow != null) rightArrow.active = scrollOffset + MAX_CARDS_VISIBLE < maps.size();
    }

    private void onSelectMap() {
        if (selectedIndex >= 0 && selectedIndex < maps.size()) {
            MapData selected = maps.get(selectedIndex);

            // Отправляем голос на сервер
            ModNetworking.sendToServer(new PlayerVotePacket(selected.getId(), false));

            // Обновляем состояние
            votedIndex = selectedIndex;
            selectedIndex = -1;
            selectButton.active = false;

            // Обновляем текст кнопки
            selectButton.setMessage(Component.literal("Перевыбрать"));
        }
    }

    public void updateVoting(int timeRemaining, Map<String, Integer> voteCount) {
        this.timeRemaining = timeRemaining;
        this.voteCount = new HashMap<>(voteCount);
        // НЕ сбрасываем selectedIndex, votedIndex и scrollOffset!

        // Обновляем текст кнопки в зависимости от состояния
        if (selectButton != null) {
            selectButton.setMessage(Component.literal(votedIndex >= 0 ? "Перевыбрать" : "Выбрать"));
        }
    }

    public void showResult(String winnerMapId, Map<String, Integer> finalVoteCount) {
        this.winnerMapId = winnerMapId;
        this.showingResult = true;
        this.animationTime = 0;
        this.resultDisplayTime = 0;

        // Определяем карты с равным количеством голосов
        if (finalVoteCount.isEmpty()) {
            tiedMaps = new ArrayList<>();
            for (MapData map : maps) {
                tiedMaps.add(map.getId());
            }
        } else {
            int maxVotes = Collections.max(finalVoteCount.values());
            tiedMaps = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : finalVoteCount.entrySet()) {
                if (entry.getValue() == maxVotes) {
                    tiedMaps.add(entry.getKey());
                }
            }
        }

        // Убираем кнопки
        this.clearWidgets();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}