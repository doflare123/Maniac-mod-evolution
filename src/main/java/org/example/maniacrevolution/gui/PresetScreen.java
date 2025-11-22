package org.example.maniacrevolution.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.example.maniacrevolution.data.ClientPlayerData;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.ApplyPresetPacket;
import org.example.maniacrevolution.network.packets.DeletePresetPacket;
import org.example.maniacrevolution.network.packets.SavePresetPacket;
import org.example.maniacrevolution.perk.Perk;
import org.example.maniacrevolution.perk.PerkRegistry;
import org.example.maniacrevolution.preset.PerkPreset;

import java.util.ArrayList;
import java.util.List;

public class PresetScreen extends Screen {
    private int guiLeft, guiTop;
    private static final int GUI_WIDTH = 280;
    private static final int GUI_HEIGHT = 200;

    private Mode currentMode = Mode.LIST;
    private EditBox presetNameField;
    private List<String> selectedPerksForPreset = new ArrayList<>();
    private int hoveredPresetIndex = -1;

    public PresetScreen() {
        super(Component.literal("Пресеты перков"));
    }

    @Override
    protected void init() {
        guiLeft = (width - GUI_WIDTH) / 2;
        guiTop = (height - GUI_HEIGHT) / 2;

        // Кнопка закрытия
        addRenderableWidget(Button.builder(Component.literal("X"), b -> onClose())
                .pos(guiLeft + GUI_WIDTH - 20, guiTop + 5).size(15, 15).build());

        // Поле для названия (для режима создания)
        presetNameField = new EditBox(font, guiLeft + 10, guiTop + 50, 150, 18, Component.literal(""));
        presetNameField.setMaxLength(20);
        presetNameField.setHint(Component.literal("Название пресета..."));
        presetNameField.setVisible(false);
        addRenderableWidget(presetNameField);

        updateButtons();
    }

    private void updateButtons() {
        // Очищаем старые кнопки (кроме X и поля ввода)
        clearWidgets();

        // Кнопка закрытия
        addRenderableWidget(Button.builder(Component.literal("X"), b -> onClose())
                .pos(guiLeft + GUI_WIDTH - 20, guiTop + 5).size(15, 15).build());

        addRenderableWidget(presetNameField);

        if (currentMode == Mode.LIST) {
            presetNameField.setVisible(false);

            // Кнопка создания нового пресета
            int maxPresets = ClientPlayerData.getMaxPresets();
            int currentPresets = ClientPlayerData.getPresets().size();
            boolean canCreate = currentPresets < maxPresets;

            Button createBtn = Button.builder(
                    Component.literal(canCreate ? "+ Создать" : "§cСлоты заполнены"),
                    b -> { if (canCreate) startCreateMode(); }
            ).pos(guiLeft + 10, guiTop + GUI_HEIGHT - 30).size(80, 20).build();
            createBtn.active = canCreate;
            addRenderableWidget(createBtn);

            // Информация о слотах
        } else if (currentMode == Mode.CREATE) {
            presetNameField.setVisible(true);
            presetNameField.setFocused(true);

            // Кнопка отмены
            addRenderableWidget(Button.builder(Component.literal("Отмена"), b -> cancelCreate())
                    .pos(guiLeft + 10, guiTop + GUI_HEIGHT - 30).size(60, 20).build());

            // Кнопка сохранения
            addRenderableWidget(Button.builder(Component.literal("Сохранить"), b -> savePreset())
                    .pos(guiLeft + 80, guiTop + GUI_HEIGHT - 30).size(70, 20).build());
        }
    }

    private void startCreateMode() {
        currentMode = Mode.CREATE;
        selectedPerksForPreset.clear();
        presetNameField.setValue("");
        updateButtons();
    }

    private void cancelCreate() {
        currentMode = Mode.LIST;
        selectedPerksForPreset.clear();
        updateButtons();
    }

    private void savePreset() {
        String name = presetNameField.getValue().trim();
        if (name.isEmpty()) {
            name = "Пресет " + (ClientPlayerData.getPresets().size() + 1);
        }

        if (selectedPerksForPreset.isEmpty()) {
            return; // Нужно выбрать хотя бы 1 перк
        }

        ModNetworking.CHANNEL.sendToServer(new SavePresetPacket(name, selectedPerksForPreset));
        currentMode = Mode.LIST;
        selectedPerksForPreset.clear();
        updateButtons();
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);

        // Фон
        gui.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xEE1a1a1a);
        gui.renderOutline(guiLeft, guiTop, GUI_WIDTH, GUI_HEIGHT, 0xFF666666);

        // Заголовок
        gui.drawCenteredString(font, "§6Пресеты перков", guiLeft + GUI_WIDTH / 2, guiTop + 7, 0xFFFFFF);

        // Информация о слотах
        int maxPresets = ClientPlayerData.getMaxPresets();
        int currentPresets = ClientPlayerData.getPresets().size();
        gui.drawString(font, "§7Слоты: §f" + currentPresets + "§7/§f" + maxPresets,
                guiLeft + 10, guiTop + 25, 0xFFFFFF, false);

        if (currentMode == Mode.LIST) {
            renderPresetList(gui, mouseX, mouseY);
        } else {
            renderCreateMode(gui, mouseX, mouseY);
        }

        super.render(gui, mouseX, mouseY, partialTick);
    }

    private void renderPresetList(GuiGraphics gui, int mouseX, int mouseY) {
        List<PerkPreset> presets = ClientPlayerData.getPresets();

        if (presets.isEmpty()) {
            gui.drawCenteredString(font, "§7У вас нет сохранённых пресетов",
                    guiLeft + GUI_WIDTH / 2, guiTop + 70, 0x888888);
            gui.drawCenteredString(font, "§8Нажмите '+ Создать' чтобы добавить",
                    guiLeft + GUI_WIDTH / 2, guiTop + 85, 0x666666);
            return;
        }

        hoveredPresetIndex = -1;

        int y = guiTop + 45;
        int entryHeight = 40;

        for (int i = 0; i < presets.size(); i++) {
            PerkPreset preset = presets.get(i);

            boolean hovered = mouseX >= guiLeft + 10 && mouseX < guiLeft + GUI_WIDTH - 10
                    && mouseY >= y && mouseY < y + entryHeight - 2;

            if (hovered) hoveredPresetIndex = i;

            renderPresetEntry(gui, preset, i, guiLeft + 10, y, GUI_WIDTH - 20, entryHeight - 3, hovered, mouseX, mouseY);
            y += entryHeight;
        }
    }

    private void renderPresetEntry(GuiGraphics gui, PerkPreset preset, int index, int x, int y,
                                   int width, int height, boolean hovered, int mouseX, int mouseY) {
        // Фон
        int bgColor = hovered ? 0xFF3a3a3a : 0xFF2a2a2a;
        gui.fill(x, y, x + width, y + height, bgColor);
        gui.renderOutline(x, y, width, height, hovered ? 0xFFFFAA00 : 0xFF555555);

        // Название
        gui.drawString(font, "§f" + preset.getName(), x + 5, y + 5, 0xFFFFFF, false);

        // Перки в пресете
        StringBuilder perksStr = new StringBuilder("§7Перки: ");
        for (String perkId : preset.getPerkIds()) {
            Perk perk = PerkRegistry.getPerk(perkId);
            if (perk != null) {
                perksStr.append("§e").append(perk.getName().getString()).append("§7, ");
            }
        }
        if (perksStr.toString().endsWith(", ")) {
            perksStr = new StringBuilder(perksStr.substring(0, perksStr.length() - 2));
        }
        gui.drawString(font, perksStr.toString(), x + 5, y + 17, 0xAAAAAA, false);

        // Кнопка применить
        int btnX = x + width - 115;
        int btnY = y + 5;
        boolean applyHovered = mouseX >= btnX && mouseX < btnX + 50 && mouseY >= btnY && mouseY < btnY + 16;
        gui.fill(btnX, btnY, btnX + 50, btnY + 16, applyHovered ? 0xFF448844 : 0xFF336633);
        gui.renderOutline(btnX, btnY, 50, 16, 0xFF55FF55);
        gui.drawCenteredString(font, "Выбрать", btnX + 25, btnY + 4, 0xFFFFFF);

        // Кнопка удалить
        int delX = x + width - 55;
        boolean delHovered = mouseX >= delX && mouseX < delX + 50 && mouseY >= btnY && mouseY < btnY + 16;
        gui.fill(delX, btnY, delX + 50, btnY + 16, delHovered ? 0xFF884444 : 0xFF663333);
        gui.renderOutline(delX, btnY, 50, 16, 0xFFFF5555);
        gui.drawCenteredString(font, "Удалить", delX + 25, btnY + 4, 0xFFFFFF);
    }

    private void renderCreateMode(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(font, "§eСоздание пресета", guiLeft + 10, guiTop + 38, 0xFFFFFF, false);

        // Выбор перков
        gui.drawString(font, "§7Выберите до 2 перков:", guiLeft + 10, guiTop + 75, 0xAAAAAA, false);

        // Список всех перков для выбора
        int x = guiLeft + 10;
        int y = guiTop + 90;
        int col = 0;

        for (Perk perk : PerkRegistry.getAllPerks()) {
            boolean selected = selectedPerksForPreset.contains(perk.getId());
            boolean hovered = mouseX >= x && mouseX < x + 80 && mouseY >= y && mouseY < y + 18;

            int bgColor = selected ? 0xFF446644 : (hovered ? 0xFF444444 : 0xFF333333);
            gui.fill(x, y, x + 80, y + 18, bgColor);

            if (selected) {
                gui.renderOutline(x, y, 80, 18, 0xFF55FF55);
            }

            String name = perk.getName().getString();
            if (name.length() > 10) name = name.substring(0, 9) + "..";
            gui.drawString(font, name, x + 3, y + 5, selected ? 0xFFFFFF : 0xAAAAAA, false);

            col++;
            x += 85;
            if (col >= 3) {
                col = 0;
                x = guiLeft + 10;
                y += 22;
            }
        }

        // Показать выбранные
        gui.drawString(font, "§7Выбрано: §f" + selectedPerksForPreset.size() + "/2",
                guiLeft + 170, guiTop + 50, 0xFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (currentMode == Mode.LIST && hoveredPresetIndex >= 0) {
                List<PerkPreset> presets = ClientPlayerData.getPresets();
                int width = GUI_WIDTH - 20;
                int x = guiLeft + 10;
                int btnX = x + width - 115;
                int delX = x + width - 55;

                // Применить
                if (mouseX >= btnX && mouseX < btnX + 50) {
                    ModNetworking.CHANNEL.sendToServer(new ApplyPresetPacket(hoveredPresetIndex));
                    onClose();
                    return true;
                }

                // Удалить
                if (mouseX >= delX && mouseX < delX + 50) {
                    ModNetworking.CHANNEL.sendToServer(new DeletePresetPacket(hoveredPresetIndex));
                    return true;
                }
            }

            if (currentMode == Mode.CREATE) {
                // Клик на перк для выбора
                int x = guiLeft + 10;
                int y = guiTop + 90;
                int col = 0;

                for (Perk perk : PerkRegistry.getAllPerks()) {
                    if (mouseX >= x && mouseX < x + 80 && mouseY >= y && mouseY < y + 18) {
                        if (selectedPerksForPreset.contains(perk.getId())) {
                            selectedPerksForPreset.remove(perk.getId());
                        } else if (selectedPerksForPreset.size() < 2) {
                            selectedPerksForPreset.add(perk.getId());
                        }
                        return true;
                    }

                    col++;
                    x += 85;
                    if (col >= 3) {
                        col = 0;
                        x = guiLeft + 10;
                        y += 22;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private enum Mode { LIST, CREATE }
}
