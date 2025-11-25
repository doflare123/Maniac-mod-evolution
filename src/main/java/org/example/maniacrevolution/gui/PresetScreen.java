package org.example.maniacrevolution.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import org.example.maniacrevolution.data.ClientPlayerData;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.ApplyPresetPacket;
import org.example.maniacrevolution.network.packets.DeletePresetPacket;
import org.example.maniacrevolution.network.packets.SavePresetPacket;
import org.example.maniacrevolution.perk.Perk;
import org.example.maniacrevolution.perk.PerkRegistry;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.preset.PerkPreset;

import java.util.ArrayList;
import java.util.List;

public class PresetScreen extends Screen {
    private int guiLeft, guiTop;
    private static final int GUI_WIDTH = 320;
    private static final int GUI_HEIGHT = 220;
    private static final int PERK_SLOT_SIZE = 32;

    private Mode currentMode = Mode.LIST;
    private EditBox presetNameField;
    private List<String> selectedPerksForPreset = new ArrayList<>();
    private int hoveredPresetIndex = -1;

    // Для режима создания
    private int createScrollOffset = 0;
    private Perk hoveredPerkInCreate = null;

    // Для отображения ошибки
    private String errorMessage = null;
    private int errorTicks = 0;

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
        presetNameField = new EditBox(font, guiLeft + 10, guiTop + 50, 200, 18, Component.literal(""));
        presetNameField.setMaxLength(20);
        presetNameField.setHint(Component.literal("Название пресета..."));
        presetNameField.setVisible(false);
        addRenderableWidget(presetNameField);

        updateButtons();
    }

    private void updateButtons() {
        // Очищаем старые кнопки
        clearWidgets();

        // Кнопка закрытия
        addRenderableWidget(Button.builder(Component.literal("X"), b -> Minecraft.getInstance().setScreen(new PerkSelectionScreen()))
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

        } else if (currentMode == Mode.CREATE) {
            presetNameField.setVisible(true);
            presetNameField.setFocused(true);

            // Кнопка отмены
            addRenderableWidget(Button.builder(Component.literal("Отмена"), b -> cancelCreate())
                    .pos(guiLeft + 10, guiTop + GUI_HEIGHT - 30).size(60, 20).build());

            // Кнопка сохранения
            Button saveBtn = Button.builder(Component.literal("Сохранить"), b -> savePreset())
                    .pos(guiLeft + 80, guiTop + GUI_HEIGHT - 30).size(70, 20).build();
            saveBtn.active = !selectedPerksForPreset.isEmpty();
            addRenderableWidget(saveBtn);
        }
    }

    private void startCreateMode() {
        currentMode = Mode.CREATE;
        selectedPerksForPreset.clear();
        presetNameField.setValue("");
        createScrollOffset = 0;
        errorMessage = null;
        errorTicks = 0;
        updateButtons();
    }

    private void cancelCreate() {
        currentMode = Mode.LIST;
        selectedPerksForPreset.clear();
        createScrollOffset = 0;
        errorMessage = null;
        errorTicks = 0;
        updateButtons();
    }

    private void savePreset() {
        String name = presetNameField.getValue().trim();
        if (name.isEmpty()) {
            name = "Пресет " + (ClientPlayerData.getPresets().size() + 1);
        }

        if (selectedPerksForPreset.isEmpty()) {
            return;
        }

        ModNetworking.CHANNEL.sendToServer(new SavePresetPacket(name, selectedPerksForPreset));
        currentMode = Mode.LIST;
        selectedPerksForPreset.clear();
        createScrollOffset = 0;
        errorMessage = null;
        errorTicks = 0;
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

        // Отображение ошибки
        if (errorMessage != null && errorTicks > 0) {
            int errorY = guiTop + GUI_HEIGHT - 55;
            gui.fill(guiLeft + 10, errorY, guiLeft + GUI_WIDTH - 10, errorY + 18, 0xDD440000);
            gui.renderOutline(guiLeft + 10, errorY, GUI_WIDTH - 20, 18, 0xFFFF0000);
            gui.drawCenteredString(font, "§c" + errorMessage, guiLeft + GUI_WIDTH / 2, errorY + 5, 0xFFFFFF);
        }

        super.render(gui, mouseX, mouseY, partialTick);
    }

    @Override
    public void tick() {
        super.tick();
        if (errorTicks > 0) {
            errorTicks--;
            if (errorTicks == 0) {
                errorMessage = null;
            }
        }
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

        // Показать выбранные
        gui.drawString(font, "§7Выбрано: §f" + selectedPerksForPreset.size() + "/2",
                guiLeft + GUI_WIDTH - 100, guiTop + 50, 0xFFFFFF, false);

        // Рендерим перки как в основном интерфейсе с прокруткой
        renderPerkGrid(gui, mouseX, mouseY);
    }

    private void renderPerkGrid(GuiGraphics gui, int mouseX, int mouseY) {
        List<Perk> allPerks = new ArrayList<>(PerkRegistry.getAllPerks());

        int startX = guiLeft + 10;
        int startY = guiTop + 75;
        int slotSize = 32;
        int spacing = 4;
        int perksPerRow = 8;

        hoveredPerkInCreate = null;

        // Область скролла
        int scrollAreaHeight = GUI_HEIGHT - 115;
        gui.enableScissor(guiLeft + 5, startY, guiLeft + GUI_WIDTH - 5, guiTop + GUI_HEIGHT - 35);

        int row = 0;
        int col = 0;

        for (Perk perk : allPerks) {
            int x = startX + col * (slotSize + spacing);
            int y = startY + row * (slotSize + spacing) - createScrollOffset;

            // Пропускаем если за пределами видимости
            if (y + slotSize < startY || y > guiTop + GUI_HEIGHT - 35) {
                col++;
                if (col >= perksPerRow) {
                    col = 0;
                    row++;
                }
                continue;
            }

            boolean selected = selectedPerksForPreset.contains(perk.getId());
            boolean hovered = mouseX >= x && mouseX < x + slotSize
                    && mouseY >= y && mouseY < y + slotSize
                    && mouseY >= startY && mouseY < guiTop + GUI_HEIGHT - 35;

            if (hovered) hoveredPerkInCreate = perk;

            // Фон слота
            int bgColor = selected ? 0xFF446644 : (hovered ? 0xFF555555 : 0xFF333333);
            gui.fill(x, y, x + slotSize, y + slotSize, bgColor);

            // Рамка по типу
            int borderColor;
            if (selected) {
                borderColor = 0xFF55FF55;
            } else {
                borderColor = switch (perk.getType()) {
                    case PASSIVE -> 0xFF5555FF;
                    case ACTIVE -> 0xFFFF5555;
                    case HYBRID -> 0xFFFF55FF;
                };
            }
            gui.renderOutline(x, y, slotSize, slotSize, borderColor);

            // Иконка
            renderPerkIcon(gui, perk, x, y, PERK_SLOT_SIZE);

            // Индикатор команды
            String teamMark = switch (perk.getTeam()) {
                case SURVIVOR -> "§aВ";
                case MANIAC -> "§cМ";
                case ALL -> "";
            };
            if (!teamMark.isEmpty()) {
                gui.drawString(font, teamMark, x + 2, y + 2, 0xFFFFFF, false);
            }

            col++;
            if (col >= perksPerRow) {
                col = 0;
                row++;
            }
        }

        gui.disableScissor();

        // Подсказка по скроллу
        int totalRows = (allPerks.size() + perksPerRow - 1) / perksPerRow;
        if (totalRows * (slotSize + spacing) > scrollAreaHeight) {
            gui.drawString(font, "§8↑↓ Колёсико мыши", guiLeft + GUI_WIDTH - 110,
                    guiTop + GUI_HEIGHT - 40, 0x666666, false);
        }

        // Тултип при наведении
        if (hoveredPerkInCreate != null) {
            renderPerkTooltip(gui, hoveredPerkInCreate, mouseX, mouseY);
        }
    }

    private void renderPerkIcon(GuiGraphics gui, Perk perk, int x, int y, int size) {
        Minecraft mc = Minecraft.getInstance();
        ResourceLocation texture = new ResourceLocation("maniacrev", "textures/perks/" + perk.getId() + ".png");

        try {
            RenderSystem.setShaderTexture(0, texture);
            RenderSystem.enableBlend();
            gui.blit(texture, x, y, 0, 0, size, size, size, size);
            RenderSystem.disableBlend();
        } catch (Exception e) {
            // Заглушка
            String initial = perk.getId().substring(0, 2).toUpperCase();
            gui.drawCenteredString(font, initial, x + size / 2, y + size / 2 - 4, 0xFFFFFF);
        }
    }

    private void renderPerkTooltip(GuiGraphics gui, Perk perk, int mouseX, int mouseY) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(perk.getName().copy().withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Тип: ").withStyle(ChatFormatting.GRAY)
                .append(perk.getType().getDisplayName()));
        tooltip.add(Component.literal("Команда: ").withStyle(ChatFormatting.GRAY)
                .append(perk.getTeam().getDisplayName()));

        if (perk.getCooldownTicks() > 0) {
            tooltip.add(Component.literal("КД: " + (perk.getCooldownTicks() / 20) + " сек")
                    .withStyle(ChatFormatting.RED));
        }

        gui.renderComponentTooltip(font, tooltip, mouseX, mouseY);
    }

    /**
     * Проверяет совместимость команд перков
     */
    private boolean canAddPerkToSelection(Perk perk) {
        if (selectedPerksForPreset.isEmpty()) {
            return true;
        }

        PerkTeam newPerkTeam = perk.getTeam();

        // Если перк для всех команд - всегда можно добавить
        if (newPerkTeam == PerkTeam.ALL) {
            return true;
        }

        // Проверяем команды уже выбранных перков
        for (String selectedId : selectedPerksForPreset) {
            Perk selectedPerk = PerkRegistry.getPerk(selectedId);
            if (selectedPerk == null) continue;

            PerkTeam selectedTeam = selectedPerk.getTeam();

            // Если уже выбранный перк для всех - можно добавить любой
            if (selectedTeam == PerkTeam.ALL) {
                continue;
            }

            // Если команды различаются (MANIAC vs SURVIVOR) - нельзя
            if (newPerkTeam != selectedTeam) {
                return false;
            }
        }

        return true;
    }

    /**
     * Показывает сообщение об ошибке с звуком
     */
    private void showError(String message) {
        errorMessage = message;
        errorTicks = 60; // 3 секунды (60 тиков)

        // Воспроизводим звук злого жителя
        Minecraft.getInstance().player.playSound(
                SoundEvents.VILLAGER_NO,
                1.0F,
                1.0F
        );
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (currentMode == Mode.LIST && hoveredPresetIndex >= 0) {
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

            if (currentMode == Mode.CREATE && hoveredPerkInCreate != null) {
                String perkId = hoveredPerkInCreate.getId();

                // Если перк уже выбран - снимаем выбор
                if (selectedPerksForPreset.contains(perkId)) {
                    selectedPerksForPreset.remove(perkId);
                    errorMessage = null;
                    errorTicks = 0;
                    updateButtons();
                    return true;
                }

                // Проверяем лимит
                if (selectedPerksForPreset.size() >= 2) {
                    showError("Максимум 2 перка в пресете!");
                    return true;
                }

                // Проверяем совместимость команд
                if (!canAddPerkToSelection(hoveredPerkInCreate)) {
                    showError("Нельзя объединить перки Маньяка и Выжившего!");
                    return true;
                }

                // Добавляем перк
                selectedPerksForPreset.add(perkId);
                errorMessage = null;
                errorTicks = 0;
                updateButtons();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (currentMode == Mode.CREATE) {
            List<Perk> allPerks = new ArrayList<>(PerkRegistry.getAllPerks());
            int perksPerRow = 8;
            int slotSize = 32;
            int spacing = 4;
            int totalRows = (allPerks.size() + perksPerRow - 1) / perksPerRow;
            int scrollAreaHeight = GUI_HEIGHT - 115;

            int maxScroll = Math.max(0, totalRows * (slotSize + spacing) - scrollAreaHeight);
            createScrollOffset = (int) Math.max(0, Math.min(maxScroll, createScrollOffset - delta * 25));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private enum Mode { LIST, CREATE }
}