package org.example.maniacrevolution.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.example.maniacrevolution.data.ClientPlayerData;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.ApplyPresetPacket;
import org.example.maniacrevolution.network.packets.DeletePresetPacket;
import org.example.maniacrevolution.network.packets.SavePresetPacket;
import org.example.maniacrevolution.perk.Perk;
import org.example.maniacrevolution.perk.PerkRegistry;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.preset.PerkPreset;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Preset manager that shares the same layout, panels and interaction language as perk selection.
 */
public class PresetScreen extends Screen {
    private static final int GAP = 6;
    private static final int HEADER_HEIGHT = 28;
    private static final int FOOTER_HEIGHT = 42;
    private static final int PANEL_HEADER_HEIGHT = 18;
    private static final int MAX_PERKS = 2;
    private static final int CARD_GAP = 4;
    private static final int PERK_CARD_HEIGHT = 44;
    private static final int PRESET_CARD_HEIGHT = 58;

    private final Screen parent;
    private final List<String> selectedPerksForPreset = new ArrayList<>(MAX_PERKS);
    private final List<PresetHit> presetHits = new ArrayList<>();
    private final List<PerkHit> perkHits = new ArrayList<>();
    private final List<SelectedHit> selectedHits = new ArrayList<>();

    private Mode currentMode = Mode.LIST;
    private EditBox presetNameField;
    private Perk hoveredPerkInCreate;
    private Perk focusedPerkInCreate;

    private int pageX;
    private int pageY;
    private int pageWidth;
    private int pageHeight;
    private Rect listPanel = Rect.EMPTY;
    private Rect listViewport = Rect.EMPTY;
    private Rect collectionPanel = Rect.EMPTY;
    private Rect createGridViewport = Rect.EMPTY;
    private Rect builderPanel = Rect.EMPTY;
    private Rect footerPanel = Rect.EMPTY;
    private Rect backButton = Rect.EMPTY;
    private Rect closeButton = Rect.EMPTY;
    private Rect createButton = Rect.EMPTY;
    private Rect cancelButton = Rect.EMPTY;
    private Rect saveButton = Rect.EMPTY;

    private int listScroll;
    private int listScrollMax;
    private int createScroll;
    private int createScrollMax;
    private String errorMessage;
    private int errorTicks;

    public PresetScreen() {
        this(null);
    }

    public PresetScreen(Screen parent) {
        super(Component.literal("Пресеты перков"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        calculateLayout();
        refreshInputWidget();
        if (focusedPerkInCreate == null) {
            focusedPerkInCreate = PerkRegistry.getAllPerks().stream().findFirst().orElse(null);
        }
    }

    private void calculateLayout() {
        int maxWidth = Math.max(240, Math.min(760, width - 12));
        int maxHeight = Math.max(155, Math.min(430, height - 12));
        int minWidth = Math.min(400, maxWidth);
        int minHeight = Math.min(240, maxHeight);

        pageWidth = Mth.clamp(Math.round(width * 0.76f), minWidth, maxWidth);
        pageHeight = Mth.clamp(Math.round(height * 0.82f), minHeight, maxHeight);
        pageX = (width - pageWidth) / 2;
        pageY = (height - pageHeight) / 2;

        int contentY = pageY + HEADER_HEIGHT;
        int contentHeight = Math.max(55, pageHeight - HEADER_HEIGHT - FOOTER_HEIGHT - GAP);
        listPanel = new Rect(pageX, contentY, pageWidth, contentHeight);
        listViewport = new Rect(listPanel.x() + 7, listPanel.y() + PANEL_HEADER_HEIGHT + 6,
                Math.max(1, listPanel.width() - 14),
                Math.max(1, listPanel.height() - PANEL_HEADER_HEIGHT - 11));

        int availableWidth = pageWidth - GAP;
        int builderWidth = Mth.clamp(availableWidth * 38 / 100,
                Math.min(122, availableWidth), 280);
        int collectionWidth = Math.max(112, availableWidth - builderWidth);
        if (collectionWidth + builderWidth > availableWidth) {
            builderWidth = Math.max(1, availableWidth - collectionWidth);
        }
        collectionPanel = new Rect(pageX, contentY, collectionWidth, contentHeight);
        builderPanel = new Rect(collectionPanel.right() + GAP, contentY,
                builderWidth, contentHeight);
        createGridViewport = new Rect(collectionPanel.x() + 6,
                collectionPanel.y() + PANEL_HEADER_HEIGHT + 6,
                Math.max(1, collectionPanel.width() - 12),
                Math.max(1, collectionPanel.height() - PANEL_HEADER_HEIGHT - 11));

        footerPanel = new Rect(pageX, contentY + contentHeight + GAP, pageWidth, FOOTER_HEIGHT);
        backButton = new Rect(pageX + 5, pageY + 5, 73, 18);
        closeButton = new Rect(pageX + pageWidth - 23, pageY + 5, 18, 18);

        int footerY = footerPanel.y() + 9;
        createButton = new Rect(footerPanel.right() - 112, footerY, 106, 23);
        saveButton = new Rect(footerPanel.right() - 100, footerY, 94, 23);
        cancelButton = new Rect(saveButton.x() - 78, footerY, 72, 23);
    }

    private void refreshInputWidget() {
        String currentName = presetNameField == null ? "" : presetNameField.getValue();
        clearWidgets();
        presetNameField = null;

        if (currentMode == Mode.CREATE) {
            int fieldX = builderPanel.x() + 8;
            int fieldY = builderPanel.y() + 35;
            presetNameField = new EditBox(font, fieldX, fieldY,
                    Math.max(20, builderPanel.width() - 16), 18,
                    Component.literal("Название пресета"));
            presetNameField.setMaxLength(20);
            presetNameField.setHint(Component.literal("Название пресета..."));
            presetNameField.setTextColor(GuideTheme.TEXT);
            presetNameField.setTextColorUneditable(GuideTheme.TEXT_MUTED);
            presetNameField.setValue(currentName);
            addRenderableWidget(presetNameField);
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);
        drawBackdrop(gui);

        presetHits.clear();
        perkHits.clear();
        selectedHits.clear();
        hoveredPerkInCreate = null;

        drawHeader(gui, mouseX, mouseY);
        if (currentMode == Mode.LIST) {
            drawPresetList(gui, mouseX, mouseY);
        } else {
            drawCreateMode(gui, mouseX, mouseY);
        }
        drawFooter(gui, mouseX, mouseY);

        super.render(gui, mouseX, mouseY, partialTick);
    }

    private void drawBackdrop(GuiGraphics gui) {
        gui.fillGradient(0, 0, width, height, 0xE5141B1A, 0xF0070C0B);
        gui.fill(pageX, pageY, pageX + pageWidth, pageY + pageHeight, 0x78101720);
        gui.renderOutline(pageX, pageY, pageWidth, pageHeight, GuideTheme.BORDER_SOFT);
    }

    private void drawHeader(GuiGraphics gui, int mouseX, int mouseY) {
        String backLabel = currentMode == Mode.CREATE ? "← ПРЕСЕТЫ" : "← ВЫБОР";
        drawButton(gui, backButton, backLabel, GuideTheme.GREEN,
                backButton.contains(mouseX, mouseY), false, true);

        String title = currentMode == Mode.CREATE ? "НОВЫЙ ПРЕСЕТ" : "ПРЕСЕТЫ ПЕРКОВ";
        title = trim(title, Math.max(30, pageWidth - 190));
        gui.drawCenteredString(font, title, pageX + pageWidth / 2, pageY + 7, GuideTheme.TEXT);
        int underlineWidth = Math.min(96, Math.max(42, font.width(title) / 2));
        gui.fill(pageX + pageWidth / 2 - underlineWidth / 2, pageY + 19,
                pageX + pageWidth / 2 + underlineWidth / 2, pageY + 20, GuideTheme.GREEN);

        int current = ClientPlayerData.getPresets().size();
        int maximum = ClientPlayerData.getMaxPresets();
        String slots = current + " / " + maximum;
        int slotsRight = closeButton.x() - 7;
        int slotsWidth = font.width(slots) + 12;
        if (slotsRight - slotsWidth > pageX + pageWidth / 2 + 45) {
            gui.fill(slotsRight - slotsWidth, pageY + 7, slotsRight, pageY + 21, 0xE5193823);
            gui.renderOutline(slotsRight - slotsWidth, pageY + 7, slotsWidth, 14,
                    GuideTheme.GREEN);
            gui.drawCenteredString(font, slots, slotsRight - slotsWidth / 2,
                    pageY + 10, GuideTheme.GREEN);
        }

        drawButton(gui, closeButton, "×", GuideTheme.RED,
                closeButton.contains(mouseX, mouseY), false, true);
    }

    private void drawPresetList(GuiGraphics gui, int mouseX, int mouseY) {
        drawPanel(gui, listPanel, "СОХРАНЁННЫЕ НАБОРЫ", GuideTheme.GREEN);
        List<PerkPreset> presets = ClientPlayerData.getPresets();
        String count = presets.size() + " шт.";
        gui.drawString(font, count, listPanel.right() - font.width(count) - 7,
                listPanel.y() + 5, GuideTheme.TEXT_MUTED, false);

        if (presets.isEmpty()) {
            int centerX = listViewport.x() + listViewport.width() / 2;
            int centerY = listViewport.y() + listViewport.height() / 2;
            gui.drawCenteredString(font, "Сохранённых наборов пока нет",
                    centerX, centerY - 10, GuideTheme.TEXT_SECONDARY);
            gui.drawCenteredString(font, "Создайте пресет из одного или двух перков",
                    centerX, centerY + 4, GuideTheme.TEXT_MUTED);
            listScroll = 0;
            listScrollMax = 0;
            return;
        }

        int totalHeight = presets.size() * PRESET_CARD_HEIGHT
                + Math.max(0, presets.size() - 1) * CARD_GAP;
        listScrollMax = Math.max(0, totalHeight - listViewport.height());
        listScroll = Mth.clamp(listScroll, 0, listScrollMax);

        gui.enableScissor(listViewport.x(), listViewport.y(),
                listViewport.right(), listViewport.bottom());
        for (int index = 0; index < presets.size(); index++) {
            int y = listViewport.y() + index * (PRESET_CARD_HEIGHT + CARD_GAP) - listScroll;
            Rect card = new Rect(listViewport.x(), y, listViewport.width(), PRESET_CARD_HEIGHT);
            if (card.bottom() <= listViewport.y() || card.y() >= listViewport.bottom()) {
                continue;
            }
            drawPresetCard(gui, presets.get(index), index, card, mouseX, mouseY);
        }
        gui.disableScissor();
        drawScrollBar(gui, listViewport, listScroll, listScrollMax, totalHeight);
    }

    private void drawPresetCard(GuiGraphics gui, PerkPreset preset, int index, Rect card,
                                int mouseX, int mouseY) {
        boolean hovered = listViewport.contains(mouseX, mouseY) && card.contains(mouseX, mouseY);
        GuideTheme.drawCard(gui, card.x(), card.y(), card.width(), card.height(),
                GuideTheme.GREEN, hovered);

        int actionWidth = Math.min(70, Math.max(54, card.width() / 5));
        Rect apply = new Rect(card.right() - actionWidth - 5, card.y() + 5,
                actionWidth, 21);
        Rect delete = new Rect(card.right() - actionWidth - 5, card.y() + 32,
                actionWidth, 19);

        int infoRight = apply.x() - 6;
        gui.drawString(font, trim(preset.getName(), Math.max(1, infoRight - card.x() - 47)),
                card.x() + 7, card.y() + 6, GuideTheme.TEXT, false);
        String amount = preset.getPerkCount() + " / " + MAX_PERKS;
        gui.drawString(font, amount, infoRight - font.width(amount), card.y() + 6,
                preset.getPerkCount() == MAX_PERKS ? GuideTheme.GREEN : GuideTheme.GOLD, false);

        int slotX = card.x() + 7;
        int slotY = card.y() + 23;
        int slotsWidth = Math.max(1, infoRight - slotX);
        int slotGap = 4;
        int slotWidth = Math.max(1, (slotsWidth - slotGap) / MAX_PERKS);
        for (int slot = 0; slot < MAX_PERKS; slot++) {
            Rect bounds = new Rect(slotX + slot * (slotWidth + slotGap), slotY,
                    slotWidth, 28);
            String perkId = slot < preset.getPerkIds().size()
                    ? preset.getPerkIds().get(slot) : null;
            drawPresetPerkSlot(gui, perkId, slot, bounds);
        }

        drawPrimaryButton(gui, apply, "ВЫБРАТЬ", apply.contains(mouseX, mouseY));
        drawButton(gui, delete, "Удалить", GuideTheme.RED,
                delete.contains(mouseX, mouseY), false, true);
        presetHits.add(new PresetHit(index, apply, delete));
    }

    private void drawPresetPerkSlot(GuiGraphics gui, String perkId, int slot, Rect bounds) {
        gui.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), 0xD9222A33);
        gui.renderOutline(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                GuideTheme.BORDER_SOFT);
        if (perkId == null) {
            gui.drawCenteredString(font, "Слот " + (slot + 1),
                    bounds.x() + bounds.width() / 2, bounds.y() + 10, GuideTheme.TEXT_MUTED);
            return;
        }

        Perk perk = PerkRegistry.getPerk(perkId);
        if (perk == null) {
            gui.drawString(font, trim(perkId, Math.max(1, bounds.width() - 6)),
                    bounds.x() + 3, bounds.y() + 10, GuideTheme.TEXT_MUTED, false);
            return;
        }

        int iconSize = 22;
        renderPerkIcon(gui, perk, bounds.x() + 3, bounds.y() + 3, iconSize);
        gui.fill(bounds.x(), bounds.bottom() - 2, bounds.right(), bounds.bottom() - 1,
                perk.getType().getArgbColor());
        int textX = bounds.x() + 29;
        int textWidth = Math.max(1, bounds.right() - textX - 3);
        gui.drawString(font, trim(perk.getName().getString(), textWidth),
                textX, bounds.y() + 5, GuideTheme.TEXT_SECONDARY, false);
        gui.drawString(font, trim(perk.getType().getDisplayName().getString(), textWidth),
                textX, bounds.y() + 16, perk.getType().getArgbColor(), false);
    }

    private void drawCreateMode(GuiGraphics gui, int mouseX, int mouseY) {
        drawPanel(gui, collectionPanel, "ПЕРКИ ДЛЯ ПРЕСЕТА", GuideTheme.GREEN);
        drawPanel(gui, builderPanel, "НАСТРОЙКА ПРЕСЕТА", GuideTheme.GREEN);
        drawCreateGrid(gui, mouseX, mouseY);
        drawBuilder(gui, mouseX, mouseY);
    }

    private void drawCreateGrid(GuiGraphics gui, int mouseX, int mouseY) {
        List<Perk> perks = List.copyOf(PerkRegistry.getAllPerks());
        String count = perks.size() + " шт.";
        gui.drawString(font, count, collectionPanel.right() - font.width(count) - 6,
                collectionPanel.y() + 5, GuideTheme.TEXT_MUTED, false);

        int columns = Mth.clamp((createGridViewport.width() + CARD_GAP)
                / (132 + CARD_GAP), 1, 3);
        int cardWidth = Math.max(1,
                (createGridViewport.width() - CARD_GAP * (columns - 1)) / columns);
        int rows = (perks.size() + columns - 1) / columns;
        int totalHeight = rows * PERK_CARD_HEIGHT + Math.max(0, rows - 1) * CARD_GAP;
        createScrollMax = Math.max(0, totalHeight - createGridViewport.height());
        createScroll = Mth.clamp(createScroll, 0, createScrollMax);

        gui.enableScissor(createGridViewport.x(), createGridViewport.y(),
                createGridViewport.right(), createGridViewport.bottom());
        for (int index = 0; index < perks.size(); index++) {
            int row = index / columns;
            int column = index % columns;
            int x = createGridViewport.x() + column * (cardWidth + CARD_GAP);
            int y = createGridViewport.y() + row * (PERK_CARD_HEIGHT + CARD_GAP) - createScroll;
            Rect card = new Rect(x, y, cardWidth, PERK_CARD_HEIGHT);
            if (card.bottom() <= createGridViewport.y()
                    || card.y() >= createGridViewport.bottom()) {
                continue;
            }

            Perk perk = perks.get(index);
            boolean hovered = createGridViewport.contains(mouseX, mouseY)
                    && card.contains(mouseX, mouseY);
            if (hovered) {
                hoveredPerkInCreate = perk;
            }
            drawCreatePerkCard(gui, perk, card, hovered);
            perkHits.add(new PerkHit(perk, card));
        }
        gui.disableScissor();
        drawScrollBar(gui, createGridViewport, createScroll, createScrollMax, totalHeight);
    }

    private void drawCreatePerkCard(GuiGraphics gui, Perk perk, Rect card, boolean hovered) {
        boolean selected = selectedPerksForPreset.contains(perk.getId());
        boolean compatible = selected || canAddPerkToSelection(perk);
        int typeColor = perk.getType().getArgbColor();
        int background = selected ? GuideTheme.SURFACE_SELECTED
                : hovered ? GuideTheme.SURFACE_HOVER : GuideTheme.SURFACE;

        gui.fill(card.x(), card.y(), card.right(), card.bottom(), background);
        gui.renderOutline(card.x(), card.y(), card.width(), card.height(),
                selected ? GuideTheme.GREEN : hovered ? typeColor : GuideTheme.BORDER_SOFT);
        gui.fill(card.x(), card.y(), card.x() + (hovered ? 3 : 2), card.bottom(), typeColor);

        int iconSize = 32;
        int iconX = card.x() + 5;
        int iconY = card.y() + 6;
        renderPerkIcon(gui, perk, iconX, iconY, iconSize);
        gui.renderOutline(iconX, iconY, iconSize, iconSize,
                selected ? GuideTheme.GREEN : GuideTheme.BORDER_SOFT);

        String mark = switch (perk.getTeam()) {
            case SURVIVOR -> "В";
            case MANIAC -> "М";
            case ALL -> "";
        };
        if (!mark.isEmpty()) {
            gui.fill(iconX, iconY, iconX + 8, iconY + 9, 0xD910151B);
            gui.drawString(font, mark, iconX + 1, iconY + 1,
                    teamColor(perk.getTeam()), false);
        }

        int textX = iconX + iconSize + 5;
        int textWidth = Math.max(1, card.right() - textX - 4);
        gui.drawString(font, trim(perk.getName().getString(), textWidth),
                textX, card.y() + 6, selected ? GuideTheme.TEXT : GuideTheme.TEXT_SECONDARY, false);
        gui.drawString(font, trim(perk.getType().getDisplayName().getString(), textWidth),
                textX, card.y() + 18, typeColor, false);
        String team = perk.getTeam() == PerkTeam.ALL
                ? "Общий" : perk.getTeam().getDisplayName().getString();
        gui.drawString(font, trim(team, textWidth), textX, card.y() + 30,
                teamColor(perk.getTeam()), false);

        if (selected) {
            int number = selectedPerksForPreset.indexOf(perk.getId()) + 1;
            gui.fill(card.right() - 10, card.y() + 2,
                    card.right() - 2, card.y() + 11, 0xE5193823);
            gui.drawCenteredString(font, Integer.toString(number),
                    card.right() - 6, card.y() + 2, GuideTheme.GREEN);
        } else if (!compatible) {
            gui.fill(card.x() + 1, card.y() + 1, card.right() - 1, card.bottom() - 1,
                    0x9910151B);
            gui.drawString(font, trim("Другая команда", Math.max(1, card.width() - 10)),
                    card.x() + 5, card.bottom() - 11, GuideTheme.RED, false);
        }
    }

    private void drawBuilder(GuiGraphics gui, int mouseX, int mouseY) {
        int x = builderPanel.x() + 8;
        int contentWidth = Math.max(1, builderPanel.width() - 16);
        gui.drawString(font, "НАЗВАНИЕ", x, builderPanel.y() + 23,
                GuideTheme.TEXT_MUTED, false);

        int y = builderPanel.y() + 62;
        gui.drawString(font, "СОСТАВ  " + selectedPerksForPreset.size() + " / " + MAX_PERKS,
                x, y, GuideTheme.TEXT_SECONDARY, false);
        y += 13;
        for (int slot = 0; slot < MAX_PERKS; slot++) {
            Rect bounds = new Rect(x, y + slot * 35, contentWidth, 31);
            drawBuilderSlot(gui, slot, bounds, mouseX, mouseY);
        }

        int previewTop = y + MAX_PERKS * 35 + 4;
        if (previewTop >= builderPanel.bottom() - 12) {
            return;
        }
        gui.fill(x, previewTop, builderPanel.right() - 8, previewTop + 1,
                GuideTheme.BORDER_SOFT);
        previewTop += 7;
        gui.drawString(font, "СВЕДЕНИЯ", x, previewTop, GuideTheme.GOLD, false);
        previewTop += 13;

        Perk perk = hoveredPerkInCreate != null ? hoveredPerkInCreate : focusedPerkInCreate;
        if (perk == null) {
            gui.drawString(font, "Наведите курсор на перк", x, previewTop,
                    GuideTheme.TEXT_MUTED, false);
            return;
        }

        int iconSize = Math.min(32, Math.max(22, contentWidth / 4));
        renderPerkIcon(gui, perk, x, previewTop, iconSize);
        gui.renderOutline(x, previewTop, iconSize, iconSize, perk.getType().getArgbColor());
        int textX = x + iconSize + 6;
        int textWidth = Math.max(1, builderPanel.right() - 8 - textX);
        gui.drawString(font, trim(perk.getName().getString(), textWidth),
                textX, previewTop + 1, GuideTheme.TEXT, false);
        gui.drawString(font, trim(perk.getType().getDisplayName().getString(), textWidth),
                textX, previewTop + 13, perk.getType().getArgbColor(), false);
        gui.drawString(font, trim(perk.getTeam().getDisplayName().getString(), textWidth),
                textX, previewTop + 24, teamColor(perk.getTeam()), false);

        int descriptionY = previewTop + iconSize + 7;
        int bottom = builderPanel.bottom() - 6;
        for (FormattedCharSequence line : font.split(perk.getDescription(), contentWidth)) {
            if (descriptionY + 9 > bottom) {
                break;
            }
            gui.drawString(font, line, x, descriptionY, GuideTheme.TEXT_MUTED, false);
            descriptionY += 10;
        }
    }

    private void drawBuilderSlot(GuiGraphics gui, int slot, Rect bounds, int mouseX, int mouseY) {
        boolean filled = slot < selectedPerksForPreset.size();
        boolean hovered = bounds.contains(mouseX, mouseY);
        gui.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(),
                hovered ? GuideTheme.SURFACE_HOVER : GuideTheme.SURFACE);
        gui.renderOutline(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                filled ? GuideTheme.GREEN : GuideTheme.BORDER_SOFT);

        if (!filled) {
            gui.drawCenteredString(font, "+ Слот " + (slot + 1),
                    bounds.x() + bounds.width() / 2, bounds.y() + 11,
                    GuideTheme.TEXT_MUTED);
            return;
        }

        Perk perk = PerkRegistry.getPerk(selectedPerksForPreset.get(slot));
        if (perk == null) {
            return;
        }
        int iconSize = 25;
        renderPerkIcon(gui, perk, bounds.x() + 3, bounds.y() + 3, iconSize);
        int textX = bounds.x() + 33;
        int textWidth = Math.max(1, bounds.right() - textX - 12);
        gui.drawString(font, trim(perk.getName().getString(), textWidth),
                textX, bounds.y() + 5, GuideTheme.TEXT, false);
        gui.drawString(font, trim(perk.getType().getDisplayName().getString(), textWidth),
                textX, bounds.y() + 17, perk.getType().getArgbColor(), false);
        gui.drawString(font, "×", bounds.right() - 9, bounds.y() + 11,
                hovered ? GuideTheme.RED : GuideTheme.TEXT_MUTED, false);
        selectedHits.add(new SelectedHit(perk, bounds));
    }

    private void drawFooter(GuiGraphics gui, int mouseX, int mouseY) {
        gui.fill(footerPanel.x(), footerPanel.y(), footerPanel.right(), footerPanel.bottom(),
                GuideTheme.PANEL);
        gui.renderOutline(footerPanel.x(), footerPanel.y(), footerPanel.width(), footerPanel.height(),
                GuideTheme.BORDER_SOFT);
        gui.fill(footerPanel.x() + 1, footerPanel.y() + 1,
                footerPanel.right() - 1, footerPanel.y() + 2, GuideTheme.GREEN);

        if (currentMode == Mode.LIST) {
            int current = ClientPlayerData.getPresets().size();
            int maximum = ClientPlayerData.getMaxPresets();
            boolean canCreate = current < maximum;
            String state = canCreate
                    ? "Свободных слотов: " + (maximum - current)
                    : "Все слоты пресетов заняты";
            gui.drawString(font, trim(state, Math.max(1, createButton.x() - footerPanel.x() - 18)),
                    footerPanel.x() + 8, footerPanel.y() + 14,
                    canCreate ? GuideTheme.TEXT_SECONDARY : GuideTheme.GOLD, false);
            drawPrimaryButton(gui, createButton, "+ СОЗДАТЬ",
                    createButton.contains(mouseX, mouseY), canCreate, true);
            return;
        }

        String state = errorMessage != null && errorTicks > 0
                ? errorMessage
                : selectedPerksForPreset.isEmpty()
                ? "Выберите один или два перка"
                : selectedPerksForPreset.size() + " / " + MAX_PERKS + " • готово к сохранению";
        int stateRight = cancelButton.x() - 7;
        gui.drawString(font, trim(state, Math.max(1, stateRight - footerPanel.x() - 15)),
                footerPanel.x() + 8, footerPanel.y() + 14,
                errorMessage != null && errorTicks > 0 ? GuideTheme.RED : GuideTheme.TEXT_SECONDARY,
                false);
        drawButton(gui, cancelButton, "Отмена", GuideTheme.RED,
                cancelButton.contains(mouseX, mouseY), false, true);
        drawPrimaryButton(gui, saveButton, "СОХРАНИТЬ",
                saveButton.contains(mouseX, mouseY), !selectedPerksForPreset.isEmpty());
    }

    private void drawPanel(GuiGraphics gui, Rect panel, String title, int accent) {
        gui.fill(panel.x(), panel.y(), panel.right(), panel.bottom(), GuideTheme.PANEL);
        gui.renderOutline(panel.x(), panel.y(), panel.width(), panel.height(), GuideTheme.BORDER_SOFT);
        gui.fill(panel.x() + 1, panel.y() + 1,
                panel.right() - 1, panel.y() + PANEL_HEADER_HEIGHT - 2, GuideTheme.HEADER);
        gui.drawCenteredString(font, trim(title, Math.max(1, panel.width() - 50)),
                panel.x() + panel.width() / 2, panel.y() + 5, GuideTheme.TEXT_SECONDARY);
        gui.fill(panel.x() + 1, panel.y() + PANEL_HEADER_HEIGHT - 2,
                panel.right() - 1, panel.y() + PANEL_HEADER_HEIGHT - 1, accent);
    }

    private void drawButton(GuiGraphics gui, Rect bounds, String label, int accent,
                            boolean hovered, boolean selected, boolean active) {
        if (!active) {
            gui.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), 0xE5222930);
            gui.renderOutline(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                    GuideTheme.BORDER_SOFT);
            gui.drawCenteredString(font, trim(label, Math.max(1, bounds.width() - 6)),
                    bounds.x() + bounds.width() / 2, bounds.y() + (bounds.height() - 8) / 2,
                    GuideTheme.TEXT_MUTED);
            return;
        }
        GuideTheme.drawButton(gui, font, bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                trim(label, Math.max(1, bounds.width() - 6)), accent, hovered, selected);
    }

    private void drawPrimaryButton(GuiGraphics gui, Rect bounds, String label, boolean hovered) {
        drawPrimaryButton(gui, bounds, label, hovered, true);
    }

    private void drawPrimaryButton(GuiGraphics gui, Rect bounds, String label,
                                   boolean hovered, boolean active) {
        drawPrimaryButton(gui, bounds, label, hovered, active, false);
    }

    private void drawPrimaryButton(GuiGraphics gui, Rect bounds, String label,
                                   boolean hovered, boolean active, boolean lightText) {
        if (!active) {
            drawButton(gui, bounds, label, GuideTheme.GREEN, false, false, false);
            return;
        }
        int background = hovered
                ? GuideTheme.lerpColor(GuideTheme.GREEN, 0xFFFFFFFF, 0.12f)
                : GuideTheme.GREEN;
        int border = hovered ? GuideTheme.TEXT
                : GuideTheme.lerpColor(GuideTheme.GREEN, 0xFFFFFFFF, 0.28f);
        gui.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), background);
        gui.renderOutline(bounds.x(), bounds.y(), bounds.width(), bounds.height(), border);
        gui.drawCenteredString(font, trim(label, Math.max(1, bounds.width() - 6)),
                bounds.x() + bounds.width() / 2, bounds.y() + (bounds.height() - 8) / 2,
                lightText ? GuideTheme.TEXT : 0xFF0D1115);
    }

    private void drawScrollBar(GuiGraphics gui, Rect viewport, int scroll,
                               int maxScroll, int contentHeight) {
        if (maxScroll <= 0 || viewport.height() <= 0 || contentHeight <= 0) {
            return;
        }
        int trackX = viewport.right() - 2;
        int thumbHeight = Math.max(12, viewport.height() * viewport.height() / contentHeight);
        int thumbTravel = Math.max(1, viewport.height() - thumbHeight);
        int thumbY = viewport.y() + Math.round(thumbTravel * (scroll / (float) maxScroll));
        gui.fill(trackX, viewport.y(), trackX + 2, viewport.bottom(), 0x66374450);
        gui.fill(trackX, thumbY, trackX + 2, thumbY + thumbHeight, GuideTheme.GREEN);
    }

    private void renderPerkIcon(GuiGraphics gui, Perk perk, int x, int y, int size) {
        ResourceLocation texture = perk.getIcon();
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.enableBlend();
        gui.blit(texture, x, y, 0, 0, size, size, size, size);
        RenderSystem.disableBlend();
    }

    private int teamColor(PerkTeam team) {
        return switch (team) {
            case SURVIVOR -> GuideTheme.BLUE;
            case MANIAC -> GuideTheme.RED;
            case ALL -> GuideTheme.TEXT_MUTED;
        };
    }

    private boolean canAddPerkToSelection(Perk perk) {
        if (selectedPerksForPreset.isEmpty() || perk.getTeam() == PerkTeam.ALL) {
            return true;
        }
        for (String selectedId : selectedPerksForPreset) {
            Perk selected = PerkRegistry.getPerk(selectedId);
            if (selected == null || selected.getTeam() == PerkTeam.ALL) {
                continue;
            }
            if (selected.getTeam() != perk.getTeam()) {
                return false;
            }
        }
        return true;
    }

    private void startCreateMode() {
        currentMode = Mode.CREATE;
        selectedPerksForPreset.clear();
        createScroll = 0;
        errorMessage = null;
        errorTicks = 0;
        refreshInputWidget();
        if (presetNameField != null) {
            presetNameField.setFocused(true);
        }
    }

    private void cancelCreate() {
        currentMode = Mode.LIST;
        selectedPerksForPreset.clear();
        createScroll = 0;
        errorMessage = null;
        errorTicks = 0;
        refreshInputWidget();
    }

    private void savePreset() {
        if (selectedPerksForPreset.isEmpty()) {
            return;
        }
        String name = presetNameField == null ? "" : presetNameField.getValue().trim();
        if (name.isEmpty()) {
            name = "Пресет " + (ClientPlayerData.getPresets().size() + 1);
        }
        ModNetworking.CHANNEL.sendToServer(new SavePresetPacket(name, selectedPerksForPreset));
        currentMode = Mode.LIST;
        selectedPerksForPreset.clear();
        createScroll = 0;
        errorMessage = null;
        errorTicks = 0;
        refreshInputWidget();
    }

    private void backToSelection() {
        Minecraft.getInstance().setScreen(parent != null ? parent : new PerkSelectionScreen());
    }

    private void showError(String message) {
        errorMessage = message;
        errorTicks = 60;
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.playSound(SoundEvents.VILLAGER_NO, 1.0F, 1.0F);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (closeButton.contains(mouseX, mouseY)) {
            onClose();
            return true;
        }
        if (backButton.contains(mouseX, mouseY)) {
            if (currentMode == Mode.CREATE) {
                cancelCreate();
            } else {
                backToSelection();
            }
            return true;
        }

        if (currentMode == Mode.LIST) {
            boolean canCreate = ClientPlayerData.getPresets().size()
                    < ClientPlayerData.getMaxPresets();
            if (createButton.contains(mouseX, mouseY) && canCreate) {
                startCreateMode();
                return true;
            }
            if (listViewport.contains(mouseX, mouseY)) {
                for (PresetHit hit : presetHits) {
                    if (hit.apply().contains(mouseX, mouseY)) {
                        ModNetworking.CHANNEL.sendToServer(new ApplyPresetPacket(hit.index()));
                        Minecraft.getInstance().setScreen(null);
                        return true;
                    }
                    if (hit.delete().contains(mouseX, mouseY)) {
                        ModNetworking.CHANNEL.sendToServer(new DeletePresetPacket(hit.index()));
                        return true;
                    }
                }
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (cancelButton.contains(mouseX, mouseY)) {
            cancelCreate();
            return true;
        }
        if (saveButton.contains(mouseX, mouseY) && !selectedPerksForPreset.isEmpty()) {
            savePreset();
            return true;
        }
        for (SelectedHit hit : selectedHits) {
            if (hit.bounds().contains(mouseX, mouseY)) {
                selectedPerksForPreset.remove(hit.perk().getId());
                errorMessage = null;
                errorTicks = 0;
                return true;
            }
        }
        if (createGridViewport.contains(mouseX, mouseY)) {
            for (PerkHit hit : perkHits) {
                if (!hit.bounds().contains(mouseX, mouseY)) {
                    continue;
                }
                Perk perk = hit.perk();
                focusedPerkInCreate = perk;
                if (selectedPerksForPreset.remove(perk.getId())) {
                    errorMessage = null;
                    errorTicks = 0;
                    return true;
                }
                if (selectedPerksForPreset.size() >= MAX_PERKS) {
                    showError("В пресете может быть только два перка");
                    return true;
                }
                if (!canAddPerkToSelection(perk)) {
                    showError("Нельзя смешивать перки разных команд");
                    return true;
                }
                selectedPerksForPreset.add(perk.getId());
                errorMessage = null;
                errorTicks = 0;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (currentMode == Mode.LIST && listViewport.contains(mouseX, mouseY)
                && listScrollMax > 0) {
            listScroll = Mth.clamp(listScroll - (int) Math.round(delta * 24.0),
                    0, listScrollMax);
            return true;
        }
        if (currentMode == Mode.CREATE && createGridViewport.contains(mouseX, mouseY)
                && createScrollMax > 0) {
            createScroll = Mth.clamp(createScroll - (int) Math.round(delta * 24.0),
                    0, createScrollMax);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (currentMode == Mode.CREATE) {
                cancelCreate();
            } else {
                backToSelection();
            }
            return true;
        }
        if (currentMode == Mode.CREATE
                && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)
                && !selectedPerksForPreset.isEmpty()) {
            savePreset();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        super.tick();
        if (presetNameField != null) {
            presetNameField.tick();
        }
        if (errorTicks > 0 && --errorTicks == 0) {
            errorMessage = null;
        }
    }

    private String trim(String text, int maxWidth) {
        if (maxWidth <= 6 || font.width(text) <= maxWidth) {
            return text;
        }
        return font.plainSubstrByWidth(text,
                Math.max(1, maxWidth - font.width("…"))) + "…";
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private enum Mode {
        LIST,
        CREATE
    }

    private record Rect(int x, int y, int width, int height) {
        private static final Rect EMPTY = new Rect(0, 0, 0, 0);

        private int right() {
            return x + width;
        }

        private int bottom() {
            return y + height;
        }

        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < right() && mouseY >= y && mouseY < bottom();
        }
    }

    private record PresetHit(int index, Rect apply, Rect delete) {
    }

    private record PerkHit(Perk perk, Rect bounds) {
    }

    private record SelectedHit(Perk perk, Rect bounds) {
    }
}
