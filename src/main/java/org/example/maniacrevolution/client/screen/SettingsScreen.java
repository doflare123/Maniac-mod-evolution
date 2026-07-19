package org.example.maniacrevolution.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.example.maniacrevolution.guide.MapGuideRegistry;
import org.example.maniacrevolution.map.MapData;
import org.example.maniacrevolution.map.MapRegistry;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.GiveSettingsToAllPacket;
import org.example.maniacrevolution.network.packets.UpdateSettingsPacket;
import org.example.maniacrevolution.settings.ClientGameSettings;
import org.example.maniacrevolution.settings.GameSettings;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/**
 * Server game settings presented as a HUD-style control panel.
 *
 * <p>The screen deliberately uses code-rendered shapes instead of a large GUI texture. This keeps
 * it responsive at every GUI scale and makes hover, scrolling and transition states feel like part
 * of the in-game HUD.</p>
 */
public class SettingsScreen extends Screen {
    private enum Category { GAME, COMPUTERS }

    private static final int PANEL_MAX_WIDTH = 600;
    private static final int PANEL_MAX_HEIGHT = 410;
    private static final int HEADER_HEIGHT = 54;
    private static final int CATEGORY_HEADER_HEIGHT = 43;
    private static final int FOOTER_HEIGHT = 43;
    private static final int ROW_HEIGHT = 45;
    private static final int ROW_GAP = 5;

    private static final int PANEL_BG = 0xF01B222A;
    private static final int PANEL_SURFACE = 0xF02A323B;
    private static final int PANEL_SURFACE_ALT = 0xF0343E49;
    private static final int PANEL_BORDER = 0xFF87929E;
    private static final int PANEL_BORDER_SOFT = 0xFF4D5864;
    private static final int TEXT_PRIMARY = 0xFFF7F9FB;
    private static final int TEXT_SECONDARY = 0xFFC7CED6;
    private static final int TEXT_MUTED = 0xFF929CA7;
    private static final int ACCENT_GAME = 0xFFFFC857;
    private static final int ACCENT_COMPUTERS = 0xFF59B7E8;
    private static final int ACCENT_DANGER = 0xFFE05A63;
    private static final int ACCENT_SUCCESS = 0xFF70E28A;

    private Category currentCategory = Category.GAME;

    private int tempHpBoost;
    private int tempManiacCount;
    private int tempGameTime;
    private int tempSelectedMap;

    private float tempHackPointsRequired;
    private float tempPointsPerPlayer;
    private float tempPointsPerSpecialist;
    private int tempMaxBonusPlayers;
    private float tempHackerRadius;
    private float tempSupportRadius;
    private int tempQteIntervalMin;
    private int tempQteIntervalMax;
    private float tempQteSuccessBonus;
    private float tempQteCritBonus;
    private int tempComputersNeededForWin;

    private int panelX;
    private int panelY;
    private int basePanelY;
    private int panelWidth;
    private int panelHeight;
    private int sidebarWidth;
    private int contentLeft;
    private int contentRight;
    private int rowsTop;
    private int rowsBottom;
    private int footerY;
    private int contentSlide;

    private long openedAt;
    private long lastFrameAt;
    private long categoryChangedAt;
    private long closeStartedAt;
    private boolean closing;
    private float scrollOffset;
    private float targetScroll;
    private float categoryIndicatorY;
    private String baselineSignature;

    private final Map<String, Float> rowHover = new HashMap<>();
    private final Map<String, Long> valuePulse = new HashMap<>();
    private final List<SettingEntry> gameSettingEntries;
    private final List<SettingEntry> computerSettingEntries;
    private List<Component> hoveredTooltip = List.of();
    private MapGuideRegistry.Entry hoveredMapPreview;
    private String activeMapPreviewId;
    private long mapPreviewStartedAt;

    public SettingsScreen() {
        super(Component.literal("Настройки матча"));
        loadFromClient();
        gameSettingEntries = buildGameEntries();
        computerSettingEntries = buildComputerEntries();
        baselineSignature = settingsSignature();
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new SettingsScreen());
    }

    private void loadFromClient() {
        tempHpBoost = ClientGameSettings.getHpBoost();
        tempManiacCount = ClientGameSettings.getManiacCount();
        tempGameTime = ClientGameSettings.getGameTime();
        tempSelectedMap = ClientGameSettings.getSelectedMap();
        tempHackPointsRequired = ClientGameSettings.getHackPointsRequired();
        tempPointsPerPlayer = ClientGameSettings.getPointsPerPlayer();
        tempPointsPerSpecialist = ClientGameSettings.getPointsPerSpecialist();
        tempMaxBonusPlayers = ClientGameSettings.getMaxBonusPlayers();
        tempHackerRadius = ClientGameSettings.getHackerRadius();
        tempSupportRadius = ClientGameSettings.getSupportRadius();
        tempQteIntervalMin = ClientGameSettings.getQteIntervalMin();
        tempQteIntervalMax = ClientGameSettings.getQteIntervalMax();
        tempQteSuccessBonus = ClientGameSettings.getQteSuccessBonus();
        tempQteCritBonus = ClientGameSettings.getQteCritBonus();
        tempComputersNeededForWin = ClientGameSettings.getComputersNeededForWin();
    }

    @Override
    protected void init() {
        super.init();
        calculateLayout();
        if (openedAt == 0L) {
            openedAt = System.currentTimeMillis();
            lastFrameAt = openedAt;
            categoryChangedAt = openedAt;
            categoryIndicatorY = categoryButtonY(Category.GAME);
        }
    }

    private void calculateLayout() {
        panelWidth = Math.min(PANEL_MAX_WIDTH, Math.max(300, width - 20));
        panelHeight = Math.min(PANEL_MAX_HEIGHT, Math.max(230, height - 16));
        panelX = (width - panelWidth) / 2;
        basePanelY = (height - panelHeight) / 2;
        panelY = basePanelY;
        sidebarWidth = panelWidth < 470 ? 96 : 116;
        updateDynamicLayout();
    }

    private void updateDynamicLayout() {
        contentLeft = panelX + sidebarWidth + 12;
        contentRight = panelX + panelWidth - 12;
        rowsTop = panelY + HEADER_HEIGHT + CATEGORY_HEADER_HEIGHT;
        footerY = panelY + panelHeight - FOOTER_HEIGHT;
        rowsBottom = footerY - 7;
    }

    @Override
    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        long now = System.currentTimeMillis();
        float deltaSeconds = Math.min(0.1f, Math.max(0.0f, (now - lastFrameAt) / 1000.0f));
        lastFrameAt = now;

        float intro = easeOutCubic(Mth.clamp((now - openedAt) / 260.0f, 0.0f, 1.0f));
        float close = closing ? Mth.clamp((now - closeStartedAt) / 150.0f, 0.0f, 1.0f) : 0.0f;
        float visibility = intro * (1.0f - close);
        int alpha = Math.round(255.0f * visibility);

        panelY = basePanelY + Math.round((1.0f - intro) * 12.0f) - Math.round(close * 5.0f);
        updateDynamicLayout();
        updateScrolling(deltaSeconds);
        updateCategoryIndicator(deltaSeconds);

        renderBackdrop(gui, now, alpha);

        RenderSystem.enableBlend();
        renderPanel(gui, mouseX, mouseY, now, deltaSeconds, alpha);
        RenderSystem.disableBlend();

        if (hoveredMapPreview != null && visibility > 0.92f) {
            renderMapPreviewCard(gui, hoveredMapPreview, mouseX, mouseY, now, alpha);
        } else if (!hoveredTooltip.isEmpty() && visibility > 0.92f) {
            gui.renderComponentTooltip(font, hoveredTooltip, mouseX, mouseY);
        }
        if (hoveredMapPreview == null) {
            activeMapPreviewId = null;
        }
    }

    private void renderBackdrop(GuiGraphics gui, long now, int alpha) {
        renderBackground(gui);
        int backdropAlpha = Math.min(190, Math.round(alpha * 0.74f));
        gui.fillGradient(0, 0, width, height,
                withAlpha(0xFF15202A, backdropAlpha), withAlpha(0xFF263746, backdropAlpha));

        int accent = accentColor();
        for (int i = 0; i < 18; i++) {
            int drift = (int) (now / (22L + i % 4 * 7L));
            int x = Math.floorMod(i * 83 + drift, Math.max(1, width + 40)) - 20;
            int wave = Math.round((float) Math.sin(now / 900.0 + i * 1.73) * 12.0f);
            int y = Math.floorMod(i * 47 + wave, Math.max(1, height));
            int particleAlpha = Math.min(alpha, 18 + (i % 4) * 7);
            gui.fill(x, y, x + (i % 5 == 0 ? 2 : 1), y + 1, withAlpha(accent, particleAlpha));
        }

        for (int y = 2; y < height; y += 4) {
            gui.fill(0, y, width, y + 1, withAlpha(0xFF000000, Math.min(alpha, 10)));
        }
    }

    private void renderPanel(GuiGraphics gui, int mouseX, int mouseY, long now,
                             float deltaSeconds, int alpha) {
        hoveredTooltip = List.of();
        hoveredMapPreview = null;

        renderPanelShell(gui, alpha);
        renderHeader(gui, mouseX, mouseY, now, alpha);
        renderSidebar(gui, mouseX, mouseY, alpha);
        renderCategoryHeader(gui, mouseX, mouseY, alpha);
        renderRows(gui, mouseX, mouseY, now, deltaSeconds, alpha);
        renderFooter(gui, mouseX, mouseY, now, alpha);
        renderScrollbar(gui, mouseX, mouseY, alpha);
    }

    private void renderPanelShell(GuiGraphics gui, int alpha) {
        int right = panelX + panelWidth;
        int bottom = panelY + panelHeight;

        gui.fill(panelX - 7, panelY + 7, right + 7, bottom + 9,
                withAlpha(0xFF000000, Math.min(alpha, 105)));
        gui.fill(panelX - 3, panelY - 3, right + 3, bottom + 3,
                withAlpha(0xFF111820, Math.min(alpha, 185)));
        gui.fill(panelX, panelY, right, bottom, withAlpha(PANEL_BG, alpha));
        gui.renderOutline(panelX, panelY, panelWidth, panelHeight, withAlpha(PANEL_BORDER, alpha));
        gui.fill(panelX + 1, panelY + 1, right - 1, panelY + 3, withAlpha(accentColor(), alpha));

        gui.fill(panelX, panelY + HEADER_HEIGHT, right, panelY + HEADER_HEIGHT + 1,
                withAlpha(PANEL_BORDER_SOFT, alpha));
        gui.fill(panelX + sidebarWidth, panelY + HEADER_HEIGHT, panelX + sidebarWidth + 1,
                bottom - FOOTER_HEIGHT, withAlpha(PANEL_BORDER_SOFT, alpha));
        gui.fill(panelX, bottom - FOOTER_HEIGHT, right, bottom - FOOTER_HEIGHT + 1,
                withAlpha(PANEL_BORDER_SOFT, alpha));

        drawCorner(gui, panelX - 2, panelY - 2, 1, 1, alpha);
        drawCorner(gui, right + 2, panelY - 2, -1, 1, alpha);
        drawCorner(gui, panelX - 2, bottom + 2, 1, -1, alpha);
        drawCorner(gui, right + 2, bottom + 2, -1, -1, alpha);
    }

    private void renderHeader(GuiGraphics gui, int mouseX, int mouseY, long now, int alpha) {
        int tileX = panelX + 13;
        int tileY = panelY + 13;
        int tileSize = 28;
        gui.fill(tileX, tileY, tileX + tileSize, tileY + tileSize,
                withAlpha(0xFF35404B, alpha));
        gui.renderOutline(tileX, tileY, tileSize, tileSize, withAlpha(accentColor(), alpha));
        gui.drawCenteredString(font, "MR", tileX + tileSize / 2, tileY + 10,
                withAlpha(TEXT_PRIMARY, alpha));

        int titleX = tileX + tileSize + 9;
        gui.drawString(font, "ПАНЕЛЬ УПРАВЛЕНИЯ", titleX, panelY + 14,
                withAlpha(TEXT_PRIMARY, alpha), false);
        gui.drawString(font, "Параметры игрового сервера", titleX, panelY + 28,
                withAlpha(TEXT_MUTED, alpha), false);

        if (hasUnsavedChanges()) {
            int pulseAlpha = 150 + Math.round((float) Math.sin(now / 230.0) * 45.0f);
            int dotX = Math.min(panelX + panelWidth - 90, titleX + font.width("ПАНЕЛЬ УПРАВЛЕНИЯ") + 7);
            gui.fill(dotX, panelY + 16, dotX + 3, panelY + 19,
                    withAlpha(ACCENT_GAME, Math.min(alpha, pulseAlpha)));
        }

        int closeX = panelX + panelWidth - 31;
        int closeY = panelY + 14;
        boolean closeHovered = inside(mouseX, mouseY, closeX, closeY, 18, 18);
        drawButton(gui, closeX, closeY, 18, 18, "×", ACCENT_DANGER,
                closeHovered, false, alpha);
        if (closeHovered) {
            setTooltip("Закрыть", hasUnsavedChanges()
                    ? "Несохранённые изменения будут потеряны."
                    : "Вернуться в игру.", null);
        }

        int serverChipRight = closeX - 9;
        String chip = "SERVER";
        int chipWidth = font.width(chip) + 12;
        int chipX = serverChipRight - chipWidth;
        gui.fill(chipX, panelY + 17, serverChipRight, panelY + 31,
                withAlpha(0xFF17251D, alpha));
        gui.renderOutline(chipX, panelY + 17, chipWidth, 14,
                withAlpha(0xFF355B43, alpha));
        gui.drawString(font, chip, chipX + 6, panelY + 20,
                withAlpha(ACCENT_SUCCESS, alpha), false);
    }

    private void renderSidebar(GuiGraphics gui, int mouseX, int mouseY, int alpha) {
        int sideLeft = panelX + 8;
        int sideRight = panelX + sidebarWidth - 8;
        int firstY = categoryButtonY(Category.GAME);
        int secondY = categoryButtonY(Category.COMPUTERS);
        int indicatorHeight = 37;

        gui.fill(panelX + 1, panelY + HEADER_HEIGHT + 1,
                panelX + sidebarWidth, footerY,
                withAlpha(0xFF222A33, alpha));

        gui.drawString(font, "РАЗДЕЛЫ", sideLeft + 2, panelY + HEADER_HEIGHT + 10,
                withAlpha(TEXT_MUTED, alpha), false);

        int indicatorY = Math.round(categoryIndicatorY);
        gui.fill(sideLeft, indicatorY, sideRight, indicatorY + indicatorHeight,
                withAlpha(0xFF37424D, alpha));
        gui.fill(sideLeft, indicatorY, sideLeft + 2, indicatorY + indicatorHeight,
                withAlpha(accentColor(), alpha));

        drawCategoryButton(gui, Category.GAME, sideLeft, firstY,
                sideRight - sideLeft, mouseX, mouseY, alpha);
        drawCategoryButton(gui, Category.COMPUTERS, sideLeft, secondY,
                sideRight - sideLeft, mouseX, mouseY, alpha);

        int hintY = footerY - 33;
        if (hintY > secondY + 43) {
            gui.drawString(font, "ПОДСКАЗКА", sideLeft + 2, hintY,
                    withAlpha(TEXT_MUTED, alpha), false);
            gui.drawString(font, "Shift  ×5", sideLeft + 2, hintY + 12,
                    withAlpha(TEXT_SECONDARY, alpha), false);
        }
    }

    private void drawCategoryButton(GuiGraphics gui, Category category, int x, int y, int width,
                                    int mouseX, int mouseY, int alpha) {
        boolean hovered = inside(mouseX, mouseY, x, y, width, 37);
        boolean selected = currentCategory == category;
        if (hovered && !selected) {
            gui.fill(x, y, x + width, y + 37, withAlpha(0xFF313B45, alpha));
        }

        int color = selected ? accentColor(category) : hovered ? TEXT_PRIMARY : TEXT_SECONDARY;
        drawCategoryIcon(gui, category, x + 9, y + 10, color, alpha);
        String name = category == Category.GAME ? "Матч" : "Компьютеры";
        int textX = x + 29;
        int maxTextWidth = width - 34;
        gui.drawString(font, trimToWidth(name, maxTextWidth), textX, y + 9,
                withAlpha(color, alpha), false);
        String count = category == Category.GAME ? "4 параметра" : "11 параметров";
        gui.drawString(font, trimToWidth(count, maxTextWidth), textX, y + 21,
                withAlpha(TEXT_MUTED, alpha), false);

        if (hovered) {
            setTooltip(name, category == Category.GAME
                    ? "Основные правила и карта текущего матча."
                    : "Скорость взлома, QTE и условия победы.", null);
        }
    }

    private void renderCategoryHeader(GuiGraphics gui, int mouseX, int mouseY, int alpha) {
        String title = currentCategory == Category.GAME ? "НАСТРОЙКИ МАТЧА" : "СИСТЕМА ВЗЛОМА";
        String subtitle = currentCategory == Category.GAME
                ? "Базовые правила следующего раунда"
                : "Баланс компьютеров, помощников и QTE";
        gui.drawString(font, title, contentLeft, panelY + HEADER_HEIGHT + 10,
                withAlpha(TEXT_PRIMARY, alpha), false);
        gui.drawString(font, trimToWidth(subtitle, Math.max(60, contentRight - contentLeft - 112)),
                contentLeft, panelY + HEADER_HEIGHT + 23,
                withAlpha(TEXT_MUTED, alpha), false);

        String resetLabel = panelWidth < 470 ? "Сброс" : "Сбросить раздел";
        int resetWidth = font.width(resetLabel) + 14;
        int resetX = contentRight - resetWidth;
        int resetY = panelY + HEADER_HEIGHT + 12;
        boolean hovered = inside(mouseX, mouseY, resetX, resetY, resetWidth, 19);
        drawButton(gui, resetX, resetY, resetWidth, 19, resetLabel,
                ACCENT_DANGER, hovered, false, alpha);
        if (hovered) {
            setTooltip("Сбросить раздел", "Вернуть значения этого раздела по умолчанию.", null);
        }
    }

    private void renderRows(GuiGraphics gui, int mouseX, int mouseY, long now,
                            float deltaSeconds, int alpha) {
        List<SettingEntry> entries = currentEntries();
        float transition = easeOutCubic(Mth.clamp((now - categoryChangedAt) / 190.0f, 0.0f, 1.0f));
        int contentAlpha = Math.min(alpha, Math.round(alpha * transition));
        contentSlide = Math.round((1.0f - transition) * 7.0f);

        gui.enableScissor(contentLeft - 2, rowsTop, contentRight + 2, rowsBottom);
        for (int i = 0; i < entries.size(); i++) {
            SettingEntry entry = entries.get(i);
            int rowY = Math.round(rowsTop + i * (ROW_HEIGHT + ROW_GAP) - scrollOffset) + contentSlide;
            if (rowY + ROW_HEIGHT < rowsTop || rowY > rowsBottom) {
                continue;
            }
            renderSettingRow(gui, entry, i, contentLeft, rowY,
                    contentRight - contentLeft, mouseX, mouseY, now, deltaSeconds, contentAlpha);
        }
        gui.disableScissor();
    }

    private void renderSettingRow(GuiGraphics gui, SettingEntry entry, int index, int x, int y,
                                  int rowWidth, int mouseX, int mouseY, long now,
                                  float deltaSeconds, int alpha) {
        boolean visibleHover = y >= rowsTop && y + ROW_HEIGHT <= rowsBottom;
        boolean hovered = visibleHover && inside(mouseX, mouseY, x, y, rowWidth, ROW_HEIGHT);
        float hover = rowHover.getOrDefault(entry.id(), 0.0f);
        float target = hovered ? 1.0f : 0.0f;
        hover += (target - hover) * Math.min(1.0f, deltaSeconds * 13.0f);
        rowHover.put(entry.id(), hover);

        int accent = accentColor();
        int bg = lerpColor(PANEL_SURFACE, PANEL_SURFACE_ALT, hover);
        int border = lerpColor(PANEL_BORDER_SOFT, accent, hover * 0.72f);
        gui.fill(x, y, x + rowWidth, y + ROW_HEIGHT, withAlpha(bg, alpha));
        gui.renderOutline(x, y, rowWidth, ROW_HEIGHT, withAlpha(border, alpha));
        gui.fill(x, y, x + (hovered ? 3 : 2), y + ROW_HEIGHT,
                withAlpha(entry.isDefault().getAsBoolean() ? PANEL_BORDER : accent, alpha));

        int badgeX = x + 10;
        int badgeY = y + 12;
        gui.fill(badgeX, badgeY, badgeX + 20, badgeY + 20,
                withAlpha(0xFF242D36, alpha));
        gui.renderOutline(badgeX, badgeY, 20, 20,
                withAlpha(entry.isDefault().getAsBoolean() ? PANEL_BORDER_SOFT : accent, alpha));
        String number = String.format(Locale.ROOT, "%02d", index + 1);
        gui.drawCenteredString(font, number, badgeX + 10, badgeY + 6,
                withAlpha(entry.isDefault().getAsBoolean() ? TEXT_MUTED : accent, alpha));

        ControlLayout controls = controlLayout(x, rowWidth);
        int textX = x + 39;
        int textWidth = Math.max(25, controls.minusX() - textX - 7);
        String title = trimToWidth(entry.title(), textWidth);
        gui.drawString(font, title, textX, y + (rowWidth >= 330 ? 8 : 17),
                withAlpha(TEXT_PRIMARY, alpha), false);
        if (rowWidth >= 330) {
            gui.drawString(font, trimToWidth(entry.description(), textWidth), textX, y + 23,
                    withAlpha(TEXT_MUTED, alpha), false);
        }

        boolean minusHover = inside(mouseX, mouseY, controls.minusX(), y + 11, 20, 23);
        boolean valueHover = inside(mouseX, mouseY, controls.valueX(), y + 11,
                controls.valueWidth(), 23);
        boolean plusHover = inside(mouseX, mouseY, controls.plusX(), y + 11, 20, 23);
        boolean resetHover = inside(mouseX, mouseY, controls.resetX(), y + 13, 18, 19);

        drawButton(gui, controls.minusX(), y + 11, 20, 23, "−", accent,
                minusHover, false, alpha);

        long pulseStarted = valuePulse.getOrDefault(entry.id(), 0L);
        float pulse = Mth.clamp(1.0f - (now - pulseStarted) / 360.0f, 0.0f, 1.0f);
        int valueBg = lerpColor(0xFF202831, accent, pulse * 0.28f);
        int valueBorder = lerpColor(PANEL_BORDER, accent, Math.max(hover * 0.45f, pulse));
        gui.fill(controls.valueX(), y + 11,
                controls.valueX() + controls.valueWidth(), y + 34,
                withAlpha(valueBg, alpha));
        gui.renderOutline(controls.valueX(), y + 11, controls.valueWidth(), 23,
                withAlpha(valueBorder, alpha));
        drawMarqueeValue(gui, entry.value().get(), controls.valueX() + 4, y + 18,
                controls.valueWidth() - 8, now,
                withAlpha(entry.isDefault().getAsBoolean() ? TEXT_SECONDARY : accent, alpha));

        drawButton(gui, controls.plusX(), y + 11, 20, 23, "+", accent,
                plusHover, false, alpha);
        drawButton(gui, controls.resetX(), y + 13, 18, 19, "↺", ACCENT_DANGER,
                resetHover, false, alpha);

        if (hovered) {
            String hint = entry.hint();
            if (minusHover) {
                setTooltip("Уменьшить", "Зажмите Shift, чтобы изменить значение в 5 раз быстрее.", hint);
            } else if (plusHover) {
                setTooltip("Увеличить", "Зажмите Shift, чтобы изменить значение в 5 раз быстрее.", hint);
            } else if (resetHover) {
                setTooltip("Сбросить параметр", "Вернуть стандартное значение.", hint);
            } else if (entry.id().equals("map") && valueHover && tempSelectedMap != 0) {
                hoveredMapPreview = selectedMapGuideEntry();
                if (hoveredMapPreview != null) {
                    hoveredTooltip = List.of();
                } else {
                    setTooltip(entry.title(), entry.tooltip(), hint);
                }
            } else {
                setTooltip(entry.title(), entry.tooltip(), hint);
            }
        }
    }

    private void renderFooter(GuiGraphics gui, int mouseX, int mouseY, long now, int alpha) {
        int y = footerY + 10;
        int buttonHeight = 23;
        String applyLabel = "Сохранить";
        String giveLabel = panelWidth < 470 ? "Всем" : "Выдать всем";
        String resetLabel = panelWidth < 470 ? "Сброс" : "Сбросить всё";
        int applyWidth = Math.max(76, font.width(applyLabel) + 24);
        int giveWidth = Math.max(58, font.width(giveLabel) + 20);
        int resetWidth = Math.max(58, font.width(resetLabel) + 18);
        int gap = 6;
        int applyX = contentRight - applyWidth;
        int giveX = applyX - gap - giveWidth;
        int resetX = giveX - gap - resetWidth;

        boolean resetHover = inside(mouseX, mouseY, resetX, y, resetWidth, buttonHeight);
        boolean giveHover = inside(mouseX, mouseY, giveX, y, giveWidth, buttonHeight);
        boolean applyHover = inside(mouseX, mouseY, applyX, y, applyWidth, buttonHeight);

        drawButton(gui, resetX, y, resetWidth, buttonHeight, resetLabel,
                ACCENT_DANGER, resetHover, false, alpha);
        drawButton(gui, giveX, y, giveWidth, buttonHeight, giveLabel,
                ACCENT_COMPUTERS, giveHover, false, alpha);
        drawButton(gui, applyX, y, applyWidth, buttonHeight, applyLabel,
                ACCENT_SUCCESS, applyHover, true, alpha);

        if (resetX >= panelX + sidebarWidth + 4) {
            int stateX = panelX + 12;
            int dotColor = hasUnsavedChanges() ? ACCENT_GAME : ACCENT_SUCCESS;
            int pulseAlpha = hasUnsavedChanges()
                    ? 165 + Math.round((float) Math.sin(now / 240.0) * 45.0f)
                    : 170;
            gui.fill(stateX, y + 9, stateX + 4, y + 13,
                    withAlpha(dotColor, Math.min(alpha, pulseAlpha)));
            String state = hasUnsavedChanges() ? "Есть изменения" : "Всё сохранено";
            gui.drawString(font, trimToWidth(state, Math.max(30, sidebarWidth - 28)),
                    stateX + 8, y + 7, withAlpha(TEXT_SECONDARY, alpha), false);
        }

        if (resetHover) {
            setTooltip("Сбросить всё", "Вернуть стандартные значения во всех разделах.",
                    "Сохранение потребуется отдельно.");
        } else if (giveHover) {
            setTooltip("Выдать всем", "Сохранить параметры и выдать предмет настроек всем игрокам.", null);
        } else if (applyHover) {
            setTooltip("Сохранить", "Применить параметры к серверу и закрыть панель.", null);
        }
    }

    private void renderScrollbar(GuiGraphics gui, int mouseX, int mouseY, int alpha) {
        int maxScroll = maxScroll();
        if (maxScroll <= 0) {
            return;
        }
        int trackX = contentRight + 4;
        int trackHeight = rowsBottom - rowsTop;
        int thumbHeight = Math.max(22, Math.round(trackHeight * (trackHeight /
                (float) (trackHeight + maxScroll))));
        int travel = trackHeight - thumbHeight;
        int thumbY = rowsTop + Math.round(travel * (scrollOffset / maxScroll));
        boolean hovered = inside(mouseX, mouseY, trackX - 2, rowsTop, 6, trackHeight);
        gui.fill(trackX, rowsTop, trackX + 2, rowsBottom,
                withAlpha(0xFF252B32, alpha));
        gui.fill(trackX - (hovered ? 1 : 0), thumbY,
                trackX + 2 + (hovered ? 1 : 0), thumbY + thumbHeight,
                withAlpha(hovered ? accentColor() : PANEL_BORDER, alpha));
    }

    private void drawButton(GuiGraphics gui, int x, int y, int width, int height, String label,
                            int accent, boolean hovered, boolean filled, int alpha) {
        int bg;
        int border;
        int text;
        if (filled) {
            bg = hovered ? lerpColor(accent, 0xFFFFFFFF, 0.12f) : accent;
            border = hovered ? 0xFFFFFFFF : lerpColor(accent, 0xFFFFFFFF, 0.28f);
            text = 0xFF0D1115;
        } else {
            bg = hovered ? lerpColor(0xFF28323C, accent, 0.17f) : 0xFF28323C;
            border = hovered ? accent : PANEL_BORDER_SOFT;
            text = hovered ? TEXT_PRIMARY : TEXT_SECONDARY;
        }
        gui.fill(x, y, x + width, y + height, withAlpha(bg, alpha));
        gui.renderOutline(x, y, width, height, withAlpha(border, alpha));
        gui.drawCenteredString(font, label, x + width / 2, y + (height - 8) / 2,
                withAlpha(text, alpha));
        if (hovered) {
            gui.fill(x + 1, y + height - 2, x + width - 1, y + height - 1,
                    withAlpha(accent, alpha));
        }
    }

    private void drawCategoryIcon(GuiGraphics gui, Category category, int x, int y,
                                  int color, int alpha) {
        int c = withAlpha(color, alpha);
        if (category == Category.GAME) {
            gui.renderOutline(x, y + 3, 14, 9, c);
            gui.fill(x + 3, y + 6, x + 8, y + 7, c);
            gui.fill(x + 5, y + 4, x + 6, y + 9, c);
            gui.fill(x + 10, y + 5, x + 11, y + 6, c);
            gui.fill(x + 12, y + 8, x + 13, y + 9, c);
        } else {
            gui.renderOutline(x, y + 1, 14, 10, c);
            gui.fill(x + 2, y + 3, x + 12, y + 9, withAlpha(0xFF18232B, alpha));
            gui.fill(x + 6, y + 11, x + 8, y + 13, c);
            gui.fill(x + 3, y + 13, x + 11, y + 14, c);
        }
    }

    private void drawCorner(GuiGraphics gui, int x, int y, int dirX, int dirY, int alpha) {
        int color = withAlpha(accentColor(), alpha);
        gui.fill(Math.min(x, x + dirX * 7), y, Math.max(x, x + dirX * 7) + 1, y + 1, color);
        gui.fill(x, Math.min(y, y + dirY * 7), x + 1, Math.max(y, y + dirY * 7) + 1, color);
    }

    private void drawMarqueeValue(GuiGraphics gui, String text, int x, int y,
                                  int availableWidth, long now, int color) {
        int textWidth = font.width(text);
        if (textWidth <= availableWidth) {
            gui.drawString(font, text, x + (availableWidth - textWidth) / 2, y, color, false);
            return;
        }

        int overflow = textWidth - availableWidth;
        float travel = (float) ((Math.sin(now / 850.0) + 1.0) * 0.5);
        gui.enableScissor(x, y - 2, x + availableWidth, y + 11);
        gui.drawString(font, text, x - Math.round(overflow * travel), y, color, false);
        gui.disableScissor();
    }

    private void renderMapPreviewCard(GuiGraphics gui, MapGuideRegistry.Entry map,
                                      int mouseX, int mouseY, long now, int screenAlpha) {
        if (!map.id().equals(activeMapPreviewId)) {
            activeMapPreviewId = map.id();
            mapPreviewStartedAt = now;
        }

        float reveal = easeOutCubic(Mth.clamp((now - mapPreviewStartedAt) / 170.0f, 0.0f, 1.0f));
        int alpha = Math.min(screenAlpha, Math.round(255.0f * reveal));
        int cardWidth = Math.min(286, Math.max(230, width - 16));
        int cardHeight = 137;
        int x = mouseX + 14;
        if (x + cardWidth > width - 8) {
            x = mouseX - cardWidth - 14;
        }
        x = Mth.clamp(x, 8, Math.max(8, width - cardWidth - 8));

        int y = mouseY + 12;
        if (y + cardHeight > height - 8) {
            y = mouseY - cardHeight - 12;
        }
        y = Mth.clamp(y + Math.round((1.0f - reveal) * 5.0f),
                8, Math.max(8, height - cardHeight - 8));

        // Text rendering is buffered. Flush the settings panel first, then place the entire
        // preview on the same elevated layer used by vanilla tooltips so older labels cannot
        // be emitted over the card afterwards.
        gui.flush();
        gui.pose().pushPose();
        gui.pose().translate(0.0f, 0.0f, 400.0f);
        RenderSystem.disableDepthTest();

        gui.fill(x + 5, y + 6, x + cardWidth + 5, y + cardHeight + 6,
                withAlpha(0xFF000000, Math.min(alpha, 105)));
        gui.fill(x, y, x + cardWidth, y + cardHeight, withAlpha(0xFF35414D, alpha));
        gui.renderOutline(x, y, cardWidth, cardHeight, withAlpha(PANEL_BORDER, alpha));
        gui.fill(x + 1, y + 1, x + cardWidth - 1, y + 3,
                withAlpha(ACCENT_GAME, alpha));

        gui.drawString(font, "КАРТА ИЗ ГАЙДА", x + 10, y + 9,
                withAlpha(TEXT_MUTED, alpha), false);

        int imageX = x + 10;
        int imageY = y + 24;
        int imageWidth = 118;
        int imageHeight = 68;
        gui.fill(imageX - 1, imageY - 1, imageX + imageWidth + 1, imageY + imageHeight + 1,
                withAlpha(0xFF171D24, alpha));
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha / 255.0f);
        gui.blit(map.previewTexture(), imageX, imageY, 0, 0,
                imageWidth, imageHeight, imageWidth, imageHeight);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
        gui.renderOutline(imageX - 1, imageY - 1, imageWidth + 2, imageHeight + 2,
                withAlpha(PANEL_BORDER, alpha));

        int infoX = imageX + imageWidth + 11;
        int infoWidth = Math.max(70, x + cardWidth - infoX - 10);
        gui.drawString(font, trimToWidth(map.name(), infoWidth), infoX, imageY + 1,
                withAlpha(TEXT_PRIMARY, alpha), false);
        gui.drawString(font, "Размер", infoX, imageY + 18,
                withAlpha(TEXT_MUTED, alpha), false);
        gui.drawString(font, trimToWidth(map.size(), infoWidth), infoX, imageY + 29,
                withAlpha(TEXT_SECONDARY, alpha), false);
        gui.drawString(font, "Сложность", infoX, imageY + 45,
                withAlpha(TEXT_MUTED, alpha), false);
        gui.drawString(font, trimToWidth(map.difficultyStars(), infoWidth), infoX, imageY + 56,
                withAlpha(ACCENT_GAME, alpha), false);

        List<String> description = wrapText(map.description(), cardWidth - 20);
        int descriptionY = y + 101;
        for (int i = 0; i < Math.min(3, description.size()); i++) {
            gui.drawString(font, description.get(i), x + 10, descriptionY + i * 10,
                    withAlpha(TEXT_SECONDARY, alpha), false);
        }

        gui.flush();
        RenderSystem.enableDepthTest();
        gui.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (closing || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        int mx = (int) mouseX;
        int my = (int) mouseY;
        int closeX = panelX + panelWidth - 31;
        if (inside(mx, my, closeX, panelY + 14, 18, 18)) {
            playClick(0.9f);
            onClose();
            return true;
        }

        for (Category category : Category.values()) {
            int x = panelX + 8;
            int y = categoryButtonY(category);
            int w = sidebarWidth - 16;
            if (inside(mx, my, x, y, w, 37)) {
                switchCategory(category);
                return true;
            }
        }

        String resetCategoryLabel = panelWidth < 470 ? "Сброс" : "Сбросить раздел";
        int resetCategoryWidth = font.width(resetCategoryLabel) + 14;
        int resetCategoryX = contentRight - resetCategoryWidth;
        int resetCategoryY = panelY + HEADER_HEIGHT + 12;
        if (inside(mx, my, resetCategoryX, resetCategoryY, resetCategoryWidth, 19)) {
            resetCategory();
            playClick(0.82f);
            return true;
        }

        if (handleSettingClick(mx, my)) {
            return true;
        }

        if (handleFooterClick(mx, my)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleSettingClick(int mouseX, int mouseY) {
        if (mouseY < rowsTop || mouseY >= rowsBottom) {
            return false;
        }
        List<SettingEntry> entries = currentEntries();
        int multiplier = hasShiftDown() ? 5 : 1;
        for (int i = 0; i < entries.size(); i++) {
            SettingEntry entry = entries.get(i);
            int y = Math.round(rowsTop + i * (ROW_HEIGHT + ROW_GAP) - scrollOffset) + contentSlide;
            if (y < rowsTop || y + ROW_HEIGHT > rowsBottom) {
                continue;
            }
            ControlLayout controls = controlLayout(contentLeft, contentRight - contentLeft);
            if (inside(mouseX, mouseY, controls.minusX(), y + 11, 20, 23)) {
                entry.decrease().accept(multiplier);
                markChanged(entry.id());
                return true;
            }
            if (inside(mouseX, mouseY, controls.plusX(), y + 11, 20, 23)) {
                entry.increase().accept(multiplier);
                markChanged(entry.id());
                return true;
            }
            if (inside(mouseX, mouseY, controls.resetX(), y + 13, 18, 19)) {
                entry.reset().run();
                markChanged(entry.id());
                return true;
            }
        }
        return false;
    }

    private boolean handleFooterClick(int mouseX, int mouseY) {
        int y = footerY + 10;
        int h = 23;
        String applyLabel = "Сохранить";
        String giveLabel = panelWidth < 470 ? "Всем" : "Выдать всем";
        String resetLabel = panelWidth < 470 ? "Сброс" : "Сбросить всё";
        int applyWidth = Math.max(76, font.width(applyLabel) + 24);
        int giveWidth = Math.max(58, font.width(giveLabel) + 20);
        int resetWidth = Math.max(58, font.width(resetLabel) + 18);
        int applyX = contentRight - applyWidth;
        int giveX = applyX - 6 - giveWidth;
        int resetX = giveX - 6 - resetWidth;

        if (inside(mouseX, mouseY, resetX, y, resetWidth, h)) {
            resetAll();
            playClick(0.8f);
            return true;
        }
        if (inside(mouseX, mouseY, giveX, y, giveWidth, h)) {
            saveSettings(true);
            playClick(1.08f);
            return true;
        }
        if (inside(mouseX, mouseY, applyX, y, applyWidth, h)) {
            saveSettings(false);
            playClick(1.15f);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (inside((int) mouseX, (int) mouseY, contentLeft - 5, rowsTop,
                contentRight - contentLeft + 12, rowsBottom - rowsTop) && maxScroll() > 0) {
            targetScroll = Mth.clamp(targetScroll - (float) delta * (ROW_HEIGHT + ROW_GAP) * 1.25f,
                    0.0f, maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            saveSettings(false);
            playClick(1.15f);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            switchCategory(currentCategory == Category.GAME ? Category.COMPUTERS : Category.GAME);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        super.tick();
        if (closing && System.currentTimeMillis() - closeStartedAt >= 155L) {
            super.onClose();
        }
    }

    @Override
    public void onClose() {
        if (!closing) {
            closing = true;
            closeStartedAt = System.currentTimeMillis();
        }
    }

    private void switchCategory(Category category) {
        if (category == currentCategory) {
            return;
        }
        currentCategory = category;
        scrollOffset = 0.0f;
        targetScroll = 0.0f;
        categoryChangedAt = System.currentTimeMillis();
        playClick(category == Category.GAME ? 0.96f : 1.04f);
    }

    private void markChanged(String id) {
        valuePulse.put(id, System.currentTimeMillis());
        playClick(1.2f);
    }

    private void playClick(float pitch) {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
        }
    }

    private void updateScrolling(float deltaSeconds) {
        int max = maxScroll();
        targetScroll = Mth.clamp(targetScroll, 0.0f, max);
        float responsiveness = 1.0f - (float) Math.pow(0.001, deltaSeconds);
        scrollOffset += (targetScroll - scrollOffset) * responsiveness;
        if (Math.abs(targetScroll - scrollOffset) < 0.05f) {
            scrollOffset = targetScroll;
        }
    }

    private void updateCategoryIndicator(float deltaSeconds) {
        float target = categoryButtonY(currentCategory);
        float responsiveness = 1.0f - (float) Math.pow(0.0005, deltaSeconds);
        categoryIndicatorY += (target - categoryIndicatorY) * responsiveness;
    }

    private int categoryButtonY(Category category) {
        return panelY + HEADER_HEIGHT + 28 + category.ordinal() * 43;
    }

    private int maxScroll() {
        int totalHeight = currentEntries().size() * (ROW_HEIGHT + ROW_GAP) - ROW_GAP;
        return Math.max(0, totalHeight - Math.max(1, rowsBottom - rowsTop));
    }

    private ControlLayout controlLayout(int rowX, int rowWidth) {
        int resetX = rowX + rowWidth - 25;
        int plusX = resetX - 25;
        int valueWidth = Mth.clamp(rowWidth / 5, 54, 78);
        int valueX = plusX - valueWidth - 4;
        int minusX = valueX - 24;
        return new ControlLayout(minusX, valueX, valueWidth, plusX, resetX);
    }

    private List<SettingEntry> currentEntries() {
        return currentCategory == Category.GAME ? gameSettingEntries : computerSettingEntries;
    }

    private List<SettingEntry> buildGameEntries() {
        return List.of(
                new SettingEntry("hp", "Дополнительное здоровье", "Запас здоровья сверх базового",
                        "Шаг: 2 HP", () -> (tempHpBoost > 0 ? "+" : "") + tempHpBoost + " HP",
                        n -> tempHpBoost = Math.max(0, tempHpBoost - 2 * n),
                        n -> tempHpBoost += 2 * n,
                        () -> tempHpBoost = GameSettings.DEFAULT_HP_BOOST,
                        () -> tempHpBoost == GameSettings.DEFAULT_HP_BOOST,
                        "Добавляется игрокам при применении настроек."),
                new SettingEntry("maniacs", "Количество маньяков", "Число противников в раунде",
                        "Шаг: 1 игрок", () -> Integer.toString(tempManiacCount),
                        n -> tempManiacCount = Math.max(1, tempManiacCount - n),
                        n -> tempManiacCount += n,
                        () -> tempManiacCount = GameSettings.DEFAULT_MANIAC_COUNT,
                        () -> tempManiacCount == GameSettings.DEFAULT_MANIAC_COUNT,
                        "Определяет, сколько игроков окажутся в команде маньяков."),
                new SettingEntry("time", "Длительность матча", "Лимит времени текущего раунда",
                        "Шаг: 1 минута", () -> tempGameTime + " мин",
                        n -> tempGameTime = Math.max(1, tempGameTime - n),
                        n -> tempGameTime += n,
                        () -> tempGameTime = GameSettings.DEFAULT_GAME_TIME,
                        () -> tempGameTime == GameSettings.DEFAULT_GAME_TIME,
                        "После сохранения таймер матча будет пересчитан в секунды."),
                new SettingEntry("map", "Карта", "Голосование или фиксированная арена",
                        "Переключение по списку", this::mapName,
                        n -> cycleMap(-n), n -> cycleMap(n),
                        () -> tempSelectedMap = GameSettings.DEFAULT_SELECTED_MAP,
                        () -> tempSelectedMap == GameSettings.DEFAULT_SELECTED_MAP,
                        "Выберите конкретную карту или оставьте выбор игрокам через голосование.")
        );
    }

    private List<SettingEntry> buildComputerEntries() {
        return List.of(
                floatEntry("hack_points", "Очки для взлома", "Объём работы одного компьютера",
                        "Шаг: 0.5 очка", () -> tempHackPointsRequired,
                        v -> tempHackPointsRequired = Math.max(1.0f, v), 0.5f,
                        GameSettings.DEFAULT_HACK_POINTS_REQUIRED, "%.1f очк.",
                        "Сколько очков нужно накопить, чтобы полностью взломать компьютер."),
                floatEntry("points_player", "Скорость игрока", "Базовый прогресс взлома в секунду",
                        "Шаг: 0.01 очка/с", () -> tempPointsPerPlayer,
                        v -> tempPointsPerPlayer = Math.max(0.01f, v), 0.01f,
                        GameSettings.DEFAULT_POINTS_PER_PLAYER, "%.2f /с",
                        "Скорость взлома за каждого обычного игрока у компьютера."),
                floatEntry("points_specialist", "Скорость специалиста", "Прогресс специалиста в секунду",
                        "Шаг: 0.01 очка/с", () -> tempPointsPerSpecialist,
                        v -> tempPointsPerSpecialist = Math.max(0.01f, v), 0.01f,
                        GameSettings.DEFAULT_POINTS_PER_SPECIALIST, "%.2f /с",
                        "Скорость взлома персонажа со специализацией на компьютерах."),
                intEntry("helpers", "Максимум помощников", "Лимит бонусных участников взлома",
                        "Шаг: 1 игрок", () -> tempMaxBonusPlayers,
                        v -> tempMaxBonusPlayers = Math.max(1, v), 1,
                        GameSettings.DEFAULT_MAX_BONUS_PLAYERS, "%d",
                        "Игроки сверх этого лимита не ускоряют текущий взлом."),
                floatEntry("hacker_radius", "Радиус хакера", "Дистанция основного участника",
                        "Шаг: 0.1 блока", () -> tempHackerRadius,
                        v -> tempHackerRadius = Math.max(0.5f, v), 0.1f,
                        GameSettings.DEFAULT_HACKER_RADIUS, "%.1f бл.",
                        "На таком расстоянии игрок считается основным хакером компьютера."),
                floatEntry("support_radius", "Радиус поддержки", "Дистанция помощников от компьютера",
                        "Шаг: 0.1 блока", () -> tempSupportRadius,
                        v -> tempSupportRadius = Math.max(0.5f, v), 0.1f,
                        GameSettings.DEFAULT_SUPPORT_RADIUS, "%.1f бл.",
                        "Игроки внутри радиуса могут добавлять бонус к скорости взлома."),
                new SettingEntry("qte_min", "Минимальный интервал QTE", "Самая ранняя проверка реакции",
                        "Шаг: 1 секунда", () -> tempQteIntervalMin + " с",
                        n -> tempQteIntervalMin = Math.max(1, tempQteIntervalMin - n),
                        n -> {
                            tempQteIntervalMin += n;
                            tempQteIntervalMax = Math.max(tempQteIntervalMax, tempQteIntervalMin);
                        },
                        () -> {
                            tempQteIntervalMin = GameSettings.DEFAULT_QTE_INTERVAL_MIN;
                            tempQteIntervalMax = Math.max(tempQteIntervalMax, tempQteIntervalMin);
                        },
                        () -> tempQteIntervalMin == GameSettings.DEFAULT_QTE_INTERVAL_MIN,
                        "Новая QTE-проверка не появится раньше указанного времени."),
                new SettingEntry("qte_max", "Максимальный интервал QTE", "Самая поздняя проверка реакции",
                        "Шаг: 1 секунда", () -> tempQteIntervalMax + " с",
                        n -> tempQteIntervalMax = Math.max(tempQteIntervalMin, tempQteIntervalMax - n),
                        n -> tempQteIntervalMax += n,
                        () -> tempQteIntervalMax = Math.max(tempQteIntervalMin,
                                GameSettings.DEFAULT_QTE_INTERVAL_MAX),
                        () -> tempQteIntervalMax == GameSettings.DEFAULT_QTE_INTERVAL_MAX,
                        "QTE-проверка гарантированно появится не позднее указанного времени."),
                floatEntry("qte_bonus", "Бонус успешной QTE", "Добавочный прогресс за успех",
                        "Шаг: 0.05 очка", () -> tempQteSuccessBonus,
                        v -> tempQteSuccessBonus = Math.max(0.0f, v), 0.05f,
                        GameSettings.DEFAULT_QTE_SUCCESS_BONUS, "+%.2f",
                        "Обычное успешное попадание сразу добавляет этот объём прогресса."),
                floatEntry("qte_crit", "Критический бонус QTE", "Добавочный прогресс за идеальный удар",
                        "Шаг: 0.05 очка", () -> tempQteCritBonus,
                        v -> tempQteCritBonus = Math.max(0.0f, v), 0.05f,
                        GameSettings.DEFAULT_QTE_CRIT_BONUS, "+%.2f",
                        "Идеальное попадание в QTE награждается увеличенным бонусом."),
                intEntry("computers", "Компьютеров для победы", "Условие победы выживших",
                        "Шаг: 1 компьютер", () -> tempComputersNeededForWin,
                        v -> tempComputersNeededForWin = Math.max(1, v), 1,
                        GameSettings.DEFAULT_COMPUTERS_NEEDED, "%d",
                        "Столько компьютеров команда должна полностью взломать для победы.")
        );
    }

    private SettingEntry intEntry(String id, String title, String description, String hint,
                                  Supplier<Integer> getter, IntConsumer setter, int step,
                                  int defaultValue, String format, String tooltip) {
        return new SettingEntry(id, title, description, hint,
                () -> String.format(Locale.ROOT, format, getter.get()),
                n -> setter.accept(getter.get() - step * n),
                n -> setter.accept(getter.get() + step * n),
                () -> setter.accept(defaultValue),
                () -> getter.get() == defaultValue,
                tooltip);
    }

    private SettingEntry floatEntry(String id, String title, String description, String hint,
                                    Supplier<Float> getter, java.util.function.Consumer<Float> setter,
                                    float step, float defaultValue, String format, String tooltip) {
        return new SettingEntry(id, title, description, hint,
                () -> String.format(Locale.ROOT, format, getter.get()),
                n -> setter.accept(roundHundredths(getter.get() - step * n)),
                n -> setter.accept(roundHundredths(getter.get() + step * n)),
                () -> setter.accept(defaultValue),
                () -> Math.abs(getter.get() - defaultValue) < 0.0001f,
                tooltip);
    }

    private void cycleMap(int amount) {
        List<MapData> maps = mapsWithVoting();
        if (maps.isEmpty()) {
            return;
        }
        int index = mapIndex(maps);
        int next = Math.floorMod(index + amount, maps.size());
        tempSelectedMap = maps.get(next).getNumericId();
    }

    private List<MapData> mapsWithVoting() {
        List<MapData> maps = new ArrayList<>();
        maps.add(new MapData("voting", 0, "Голосование", "", null));
        maps.addAll(MapRegistry.getAllMaps());
        return maps;
    }

    private int mapIndex(List<MapData> maps) {
        for (int i = 0; i < maps.size(); i++) {
            if (maps.get(i).getNumericId() == tempSelectedMap) {
                return i;
            }
        }
        return 0;
    }

    private String mapName() {
        if (tempSelectedMap == 0) {
            return "Голосование";
        }
        MapData map = MapRegistry.getMapByNumericId(tempSelectedMap);
        if (map == null) {
            return "Неизвестная карта";
        }
        MapGuideRegistry.Entry guideEntry = MapGuideRegistry.getById(map.getId());
        return guideEntry != null ? guideEntry.name() : map.getName();
    }

    private MapGuideRegistry.Entry selectedMapGuideEntry() {
        MapData map = MapRegistry.getMapByNumericId(tempSelectedMap);
        return map != null ? MapGuideRegistry.getById(map.getId()) : null;
    }

    private void resetCategory() {
        for (SettingEntry entry : currentEntries()) {
            entry.reset().run();
            valuePulse.put(entry.id(), System.currentTimeMillis());
        }
        if (currentCategory == Category.COMPUTERS) {
            tempQteIntervalMax = Math.max(tempQteIntervalMin, tempQteIntervalMax);
        }
    }

    private void resetAll() {
        tempHpBoost = GameSettings.DEFAULT_HP_BOOST;
        tempManiacCount = GameSettings.DEFAULT_MANIAC_COUNT;
        tempGameTime = GameSettings.DEFAULT_GAME_TIME;
        tempSelectedMap = GameSettings.DEFAULT_SELECTED_MAP;
        tempHackPointsRequired = GameSettings.DEFAULT_HACK_POINTS_REQUIRED;
        tempPointsPerPlayer = GameSettings.DEFAULT_POINTS_PER_PLAYER;
        tempPointsPerSpecialist = GameSettings.DEFAULT_POINTS_PER_SPECIALIST;
        tempMaxBonusPlayers = GameSettings.DEFAULT_MAX_BONUS_PLAYERS;
        tempHackerRadius = GameSettings.DEFAULT_HACKER_RADIUS;
        tempSupportRadius = GameSettings.DEFAULT_SUPPORT_RADIUS;
        tempQteIntervalMin = GameSettings.DEFAULT_QTE_INTERVAL_MIN;
        tempQteIntervalMax = GameSettings.DEFAULT_QTE_INTERVAL_MAX;
        tempQteSuccessBonus = GameSettings.DEFAULT_QTE_SUCCESS_BONUS;
        tempQteCritBonus = GameSettings.DEFAULT_QTE_CRIT_BONUS;
        tempComputersNeededForWin = GameSettings.DEFAULT_COMPUTERS_NEEDED;
        long now = System.currentTimeMillis();
        for (SettingEntry entry : currentEntries()) {
            valuePulse.put(entry.id(), now);
        }
    }

    private void saveSettings(boolean giveToAll) {
        tempQteIntervalMax = Math.max(tempQteIntervalMin, tempQteIntervalMax);
        ModNetworking.sendToServer(new UpdateSettingsPacket(
                tempHpBoost, tempManiacCount, tempGameTime, tempSelectedMap,
                tempHackPointsRequired, tempPointsPerPlayer, tempPointsPerSpecialist,
                tempMaxBonusPlayers, tempHackerRadius, tempSupportRadius,
                tempQteIntervalMin, tempQteIntervalMax, tempQteSuccessBonus,
                tempQteCritBonus, tempComputersNeededForWin));

        ClientGameSettings.setSettings(tempHpBoost, tempManiacCount, tempGameTime, tempSelectedMap);
        ClientGameSettings.setComputerSettings(
                tempHackPointsRequired, tempPointsPerPlayer, tempPointsPerSpecialist,
                tempMaxBonusPlayers, tempHackerRadius, tempSupportRadius,
                tempQteIntervalMin, tempQteIntervalMax, tempQteSuccessBonus,
                tempQteCritBonus, tempComputersNeededForWin);

        if (giveToAll) {
            ModNetworking.sendToServer(new GiveSettingsToAllPacket());
        }
        baselineSignature = settingsSignature();
        onClose();
    }

    private boolean hasUnsavedChanges() {
        return !settingsSignature().equals(baselineSignature);
    }

    private String settingsSignature() {
        return tempHpBoost + ":" + tempManiacCount + ":" + tempGameTime + ":" + tempSelectedMap + ":"
                + Float.floatToIntBits(tempHackPointsRequired) + ":"
                + Float.floatToIntBits(tempPointsPerPlayer) + ":"
                + Float.floatToIntBits(tempPointsPerSpecialist) + ":" + tempMaxBonusPlayers + ":"
                + Float.floatToIntBits(tempHackerRadius) + ":" + Float.floatToIntBits(tempSupportRadius) + ":"
                + tempQteIntervalMin + ":" + tempQteIntervalMax + ":"
                + Float.floatToIntBits(tempQteSuccessBonus) + ":"
                + Float.floatToIntBits(tempQteCritBonus) + ":" + tempComputersNeededForWin;
    }

    private void setTooltip(String title, String description, String hint) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(title).withStyle(ChatFormatting.WHITE));
        for (String line : wrapText(description, 230)) {
            lines.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
        }
        if (hint != null && !hint.isBlank()) {
            lines.add(Component.empty());
            for (String line : wrapText(hint, 230)) {
                lines.add(Component.literal(line).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        hoveredTooltip = lines;
    }

    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            String test = current.isEmpty() ? word : current + " " + word;
            if (font.width(test) <= maxWidth) {
                if (!current.isEmpty()) {
                    current.append(' ');
                }
                current.append(word);
            } else {
                if (!current.isEmpty()) {
                    lines.add(current.toString());
                }
                current = new StringBuilder(word);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

    private String trimToWidth(String text, int maxWidth) {
        if (maxWidth <= 0 || font.width(text) <= maxWidth) {
            return maxWidth <= 0 ? "" : text;
        }
        String suffix = "...";
        int suffixWidth = font.width(suffix);
        return font.plainSubstrByWidth(text, Math.max(0, maxWidth - suffixWidth)) + suffix;
    }

    private int accentColor() {
        return accentColor(currentCategory);
    }

    private int accentColor(Category category) {
        return category == Category.GAME ? ACCENT_GAME : ACCENT_COMPUTERS;
    }

    private static float roundHundredths(float value) {
        return Math.round(value * 100.0f) / 100.0f;
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static float easeOutCubic(float value) {
        float inverse = 1.0f - value;
        return 1.0f - inverse * inverse * inverse;
    }

    private static int withAlpha(int color, int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (color & 0x00FFFFFF);
    }

    private static int lerpColor(int from, int to, float amount) {
        float t = Mth.clamp(amount, 0.0f, 1.0f);
        int a = Math.round(((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * t);
        int r = Math.round(((from >>> 16) & 0xFF) + (((to >>> 16) & 0xFF) - ((from >>> 16) & 0xFF)) * t);
        int g = Math.round(((from >>> 8) & 0xFF) + (((to >>> 8) & 0xFF) - ((from >>> 8) & 0xFF)) * t);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record ControlLayout(int minusX, int valueX, int valueWidth, int plusX, int resetX) {}

    private record SettingEntry(String id, String title, String description, String hint,
                                Supplier<String> value, IntConsumer decrease, IntConsumer increase,
                                Runnable reset, BooleanSupplier isDefault, String tooltip) {}
}
