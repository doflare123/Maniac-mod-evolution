package org.example.maniacrevolution.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.example.maniacrevolution.map.MapData;
import org.example.maniacrevolution.map.MapRegistry;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.UpdateSettingsPacket;
import org.example.maniacrevolution.network.packets.GiveSettingsToAllPacket;
import org.example.maniacrevolution.settings.ClientGameSettings;
import org.example.maniacrevolution.settings.GameSettings;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SettingsScreen extends Screen {

    // ── Категории ─────────────────────────────────────────────────────────
    private enum Category { GAME, COMPUTERS }
    private Category currentCategory = Category.GAME;

    // ── Временные значения — Игра ─────────────────────────────────────────
    private int   tempHpBoost;
    private int   tempManiacCount;
    private int   tempGameTime;
    private int   tempSelectedMap;

    // ── Временные значения — Компьютеры ──────────────────────────────────
    private float tempHackPointsRequired;
    private float tempPointsPerPlayer;
    private float tempPointsPerSpecialist;
    private int   tempMaxBonusPlayers;
    private float tempHackerRadius;
    private float tempSupportRadius;
    private int   tempQteIntervalMin;
    private int   tempQteIntervalMax;
    private float tempQteSuccessBonus;
    private float tempQteCritBonus;
    private int   tempComputersNeededForWin;

    // ── Layout ───────────────────────────────────────────────────────────
    private static final int GUI_WIDTH      = 380;
    private static final int HEADER_HEIGHT  = 22;
    private static final int TAB_HEIGHT     = 24;
    private static final int FOOTER_HEIGHT  = 80;
    private static final int ROW_HEIGHT     = 28;

    // Игра: 6 строк, Компьютеры: 11 строк
    private static final int GAME_ROWS      = 4;
    private static final int COMPUTER_ROWS  = 11;

    private static final int SCROLL_AREA_H_GAME     = ROW_HEIGHT * GAME_ROWS;
    private static final int SCROLL_AREA_H_COMPUTERS = ROW_HEIGHT * COMPUTER_ROWS;

    private int guiHeight() {
        int rows = currentCategory == Category.GAME ? GAME_ROWS : COMPUTER_ROWS;
        return HEADER_HEIGHT + TAB_HEIGHT + ROW_HEIGHT * rows + FOOTER_HEIGHT;
    }

    // Колонки
    private static final int COL_LABEL    = 12;
    private static final int COL_MINUS    = 228;
    private static final int COL_VALUE    = 254;
    private static final int VALUE_W      = 56;
    private static final int COL_PLUS     = 314;
    private static final int COL_RESET    = 340;
    private static final int BTN_W        = 22;
    private static final int BTN_RESET_W  = 28;

    private int leftPos;
    private int topPos;

    // ── Marquee ──────────────────────────────────────────────────────────
    private float   marqueeOffset = 0f;
    private boolean marqueeDir    = true;
    private int     marqueePause  = 60;
    private long    marqueeLastMs = 0;

    public SettingsScreen() {
        super(Component.literal("Настройки игры"));
        loadFromClient();
    }

    private void loadFromClient() {
        tempHpBoost              = ClientGameSettings.getHpBoost();
        tempManiacCount          = ClientGameSettings.getManiacCount();
        tempGameTime             = ClientGameSettings.getGameTime();
        tempSelectedMap          = ClientGameSettings.getSelectedMap();
        tempHackPointsRequired   = ClientGameSettings.getHackPointsRequired();
        tempPointsPerPlayer      = ClientGameSettings.getPointsPerPlayer();
        tempPointsPerSpecialist  = ClientGameSettings.getPointsPerSpecialist();
        tempMaxBonusPlayers      = ClientGameSettings.getMaxBonusPlayers();
        tempHackerRadius         = ClientGameSettings.getHackerRadius();
        tempSupportRadius        = ClientGameSettings.getSupportRadius();
        tempQteIntervalMin       = ClientGameSettings.getQteIntervalMin();
        tempQteIntervalMax       = ClientGameSettings.getQteIntervalMax();
        tempQteSuccessBonus      = ClientGameSettings.getQteSuccessBonus();
        tempQteCritBonus         = ClientGameSettings.getQteCritBonus();
        tempComputersNeededForWin = ClientGameSettings.getComputersNeededForWin();
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new SettingsScreen());
    }

    // ── Init ─────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        super.init();
        recalcPos();
        resetMarquee();
        rebuildButtons();
    }

    private void recalcPos() {
        leftPos = (width  - GUI_WIDTH)  / 2;
        topPos  = (height - guiHeight()) / 2;
    }

    private void rebuildButtons() {
        clearWidgets();

        // ── Кнопки категорий ──────────────────────────────────────────────
        int tabY = topPos + HEADER_HEIGHT + 2;
        int tabW = (GUI_WIDTH - 8) / 2;

        addRenderableWidget(Button.builder(
                Component.literal("🎮 Игра"),
                b -> { currentCategory = Category.GAME; recalcPos(); rebuildButtons(); })
                .bounds(leftPos + 4, tabY, tabW, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("💻 Компьютеры"),
                b -> { currentCategory = Category.COMPUTERS; recalcPos(); rebuildButtons(); })
                .bounds(leftPos + 4 + tabW + 2, tabY, tabW, 20).build());

        // ── Строки настроек ───────────────────────────────────────────────
        int contentStart = topPos + HEADER_HEIGHT + TAB_HEIGHT + 2;

        if (currentCategory == Category.GAME) {
            buildGameRows(contentStart);
        } else {
            buildComputerRows(contentStart);
        }

        // ── Подвал ────────────────────────────────────────────────────────
        int rows = currentCategory == Category.GAME ? GAME_ROWS : COMPUTER_ROWS;
        int fy = contentStart + rows * ROW_HEIGHT + 4;

        addRenderableWidget(Button.builder(Component.literal("Применить"),
                b -> applySettings())
                .bounds(leftPos + 8, fy, 176, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Сбросить все"),
                b -> resetAll())
                .bounds(leftPos + 190, fy, 182, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Выдать настройки всем"),
                b -> giveSettingsToAll())
                .bounds(leftPos + 8, fy + 26, 364, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Закрыть"),
                b -> onClose())
                .bounds(leftPos + 8, fy + 52, 364, 20).build());
    }

    private void buildGameRows(int startY) {

        addRow(startY, 0, () -> tempHpBoost,
                v -> tempHpBoost = Math.max(0, v), 2,
                () -> tempHpBoost = GameSettings.DEFAULT_HP_BOOST);

        addRow(startY, 1, () -> tempManiacCount,
                v -> tempManiacCount = Math.max(1, v), 1,
                () -> tempManiacCount = GameSettings.DEFAULT_MANIAC_COUNT);

        addRow(startY, 2, () -> tempGameTime,
                v -> tempGameTime = Math.max(1, v), 1,
                () -> tempGameTime = GameSettings.DEFAULT_GAME_TIME);

        addMapRow(startY, 3);
    }

    private void buildComputerRows(int startY) {
        // Очков для взлома (float, шаг 0.5)
        addFloatRow(startY, 0, () -> tempHackPointsRequired,
                v -> tempHackPointsRequired = Math.max(1f, v), 0.5f,
                () -> tempHackPointsRequired = GameSettings.DEFAULT_HACK_POINTS_REQUIRED);

        // Очков/сек на игрока (шаг 0.01)
        addFloatRow(startY, 1, () -> tempPointsPerPlayer,
                v -> tempPointsPerPlayer = Math.max(0.01f, v), 0.01f,
                () -> tempPointsPerPlayer = GameSettings.DEFAULT_POINTS_PER_PLAYER);

        // Очков/сек на специалиста (шаг 0.01)
        addFloatRow(startY, 2, () -> tempPointsPerSpecialist,
                v -> tempPointsPerSpecialist = Math.max(0.01f, v), 0.01f,
                () -> tempPointsPerSpecialist = GameSettings.DEFAULT_POINTS_PER_SPECIALIST);

        // Макс бонусников (шаг 1)
        addRow(startY, 3, () -> tempMaxBonusPlayers,
                v -> tempMaxBonusPlayers = Math.max(1, v), 1,
                () -> tempMaxBonusPlayers = GameSettings.DEFAULT_MAX_BONUS_PLAYERS);

        // Радиус хакера (шаг 0.1)
        addFloatRow(startY, 4, () -> tempHackerRadius,
                v -> tempHackerRadius = Math.max(0.5f, v), 0.1f,
                () -> tempHackerRadius = GameSettings.DEFAULT_HACKER_RADIUS);

        // Радиус поддержки (шаг 0.1)
        addFloatRow(startY, 5, () -> tempSupportRadius,
                v -> tempSupportRadius = Math.max(0.5f, v), 0.1f,
                () -> tempSupportRadius = GameSettings.DEFAULT_SUPPORT_RADIUS);

        // QTE мин интервал (шаг 1)
        addRow(startY, 6, () -> tempQteIntervalMin,
                v -> tempQteIntervalMin = Math.max(1, v), 1,
                () -> tempQteIntervalMin = GameSettings.DEFAULT_QTE_INTERVAL_MIN);

        // QTE макс интервал (шаг 1)
        addRow(startY, 7, () -> tempQteIntervalMax,
                v -> tempQteIntervalMax = Math.max(tempQteIntervalMin, v), 1,
                () -> tempQteIntervalMax = GameSettings.DEFAULT_QTE_INTERVAL_MAX);

        // QTE бонус обычный (шаг 0.05)
        addFloatRow(startY, 8, () -> tempQteSuccessBonus,
                v -> tempQteSuccessBonus = Math.max(0f, v), 0.05f,
                () -> tempQteSuccessBonus = GameSettings.DEFAULT_QTE_SUCCESS_BONUS);

        // QTE бонус крит (шаг 0.05)
        addFloatRow(startY, 9, () -> tempQteCritBonus,
                v -> tempQteCritBonus = Math.max(0f, v), 0.05f,
                () -> tempQteCritBonus = GameSettings.DEFAULT_QTE_CRIT_BONUS);

        // Компов для победы (шаг 1)
        addRow(startY, 10, () -> tempComputersNeededForWin,
                v -> tempComputersNeededForWin = Math.max(1, v), 1,
                () -> tempComputersNeededForWin = GameSettings.DEFAULT_COMPUTERS_NEEDED);
    }

    // ── Хелперы строк ─────────────────────────────────────────────────────

    private int rowY(int startY, int idx) {
        return startY + idx * ROW_HEIGHT;
    }

    private void addRow(int startY, int idx, Supplier<Integer> get, Consumer<Integer> set,
                        int step, Runnable reset) {
        int by = rowY(startY, idx) + (ROW_HEIGHT - 20) / 2;
        addRenderableWidget(Button.builder(Component.literal("-"),
                b -> { set.accept(get.get() - step); rebuildButtons(); })
                .bounds(leftPos + COL_MINUS, by, BTN_W, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"),
                b -> { set.accept(get.get() + step); rebuildButtons(); })
                .bounds(leftPos + COL_PLUS, by, BTN_W, 20).build());
        addRenderableWidget(Button.builder(Component.literal("↺"),
                b -> { reset.run(); rebuildButtons(); })
                .bounds(leftPos + COL_RESET, by, BTN_RESET_W, 20).build());
    }

    private void addFloatRow(int startY, int idx, Supplier<Float> get, Consumer<Float> set,
                             float step, Runnable reset) {
        int by = rowY(startY, idx) + (ROW_HEIGHT - 20) / 2;
        addRenderableWidget(Button.builder(Component.literal("-"),
                b -> { set.accept(Math.round((get.get() - step) * 100f) / 100f); rebuildButtons(); })
                .bounds(leftPos + COL_MINUS, by, BTN_W, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"),
                b -> { set.accept(Math.round((get.get() + step) * 100f) / 100f); rebuildButtons(); })
                .bounds(leftPos + COL_PLUS, by, BTN_W, 20).build());
        addRenderableWidget(Button.builder(Component.literal("↺"),
                b -> { reset.run(); rebuildButtons(); })
                .bounds(leftPos + COL_RESET, by, BTN_RESET_W, 20).build());
    }

    private void addMapRow(int startY, int idx) {
        int by = rowY(startY, idx) + (ROW_HEIGHT - 20) / 2;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            List<MapData> maps = mapsWithVoting();
            int i = mapIndex(maps);
            tempSelectedMap = maps.get((i - 1 + maps.size()) % maps.size()).getNumericId();
            resetMarquee(); rebuildButtons();
        }).bounds(leftPos + COL_MINUS, by, BTN_W, 20).build());

        addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            List<MapData> maps = mapsWithVoting();
            int i = mapIndex(maps);
            tempSelectedMap = maps.get((i + 1) % maps.size()).getNumericId();
            resetMarquee(); rebuildButtons();
        }).bounds(leftPos + COL_PLUS, by, BTN_W, 20).build());

        addRenderableWidget(Button.builder(Component.literal("↺"), b -> {
            tempSelectedMap = GameSettings.DEFAULT_SELECTED_MAP;
            resetMarquee(); rebuildButtons();
        }).bounds(leftPos + COL_RESET, by, BTN_RESET_W, 20).build());
    }

    // ── Marquee ──────────────────────────────────────────────────────────

    private void resetMarquee() {
        marqueeOffset = 0f; marqueeDir = true; marqueePause = 60;
        marqueeLastMs = System.currentTimeMillis();
    }

    private void tickMarquee(int textWidth) {
        if (textWidth <= VALUE_W) { marqueeOffset = 0; return; }
        long now = System.currentTimeMillis();
        float dt = (now - marqueeLastMs) / 50f;
        marqueeLastMs = now;
        float maxOff = textWidth - VALUE_W;
        if (marqueePause > 0) { marqueePause -= dt; return; }
        if (marqueeDir) {
            marqueeOffset += 0.5f * dt;
            if (marqueeOffset >= maxOff) { marqueeOffset = maxOff; marqueeDir = false; marqueePause = 60; }
        } else {
            marqueeOffset -= 0.5f * dt;
            if (marqueeOffset <= 0) { marqueeOffset = 0; marqueeDir = true; marqueePause = 60; }
        }
    }

    // ── Render ───────────────────────────────────────────────────────────

    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        int gh = guiHeight();

        // Тень + фон
        g.fill(leftPos - 2, topPos - 2, leftPos + GUI_WIDTH + 2, topPos + gh + 2, 0x88000000);
        g.fill(leftPos,     topPos,     leftPos + GUI_WIDTH,     topPos + gh,     0xFF1A1A1A);
        g.fill(leftPos + 1, topPos + 1, leftPos + GUI_WIDTH - 1, topPos + gh - 1, 0xFF242424);

        // Заголовок
        g.fill(leftPos + 2, topPos + 2, leftPos + GUI_WIDTH - 2, topPos + HEADER_HEIGHT - 1, 0xFF303030);
        g.fill(leftPos + 2, topPos + HEADER_HEIGHT - 1, leftPos + GUI_WIDTH - 2, topPos + HEADER_HEIGHT, 0xFF555555);
        g.drawCenteredString(font, title, leftPos + GUI_WIDTH / 2, topPos + 7, 0xFFFFFF);

        // Подсветка активной вкладки
        int tabY = topPos + HEADER_HEIGHT + 2;
        int tabW = (GUI_WIDTH - 8) / 2;
        if (currentCategory == Category.GAME) {
            g.fill(leftPos + 4, tabY, leftPos + 4 + tabW, tabY + 20, 0x44FFFF00);
        } else {
            g.fill(leftPos + 4 + tabW + 2, tabY, leftPos + 4 + tabW * 2 + 2, tabY + 20, 0x4400AAFF);
        }

        // Разделитель под вкладками
        int contentStart = topPos + HEADER_HEIGHT + TAB_HEIGHT + 2;
        g.fill(leftPos + 2, contentStart - 2, leftPos + GUI_WIDTH - 2, contentStart - 1, 0xFF555555);

        // Строки
        renderRows(g, contentStart);

        // Разделитель перед подвалом
        int rows = currentCategory == Category.GAME ? GAME_ROWS : COMPUTER_ROWS;
        int flY = contentStart + rows * ROW_HEIGHT;
        g.fill(leftPos + 2, flY, leftPos + GUI_WIDTH - 2, flY + 1, 0xFF555555);

        super.render(g, mx, my, pt);
    }

    private void renderRows(GuiGraphics g, int startY) {
        String[] labels;
        String[] values;

        if (currentCategory == Category.GAME) {
            String mapName = mapName();
            tickMarquee(font.width(mapName));
            labels = new String[]{"Доп. ХП:",
                    "Маньяков:", "Время (мин):", "Карта:"};
            values = new String[]{
                    String.valueOf(tempHpBoost),
                    String.valueOf(tempManiacCount),
                    String.valueOf(tempGameTime),
                    mapName
            };
            renderRowList(g, startY, labels, values, GAME_ROWS, true);
        } else {
            labels = new String[]{
                    "Очков для взлома:", "Очков/сек (игрок):", "Очков/сек (спец):",
                    "Макс. помощников:", "Радиус хакера:", "Радиус поддержки:",
                    "QTE мин. (сек):", "QTE макс. (сек):", "QTE бонус:", "QTE крит бонус:",
                    "Компов для победы:"
            };
            values = new String[]{
                    String.format("%.2f", tempHackPointsRequired),
                    String.format("%.2f", tempPointsPerPlayer),
                    String.format("%.2f", tempPointsPerSpecialist),
                    String.valueOf(tempMaxBonusPlayers),
                    String.format("%.1f", tempHackerRadius),
                    String.format("%.1f", tempSupportRadius),
                    String.valueOf(tempQteIntervalMin),
                    String.valueOf(tempQteIntervalMax),
                    String.format("%.2f", tempQteSuccessBonus),
                    String.format("%.2f", tempQteCritBonus),
                    String.valueOf(tempComputersNeededForWin)
            };
            renderRowList(g, startY, labels, values, COMPUTER_ROWS, false);
        }
    }

    private void renderRowList(GuiGraphics g, int startY, String[] labels,
                               String[] values, int rowCount, boolean lastIsMap) {
        for (int i = 0; i < rowCount; i++) {
            int ry = startY + i * ROW_HEIGHT;
            int ty = ry + (ROW_HEIGHT - 8) / 2;
            if (i % 2 == 0) g.fill(leftPos + 2, ry, leftPos + GUI_WIDTH - 2, ry + ROW_HEIGHT, 0x14FFFFFF);
            g.drawString(font, labels[i], leftPos + COL_LABEL, ty, 0xBBBBBB, false);

            if (lastIsMap && i == rowCount - 1) {
                int cx1 = leftPos + COL_VALUE;
                int cx2 = cx1 + VALUE_W;
                g.enableScissor(cx1, ry, cx2, ry + ROW_HEIGHT);
                g.drawString(font, values[i], cx1 - (int) marqueeOffset, ty, 0xFFFF55, false);
                g.disableScissor();
            } else {
                g.drawString(font, values[i], leftPos + COL_VALUE, ty, 0xFFFF55, false);
            }
        }
    }

    // ── Actions ──────────────────────────────────────────────────────────

    private void applySettings() {
        ModNetworking.sendToServer(new UpdateSettingsPacket(
                tempHpBoost,
                tempManiacCount, tempGameTime, tempSelectedMap,
                tempHackPointsRequired, tempPointsPerPlayer,
                tempPointsPerSpecialist, tempMaxBonusPlayers,
                tempHackerRadius, tempSupportRadius,
                tempQteIntervalMin, tempQteIntervalMax,
                tempQteSuccessBonus, tempQteCritBonus,
                tempComputersNeededForWin));

        ClientGameSettings.setSettings(tempHpBoost,
                tempManiacCount, tempGameTime, tempSelectedMap);
        ClientGameSettings.setComputerSettings(
                tempHackPointsRequired, tempPointsPerPlayer,
                tempPointsPerSpecialist, tempMaxBonusPlayers,
                tempHackerRadius, tempSupportRadius,
                tempQteIntervalMin, tempQteIntervalMax,
                tempQteSuccessBonus, tempQteCritBonus,
                tempComputersNeededForWin);
        onClose();
    }

    private void resetAll() {
        tempHpBoost              = GameSettings.DEFAULT_HP_BOOST;
        tempManiacCount          = GameSettings.DEFAULT_MANIAC_COUNT;
        tempGameTime             = GameSettings.DEFAULT_GAME_TIME;
        tempSelectedMap          = GameSettings.DEFAULT_SELECTED_MAP;
        tempHackPointsRequired   = GameSettings.DEFAULT_HACK_POINTS_REQUIRED;
        tempPointsPerPlayer      = GameSettings.DEFAULT_POINTS_PER_PLAYER;
        tempPointsPerSpecialist  = GameSettings.DEFAULT_POINTS_PER_SPECIALIST;
        tempMaxBonusPlayers      = GameSettings.DEFAULT_MAX_BONUS_PLAYERS;
        tempHackerRadius         = GameSettings.DEFAULT_HACKER_RADIUS;
        tempSupportRadius        = GameSettings.DEFAULT_SUPPORT_RADIUS;
        tempQteIntervalMin       = GameSettings.DEFAULT_QTE_INTERVAL_MIN;
        tempQteIntervalMax       = GameSettings.DEFAULT_QTE_INTERVAL_MAX;
        tempQteSuccessBonus      = GameSettings.DEFAULT_QTE_SUCCESS_BONUS;
        tempQteCritBonus         = GameSettings.DEFAULT_QTE_CRIT_BONUS;
        tempComputersNeededForWin = GameSettings.DEFAULT_COMPUTERS_NEEDED;
        resetMarquee();
        rebuildButtons();
    }

    private void giveSettingsToAll() {
        applySettings();
        ModNetworking.sendToServer(new GiveSettingsToAllPacket());
    }

    // ── Map helpers ───────────────────────────────────────────────────────

    private List<MapData> mapsWithVoting() {
        List<MapData> list = new ArrayList<>();
        list.add(new MapData("voting", 0, "Голосование", "", null));
        list.addAll(MapRegistry.getAllMaps());
        return list;
    }

    private int mapIndex(List<MapData> maps) {
        for (int i = 0; i < maps.size(); i++)
            if (maps.get(i).getNumericId() == tempSelectedMap) return i;
        return 0;
    }

    private String mapName() {
        if (tempSelectedMap == 0) return "Голосование";
        MapData m = MapRegistry.getMapByNumericId(tempSelectedMap);
        return m != null ? m.getName() : "Неизвестно";
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
