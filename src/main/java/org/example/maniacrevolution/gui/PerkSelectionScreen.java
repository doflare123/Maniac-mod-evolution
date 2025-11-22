package org.example.maniacrevolution.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.SelectPerkPacket;
import org.example.maniacrevolution.perk.Perk;
import org.example.maniacrevolution.perk.PerkRegistry;
import org.example.maniacrevolution.perk.PerkTeam;

import java.util.ArrayList;
import java.util.List;

public class PerkSelectionScreen extends Screen {
    private static final ResourceLocation BG = new ResourceLocation(Maniacrev.MODID, "textures/gui/perk_selection.png");

    private List<Perk> availablePerks = new ArrayList<>();
    private List<String> selectedPerkIds = new ArrayList<>();
    private int scrollOffset = 0;
    private Tab currentTab = Tab.ALL;

    // Размеры
    private int guiLeft, guiTop;
    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 200;
    private static final int PERK_SLOT_SIZE = 32;
    private static final int PERKS_PER_ROW = 6;

    public PerkSelectionScreen() {
        super(Component.literal("Выбор перков"));
    }

    @Override
    protected void init() {
        guiLeft = (width - GUI_WIDTH) / 2;
        guiTop = (height - GUI_HEIGHT) / 2;

        // Вкладки
        int tabY = guiTop - 20;
        addRenderableWidget(Button.builder(Component.literal("Все"), b -> switchTab(Tab.ALL))
                .pos(guiLeft, tabY).size(50, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Пресеты"), b -> Minecraft.getInstance().setScreen(new PresetScreen()))
                .pos(guiLeft + 55, tabY).size(60, 20).build());


        // Кнопка подтверждения
        addRenderableWidget(Button.builder(Component.literal("Готово"), b -> confirm())
                .pos(guiLeft + GUI_WIDTH - 60, guiTop + GUI_HEIGHT - 25).size(55, 20).build());

        // Кнопка очистки
        addRenderableWidget(Button.builder(Component.literal("Сброс"), b -> clearSelection())
                .pos(guiLeft + 5, guiTop + GUI_HEIGHT - 25).size(50, 20).build());

        loadAvailablePerks();
    }

    private void loadAvailablePerks() {
        availablePerks.clear();

        // Получаем команду игрока из локальных данных
        // (В реальности это должно приходить с сервера)
        PerkTeam playerTeam = getPlayerTeam();

        for (Perk perk : PerkRegistry.getAllPerks()) {
            if (playerTeam == null || perk.isAvailableForTeam(playerTeam)) {
                availablePerks.add(perk);
            }
        }
    }

    private PerkTeam getPlayerTeam() {
        if (minecraft == null || minecraft.player == null) return null;
        var team = minecraft.player.getTeam();
        if (team == null) return null;
        String name = team.getName();
        if ("maniac".equals(name)) return PerkTeam.MANIAC;
        if ("survivors".equals(name)) return PerkTeam.SURVIVOR;
        return null;
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);

        // Фон
        gui.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xCC222222);
        gui.renderOutline(guiLeft, guiTop, GUI_WIDTH, GUI_HEIGHT, 0xFFAAAAAA);

        // Заголовок
        gui.drawCenteredString(font, "Выберите 2 перка", guiLeft + GUI_WIDTH / 2, guiTop + 5, 0xFFFFFF);

        // Выбранные перки
        String selected = "Выбрано: " + selectedPerkIds.size() + "/2";
        gui.drawString(font, selected, guiLeft + 5, guiTop + 18,
                selectedPerkIds.size() == 2 ? 0x55FF55 : 0xFFFF55);

        if (currentTab == Tab.ALL) {
            renderPerksGrid(gui, mouseX, mouseY);
        } else {
            renderPresetsTab(gui, mouseX, mouseY);
        }

        super.render(gui, mouseX, mouseY, partialTick);

        // Тултип
        renderPerkTooltip(gui, mouseX, mouseY);
    }

    private void renderPerksGrid(GuiGraphics gui, int mouseX, int mouseY) {
        int startX = guiLeft + 10;
        int startY = guiTop + 35;

        for (int i = 0; i < availablePerks.size(); i++) {
            Perk perk = availablePerks.get(i);
            int row = i / PERKS_PER_ROW;
            int col = i % PERKS_PER_ROW;

            int x = startX + col * (PERK_SLOT_SIZE + 4);
            int y = startY + row * (PERK_SLOT_SIZE + 4) - scrollOffset;

            if (y < startY - PERK_SLOT_SIZE || y > guiTop + GUI_HEIGHT - 30) continue;

            boolean isSelected = selectedPerkIds.contains(perk.getId());
            boolean isHovered = mouseX >= x && mouseX < x + PERK_SLOT_SIZE
                    && mouseY >= y && mouseY < y + PERK_SLOT_SIZE;

            // Фон слота
            int bgColor = isSelected ? 0xFF446644 : (isHovered ? 0xFF555555 : 0xFF333333);
            gui.fill(x, y, x + PERK_SLOT_SIZE, y + PERK_SLOT_SIZE, bgColor);

            // Рамка по типу
            int borderColor = switch (perk.getType()) {
                case PASSIVE -> 0xFF5555FF;
                case ACTIVE -> 0xFFFF5555;
                case HYBRID -> 0xFFFF55FF;
            };
            if (isSelected) borderColor = 0xFF55FF55;
            gui.renderOutline(x, y, PERK_SLOT_SIZE, PERK_SLOT_SIZE, borderColor);

            // Иконка (заглушка)
            String initial = perk.getId().substring(0, 2).toUpperCase();
            gui.drawCenteredString(font, initial, x + PERK_SLOT_SIZE / 2, y + PERK_SLOT_SIZE / 2 - 4, 0xFFFFFF);

            // Индикатор команды
            String teamMark = switch (perk.getTeam()) {
                case SURVIVOR -> "§aВ";
                case MANIAC -> "§cМ";
                case ALL -> "";
            };
            if (!teamMark.isEmpty()) {
                gui.drawString(font, teamMark, x + 2, y + 2, 0xFFFFFF, false);
            }
        }
    }

    private void renderPresetsTab(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawCenteredString(font, "§7Пресеты (в разработке)",
                guiLeft + GUI_WIDTH / 2, guiTop + 80, 0xAAAAAA);
        gui.drawCenteredString(font, "§7Здесь можно будет сохранять",
                guiLeft + GUI_WIDTH / 2, guiTop + 95, 0x888888);
        gui.drawCenteredString(font, "§7наборы перков",
                guiLeft + GUI_WIDTH / 2, guiTop + 105, 0x888888);
    }

    private void renderPerkTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        if (currentTab != Tab.ALL) return;

        int startX = guiLeft + 10;
        int startY = guiTop + 35;

        for (int i = 0; i < availablePerks.size(); i++) {
            Perk perk = availablePerks.get(i);
            int row = i / PERKS_PER_ROW;
            int col = i % PERKS_PER_ROW;

            int x = startX + col * (PERK_SLOT_SIZE + 4);
            int y = startY + row * (PERK_SLOT_SIZE + 4) - scrollOffset;

            if (mouseX >= x && mouseX < x + PERK_SLOT_SIZE
                    && mouseY >= y && mouseY < y + PERK_SLOT_SIZE) {
                gui.renderComponentTooltip(font, perk.getTooltip(), mouseX, mouseY);
                break;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (currentTab == Tab.ALL && button == 0) {
            int startX = guiLeft + 10;
            int startY = guiTop + 35;

            for (int i = 0; i < availablePerks.size(); i++) {
                Perk perk = availablePerks.get(i);
                int row = i / PERKS_PER_ROW;
                int col = i % PERKS_PER_ROW;

                int x = startX + col * (PERK_SLOT_SIZE + 4);
                int y = startY + row * (PERK_SLOT_SIZE + 4) - scrollOffset;

                if (mouseX >= x && mouseX < x + PERK_SLOT_SIZE
                        && mouseY >= y && mouseY < y + PERK_SLOT_SIZE) {
                    togglePerkSelection(perk);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void togglePerkSelection(Perk perk) {
        if (selectedPerkIds.contains(perk.getId())) {
            selectedPerkIds.remove(perk.getId());
        } else if (selectedPerkIds.size() < 2) {
            selectedPerkIds.add(perk.getId());
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int maxScroll = Math.max(0, (availablePerks.size() / PERKS_PER_ROW + 1) * (PERK_SLOT_SIZE + 4) - 120);
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - delta * 20));
        return true;
    }

    private void switchTab(Tab tab) {
        currentTab = tab;
    }

    private void clearSelection() {
        selectedPerkIds.clear();
    }

    private void confirm() {
        ModNetworking.CHANNEL.sendToServer(new SelectPerkPacket(selectedPerkIds));
        onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private enum Tab { ALL, PRESETS }
}
