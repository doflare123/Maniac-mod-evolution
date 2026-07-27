package org.example.maniacrevolution.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.example.maniacrevolution.data.ClientPlayerData;
import org.example.maniacrevolution.keybind.ModKeybinds;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.SelectPerkPacket;
import org.example.maniacrevolution.perk.Perk;
import org.example.maniacrevolution.perk.PerkPhase;
import org.example.maniacrevolution.perk.PerkRegistry;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.perk.PerkType;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Responsive perk loadout screen built from the same graphite panels and status accents as the
 * character selection screen and the custom HUD.
 */
public class PerkSelectionScreen extends Screen {
    private static final int GAP = 6;
    private static final int HEADER_HEIGHT = 28;
    private static final int FOOTER_HEIGHT = 65;
    private static final int PANEL_HEADER_HEIGHT = 18;
    private static final int CARD_GAP = 4;
    private static final int CARD_HEIGHT = 44;
    private static final int MAX_SELECTED_PERKS = 2;

    private final List<Perk> availablePerks = new ArrayList<>();
    private final List<String> selectedPerkIds = new ArrayList<>(MAX_SELECTED_PERKS);
    private final List<CardHit> cardHits = new ArrayList<>();
    private final List<SelectedHit> selectedHits = new ArrayList<>();

    private Perk focusedPerk;
    private Perk hoveredPerk;
    private String selectionMessage = "";

    private int pageX;
    private int pageY;
    private int pageWidth;
    private int pageHeight;
    private Rect collectionPanel = Rect.EMPTY;
    private Rect detailsPanel = Rect.EMPTY;
    private Rect footerPanel = Rect.EMPTY;
    private Rect gridViewport = Rect.EMPTY;
    private Rect detailViewport = Rect.EMPTY;
    private Rect closeButton = Rect.EMPTY;
    private Rect clearButton = Rect.EMPTY;
    private Rect presetsButton = Rect.EMPTY;
    private Rect confirmButton = Rect.EMPTY;

    private int gridScroll;
    private int gridScrollMax;
    private int detailScroll;
    private int detailScrollMax;
    private int detailContentHeight;

    public PerkSelectionScreen() {
        super(Component.literal("Выбор перков"));
        for (ClientPlayerData.ClientPerkData perkData : ClientPlayerData.getSelectedPerks()) {
            if (selectedPerkIds.size() >= MAX_SELECTED_PERKS) {
                break;
            }
            if (PerkRegistry.getPerk(perkData.id()) != null) {
                selectedPerkIds.add(perkData.id());
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        calculateLayout();
        loadAvailablePerks();
    }

    private void calculateLayout() {
        int maxWidth = Math.max(220, Math.min(760, width - 12));
        int maxHeight = Math.max(145, Math.min(430, height - 12));
        int minWidth = Math.min(380, maxWidth);
        int minHeight = Math.min(230, maxHeight);

        pageWidth = Mth.clamp(Math.round(width * 0.76f), minWidth, maxWidth);
        pageHeight = Mth.clamp(Math.round(height * 0.82f), minHeight, maxHeight);
        pageX = (width - pageWidth) / 2;
        pageY = (height - pageHeight) / 2;

        int mainY = pageY + HEADER_HEIGHT;
        int mainHeight = Math.max(45, pageHeight - HEADER_HEIGHT - FOOTER_HEIGHT - GAP);
        int availableWidth = pageWidth - GAP;
        int detailsWidth = Mth.clamp(availableWidth * 38 / 100, Math.min(112, availableWidth), 280);
        int collectionWidth = Math.max(102, availableWidth - detailsWidth);
        if (collectionWidth + detailsWidth > availableWidth) {
            detailsWidth = Math.max(1, availableWidth - collectionWidth);
        }

        collectionPanel = new Rect(pageX, mainY, collectionWidth, mainHeight);
        detailsPanel = new Rect(collectionPanel.right() + GAP, mainY, detailsWidth, mainHeight);
        footerPanel = new Rect(pageX, mainY + mainHeight + GAP, pageWidth, FOOTER_HEIGHT);

        gridViewport = new Rect(collectionPanel.x() + 6,
                collectionPanel.y() + PANEL_HEADER_HEIGHT + 6,
                Math.max(1, collectionPanel.width() - 12),
                Math.max(1, collectionPanel.height() - PANEL_HEADER_HEIGHT - 11));
        detailViewport = new Rect(detailsPanel.x() + 7,
                detailsPanel.y() + PANEL_HEADER_HEIGHT + 5,
                Math.max(1, detailsPanel.width() - 14),
                Math.max(1, detailsPanel.height() - PANEL_HEADER_HEIGHT - 10));

        closeButton = new Rect(pageX + pageWidth - 23, pageY + 5, 18, 18);

        int actionGap = 4;
        int actionX = footerPanel.x() + 6;
        int actionWidth = Math.max(1, footerPanel.width() - 12);
        int clearWidth = Mth.clamp(actionWidth * 25 / 100, Math.min(42, actionWidth), 70);
        int presetsWidth = Mth.clamp(actionWidth * 30 / 100,
                Math.min(54, Math.max(1, actionWidth - clearWidth)), 86);
        int confirmWidth = Math.max(1, actionWidth - clearWidth - presetsWidth - actionGap * 2);
        int actionY = footerPanel.bottom() - 26;

        clearButton = new Rect(actionX, actionY, clearWidth, 21);
        presetsButton = new Rect(clearButton.right() + actionGap, actionY, presetsWidth, 21);
        confirmButton = new Rect(presetsButton.right() + actionGap, actionY, confirmWidth, 21);
    }

    private void loadAvailablePerks() {
        availablePerks.clear();
        PerkTeam playerTeam = getPlayerTeam();
        for (Perk perk : PerkRegistry.getAllPerks()) {
            if (playerTeam == null || perk.isAvailableForTeam(playerTeam)) {
                availablePerks.add(perk);
            }
        }

        selectedPerkIds.removeIf(id -> availablePerks.stream()
                .noneMatch(perk -> perk.getId().equals(id)));
        if (focusedPerk == null || !availablePerks.contains(focusedPerk)) {
            focusedPerk = availablePerks.isEmpty() ? null : availablePerks.get(0);
        }
        gridScroll = Mth.clamp(gridScroll, 0, gridScrollMax);
    }

    private PerkTeam getPlayerTeam() {
        if (minecraft == null || minecraft.player == null || minecraft.player.getTeam() == null) {
            return null;
        }
        String name = minecraft.player.getTeam().getName();
        if ("maniac".equals(name)) {
            return PerkTeam.MANIAC;
        }
        if ("survivors".equals(name)) {
            return PerkTeam.SURVIVOR;
        }
        return null;
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);
        drawBackdrop(gui);

        hoveredPerk = null;
        cardHits.clear();
        selectedHits.clear();

        drawHeader(gui, mouseX, mouseY);
        drawCollectionPanel(gui, mouseX, mouseY);
        drawDetailsPanel(gui);
        drawFooter(gui, mouseX, mouseY);
    }

    private void drawBackdrop(GuiGraphics gui) {
        PerkTeam team = getPlayerTeam();
        int top = team == PerkTeam.MANIAC ? 0xE5211013
                : team == PerkTeam.SURVIVOR ? 0xE5101B23
                : 0xE5141B1A;
        int bottom = team == PerkTeam.MANIAC ? 0xF00D0708
                : team == PerkTeam.SURVIVOR ? 0xF0060B10
                : 0xF0070C0B;
        gui.fillGradient(0, 0, width, height, top, bottom);
        gui.fill(pageX, pageY, pageX + pageWidth, pageY + pageHeight, 0x78101720);
        gui.renderOutline(pageX, pageY, pageWidth, pageHeight, GuideTheme.BORDER_SOFT);
    }

    private void drawHeader(GuiGraphics gui, int mouseX, int mouseY) {
        String title = trim("ВЫБОР ПЕРКОВ", Math.max(30, pageWidth - 150));
        gui.drawCenteredString(font, title, pageX + pageWidth / 2, pageY + 7, GuideTheme.TEXT);
        int underlineWidth = Math.min(92, Math.max(42, font.width(title) / 2));
        gui.fill(pageX + pageWidth / 2 - underlineWidth / 2, pageY + 19,
                pageX + pageWidth / 2 + underlineWidth / 2, pageY + 20, GuideTheme.GREEN);

        if (pageWidth >= 330) {
            PerkTeam team = getPlayerTeam();
            String teamText = team == null ? "ВСЕ КОМАНДЫ" : team.getDisplayName().getString().toUpperCase();
            String context = teamText + "  •  " + availablePerks.size();
            gui.drawString(font, trim(context, Math.max(40, pageWidth / 3)),
                    pageX + 6, pageY + 7, teamColor(team), false);
        }

        drawButton(gui, closeButton, "×", GuideTheme.RED,
                closeButton.contains(mouseX, mouseY), false, true);
    }

    private void drawCollectionPanel(GuiGraphics gui, int mouseX, int mouseY) {
        drawPanel(gui, collectionPanel, "ДОСТУПНЫЕ ПЕРКИ", GuideTheme.GREEN);
        String count = availablePerks.size() + " шт.";
        gui.drawString(font, count, collectionPanel.right() - font.width(count) - 6,
                collectionPanel.y() + 5, GuideTheme.TEXT_MUTED, false);

        int columns = Mth.clamp((gridViewport.width() + CARD_GAP) / (132 + CARD_GAP), 1, 3);
        int cardWidth = Math.max(1,
                (gridViewport.width() - CARD_GAP * (columns - 1)) / columns);
        int rows = (availablePerks.size() + columns - 1) / columns;
        int totalHeight = rows * CARD_HEIGHT + Math.max(0, rows - 1) * CARD_GAP;
        gridScrollMax = Math.max(0, totalHeight - gridViewport.height());
        gridScroll = Mth.clamp(gridScroll, 0, gridScrollMax);

        gui.enableScissor(gridViewport.x(), gridViewport.y(),
                gridViewport.right(), gridViewport.bottom());
        for (int index = 0; index < availablePerks.size(); index++) {
            int row = index / columns;
            int column = index % columns;
            int x = gridViewport.x() + column * (cardWidth + CARD_GAP);
            int y = gridViewport.y() + row * (CARD_HEIGHT + CARD_GAP) - gridScroll;
            Rect card = new Rect(x, y, cardWidth, CARD_HEIGHT);
            if (card.bottom() <= gridViewport.y() || card.y() >= gridViewport.bottom()) {
                continue;
            }

            Perk perk = availablePerks.get(index);
            boolean hovered = gridViewport.contains(mouseX, mouseY) && card.contains(mouseX, mouseY);
            if (hovered) {
                hoveredPerk = perk;
            }
            drawPerkCard(gui, perk, card, hovered);
            cardHits.add(new CardHit(perk, card));
        }
        gui.disableScissor();

        drawScrollBar(gui, gridViewport, gridScroll, gridScrollMax, totalHeight, GuideTheme.GREEN);
    }

    private void drawPerkCard(GuiGraphics gui, Perk perk, Rect card, boolean hovered) {
        boolean selected = selectedPerkIds.contains(perk.getId());
        int typeAccent = typeColor(perk.getType());
        int background = selected ? GuideTheme.SURFACE_SELECTED
                : hovered ? GuideTheme.SURFACE_HOVER
                : GuideTheme.SURFACE;
        int border = selected ? GuideTheme.GREEN
                : hovered ? typeAccent
                : GuideTheme.BORDER_SOFT;

        gui.fill(card.x(), card.y(), card.right(), card.bottom(), background);
        gui.renderOutline(card.x(), card.y(), card.width(), card.height(), border);
        gui.fill(card.x(), card.y(), card.x() + (hovered ? 3 : 2), card.bottom(), typeAccent);
        if (selected) {
            gui.fill(card.x() + 1, card.bottom() - 2, card.right() - 1, card.bottom() - 1,
                    GuideTheme.GREEN);
        }

        int iconSize = Math.min(34, Math.max(20, card.height() - 10));
        int iconX = card.x() + 5;
        int iconY = card.y() + (card.height() - iconSize) / 2;
        gui.fill(iconX - 1, iconY - 1, iconX + iconSize + 1, iconY + iconSize + 1, 0xFF10161C);
        gui.renderOutline(iconX - 1, iconY - 1, iconSize + 2, iconSize + 2,
                selected ? GuideTheme.GREEN : GuideTheme.BORDER_SOFT);
        renderPerkIcon(gui, perk, iconX, iconY, iconSize);

        String teamMark = switch (perk.getTeam()) {
            case SURVIVOR -> "В";
            case MANIAC -> "М";
            case ALL -> "";
        };
        if (!teamMark.isEmpty()) {
            gui.fill(iconX, iconY, iconX + 8, iconY + 9, 0xD910151B);
            gui.drawString(font, teamMark, iconX + 1, iconY + 1,
                    teamColor(perk.getTeam()), false);
        }

        int textX = iconX + iconSize + 5;
        int textWidth = Math.max(1, card.right() - textX - 4);
        gui.drawString(font, trim(perk.getName().getString(), textWidth),
                textX, card.y() + 5, selected ? GuideTheme.TEXT : GuideTheme.TEXT_SECONDARY, false);
        gui.drawString(font, trim(perk.getType().getDisplayName().getString(), textWidth),
                textX, card.y() + 17, typeAccent, false);

        String resource = compactManaText(perk);
        if (!resource.isEmpty()) {
            drawSmallMutedText(gui, resource, textX, card.y() + 29,
                    textWidth, GuideTheme.BLUE);
        } else {
            gui.drawString(font, perk.getTeam() == PerkTeam.ALL ? "Общий" : "Командный",
                    textX, card.y() + 29, GuideTheme.TEXT_MUTED, false);
        }

        if (selected) {
            int slot = selectedPerkIds.indexOf(perk.getId()) + 1;
            String slotText = Integer.toString(slot);
            int chipX = card.right() - 10;
            gui.fill(chipX, card.y() + 2, card.right() - 2, card.y() + 11, 0xE5193823);
            gui.drawCenteredString(font, slotText, chipX + 4, card.y() + 2, GuideTheme.GREEN);
        }
    }

    private void drawDetailsPanel(GuiGraphics gui) {
        drawPanel(gui, detailsPanel, "СВОДКА", GuideTheme.GREEN);
        Perk perk = hoveredPerk != null ? hoveredPerk : focusedPerk;
        if (perk == null) {
            gui.drawCenteredString(font, "Нет доступных перков",
                    detailsPanel.x() + detailsPanel.width() / 2,
                    detailsPanel.y() + detailsPanel.height() / 2 - 5,
                    GuideTheme.TEXT_MUTED);
            detailContentHeight = 0;
            detailScrollMax = 0;
            return;
        }

        gui.enableScissor(detailViewport.x(), detailViewport.y(),
                detailViewport.right(), detailViewport.bottom());
        int contentTop = detailViewport.y() - detailScroll;
        int y = contentTop;
        int iconSize = Math.min(42, Math.max(28, detailViewport.width() / 4));
        renderPerkIcon(gui, perk, detailViewport.x(), y, iconSize);
        gui.renderOutline(detailViewport.x(), y, iconSize, iconSize,
                selectedPerkIds.contains(perk.getId()) ? GuideTheme.GREEN : typeColor(perk.getType()));

        int titleX = detailViewport.x() + iconSize + 7;
        int titleWidth = Math.max(1, detailViewport.right() - titleX);
        List<FormattedCharSequence> titleLines = font.split(perk.getName(), titleWidth);
        int titleY = y + 1;
        for (int index = 0; index < Math.min(2, titleLines.size()); index++) {
            gui.drawString(font, titleLines.get(index), titleX, titleY,
                    GuideTheme.TEXT, false);
            titleY += 10;
        }

        String state = selectedPerkIds.contains(perk.getId())
                ? "ВЫБРАН • СЛОТ " + (selectedPerkIds.indexOf(perk.getId()) + 1)
                : selectedPerkIds.size() >= MAX_SELECTED_PERKS ? "НАБОР ЗАПОЛНЕН" : "ДОСТУПЕН";
        int stateColor = selectedPerkIds.contains(perk.getId()) ? GuideTheme.GREEN
                : selectedPerkIds.size() >= MAX_SELECTED_PERKS ? GuideTheme.TEXT_MUTED
                : typeColor(perk.getType());
        gui.drawString(font, trim(state, titleWidth), titleX, y + iconSize - 9, stateColor, false);

        y += iconSize + 8;
        gui.fill(detailViewport.x(), y, detailViewport.right(), y + 1, GuideTheme.BORDER_SOFT);
        y += 7;

        y = drawStat(gui, "Тип", perk.getType().getDisplayName().getString(),
                typeColor(perk.getType()), y);
        y = drawStat(gui, "Команда", perk.getTeam().getDisplayName().getString(),
                teamColor(perk.getTeam()), y);
        y = drawStat(gui, "Фазы", phasesText(perk), GuideTheme.GOLD, y);
        y = drawStat(gui, "Откат", perk.getCooldownTicks() > 0
                        ? (perk.getCooldownTicks() / 20) + " сек." : "нет",
                perk.getCooldownTicks() > 0 ? GuideTheme.RED : GuideTheme.TEXT_MUTED, y);
        y = drawStat(gui, "Мана", perk.getManaCost() > 0
                        ? Integer.toString((int) perk.getManaCost()) : "не нужна",
                perk.getManaCost() > 0 ? GuideTheme.BLUE : GuideTheme.TEXT_MUTED, y);

        y += 3;
        gui.fill(detailViewport.x(), y, detailViewport.right(), y + 1, GuideTheme.BORDER_SOFT);
        y += 7;
        gui.drawString(font, "ОПИСАНИЕ", detailViewport.x(), y, GuideTheme.GOLD, false);
        y += 12;

        List<FormattedCharSequence> description = font.split(
                perk.getDescription(), Math.max(1, detailViewport.width()));
        for (FormattedCharSequence line : description) {
            gui.drawString(font, line, detailViewport.x(), y, GuideTheme.TEXT_SECONDARY, false);
            y += 10;
        }

        y += 5;
        int hintColor = typeColor(perk.getType());
        gui.fill(detailViewport.x(), y, detailViewport.right(), y + 1, hintColor);
        y += 6;
        for (String hint : usageHint(perk.getType())) {
            for (FormattedCharSequence line : font.split(Component.literal(hint),
                    Math.max(1, detailViewport.width()))) {
                gui.drawString(font, line, detailViewport.x(), y,
                        hint.startsWith("Управление") ? hintColor : GuideTheme.TEXT_MUTED, false);
                y += 10;
            }
        }

        detailContentHeight = Math.max(0, y - contentTop);
        detailScrollMax = Math.max(0, detailContentHeight - detailViewport.height());
        detailScroll = Mth.clamp(detailScroll, 0, detailScrollMax);
        gui.disableScissor();

        drawScrollBar(gui, detailViewport, detailScroll, detailScrollMax,
                detailContentHeight, GuideTheme.GREEN);
    }

    private int drawStat(GuiGraphics gui, String label, String value, int valueColor, int y) {
        gui.drawString(font, label, detailViewport.x(), y, GuideTheme.TEXT_MUTED, false);
        int valueX = detailViewport.x() + Math.min(46, Math.max(30, detailViewport.width() / 3));
        int valueWidth = Math.max(1, detailViewport.right() - valueX);
        gui.drawString(font, trim(value, valueWidth), valueX, y, valueColor, false);
        return y + 12;
    }

    private void drawFooter(GuiGraphics gui, int mouseX, int mouseY) {
        gui.fill(footerPanel.x(), footerPanel.y(), footerPanel.right(), footerPanel.bottom(),
                GuideTheme.PANEL);
        gui.renderOutline(footerPanel.x(), footerPanel.y(), footerPanel.width(), footerPanel.height(),
                GuideTheme.BORDER_SOFT);
        gui.fill(footerPanel.x() + 1, footerPanel.y() + 1,
                footerPanel.right() - 1, footerPanel.y() + 2, GuideTheme.GREEN);

        int labelWidth = Math.min(88, Math.max(52, footerPanel.width() / 4));
        String loadoutTitle = "НАБОР " + selectedPerkIds.size() + " / " + MAX_SELECTED_PERKS;
        gui.drawString(font, trim(loadoutTitle, Math.max(1, labelWidth - 10)),
                footerPanel.x() + 7, footerPanel.y() + 7,
                GuideTheme.TEXT, false);
        String status = selectionMessage.isEmpty()
                ? switch (selectedPerkIds.size()) {
                    case 0 -> "Выберите перки";
                    case 1 -> "Нужен ещё один";
                    default -> "Набор готов";
                }
                : selectionMessage;
        gui.drawString(font, trim(status, Math.max(1, labelWidth - 10)),
                footerPanel.x() + 7, footerPanel.y() + 19,
                selectedPerkIds.size() == MAX_SELECTED_PERKS ? GuideTheme.GREEN : GuideTheme.GOLD,
                false);

        int slotsX = footerPanel.x() + labelWidth;
        int slotsWidth = Math.max(1, footerPanel.right() - 6 - slotsX);
        int slotGap = 4;
        int slotWidth = Math.max(1, (slotsWidth - slotGap) / 2);
        for (int slot = 0; slot < MAX_SELECTED_PERKS; slot++) {
            Rect bounds = new Rect(slotsX + slot * (slotWidth + slotGap),
                    footerPanel.y() + 5, slotWidth, 27);
            drawSelectedSlot(gui, slot, bounds, mouseX, mouseY);
        }

        drawButton(gui, clearButton, "Сбросить", GuideTheme.RED,
                clearButton.contains(mouseX, mouseY), false, !selectedPerkIds.isEmpty());
        drawButton(gui, presetsButton, "Пресеты", GuideTheme.BLUE,
                presetsButton.contains(mouseX, mouseY), false, true);
        drawPrimaryButton(gui, confirmButton, "ГОТОВО",
                confirmButton.contains(mouseX, mouseY));

    }

    private void drawSelectedSlot(GuiGraphics gui, int slot, Rect bounds, int mouseX, int mouseY) {
        boolean filled = slot < selectedPerkIds.size();
        boolean hovered = bounds.contains(mouseX, mouseY);
        int background = hovered ? GuideTheme.SURFACE_HOVER : GuideTheme.SURFACE;
        int border = filled ? GuideTheme.GREEN : GuideTheme.BORDER_SOFT;
        gui.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), background);
        gui.renderOutline(bounds.x(), bounds.y(), bounds.width(), bounds.height(), border);

        if (!filled) {
            String empty = "+ Слот " + (slot + 1);
            gui.drawCenteredString(font, trim(empty, Math.max(1, bounds.width() - 6)),
                    bounds.x() + bounds.width() / 2, bounds.y() + 10, GuideTheme.TEXT_MUTED);
            return;
        }

        Perk perk = PerkRegistry.getPerk(selectedPerkIds.get(slot));
        if (perk == null) {
            return;
        }
        int iconSize = 21;
        renderPerkIcon(gui, perk, bounds.x() + 3, bounds.y() + 3, iconSize);
        int textX = bounds.x() + 28;
        int textWidth = Math.max(1, bounds.right() - textX - 12);
        gui.drawString(font, trim(perk.getName().getString(), textWidth),
                textX, bounds.y() + 5, GuideTheme.TEXT, false);
        gui.drawString(font, "Слот " + (slot + 1), textX, bounds.y() + 16,
                GuideTheme.TEXT_MUTED, false);
        gui.drawString(font, "×", bounds.right() - 9, bounds.y() + 9,
                hovered ? GuideTheme.RED : GuideTheme.TEXT_MUTED, false);
        selectedHits.add(new SelectedHit(perk, bounds));
    }

    private void drawPanel(GuiGraphics gui, Rect panel, String title, int accent) {
        gui.fill(panel.x(), panel.y(), panel.right(), panel.bottom(), GuideTheme.PANEL);
        gui.renderOutline(panel.x(), panel.y(), panel.width(), panel.height(), GuideTheme.BORDER_SOFT);
        gui.fill(panel.x() + 1, panel.y() + 1, panel.right() - 1,
                panel.y() + PANEL_HEADER_HEIGHT - 2, GuideTheme.HEADER);
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
        int background = hovered
                ? GuideTheme.lerpColor(GuideTheme.GREEN, 0xFFFFFFFF, 0.12f)
                : GuideTheme.GREEN;
        int border = hovered ? GuideTheme.TEXT
                : GuideTheme.lerpColor(GuideTheme.GREEN, 0xFFFFFFFF, 0.28f);
        gui.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), background);
        gui.renderOutline(bounds.x(), bounds.y(), bounds.width(), bounds.height(), border);
        gui.drawCenteredString(font, trim(label, Math.max(1, bounds.width() - 6)),
                bounds.x() + bounds.width() / 2, bounds.y() + (bounds.height() - 8) / 2,
                GuideTheme.TEXT);
        if (hovered) {
            gui.fill(bounds.x() + 1, bounds.bottom() - 2,
                    bounds.right() - 1, bounds.bottom() - 1, GuideTheme.TEXT);
        }
    }

    private void drawScrollBar(GuiGraphics gui, Rect viewport, int scroll, int maxScroll,
                               int contentHeight, int accent) {
        if (maxScroll <= 0 || viewport.height() <= 0 || contentHeight <= 0) {
            return;
        }
        int trackX = viewport.right() - 2;
        int thumbHeight = Math.max(12, viewport.height() * viewport.height() / contentHeight);
        int thumbTravel = Math.max(1, viewport.height() - thumbHeight);
        int thumbY = viewport.y() + Math.round(thumbTravel * (scroll / (float) maxScroll));
        gui.fill(trackX, viewport.y(), trackX + 2, viewport.bottom(), 0x66374450);
        gui.fill(trackX, thumbY, trackX + 2, thumbY + thumbHeight, accent);
    }

    private void renderPerkIcon(GuiGraphics gui, Perk perk, int x, int y, int size) {
        ResourceLocation texture = perk.getIcon();
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.enableBlend();
        gui.blit(texture, x, y, 0, 0, size, size, size, size);
        RenderSystem.disableBlend();
    }

    private String compactManaText(Perk perk) {
        if (perk.getManaCost() > 0) {
            return "Мана " + (int) perk.getManaCost();
        }
        return "";
    }

    private String phasesText(Perk perk) {
        if (perk.getActivePhases().contains(PerkPhase.ANY)) {
            return "Любая";
        }
        StringJoiner phases = new StringJoiner(", ");
        for (PerkPhase phase : perk.getActivePhases()) {
            phases.add(phase.getDisplayName().getString());
        }
        return phases.toString();
    }

    private List<String> usageHint(PerkType type) {
        String key = ModKeybinds.ACTIVATE_PERK.getTranslatedKeyMessage().getString();
        return switch (type) {
            case PASSIVE -> List.of("Управление: автоматически", "Эффект не требует активации.");
            case PASSIVE_COOLDOWN -> List.of("Управление: автоматически",
                    "После срабатывания уходит на откат.");
            case ACTIVE -> List.of("Управление: клавиша [" + key + "]",
                    "Срабатывает по команде игрока.");
            case HYBRID -> List.of("Управление: пассивно + [" + key + "]",
                    "Совмещает постоянный и активный эффекты.");
        };
    }

    private int typeColor(PerkType type) {
        return type.getArgbColor();
    }

    private int teamColor(PerkTeam team) {
        if (team == null || team == PerkTeam.ALL) {
            return GuideTheme.TEXT_SECONDARY;
        }
        return team == PerkTeam.SURVIVOR ? GuideTheme.BLUE : GuideTheme.RED;
    }

    private String trim(String text, int maxWidth) {
        if (maxWidth <= 6 || font.width(text) <= maxWidth) {
            return text;
        }
        return font.plainSubstrByWidth(text,
                Math.max(1, maxWidth - font.width("…"))) + "…";
    }

    private void drawSmallMutedText(GuiGraphics gui, String text, int x, int y,
                                    int maxWidth, int sourceColor) {
        float scale = 1.00f;
        int unscaledWidth = Math.max(1, Math.round(maxWidth / scale));
        String clipped = trim(text, unscaledWidth);
        int mutedColor = GuideTheme.lerpColor(sourceColor, GuideTheme.TEXT_MUTED, 0.62f);

        gui.pose().pushPose();
        gui.pose().scale(scale, scale, 1.0f);
        gui.drawString(font, clipped, Math.round(x / scale), Math.round(y / scale),
                mutedColor, false);
        gui.pose().popPose();
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
        if (clearButton.contains(mouseX, mouseY) && !selectedPerkIds.isEmpty()) {
            clearSelection();
            return true;
        }
        if (presetsButton.contains(mouseX, mouseY)) {
            Minecraft.getInstance().setScreen(new PresetScreen(this));
            return true;
        }
        if (confirmButton.contains(mouseX, mouseY)) {
            confirm();
            return true;
        }
        for (SelectedHit hit : selectedHits) {
            if (hit.bounds().contains(mouseX, mouseY)) {
                focusedPerk = hit.perk();
                togglePerkSelection(hit.perk());
                return true;
            }
        }
        if (gridViewport.contains(mouseX, mouseY)) {
            for (CardHit hit : cardHits) {
                if (hit.bounds().contains(mouseX, mouseY)) {
                    focusedPerk = hit.perk();
                    detailScroll = 0;
                    togglePerkSelection(hit.perk());
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void togglePerkSelection(Perk perk) {
        if (selectedPerkIds.remove(perk.getId())) {
            selectionMessage = selectedPerkIds.isEmpty() ? "Набор пуст" : "Нужен ещё один";
            return;
        }
        if (selectedPerkIds.size() >= MAX_SELECTED_PERKS) {
            selectionMessage = "Слоты заняты";
            return;
        }
        selectedPerkIds.add(perk.getId());
        selectionMessage = selectedPerkIds.size() == MAX_SELECTED_PERKS
                ? "Набор готов" : "Нужен ещё один";
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (gridViewport.contains(mouseX, mouseY) && gridScrollMax > 0) {
            gridScroll = Mth.clamp(gridScroll - (int) Math.round(delta * 24.0),
                    0, gridScrollMax);
            return true;
        }
        if (detailViewport.contains(mouseX, mouseY) && detailScrollMax > 0) {
            detailScroll = Mth.clamp(detailScroll - (int) Math.round(delta * 18.0),
                    0, detailScrollMax);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void clearSelection() {
        selectedPerkIds.clear();
        selectionMessage = "Набор очищен";
    }

    private void confirm() {
        ModNetworking.CHANNEL.sendToServer(new SelectPerkPacket(selectedPerkIds));
        onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
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

    private record CardHit(Perk perk, Rect bounds) {
    }

    private record SelectedHit(Perk perk, Rect bounds) {
    }
}
