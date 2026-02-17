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

    // ── Временные значения ───────────────────────────────────────────────
    private int tempComputerCount;
    private int tempHackPoints;
    private int tempHpBoost;
    private int tempManiacCount;
    private int tempGameTime;
    private int tempSelectedMap;

    // ── Layout ───────────────────────────────────────────────────────────
    private static final int GUI_WIDTH      = 360;
    private static final int HEADER_HEIGHT  = 22;
    private static final int FOOTER_HEIGHT  = 80;
    private static final int ROW_HEIGHT     = 28;
    private static final int ROW_COUNT      = 6;
    private static final int CONTENT_HEIGHT = ROW_HEIGHT * ROW_COUNT;  // 168
    private static final int SCROLL_AREA_H  = 168;                      // все строки влезают без скролла
    private static final int GUI_HEIGHT     = HEADER_HEIGHT + SCROLL_AREA_H + FOOTER_HEIGHT;

    // Колонки (от левого края GUI)
    private static final int COL_LABEL  = 12;
    private static final int COL_MINUS  = 218;
    private static final int COL_VALUE  = 244;   // текст значения
    private static final int VALUE_W    = 46;    // ширина «окошка» для значения
    private static final int COL_PLUS   = 294;
    private static final int COL_RESET  = 320;
    private static final int BTN_W      = 22;
    private static final int BTN_RESET_W = 28;

    private int leftPos;
    private int topPos;

    // ── Marquee ──────────────────────────────────────────────────────────
    private float  marqueeOffset    = 0f;
    private boolean marqueeDir      = true;   // true = вправо
    private int    marqueePause     = 60;     // тики паузы на краях
    private long   marqueeLastMs    = 0;

    public SettingsScreen() {
        super(Component.literal("Настройки игры"));
        tempComputerCount = ClientGameSettings.getComputerCount();
        tempHackPoints    = ClientGameSettings.getHackPoints();
        tempHpBoost       = ClientGameSettings.getHpBoost();
        tempManiacCount   = ClientGameSettings.getManiacCount();
        tempGameTime      = ClientGameSettings.getGameTime();
        tempSelectedMap   = ClientGameSettings.getSelectedMap();
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new SettingsScreen());
    }

    // ── Init ─────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        super.init();
        leftPos = (width  - GUI_WIDTH)  / 2;
        topPos  = (height - GUI_HEIGHT) / 2;
        resetMarquee();
        rebuildButtons();
    }

    private void rebuildButtons() {
        clearWidgets();

        addSettingRow(0, () -> tempComputerCount,
                v -> tempComputerCount = Math.max(1, Math.min(9, v)), 1,
                () -> tempComputerCount = GameSettings.DEFAULT_COMPUTER_COUNT);

        addSettingRow(1, () -> tempHackPoints,
                v -> tempHackPoints = Math.max(500, v), 500,
                () -> tempHackPoints = GameSettings.DEFAULT_HACK_POINTS);

        addSettingRow(2, () -> tempHpBoost,
                v -> tempHpBoost = Math.max(0, v), 2,
                () -> tempHpBoost = GameSettings.DEFAULT_HP_BOOST);

        addSettingRow(3, () -> tempManiacCount,
                v -> tempManiacCount = Math.max(1, v), 1,
                () -> tempManiacCount = GameSettings.DEFAULT_MANIAC_COUNT);

        addSettingRow(4, () -> tempGameTime,
                v -> tempGameTime = Math.max(1, v), 1,
                () -> tempGameTime = GameSettings.DEFAULT_GAME_TIME);

        addMapRow(5);

        // Подвал
        int fy = topPos + HEADER_HEIGHT + SCROLL_AREA_H + 7;
        addRenderableWidget(Button.builder(Component.literal("Применить"),
                        b -> applySettings())
                .bounds(leftPos + 8, fy, 164, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Сбросить все"),
                        b -> resetAll())
                .bounds(leftPos + 178, fy, 174, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Выдать настройки всем"),
                        b -> giveSettingsToAll())
                .bounds(leftPos + 8, fy + 26, 344, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Закрыть"),
                        b -> onClose())
                .bounds(leftPos + 8, fy + 52, 344, 20).build());
    }

    private int rowY(int index) {
        return topPos + HEADER_HEIGHT + index * ROW_HEIGHT;
    }

    private void addSettingRow(int idx, Supplier<Integer> get, Consumer<Integer> set,
                               int step, Runnable reset) {
        int by = rowY(idx) + (ROW_HEIGHT - 20) / 2;
        addRenderableWidget(Button.builder(Component.literal("-"),
                        b -> set.accept(get.get() - step))
                .bounds(leftPos + COL_MINUS, by, BTN_W, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"),
                        b -> set.accept(get.get() + step))
                .bounds(leftPos + COL_PLUS, by, BTN_W, 20).build());
        addRenderableWidget(Button.builder(Component.literal("↺"),
                        b -> reset.run())
                .bounds(leftPos + COL_RESET, by, BTN_RESET_W, 20).build());
    }

    private void addMapRow(int idx) {
        int by = rowY(idx) + (ROW_HEIGHT - 20) / 2;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            List<MapData> maps = mapsWithVoting();
            int i = mapIndex(maps);
            tempSelectedMap = maps.get((i - 1 + maps.size()) % maps.size()).getNumericId();
            resetMarquee();
        }).bounds(leftPos + COL_MINUS, by, BTN_W, 20).build());

        addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            List<MapData> maps = mapsWithVoting();
            int i = mapIndex(maps);
            tempSelectedMap = maps.get((i + 1) % maps.size()).getNumericId();
            resetMarquee();
        }).bounds(leftPos + COL_PLUS, by, BTN_W, 20).build());

        addRenderableWidget(Button.builder(Component.literal("↺"), b -> {
            tempSelectedMap = GameSettings.DEFAULT_SELECTED_MAP;
            resetMarquee();
        }).bounds(leftPos + COL_RESET, by, BTN_RESET_W, 20).build());
    }

    // ── Marquee ──────────────────────────────────────────────────────────

    private void resetMarquee() {
        marqueeOffset  = 0f;
        marqueeDir     = true;
        marqueePause   = 60;
        marqueeLastMs  = System.currentTimeMillis();
    }

    private void tickMarquee(int textWidth) {
        if (textWidth <= VALUE_W) { marqueeOffset = 0; return; }

        long now     = System.currentTimeMillis();
        float dtTick = (now - marqueeLastMs) / 50f;   // 1 тик = 50 мс
        marqueeLastMs = now;

        float maxOff = textWidth - VALUE_W;

        if (marqueePause > 0) { marqueePause -= dtTick; return; }

        if (marqueeDir) {
            marqueeOffset += 0.5f * dtTick;
            if (marqueeOffset >= maxOff) { marqueeOffset = maxOff; marqueeDir = false; marqueePause = 60; }
        } else {
            marqueeOffset -= 0.5f * dtTick;
            if (marqueeOffset <= 0)      { marqueeOffset = 0;      marqueeDir = true;  marqueePause = 60; }
        }
    }

    // ── Render ───────────────────────────────────────────────────────────

    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float pt) {
        // 1. Стандартный затемнённый фон Minecraft
        renderBackground(g);

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        // 2. Тень
        g.fill(leftPos - 2, topPos - 2, leftPos + GUI_WIDTH + 2, topPos + GUI_HEIGHT + 2, 0x88000000);
        // 3. Фон окна
        g.fill(leftPos,     topPos,     leftPos + GUI_WIDTH,     topPos + GUI_HEIGHT,     0xFF1A1A1A);
        // 4. Внутренняя рамка
        g.fill(leftPos + 1, topPos + 1, leftPos + GUI_WIDTH - 1, topPos + GUI_HEIGHT - 1, 0xFF242424);

        // 5. Заголовок
        g.fill(leftPos + 2, topPos + 2, leftPos + GUI_WIDTH - 2, topPos + HEADER_HEIGHT - 1, 0xFF303030);
        g.fill(leftPos + 2, topPos + HEADER_HEIGHT - 1, leftPos + GUI_WIDTH - 2, topPos + HEADER_HEIGHT, 0xFF555555);
        g.drawCenteredString(font, title, leftPos + GUI_WIDTH / 2, topPos + 7, 0xFFFFFF);

        // 6. Линия перед подвалом
        int flY = topPos + HEADER_HEIGHT + SCROLL_AREA_H;
        g.fill(leftPos + 2, flY, leftPos + GUI_WIDTH - 2, flY + 1, 0xFF555555);

        // 7. Строки настроек
        String mapName = mapName();
        tickMarquee(font.width(mapName));

        String[] labels = {"Компьютеров:", "Очков для хака:", "Доп. ХП:",
                "Маньяков:", "Время (мин):", "Карта:"};
        String[] values = {
                String.valueOf(tempComputerCount), String.valueOf(tempHackPoints),
                String.valueOf(tempHpBoost),       String.valueOf(tempManiacCount),
                String.valueOf(tempGameTime),      mapName
        };

        for (int i = 0; i < ROW_COUNT; i++) {
            int ry = rowY(i);
            int ty = ry + (ROW_HEIGHT - 8) / 2;

            // Зебра
            if (i % 2 == 0) g.fill(leftPos + 2, ry, leftPos + GUI_WIDTH - 2, ry + ROW_HEIGHT, 0x14FFFFFF);

            g.drawString(font, labels[i], leftPos + COL_LABEL, ty, 0xBBBBBB, false);

            if (i == ROW_COUNT - 1) {
                // Карта: marquee — одиночный scissor, без вложенности
                int cx1 = leftPos + COL_VALUE;
                int cx2 = cx1 + VALUE_W;
                g.enableScissor(cx1, ry, cx2, ry + ROW_HEIGHT);
                g.drawString(font, mapName, cx1 - (int) marqueeOffset, ty, 0xFFFF55, false);
                g.disableScissor();
            } else {
                g.drawString(font, values[i], leftPos + COL_VALUE, ty, 0xFFFF55, false);
            }
        }

        // 8. Кнопки (super.render)
        super.render(g, mx, my, pt);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

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

    private void applySettings() {
        ModNetworking.sendToServer(new UpdateSettingsPacket(
                tempComputerCount, tempHackPoints, tempHpBoost,
                tempManiacCount, tempGameTime, tempSelectedMap));
        ClientGameSettings.setSettings(tempComputerCount, tempHackPoints, tempHpBoost,
                tempManiacCount, tempGameTime, tempSelectedMap);
        onClose();
    }

    private void resetAll() {
        tempComputerCount = GameSettings.DEFAULT_COMPUTER_COUNT;
        tempHackPoints    = GameSettings.DEFAULT_HACK_POINTS;
        tempHpBoost       = GameSettings.DEFAULT_HP_BOOST;
        tempManiacCount   = GameSettings.DEFAULT_MANIAC_COUNT;
        tempGameTime      = GameSettings.DEFAULT_GAME_TIME;
        tempSelectedMap   = GameSettings.DEFAULT_SELECTED_MAP;
        resetMarquee();
        rebuildButtons();
    }

    private void giveSettingsToAll() {
        // Сначала применяем текущие настройки, потом выдаём предмет всем
        ModNetworking.sendToServer(new UpdateSettingsPacket(
                tempComputerCount, tempHackPoints, tempHpBoost,
                tempManiacCount, tempGameTime, tempSelectedMap));
        ClientGameSettings.setSettings(tempComputerCount, tempHackPoints, tempHpBoost,
                tempManiacCount, tempGameTime, tempSelectedMap);
        ModNetworking.sendToServer(new GiveSettingsToAllPacket());
        onClose();
    }

    @Override
    public boolean isPauseScreen() { return false; }
}