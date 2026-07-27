package org.example.maniacrevolution.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.example.maniacrevolution.character.CharacterClass;
import org.example.maniacrevolution.character.CharacterRegistry;
import org.example.maniacrevolution.character.CharacterType;
import org.example.maniacrevolution.character.TagRegistry;
import org.example.maniacrevolution.client.CharacterSelectionPreferences;
import org.example.maniacrevolution.data.ClientKeeperFormData;
import org.example.maniacrevolution.data.ClientPlayerData;
import org.example.maniacrevolution.gui.GuideTheme;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.SelectCharacterPacket;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CharacterSelectionScreen extends Screen {
    private static final int GAP = 6;
    private static final int CARD_GAP = 4;
    private static final int ITEM_CELL_HEIGHT = 29;
    private static final int SLOT_SIZE = 22;
    private static final UUID FALLBACK_PROFILE = new UUID(0L, 0L);

    private static final Map<String, FrescoDimensions> FRESCO_DIMENSIONS = Map.ofEntries(
            Map.entry("agent", new FrescoDimensions(442, 969)),
            Map.entry("alchemist", new FrescoDimensions(390, 913)),
            Map.entry("death", new FrescoDimensions(441, 968)),
            Map.entry("doctor", new FrescoDimensions(440, 968)),
            Map.entry("dodepovich", new FrescoDimensions(887, 1774)),
            Map.entry("freddy_bear", new FrescoDimensions(452, 904)),
            Map.entry("ghost", new FrescoDimensions(377, 950)),
            Map.entry("keeper_of_nightmares", new FrescoDimensions(941, 1672)),
            Map.entry("mefedronshchik", new FrescoDimensions(442, 972)),
            Map.entry("necromancer", new FrescoDimensions(436, 910)),
            Map.entry("plague_doctor", new FrescoDimensions(396, 915)),
            Map.entry("pudge", new FrescoDimensions(433, 910)),
            Map.entry("scientist", new FrescoDimensions(445, 906)),
            Map.entry("shaman", new FrescoDimensions(481, 1126)),
            Map.entry("ursa", new FrescoDimensions(463, 1126))
    );

    private final CharacterType type;
    private final List<CharacterClass> allCharacters;
    private final Set<String> activeFilters = new LinkedHashSet<>();
    private final List<CardHit> cardHits = new ArrayList<>();
    private final List<FilterHit> filterHits = new ArrayList<>();

    private List<CharacterClass> filteredCharacters = List.of();
    private List<CharacterLoadoutPreview.Entry> previewEntries = List.of();
    private int selectedIndex;
    private boolean largeCards;
    private boolean filtersOpen;

    private int pageX;
    private int pageY;
    private int pageWidth;
    private int pageHeight;
    private Rect listPanel = Rect.EMPTY;
    private Rect previewPanel = Rect.EMPTY;
    private Rect detailsPanel = Rect.EMPTY;
    private Rect cardViewport = Rect.EMPTY;
    private Rect filterButtonRect = Rect.EMPTY;
    private Rect cardModeToggleRect = Rect.EMPTY;
    private Rect largePreviousRect = Rect.EMPTY;
    private Rect largeNextRect = Rect.EMPTY;
    private Rect detailViewport = Rect.EMPTY;
    private Rect itemViewport = Rect.EMPTY;
    private Rect filterPanel = Rect.EMPTY;
    private Rect filterCloseRect = Rect.EMPTY;
    private Rect filterClearRect = Rect.EMPTY;

    private int cardScroll;
    private int cardScrollMax;
    private int detailScroll;
    private int detailScrollMax;
    private int itemScroll;
    private int itemScrollMax;
    private int filterScroll;
    private int filterScrollMax;

    private Button selectButton;
    private RemotePlayer previewPlayer;
    private CharacterLoadoutPreview.Entry hoveredEntry;
    private String hoveredFilterTag;

    public CharacterSelectionScreen(CharacterType type) {
        super(Component.literal("Выбор персонажа"));
        this.type = type;
        this.allCharacters = List.copyOf(CharacterRegistry.getClassesByType(type));
        applyFilters(null);
    }

    @Override
    protected void init() {
        super.init();
        calculateLayout();

        String selectedId = selectedCharacter() == null ? null : selectedCharacter().getId();
        largeCards = CharacterSelectionPreferences.useLargeCards(profileId());
        applyFilters(selectedId);

        selectButton = Button.builder(
                        Component.literal("ВЫБРАТЬ ПЕРСОНАЖА"),
                        button -> selectCharacter()
                )
                .bounds(detailsPanel.x() + 6, detailsPanel.bottom() - 24,
                        Math.max(60, detailsPanel.width() - 12), 20)
                .build();
        selectButton.active = !filteredCharacters.isEmpty();
        addRenderableWidget(selectButton);
    }

    private void calculateLayout() {
        int maxWidth = Math.max(240, this.width - 8);
        int maxHeight = Math.max(150, this.height - 8);
        int minWidth = Math.min(360, maxWidth);
        int minHeight = Math.min(210, maxHeight);
        pageWidth = Mth.clamp(Math.round(this.width * 0.72f), minWidth, maxWidth);
        pageHeight = Mth.clamp(Math.round(this.height * 0.76f), minHeight, maxHeight);
        pageX = (this.width - pageWidth) / 2;
        pageY = (this.height - pageHeight) / 2;

        int contentY = pageY + 27;
        int contentHeight = Math.max(115, pageHeight - 27);
        int availableWidth = pageWidth - GAP * 2;

        int listWidth = Mth.clamp(availableWidth * 24 / 100, 86, 188);
        int detailsWidth = Mth.clamp(availableWidth * 36 / 100, 120, 320);
        int previewWidth = availableWidth - listWidth - detailsWidth;

        if (previewWidth < 86) {
            int missing = 86 - previewWidth;
            int detailsReduction = Math.min(missing, Math.max(0, detailsWidth - 120));
            detailsWidth -= detailsReduction;
            missing -= detailsReduction;
            listWidth -= Math.min(missing, Math.max(0, listWidth - 86));
            previewWidth = availableWidth - listWidth - detailsWidth;
        }

        listPanel = new Rect(pageX, contentY, listWidth, contentHeight);
        previewPanel = new Rect(listPanel.right() + GAP, contentY, previewWidth, contentHeight);
        detailsPanel = new Rect(previewPanel.right() + GAP, contentY, detailsWidth, contentHeight);
    }

    private void applyFilters(String preferredCharacterId) {
        List<CharacterClass> matches = allCharacters.stream()
                .filter(character -> activeFilters.isEmpty()
                        || character.getTags().stream().anyMatch(activeFilters::contains))
                .toList();

        filteredCharacters = matches;
        if (matches.isEmpty()) {
            selectedIndex = 0;
            previewEntries = List.of();
        } else {
            int preferredIndex = -1;
            if (preferredCharacterId != null) {
                for (int index = 0; index < matches.size(); index++) {
                    if (matches.get(index).getId().equals(preferredCharacterId)) {
                        preferredIndex = index;
                        break;
                    }
                }
            }
            selectedIndex = preferredIndex >= 0 ? preferredIndex : Mth.clamp(selectedIndex, 0, matches.size() - 1);
            refreshPreview();
        }

        cardScroll = Mth.clamp(cardScroll, 0, Math.max(0, cardScrollMax));
        if (selectButton != null) {
            selectButton.active = !filteredCharacters.isEmpty();
        }
    }

    private void setSelectedIndex(int index) {
        if (filteredCharacters.isEmpty()) {
            return;
        }

        selectedIndex = Mth.clamp(index, 0, filteredCharacters.size() - 1);
        detailScroll = 0;
        itemScroll = 0;
        refreshPreview();
        ensureSelectedCardVisible();
    }

    private void refreshPreview() {
        CharacterClass selected = selectedCharacter();
        previewEntries = selected == null
                ? List.of()
                : CharacterLoadoutPreview.forCharacter(selected);
    }

    private void ensureSelectedCardVisible() {
        if (largeCards || cardViewport.height() <= 0 || filteredCharacters.isEmpty()) {
            return;
        }

        int cardHeight = cardHeight();
        int cardTop = selectedIndex * (cardHeight + CARD_GAP);
        int cardBottom = cardTop + cardHeight;
        if (cardTop < cardScroll) {
            cardScroll = cardTop;
        } else if (cardBottom > cardScroll + cardViewport.height()) {
            cardScroll = cardBottom - cardViewport.height();
        }
        cardScroll = Mth.clamp(cardScroll, 0, cardScrollMax);
    }

    private CharacterClass selectedCharacter() {
        if (filteredCharacters.isEmpty() || selectedIndex < 0 || selectedIndex >= filteredCharacters.size()) {
            return null;
        }
        return filteredCharacters.get(selectedIndex);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        drawGradientBackground(graphics);

        hoveredEntry = null;
        hoveredFilterTag = null;
        cardHits.clear();
        filterHits.clear();

        if (filtersOpen) {
            drawFilterOverlay(graphics, mouseX, mouseY);
            drawTooltip(graphics, mouseX, mouseY);
            return;
        }

        drawHeader(graphics);
        drawCharacterList(graphics, mouseX, mouseY);
        drawPreview(graphics, mouseX, mouseY);
        drawDetails(graphics, mouseX, mouseY);

        super.render(graphics, mouseX, mouseY, partialTick);

        drawTooltip(graphics, mouseX, mouseY);
    }

    private void drawGradientBackground(GuiGraphics graphics) {
        int top = type == CharacterType.SURVIVOR ? 0xE5101B23 : 0xE5211013;
        int bottom = type == CharacterType.SURVIVOR ? 0xF0060B10 : 0xF00D0708;
        graphics.fillGradient(0, 0, width, height, top, bottom);
        graphics.fill(pageX, pageY, pageX + pageWidth, pageY + pageHeight, 0x78101720);
        graphics.renderOutline(pageX, pageY, pageWidth, pageHeight, GuideTheme.BORDER_SOFT);
    }

    private void drawHeader(GuiGraphics graphics) {
        int accent = accent();
        String heading = "ВЫБОР ГЕРОЯ — " + type.getDisplayName().toUpperCase();
        graphics.drawCenteredString(font, heading, pageX + pageWidth / 2, pageY + 7, GuideTheme.TEXT);
        int underlineWidth = Math.min(100, Math.max(42, font.width(heading) / 3));
        graphics.fill(pageX + pageWidth / 2 - underlineWidth / 2, pageY + 19,
                pageX + pageWidth / 2 + underlineWidth / 2, pageY + 20, accent);

        String current = "Текущий класс: " + currentClassName();
        graphics.drawString(font, trim(current, Math.max(60, pageWidth / 3)),
                pageX + 5, pageY + 7, GuideTheme.TEXT_MUTED, false);
    }

    private void drawCharacterList(GuiGraphics graphics, int mouseX, int mouseY) {
        drawPanel(graphics, listPanel, "ПЕРСОНАЖИ");

        int controlsY = listPanel.y() + 19;
        filterButtonRect = new Rect(listPanel.right() - 25, controlsY, 20, 18);
        cardModeToggleRect = new Rect(listPanel.x() + 5, controlsY,
                Math.max(24, filterButtonRect.x() - listPanel.x() - 9), 18);

        drawCardModeToggle(graphics, mouseX, mouseY);

        int filterCount = activeFilters.size();
        GuideTheme.drawButton(graphics, font, filterButtonRect.x(), filterButtonRect.y(),
                filterButtonRect.width(), filterButtonRect.height(), "",
                accent(), filterButtonRect.contains(mouseX, mouseY), filterCount > 0);
        drawFilterIcon(graphics, filterButtonRect, filterCount > 0);

        cardViewport = new Rect(listPanel.x() + 5, cardModeToggleRect.bottom() + 5,
                Math.max(10, listPanel.width() - 10),
                Math.max(10, listPanel.bottom() - cardModeToggleRect.bottom() - 10));

        if (filteredCharacters.isEmpty()) {
            graphics.drawCenteredString(font, "Нет совпадений", cardViewport.x() + cardViewport.width() / 2,
                    cardViewport.y() + 18, GuideTheme.TEXT_MUTED);
            graphics.drawCenteredString(font, "Откройте фильтры", cardViewport.x() + cardViewport.width() / 2,
                    cardViewport.y() + 31, GuideTheme.TEXT_MUTED);
            cardScrollMax = 0;
            cardScroll = 0;
            return;
        }

        if (largeCards) {
            drawLargeCardCarousel(graphics, mouseX, mouseY);
        } else {
            drawCompactCards(graphics, mouseX, mouseY);
        }
    }

    private void drawFilterIcon(GuiGraphics graphics, Rect button, boolean active) {
        int iconX = button.x() + (button.width() - 10) / 2;
        int iconY = button.y() + 4;
        int color = active ? GuideTheme.TEXT : GuideTheme.TEXT_SECONDARY;
        graphics.fill(iconX, iconY, iconX + 10, iconY + 2, color);
        graphics.fill(iconX + 1, iconY + 2, iconX + 9, iconY + 4, color);
        graphics.fill(iconX + 3, iconY + 4, iconX + 7, iconY + 6, color);
        graphics.fill(iconX + 4, iconY + 6, iconX + 6, iconY + 11, color);
        if (active) {
            graphics.fill(button.right() - 4, button.y() + 3,
                    button.right() - 2, button.y() + 5, GuideTheme.GOLD);
        }
    }

    private void drawCardModeToggle(GuiGraphics graphics, int mouseX, int mouseY) {
        boolean hovered = cardModeToggleRect.contains(mouseX, mouseY);
        graphics.fill(cardModeToggleRect.x(), cardModeToggleRect.y(),
                cardModeToggleRect.right(), cardModeToggleRect.bottom(),
                hovered ? GuideTheme.SURFACE_HOVER : GuideTheme.SURFACE);
        graphics.renderOutline(cardModeToggleRect.x(), cardModeToggleRect.y(),
                cardModeToggleRect.width(), cardModeToggleRect.height(),
                largeCards ? accent() : GuideTheme.BORDER_SOFT);

        int switchWidth = 24;
        int switchX = cardModeToggleRect.right() - switchWidth - 4;
        int switchY = cardModeToggleRect.y() + 4;
        graphics.fill(switchX, switchY, switchX + switchWidth, switchY + 9,
                largeCards ? accent() : 0xFF4C5864);
        int knobX = largeCards ? switchX + switchWidth - 8 : switchX + 2;
        graphics.fill(knobX, switchY + 2, knobX + 6, switchY + 7, GuideTheme.TEXT);
        graphics.drawString(font, trim("Крупные карточки", Math.max(10, switchX - cardModeToggleRect.x() - 7)),
                cardModeToggleRect.x() + 4, cardModeToggleRect.y() + 5,
                largeCards ? GuideTheme.TEXT : GuideTheme.TEXT_SECONDARY, false);
    }

    private void drawCompactCards(GuiGraphics graphics, int mouseX, int mouseY) {
        int cardHeight = cardHeight();
        int totalHeight = filteredCharacters.size() * (cardHeight + CARD_GAP) - CARD_GAP;
        cardScrollMax = Math.max(0, totalHeight - cardViewport.height());
        cardScroll = Mth.clamp(cardScroll, 0, cardScrollMax);

        graphics.enableScissor(cardViewport.x(), cardViewport.y(), cardViewport.right(), cardViewport.bottom());
        for (int index = 0; index < filteredCharacters.size(); index++) {
            CharacterClass character = filteredCharacters.get(index);
            int cardY = cardViewport.y() + index * (cardHeight + CARD_GAP) - cardScroll;
            if (cardY + cardHeight < cardViewport.y() || cardY > cardViewport.bottom()) {
                continue;
            }

            Rect card = new Rect(cardViewport.x(), cardY, cardViewport.width() - (cardScrollMax > 0 ? 3 : 0), cardHeight);
            boolean selected = index == selectedIndex;
            boolean hovered = card.contains(mouseX, mouseY);
            int background = selected ? GuideTheme.SURFACE_SELECTED
                    : hovered ? GuideTheme.SURFACE_HOVER : GuideTheme.SURFACE;
            graphics.fill(card.x(), card.y(), card.right(), card.bottom(), background);
            graphics.renderOutline(card.x(), card.y(), card.width(), card.height(),
                    selected ? accent() : GuideTheme.BORDER_SOFT);
            if (selected) {
                graphics.fill(card.x(), card.y(), card.x() + 3, card.bottom(), accent());
            }

            int imageHeight = Math.max(20, card.height() - 17);
            Rect image = new Rect(card.x() + 2, card.y() + 2, Math.max(1, card.width() - 4), imageHeight);
            drawCroppedFresco(graphics, character, image);
            graphics.fill(card.x() + 1, card.bottom() - 16, card.right() - 1, card.bottom() - 1, 0xE511171E);

            String name = trim(character.getName(), Math.max(10, card.width() - 20));
            graphics.drawString(font, name, card.x() + 5, card.bottom() - 13,
                    selected ? GuideTheme.TEXT : GuideTheme.TEXT_SECONDARY, false);

            cardHits.add(new CardHit(index, card));
        }
        graphics.disableScissor();
        drawScrollBar(graphics, cardViewport, cardScroll, cardScrollMax, totalHeight);
    }

    private void drawLargeCardCarousel(GuiGraphics graphics, int mouseX, int mouseY) {
        cardScroll = 0;
        cardScrollMax = 0;
        int cardWidth = Math.max(28, cardViewport.width() - 18);
        int cardHeight = Math.max(42, cardViewport.height() - 18);
        int centerX = cardViewport.x() + cardViewport.width() / 2;
        int cardY = cardViewport.y() + 1;

        graphics.enableScissor(cardViewport.x(), cardViewport.y(), cardViewport.right(), cardViewport.bottom());
        for (int index = Math.max(0, selectedIndex - 1);
             index <= Math.min(filteredCharacters.size() - 1, selectedIndex + 1);
             index++) {
            int offset = index - selectedIndex;
            int x = centerX - cardWidth / 2 + offset * (cardWidth + 7);
            Rect card = new Rect(x, cardY, cardWidth, cardHeight);
            boolean selected = index == selectedIndex;
            boolean hovered = card.contains(mouseX, mouseY);
            graphics.fill(card.x(), card.y(), card.right(), card.bottom(),
                    selected ? GuideTheme.SURFACE_SELECTED
                            : hovered ? GuideTheme.SURFACE_HOVER : GuideTheme.SURFACE);
            graphics.renderOutline(card.x(), card.y(), card.width(), card.height(),
                    selected ? accent() : GuideTheme.BORDER_SOFT);

            Rect image = new Rect(card.x() + 3, card.y() + 3,
                    Math.max(1, card.width() - 6), Math.max(1, card.height() - 22));
            drawFittedFresco(graphics, filteredCharacters.get(index), image);
            graphics.fill(card.x() + 1, card.bottom() - 18, card.right() - 1, card.bottom() - 1, 0xE511171E);
            graphics.drawCenteredString(font,
                    trim(filteredCharacters.get(index).getName(), card.width() - 8),
                    card.x() + card.width() / 2, card.bottom() - 14,
                    selected ? GuideTheme.TEXT : GuideTheme.TEXT_SECONDARY);
            cardHits.add(new CardHit(index, card));
        }
        graphics.disableScissor();

        largePreviousRect = selectedIndex > 0
                ? new Rect(cardViewport.x() + 1, cardViewport.y() + cardViewport.height() / 2 - 10, 16, 20)
                : Rect.EMPTY;
        largeNextRect = selectedIndex < filteredCharacters.size() - 1
                ? new Rect(cardViewport.right() - 17, cardViewport.y() + cardViewport.height() / 2 - 10, 16, 20)
                : Rect.EMPTY;

        if (largePreviousRect.width() > 0) {
            GuideTheme.drawButton(graphics, font, largePreviousRect.x(), largePreviousRect.y(),
                    largePreviousRect.width(), largePreviousRect.height(), "‹", accent(),
                    largePreviousRect.contains(mouseX, mouseY), false);
        }
        if (largeNextRect.width() > 0) {
            GuideTheme.drawButton(graphics, font, largeNextRect.x(), largeNextRect.y(),
                    largeNextRect.width(), largeNextRect.height(), "›", accent(),
                    largeNextRect.contains(mouseX, mouseY), false);
        }
        graphics.drawCenteredString(font, (selectedIndex + 1) + " / " + filteredCharacters.size(),
                cardViewport.x() + cardViewport.width() / 2, cardViewport.bottom() - 10, GuideTheme.TEXT_MUTED);
    }

    private void drawCroppedFresco(GuiGraphics graphics, CharacterClass character, Rect destination) {
        FrescoDimensions source = FRESCO_DIMENSIONS.getOrDefault(
                character.getId(),
                new FrescoDimensions(destination.width(), destination.height())
        );
        float targetAspect = destination.width() / (float) Math.max(1, destination.height());
        float sourceAspect = source.width() / (float) source.height();

        int sourceX = 0;
        int sourceY = 0;
        int sourceWidth = source.width();
        int sourceHeight = source.height();
        if (sourceAspect > targetAspect) {
            sourceWidth = Math.max(1, Math.round(sourceHeight * targetAspect));
            sourceX = Math.max(0, (source.width() - sourceWidth) / 2);
        } else {
            sourceHeight = Math.max(1, Math.round(sourceWidth / targetAspect));
            int remaining = Math.max(0, source.height() - sourceHeight);
            sourceY = Math.min(remaining, remaining / 8);
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        graphics.blit(character.getFrescoTexture(),
                destination.x(), destination.y(), destination.width(), destination.height(),
                sourceX, sourceY, sourceWidth, sourceHeight, source.width(), source.height());
    }

    private void drawFittedFresco(GuiGraphics graphics, CharacterClass character, Rect destination) {
        FrescoDimensions source = FRESCO_DIMENSIONS.getOrDefault(
                character.getId(),
                new FrescoDimensions(destination.width(), destination.height())
        );
        float sourceAspect = source.width() / (float) Math.max(1, source.height());
        float targetAspect = destination.width() / (float) Math.max(1, destination.height());
        int width = destination.width();
        int height = destination.height();
        if (sourceAspect > targetAspect) {
            height = Math.max(1, Math.round(width / sourceAspect));
        } else {
            width = Math.max(1, Math.round(height * sourceAspect));
        }
        int x = destination.x() + (destination.width() - width) / 2;
        int y = destination.y() + (destination.height() - height) / 2;
        graphics.fill(destination.x(), destination.y(), destination.right(), destination.bottom(), 0xFF090C10);
        graphics.blit(character.getFrescoTexture(), x, y, width, height,
                0, 0, source.width(), source.height(), source.width(), source.height());
    }

    private void drawPreview(GuiGraphics graphics, int mouseX, int mouseY) {
        drawPanel(graphics, previewPanel, "МОДЕЛЬ И ЭКИПИРОВКА");
        CharacterClass selected = selectedCharacter();
        if (selected == null) {
            graphics.drawCenteredString(font, "Выберите персонажа",
                    previewPanel.x() + previewPanel.width() / 2, previewPanel.y() + 36, GuideTheme.TEXT_MUTED);
            return;
        }

        Rect frescoBackdrop = new Rect(previewPanel.x() + 2, previewPanel.y() + 18,
                Math.max(1, previewPanel.width() - 4), Math.max(1, previewPanel.height() - 20));
        drawCroppedFresco(graphics, selected, frescoBackdrop);
        graphics.fill(frescoBackdrop.x(), frescoBackdrop.y(), frescoBackdrop.right(), frescoBackdrop.bottom(),
                0x8A070B10);

        List<CharacterLoadoutPreview.Entry> equipment = previewEntries.stream()
                .filter(entry -> entry.placement().isEquipment())
                .toList();

        int handIndex = 0;
        int armorIndex = 0;
        for (CharacterLoadoutPreview.Entry entry : equipment) {
            boolean hand = entry.placement() == CharacterLoadoutPreview.Placement.MAIN_HAND
                    || entry.placement() == CharacterLoadoutPreview.Placement.OFF_HAND;
            int columnIndex = hand ? handIndex++ : armorIndex++;
            int x = hand ? previewPanel.x() + 5 : previewPanel.right() - SLOT_SIZE - 5;
            int y = previewPanel.y() + 24 + columnIndex * (SLOT_SIZE + 4);
            if (y + SLOT_SIZE > previewPanel.bottom() - 4) {
                continue;
            }

            Rect slot = new Rect(x, y, SLOT_SIZE, SLOT_SIZE);
            boolean hovered = slot.contains(mouseX, mouseY);
            graphics.fill(slot.x(), slot.y(), slot.right(), slot.bottom(),
                    hovered ? GuideTheme.SURFACE_HOVER : 0xE5161C23);
            graphics.renderOutline(slot.x(), slot.y(), slot.width(), slot.height(),
                    hovered ? accent() : GuideTheme.BORDER_SOFT);
            graphics.renderItem(entry.stack(), slot.x() + 3, slot.y() + 3);
            graphics.renderItemDecorations(font, entry.stack(), slot.x() + 3, slot.y() + 3);
            if (hovered) {
                hoveredEntry = entry;
            }
        }

        if (minecraft == null || minecraft.player == null || minecraft.level == null) {
            graphics.drawCenteredString(font, "Модель недоступна",
                    previewPanel.x() + previewPanel.width() / 2, previewPanel.y() + 45, GuideTheme.TEXT_MUTED);
            return;
        }

        if (previewPlayer == null
                || !previewPlayer.getGameProfile().getId().equals(minecraft.player.getGameProfile().getId())) {
            previewPlayer = new RemotePlayer(minecraft.level, minecraft.player.getGameProfile());
        }
        ClientKeeperFormData.setPreviewKeeper(previewPlayer, "keeper_of_nightmares".equals(selected.getId()));

        int modelX = previewPanel.x() + previewPanel.width() / 2;
        int modelBottom = previewPanel.bottom() - 5;
        int modelHeight = Math.max(45, previewPanel.height() - 25);
        int modelScale = Mth.clamp(
                Math.min(Math.round(previewPanel.width() * 0.38f), Math.round(modelHeight * 0.43f)),
                24,
                92
        );

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            previewPlayer.setItemSlot(slot, ItemStack.EMPTY);
        }
        for (CharacterLoadoutPreview.Entry entry : equipment) {
            previewPlayer.setItemSlot(entry.placement().equipmentSlot(), entry.stack().copy());
        }

        graphics.enableScissor(frescoBackdrop.x(), frescoBackdrop.y(),
                frescoBackdrop.right(), frescoBackdrop.bottom());
        InventoryScreen.renderEntityInInventoryFollowsAngle(
                graphics,
                modelX,
                modelBottom,
                modelScale,
                0.0F,
                0.0F,
                previewPlayer
        );
        graphics.disableScissor();

        if (equipment.isEmpty()) {
            graphics.drawCenteredString(font, "Без экипировки", modelX,
                    previewPanel.bottom() - 18, GuideTheme.TEXT_MUTED);
        }
    }

    private void drawDetails(GuiGraphics graphics, int mouseX, int mouseY) {
        drawPanel(graphics, detailsPanel, "ДАННЫЕ ПЕРСОНАЖА");
        CharacterClass selected = selectedCharacter();
        if (selected == null) {
            return;
        }

        int textX = detailsPanel.x() + 6;
        int contentWidth = Math.max(30, detailsPanel.width() - 12);
        graphics.drawString(font, trim(selected.getName(), contentWidth), textX,
                detailsPanel.y() + 20, accent(), false);

        int actionTop = detailsPanel.bottom() - 28;
        int itemsHeight = Mth.clamp(detailsPanel.height() / 3, 54, 100);
        int itemsTop = Math.max(detailsPanel.y() + 73, actionTop - itemsHeight);
        detailViewport = new Rect(textX, detailsPanel.y() + 33, contentWidth,
                Math.max(22, itemsTop - detailsPanel.y() - 38));
        drawDetailText(graphics, selected, detailViewport);

        graphics.drawString(font, "ПРЕДМЕТЫ", textX, itemsTop, GuideTheme.GOLD, false);
        itemViewport = new Rect(textX, itemsTop + 11, contentWidth,
                Math.max(18, actionTop - itemsTop - 14));
        drawOtherItems(graphics, mouseX, mouseY, selected);
    }

    private void drawDetailText(GuiGraphics graphics, CharacterClass selected, Rect viewport) {
        int y = viewport.y() + 2 - detailScroll;
        int contentStart = viewport.y() + 2;
        int lineHeight = 10;

        graphics.enableScissor(viewport.x(), viewport.y(), viewport.right(), viewport.bottom());

        graphics.drawString(font, "Сложность: " + selected.getDifficulty() + "/5",
                viewport.x(), y, GuideTheme.TEXT_SECONDARY, false);
        y += 12;

        if (!selected.getTags().isEmpty()) {
            graphics.drawString(font, "Роли:", viewport.x(), y, GuideTheme.BLUE, false);
            y += lineHeight;
            y = drawWrapped(graphics, String.join(" • ", selected.getTags()),
                    viewport.x(), y, viewport.width(), GuideTheme.TEXT_SECONDARY, lineHeight);
            y += 4;
        }

        graphics.drawString(font, "Описание:", viewport.x(), y, GuideTheme.GOLD, false);
        y += lineHeight;
        y = drawWrapped(graphics, selected.getDescription(), viewport.x(), y,
                viewport.width(), GuideTheme.TEXT_SECONDARY, lineHeight);
        y += 5;

        if (!selected.getFeatures().isEmpty()) {
            graphics.drawString(font, "Способности:", viewport.x(), y, GuideTheme.GREEN, false);
            y += lineHeight + 1;
            for (CharacterClass.Feature feature : selected.getFeatures()) {
                y = drawWrapped(graphics, "• " + feature.getName(), viewport.x(), y,
                        viewport.width(), GuideTheme.TEXT, lineHeight);
                y = drawWrapped(graphics, feature.getDescription(), viewport.x() + 5, y,
                        Math.max(20, viewport.width() - 5), GuideTheme.TEXT_MUTED, lineHeight);
                y += 4;
            }
        }

        graphics.disableScissor();

        int contentHeight = y + detailScroll - contentStart;
        detailScrollMax = Math.max(0, contentHeight - viewport.height());
        detailScroll = Mth.clamp(detailScroll, 0, detailScrollMax);
        drawScrollBar(graphics, viewport, detailScroll, detailScrollMax, contentHeight);
    }

    private int drawWrapped(GuiGraphics graphics, String text, int x, int y,
                            int maxWidth, int color, int lineHeight) {
        for (FormattedCharSequence line : font.split(Component.literal(text), Math.max(10, maxWidth))) {
            graphics.drawString(font, line, x, y, color, false);
            y += lineHeight;
        }
        return y;
    }

    private void drawOtherItems(GuiGraphics graphics, int mouseX, int mouseY, CharacterClass selected) {
        List<CharacterLoadoutPreview.Entry> otherItems = previewEntries.stream()
                .filter(entry -> !entry.placement().isEquipment())
                .toList();

        if (otherItems.isEmpty()) {
            graphics.drawCenteredString(font, "Нет отдельных предметов",
                    itemViewport.x() + itemViewport.width() / 2, itemViewport.y() + 5, GuideTheme.TEXT_MUTED);
            itemScroll = 0;
            itemScrollMax = 0;
            return;
        }

        int columns = 2;
        int columnGap = 3;
        int scrollbarSpace = otherItems.size() > 4 ? 3 : 0;
        int cellWidth = Math.max(20, (itemViewport.width() - columnGap - scrollbarSpace) / columns);
        int rows = (otherItems.size() + columns - 1) / columns;
        int totalHeight = rows * ITEM_CELL_HEIGHT;
        itemScrollMax = Math.max(0, totalHeight - itemViewport.height());
        itemScroll = Mth.clamp(itemScroll, 0, itemScrollMax);

        graphics.enableScissor(itemViewport.x(), itemViewport.y(), itemViewport.right(), itemViewport.bottom());
        for (int index = 0; index < otherItems.size(); index++) {
            CharacterLoadoutPreview.Entry entry = otherItems.get(index);
            int column = index % columns;
            int row = index / columns;
            int x = itemViewport.x() + column * (cellWidth + columnGap);
            int y = itemViewport.y() + row * ITEM_CELL_HEIGHT - itemScroll;
            Rect cell = new Rect(x, y, cellWidth, ITEM_CELL_HEIGHT - 3);
            if (cell.bottom() < itemViewport.y() || cell.y() > itemViewport.bottom()) {
                continue;
            }

            boolean hovered = cell.contains(mouseX, mouseY);
            graphics.fill(cell.x(), cell.y(), cell.right(), cell.bottom(),
                    hovered ? GuideTheme.SURFACE_HOVER : 0xE5161C23);
            graphics.renderOutline(cell.x(), cell.y(), cell.width(), cell.height(),
                    hovered ? accent() : GuideTheme.BORDER_SOFT);
            graphics.renderItem(entry.stack(), cell.x() + 4, cell.y() + 5);
            graphics.renderItemDecorations(font, entry.stack(), cell.x() + 4, cell.y() + 5);

            if (cell.width() >= 45) {
                graphics.drawString(font, trim(entry.name(), cell.width() - 25),
                        cell.x() + 23, cell.y() + 9, GuideTheme.TEXT_SECONDARY, false);
            }

            if (hovered) {
                hoveredEntry = entry;
            }
        }
        graphics.disableScissor();
        drawScrollBar(graphics, itemViewport, itemScroll, itemScrollMax, totalHeight);
    }

    private void drawFilterOverlay(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.fill(0, 0, width, height, 0xB8000000);

        int overlayWidth = Math.min(Math.max(250, pageWidth - 60), 440);
        int overlayHeight = Math.min(Math.max(150, pageHeight - 36), 270);
        filterPanel = new Rect((width - overlayWidth) / 2, (height - overlayHeight) / 2,
                overlayWidth, overlayHeight);
        graphics.fill(filterPanel.x(), filterPanel.y(), filterPanel.right(), filterPanel.bottom(), GuideTheme.PANEL);
        graphics.renderOutline(filterPanel.x(), filterPanel.y(), filterPanel.width(), filterPanel.height(), accent());

        graphics.drawCenteredString(font, "ФИЛЬТРЫ ПЕРСОНАЖЕЙ",
                filterPanel.x() + filterPanel.width() / 2, filterPanel.y() + 8, GuideTheme.TEXT);

        filterCloseRect = new Rect(filterPanel.right() - 20, filterPanel.y() + 4, 16, 16);
        GuideTheme.drawButton(graphics, font, filterCloseRect.x(), filterCloseRect.y(),
                filterCloseRect.width(), filterCloseRect.height(), "×", accent(),
                filterCloseRect.contains(mouseX, mouseY), false);

        int actionsY = filterPanel.y() + 25;
        filterClearRect = new Rect(filterPanel.x() + 5, actionsY, filterPanel.width() - 10, 18);
        GuideTheme.drawButton(graphics, font, filterClearRect.x(), filterClearRect.y(),
                filterClearRect.width(), filterClearRect.height(), "СБРОСИТЬ ФИЛЬТРЫ",
                accent(), filterClearRect.contains(mouseX, mouseY),
                activeFilters.isEmpty());

        graphics.drawString(font, "Совпадение с любым выбранным тегом",
                filterPanel.x() + 6, actionsY + 23, GuideTheme.TEXT_MUTED, false);

        List<String> tags = allCharacters.stream()
                .flatMap(character -> character.getTags().stream())
                .distinct()
                .sorted()
                .toList();

        Rect tagsViewport = new Rect(filterPanel.x() + 5, actionsY + 36,
                filterPanel.width() - 10, filterPanel.bottom() - actionsY - 41);
        int columns = 2;
        int columnGap = 5;
        int cellHeight = 21;
        int cellWidth = (tagsViewport.width() - columnGap - 3) / columns;
        int rows = (tags.size() + columns - 1) / columns;
        int totalHeight = rows * cellHeight;
        filterScrollMax = Math.max(0, totalHeight - tagsViewport.height());
        filterScroll = Mth.clamp(filterScroll, 0, filterScrollMax);

        graphics.enableScissor(tagsViewport.x(), tagsViewport.y(), tagsViewport.right(), tagsViewport.bottom());
        for (int index = 0; index < tags.size(); index++) {
            String tag = tags.get(index);
            int column = index % columns;
            int row = index / columns;
            int x = tagsViewport.x() + column * (cellWidth + columnGap);
            int y = tagsViewport.y() + row * cellHeight - filterScroll;
            Rect chip = new Rect(x, y, cellWidth, 18);
            if (chip.bottom() < tagsViewport.y() || chip.y() > tagsViewport.bottom()) {
                continue;
            }
            boolean hovered = chip.contains(mouseX, mouseY);
            boolean active = activeFilters.contains(tag);
            GuideTheme.drawButton(graphics, font, chip.x(), chip.y(), chip.width(), chip.height(),
                    trim((active ? "✓ " : "") + tag, chip.width() - 8),
                    accent(), hovered, active);
            filterHits.add(new FilterHit(tag, chip));
            if (hovered) {
                hoveredFilterTag = tag;
            }
        }
        graphics.disableScissor();
        drawScrollBar(graphics, tagsViewport, filterScroll, filterScrollMax, totalHeight);
    }

    private void drawTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!filtersOpen && filterButtonRect.contains(mouseX, mouseY)) {
            String suffix = activeFilters.isEmpty() ? "" : " (" + activeFilters.size() + ")";
            graphics.renderComponentTooltip(font,
                    List.of(Component.literal("§eФильтры персонажей" + suffix)), mouseX, mouseY);
            return;
        }

        if (filtersOpen && hoveredFilterTag != null) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal("§e§l" + hoveredFilterTag));
            appendWrappedTooltip(tooltip, "§7", TagRegistry.getTagDescription(hoveredFilterTag), 220);
            graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
            return;
        }

        if (filtersOpen || hoveredEntry == null) {
            return;
        }

        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.literal("§e§l" + hoveredEntry.name()));
        if (!hoveredEntry.description().isBlank()) {
            appendWrappedTooltip(tooltip, "§7", hoveredEntry.description(), 230);
        }

        List<Component> vanillaTooltip = Screen.getTooltipFromItem(minecraft, hoveredEntry.stack());
        if (vanillaTooltip.size() > 1) {
            tooltip.add(Component.empty());
            int limit = Math.min(vanillaTooltip.size(), 7);
            for (int index = 1; index < limit; index++) {
                tooltip.add(vanillaTooltip.get(index));
            }
        }

        if (!hoveredEntry.lore().isBlank()) {
            tooltip.add(Component.empty());
            tooltip.add(Component.literal("§6Лор"));
            appendWrappedTooltip(tooltip, "§8§o", hoveredEntry.lore(), 230);
        }

        graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
    }

    private void appendWrappedTooltip(List<Component> tooltip, String prefix, String text, int maxWidth) {
        if (text == null || text.isBlank()) {
            return;
        }

        StringBuilder line = new StringBuilder();
        for (String word : text.split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && font.width(candidate) > maxWidth) {
                tooltip.add(Component.literal(prefix + line));
                line = new StringBuilder(word);
            } else {
                if (!line.isEmpty()) {
                    line.append(' ');
                }
                line.append(word);
            }
        }
        if (!line.isEmpty()) {
            tooltip.add(Component.literal(prefix + line));
        }
    }

    private void drawPanel(GuiGraphics graphics, Rect panel, String title) {
        graphics.fill(panel.x(), panel.y(), panel.right(), panel.bottom(), GuideTheme.PANEL);
        graphics.renderOutline(panel.x(), panel.y(), panel.width(), panel.height(), GuideTheme.BORDER_SOFT);
        graphics.fill(panel.x() + 1, panel.y() + 1, panel.right() - 1, panel.y() + 16, GuideTheme.HEADER);
        graphics.drawCenteredString(font, title, panel.x() + panel.width() / 2,
                panel.y() + 5, GuideTheme.TEXT_SECONDARY);
        graphics.fill(panel.x() + 1, panel.y() + 16, panel.right() - 1, panel.y() + 17, accent());
    }

    private void drawScrollBar(GuiGraphics graphics, Rect viewport, int scroll, int maxScroll, int contentHeight) {
        if (maxScroll <= 0 || viewport.height() <= 0 || contentHeight <= 0) {
            return;
        }

        int trackX = viewport.right() - 2;
        int thumbHeight = Math.max(12, viewport.height() * viewport.height() / contentHeight);
        int thumbTravel = Math.max(1, viewport.height() - thumbHeight);
        int thumbY = viewport.y() + Math.round(thumbTravel * (scroll / (float) maxScroll));
        graphics.fill(trackX, viewport.y(), trackX + 2, viewport.bottom(), 0x66374450);
        graphics.fill(trackX, thumbY, trackX + 2, thumbY + thumbHeight, accent());
    }

    private int cardHeight() {
        return Mth.clamp(listPanel.height() / 4, 46, 66);
    }

    private int accent() {
        return type == CharacterType.SURVIVOR ? GuideTheme.BLUE : GuideTheme.RED;
    }

    private String currentClassName() {
        int currentId = type == CharacterType.SURVIVOR
                ? ClientPlayerData.getSurvivorClassId()
                : ClientPlayerData.getManiacClassId();
        return allCharacters.stream()
                .filter(character -> character.getScoreboardId() == currentId)
                .map(CharacterClass::getName)
                .findFirst()
                .orElse("не выбран");
    }

    private UUID profileId() {
        return minecraft != null && minecraft.player != null
                ? minecraft.player.getUUID()
                : FALLBACK_PROFILE;
    }

    private String trim(String text, int maxWidth) {
        if (maxWidth <= 6 || font.width(text) <= maxWidth) {
            return text;
        }
        return font.plainSubstrByWidth(text, Math.max(1, maxWidth - font.width("…"))) + "…";
    }

    private void selectCharacter() {
        CharacterClass selected = selectedCharacter();
        if (selected == null) {
            return;
        }
        ModNetworking.sendToServer(new SelectCharacterPacket(selected.getId()));
        onClose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && filtersOpen) {
            if (filterCloseRect.contains(mouseX, mouseY) || !filterPanel.contains(mouseX, mouseY)) {
                filtersOpen = false;
                return true;
            }
            if (filterClearRect.contains(mouseX, mouseY)) {
                activeFilters.clear();
                filterScroll = 0;
                applyFilters(null);
                return true;
            }
            for (FilterHit hit : filterHits) {
                if (hit.bounds().contains(mouseX, mouseY)) {
                    String selectedId = selectedCharacter() == null ? null : selectedCharacter().getId();
                    if (!activeFilters.remove(hit.tag())) {
                        activeFilters.add(hit.tag());
                    }
                    applyFilters(selectedId);
                    return true;
                }
            }
            return true;
        }

        if (button == 0 && filterButtonRect.contains(mouseX, mouseY)) {
            filtersOpen = true;
            return true;
        }

        if (button == 0 && cardModeToggleRect.contains(mouseX, mouseY)) {
            largeCards = !largeCards;
            CharacterSelectionPreferences.setLargeCards(profileId(), largeCards);
            cardScroll = 0;
            ensureSelectedCardVisible();
            return true;
        }

        if (button == 0 && largeCards && largePreviousRect.contains(mouseX, mouseY)) {
            setSelectedIndex(selectedIndex - 1);
            return true;
        }

        if (button == 0 && largeCards && largeNextRect.contains(mouseX, mouseY)) {
            setSelectedIndex(selectedIndex + 1);
            return true;
        }

        if (button == 0) {
            for (CardHit hit : cardHits) {
                if (hit.bounds().contains(mouseX, mouseY)) {
                    setSelectedIndex(hit.index());
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int direction = delta > 0 ? -1 : 1;
        if (filtersOpen && filterPanel.contains(mouseX, mouseY)) {
            filterScroll = Mth.clamp(filterScroll + direction * 24, 0, filterScrollMax);
            return true;
        }
        if (cardViewport.contains(mouseX, mouseY)) {
            if (largeCards) {
                setSelectedIndex(selectedIndex + direction);
            } else {
                cardScroll = Mth.clamp(cardScroll + direction * (cardHeight() + CARD_GAP), 0, cardScrollMax);
            }
            return true;
        }
        if (itemViewport.contains(mouseX, mouseY)) {
            itemScroll = Mth.clamp(itemScroll + direction * ITEM_CELL_HEIGHT, 0, itemScrollMax);
            return true;
        }
        if (detailViewport.contains(mouseX, mouseY)) {
            detailScroll = Mth.clamp(detailScroll + direction * 20, 0, detailScrollMax);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            if (filtersOpen) {
                filtersOpen = false;
            } else {
                onClose();
            }
            return true;
        }
        if (keyCode == 265) {
            setSelectedIndex(selectedIndex - 1);
            return true;
        }
        if (keyCode == 264) {
            setSelectedIndex(selectedIndex + 1);
            return true;
        }
        if (keyCode == 263) {
            setSelectedIndex(selectedIndex - 1);
            return true;
        }
        if (keyCode == 262) {
            setSelectedIndex(selectedIndex + 1);
            return true;
        }
        if (keyCode == 268) {
            setSelectedIndex(0);
            return true;
        }
        if (keyCode == 269) {
            setSelectedIndex(filteredCharacters.size() - 1);
            return true;
        }
        if (keyCode == 70) {
            filtersOpen = !filtersOpen;
            return true;
        }
        if (keyCode == 257 || keyCode == 335) {
            selectCharacter();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record FrescoDimensions(int width, int height) {
    }

    private record Rect(int x, int y, int width, int height) {
        private static final Rect EMPTY = new Rect(0, 0, 0, 0);

        int right() {
            return x + width;
        }

        int bottom() {
            return y + height;
        }

        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < right() && mouseY >= y && mouseY < bottom();
        }
    }

    private record CardHit(int index, Rect bounds) {
    }

    private record FilterHit(String tag, Rect bounds) {
    }
}
