package org.example.maniacrevolution.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.example.maniacrevolution.gui.GuideTheme;
import org.example.maniacrevolution.guide.MapGuideRegistry;
import org.example.maniacrevolution.map.MapData;
import org.example.maniacrevolution.map.MapRegistry;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.PlayerVotePacket;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Responsive map vote screen using the same graphite surfaces and state accents as the
 * character, perk and settings screens.
 */
public class MapVotingScreen extends Screen {
    private static final int GAP = 6;
    private static final int HEADER_HEIGHT = 32;
    private static final int FOOTER_HEIGHT = 52;
    private static final int PANEL_HEADER_HEIGHT = 18;
    private static final int CARD_GAP = 6;
    private static final int MIN_CARD_WIDTH = 142;
    private static final int MAX_CARDS_VISIBLE = 4;
    private static final int RESULT_DISPLAY_DURATION = 200;
    private static final float TIE_REVEAL_DURATION = 2.6f;

    private final List<MapData> maps;
    private final List<CardHit> cardHits = new ArrayList<>();

    private int selectedIndex = -1;
    private int votedIndex = -1;
    private int scrollOffset;
    private int visibleCards;
    private int timeRemaining;
    private Map<String, Integer> voteCount;

    private int pageX;
    private int pageY;
    private int pageWidth;
    private int pageHeight;
    private Rect mapPanel = Rect.EMPTY;
    private Rect footerPanel = Rect.EMPTY;
    private Rect cardViewport = Rect.EMPTY;
    private Rect leftArrow = Rect.EMPTY;
    private Rect rightArrow = Rect.EMPTY;
    private Rect closeButton = Rect.EMPTY;
    private Rect confirmButton = Rect.EMPTY;

    private boolean showingResult;
    private String winnerMapId;
    private List<String> tiedMaps = new ArrayList<>();
    private int resultDisplayTime;

    public MapVotingScreen(int timeRemaining, Map<String, Integer> voteCount,
                           String playerVotedMapId) {
        super(Component.literal("Голосование за карту"));
        this.maps = new ArrayList<>(MapRegistry.getAllMaps());
        this.timeRemaining = timeRemaining;
        this.voteCount = new HashMap<>(voteCount);

        if (playerVotedMapId != null) {
            for (int index = 0; index < maps.size(); index++) {
                if (maps.get(index).getId().equals(playerVotedMapId)) {
                    votedIndex = index;
                    selectedIndex = index;
                    break;
                }
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        calculateLayout();
    }

    private void calculateLayout() {
        int maxWidth = Math.max(220, Math.min(760, width - 12));
        int maxHeight = Math.max(160, Math.min(430, height - 12));
        int minWidth = Math.min(360, maxWidth);
        int minHeight = Math.min(230, maxHeight);

        pageWidth = Mth.clamp(Math.round(width * 0.78f), minWidth, maxWidth);
        pageHeight = Mth.clamp(Math.round(height * 0.82f), minHeight, maxHeight);
        pageX = (width - pageWidth) / 2;
        pageY = (height - pageHeight) / 2;

        int panelY = pageY + HEADER_HEIGHT;
        int panelHeight = Math.max(55, pageHeight - HEADER_HEIGHT - FOOTER_HEIGHT - GAP);
        mapPanel = new Rect(pageX, panelY, pageWidth, panelHeight);
        footerPanel = new Rect(pageX, mapPanel.bottom() + GAP, pageWidth, FOOTER_HEIGHT);
        closeButton = new Rect(pageX + pageWidth - 23, pageY + 5, 18, 18);

        int innerWidth = Math.max(1, mapPanel.width() - 12);
        int possibleCards = Math.max(1,
                (innerWidth + CARD_GAP) / (MIN_CARD_WIDTH + CARD_GAP));
        visibleCards = Math.min(maps.size(), Math.min(MAX_CARDS_VISIBLE, possibleCards));

        boolean needsNavigation = maps.size() > visibleCards && visibleCards > 0;
        int navigationInset = needsNavigation ? 21 : 0;
        cardViewport = new Rect(
                mapPanel.x() + 6 + navigationInset,
                mapPanel.y() + PANEL_HEADER_HEIGHT + 6,
                Math.max(1, innerWidth - navigationInset * 2),
                Math.max(1, mapPanel.height() - PANEL_HEADER_HEIGHT - 11)
        );

        if (needsNavigation) {
            int arrowY = cardViewport.y() + cardViewport.height() / 2 - 14;
            leftArrow = new Rect(mapPanel.x() + 5, arrowY, 16, 28);
            rightArrow = new Rect(mapPanel.right() - 21, arrowY, 16, 28);
        } else {
            leftArrow = Rect.EMPTY;
            rightArrow = Rect.EMPTY;
        }

        int actionWidth = Mth.clamp(pageWidth * 39 / 100,
                Math.min(96, pageWidth), Math.min(190, pageWidth));
        confirmButton = new Rect(
                footerPanel.right() - actionWidth - 6,
                footerPanel.y() + 15,
                actionWidth,
                24
        );

        scrollOffset = Mth.clamp(scrollOffset, 0, maxScrollOffset());
        ensureSelectionVisible();
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);
        drawBackdrop(gui);
        cardHits.clear();

        if (showingResult) {
            renderResult(gui, partialTick);
        } else {
            renderVoting(gui, mouseX, mouseY);
        }
    }

    private void drawBackdrop(GuiGraphics gui) {
        gui.fillGradient(0, 0, width, height, 0xE5162028, 0xF0080B0F);
        for (int y = 2; y < height; y += 4) {
            gui.fill(0, y, width, y + 1, 0x0A000000);
        }
    }

    private void drawShell(GuiGraphics gui, int accent) {
        gui.fill(pageX - 5, pageY + 6, pageX + pageWidth + 5,
                pageY + pageHeight + 7, 0x69000000);
        gui.fill(pageX, pageY, pageX + pageWidth, pageY + pageHeight, 0xD8101720);
        gui.renderOutline(pageX, pageY, pageWidth, pageHeight, GuideTheme.BORDER_SOFT);
        gui.fill(pageX + 1, pageY + 1, pageX + pageWidth - 1, pageY + 3, accent);
    }

    private void renderVoting(GuiGraphics gui, int mouseX, int mouseY) {
        drawShell(gui, GuideTheme.RED);
        drawHeader(gui, mouseX, mouseY);
        drawMapPanel(gui, mouseX, mouseY);
        drawFooter(gui, mouseX, mouseY);
    }

    private void drawHeader(GuiGraphics gui, int mouseX, int mouseY) {
        String title = trim("ГОЛОСОВАНИЕ ЗА КАРТУ", Math.max(60, pageWidth - 190));
        gui.drawCenteredString(font, title, pageX + pageWidth / 2,
                pageY + 8, GuideTheme.TEXT);
        int underlineWidth = Math.min(108, Math.max(42, font.width(title) / 2));
        gui.fill(pageX + pageWidth / 2 - underlineWidth / 2, pageY + 21,
                pageX + pageWidth / 2 + underlineWidth / 2, pageY + 22,
                GuideTheme.RED);

        if (pageWidth >= 430) {
            String mapCount = "АРЕНЫ  •  " + maps.size();
            gui.drawString(font, mapCount, pageX + 7, pageY + 9,
                    GuideTheme.TEXT_MUTED, false);
        }

        int timerRight = closeButton.x() - 6;
        int timerWidth = 48;
        int timerX = timerRight - timerWidth;
        int timerColor = timeRemaining <= 10 ? GuideTheme.RED : GuideTheme.GOLD;
        String timerText = formatTime(timeRemaining);
        gui.fill(timerX, pageY + 6, timerRight, pageY + 24, GuideTheme.SURFACE);
        gui.renderOutline(timerX, pageY + 6, timerWidth, 18, timerColor);
        gui.drawCenteredString(font, timerText, timerX + timerWidth / 2,
                pageY + 11, timerColor);

        GuideTheme.drawButton(gui, font, closeButton.x(), closeButton.y(),
                closeButton.width(), closeButton.height(), "×", GuideTheme.RED,
                closeButton.contains(mouseX, mouseY), false);
    }

    private void drawMapPanel(GuiGraphics gui, int mouseX, int mouseY) {
        drawPanel(gui, mapPanel, "ВЫБЕРИТЕ АРЕНУ", GuideTheme.RED);

        String totalText = totalVotes() + " голосов";
        gui.drawString(font, totalText,
                mapPanel.right() - font.width(totalText) - 7,
                mapPanel.y() + 5, GuideTheme.TEXT_MUTED, false);

        if (maps.isEmpty() || visibleCards <= 0) {
            gui.drawCenteredString(font, "Нет доступных карт",
                    mapPanel.x() + mapPanel.width() / 2,
                    mapPanel.y() + mapPanel.height() / 2 - 5,
                    GuideTheme.TEXT_MUTED);
            return;
        }

        scrollOffset = Mth.clamp(scrollOffset, 0, maxScrollOffset());
        int cardWidth = Math.max(1,
                (cardViewport.width() - CARD_GAP * Math.max(0, visibleCards - 1))
                        / visibleCards);
        int endIndex = Math.min(maps.size(), scrollOffset + visibleCards);

        gui.enableScissor(cardViewport.x(), cardViewport.y(),
                cardViewport.right(), cardViewport.bottom());
        for (int index = scrollOffset; index < endIndex; index++) {
            int column = index - scrollOffset;
            Rect card = new Rect(
                    cardViewport.x() + column * (cardWidth + CARD_GAP),
                    cardViewport.y(),
                    cardWidth,
                    cardViewport.height()
            );
            boolean hovered = card.contains(mouseX, mouseY);
            drawMapCard(gui, maps.get(index), card, hovered,
                    index == selectedIndex, index == votedIndex, false);
            cardHits.add(new CardHit(index, card));
        }
        gui.disableScissor();

        if (leftArrow.width() > 0) {
            drawNavigationButton(gui, leftArrow, "‹",
                    leftArrow.contains(mouseX, mouseY), scrollOffset > 0);
            drawNavigationButton(gui, rightArrow, "›",
                    rightArrow.contains(mouseX, mouseY),
                    scrollOffset < maxScrollOffset());
        }
    }

    private void drawMapCard(GuiGraphics gui, MapData map, Rect card, boolean hovered,
                             boolean selected, boolean voted, boolean winner) {
        int accent = winner || voted ? GuideTheme.GREEN
                : selected ? GuideTheme.GOLD
                : hovered ? GuideTheme.RED
                : GuideTheme.BORDER_SOFT;
        int background = selected || voted || winner ? GuideTheme.SURFACE_SELECTED
                : hovered ? GuideTheme.SURFACE_HOVER
                : GuideTheme.SURFACE;

        gui.fill(card.x(), card.y(), card.right(), card.bottom(), background);
        gui.renderOutline(card.x(), card.y(), card.width(), card.height(), accent);
        gui.fill(card.x(), card.y(), card.x() + (selected || voted || winner ? 3 : 2),
                card.bottom(), accent);

        int imageX = card.x() + 4;
        int imageY = card.y() + 4;
        int imageWidth = Math.max(1, card.width() - 8);
        int desiredImageHeight = Math.max(1, imageWidth * 9 / 16);
        int reservedHeight = card.height() >= 170 ? 92 : 47;
        int maxImageHeight = Math.max(28, card.height() - reservedHeight);
        int minImageHeight = Math.min(54, maxImageHeight);
        int imageHeight = Mth.clamp(desiredImageHeight, minImageHeight, maxImageHeight);

        gui.fill(imageX - 1, imageY - 1, imageX + imageWidth + 1,
                imageY + imageHeight + 1, 0xFF10161C);
        try {
            RenderSystem.enableBlend();
            gui.blit(map.getLogoTexture(), imageX, imageY, 0, 0,
                    imageWidth, imageHeight, imageWidth, imageHeight);
        } catch (RuntimeException ignored) {
            gui.fill(imageX, imageY, imageX + imageWidth, imageY + imageHeight,
                    0xFF202830);
        } finally {
            RenderSystem.disableBlend();
        }
        gui.renderOutline(imageX - 1, imageY - 1,
                imageWidth + 2, imageHeight + 2, accent);

        String stateLabel = winner ? "ПОБЕДИТЕЛЬ"
                : voted ? "МОЙ ГОЛОС"
                : selected ? "ВЫБРАНО"
                : "";
        if (!stateLabel.isEmpty() && imageWidth >= 54) {
            int chipWidth = Math.min(imageWidth - 6, font.width(stateLabel) + 10);
            gui.fill(imageX + 3, imageY + 3, imageX + 3 + chipWidth,
                    imageY + 16, winner || voted ? 0xE5193823 : 0xE53D321C);
            gui.renderOutline(imageX + 3, imageY + 3, chipWidth, 13, accent);
            gui.drawCenteredString(font, trim(stateLabel, chipWidth - 5),
                    imageX + 3 + chipWidth / 2, imageY + 6,
                    winner || voted ? GuideTheme.GREEN : GuideTheme.GOLD);
        }

        int voteTextY = card.bottom() - 19;
        int titleY = imageY + imageHeight + 6;
        if (titleY < voteTextY - 7) {
            gui.drawCenteredString(font, trim(map.getName(), card.width() - 12),
                    card.x() + card.width() / 2, titleY,
                    selected || voted || winner ? GuideTheme.TEXT : GuideTheme.TEXT_SECONDARY);
        }

        MapGuideRegistry.Entry guide = MapGuideRegistry.getById(map.getId());
        int metaY = titleY + 13;
        if (guide != null && metaY < voteTextY - 8 && card.height() >= 108) {
            String facts = guide.size() + "  •  " + guide.difficultyStars();
            gui.drawCenteredString(font, trim(facts, card.width() - 12),
                    card.x() + card.width() / 2, metaY, GuideTheme.GOLD);
        }

        int descriptionY = guide == null ? metaY : metaY + 14;
        int descriptionLines = Math.max(0, (voteTextY - descriptionY - 3) / 10);
        if (card.height() >= 150 && descriptionLines > 0) {
            List<FormattedCharSequence> lines = font.split(
                    Component.literal(map.getDescription()), Math.max(1, card.width() - 12));
            int linesToDraw = Math.min(descriptionLines, lines.size());
            for (int index = 0; index < linesToDraw; index++) {
                gui.drawString(font, lines.get(index), card.x() + 6,
                        descriptionY + index * 10, GuideTheme.TEXT_MUTED, false);
            }
        }

        int votes = voteCount.getOrDefault(map.getId(), 0);
        String votesText = votes + " голосов";
        gui.drawString(font, trim(votesText, Math.max(1, card.width() - 20)),
                card.x() + 6, voteTextY, winner || voted ? GuideTheme.GREEN
                        : selected ? GuideTheme.GOLD : GuideTheme.TEXT_SECONDARY, false);

        String numberText = "#" + map.getNumericId();
        gui.drawString(font, numberText,
                card.right() - font.width(numberText) - 6, voteTextY,
                GuideTheme.TEXT_MUTED, false);

        int trackX = card.x() + 4;
        int trackWidth = Math.max(1, card.width() - 8);
        gui.fill(trackX, card.bottom() - 5, trackX + trackWidth,
                card.bottom() - 3, 0xFF202830);
        int total = totalVotes();
        if (votes > 0 && total > 0) {
            int fillWidth = Math.max(1, Math.round(trackWidth * (votes / (float) total)));
            gui.fill(trackX, card.bottom() - 5, trackX + fillWidth,
                    card.bottom() - 3, accent);
        }
    }

    private void drawFooter(GuiGraphics gui, int mouseX, int mouseY) {
        gui.fill(footerPanel.x(), footerPanel.y(), footerPanel.right(),
                footerPanel.bottom(), GuideTheme.PANEL);
        gui.renderOutline(footerPanel.x(), footerPanel.y(),
                footerPanel.width(), footerPanel.height(), GuideTheme.BORDER_SOFT);
        gui.fill(footerPanel.x() + 1, footerPanel.y() + 1,
                footerPanel.right() - 1, footerPanel.y() + 3, GuideTheme.RED);

        boolean pending = hasPendingVote();
        MapData selected = mapAt(selectedIndex);
        MapData voted = mapAt(votedIndex);
        String statusTitle;
        String statusSubtitle;
        int statusColor;

        if (pending && selected != null) {
            statusTitle = "ВЫБРАНО: " + selected.getName();
            statusSubtitle = voted == null
                    ? "Подтвердите свой голос"
                    : "Текущий голос: " + voted.getName();
            statusColor = GuideTheme.GOLD;
        } else if (voted != null) {
            statusTitle = "ГОЛОС ПРИНЯТ: " + voted.getName();
            statusSubtitle = "Можно выбрать другую карту";
            statusColor = GuideTheme.GREEN;
        } else {
            statusTitle = "КАРТА НЕ ВЫБРАНА";
            statusSubtitle = "Нажмите на карточку арены";
            statusColor = GuideTheme.GOLD;
        }

        int textX = footerPanel.x() + 8;
        int textWidth = Math.max(1, confirmButton.x() - textX - 8);
        gui.drawString(font, trim(statusTitle, textWidth), textX,
                footerPanel.y() + 13, statusColor, false);
        gui.drawString(font, trim(statusSubtitle, textWidth), textX,
                footerPanel.y() + 27, GuideTheme.TEXT_MUTED, false);

        String buttonLabel = pending
                ? votedIndex >= 0 ? "ИЗМЕНИТЬ ГОЛОС" : "ПОДТВЕРДИТЬ"
                : votedIndex >= 0 ? "ГОЛОС ПРИНЯТ" : "ВЫБЕРИТЕ КАРТУ";
        drawConfirmButton(gui, buttonLabel,
                confirmButton.contains(mouseX, mouseY), pending);
    }

    private void renderResult(GuiGraphics gui, float partialTick) {
        float elapsed = (resultDisplayTime + partialTick) / 20.0f;
        boolean resolvingTie = tiedMaps.size() > 1 && elapsed < TIE_REVEAL_DURATION;

        drawShell(gui, resolvingTie ? GuideTheme.GOLD : GuideTheme.GREEN);

        String title = resolvingTie ? "НИЧЬЯ — ЖЕРЕБЬЁВКА" : "ГОЛОСОВАНИЕ ЗАВЕРШЕНО";
        gui.drawCenteredString(font, trim(title, pageWidth - 30),
                pageX + pageWidth / 2, pageY + 8, GuideTheme.TEXT);
        int underlineWidth = Math.min(116, Math.max(42, font.width(title) / 2));
        int headerAccent = resolvingTie ? GuideTheme.GOLD : GuideTheme.GREEN;
        gui.fill(pageX + pageWidth / 2 - underlineWidth / 2, pageY + 21,
                pageX + pageWidth / 2 + underlineWidth / 2, pageY + 22,
                headerAccent);

        drawPanel(gui, mapPanel,
                resolvingTie ? "СИСТЕМА ВЫБИРАЕТ АРЕНУ" : "ПОБЕДИТЕЛЬ",
                headerAccent);

        MapData displayed = resolvingTie ? currentTieCandidate(elapsed)
                : MapRegistry.getMapById(winnerMapId);
        if (displayed != null) {
            int maxCardWidth = Math.max(1, mapPanel.width() - 28);
            int cardWidth = Math.min(272, maxCardWidth);
            int cardHeight = Math.max(1,
                    mapPanel.height() - PANEL_HEADER_HEIGHT - 14);
            Rect resultCard = new Rect(
                    mapPanel.x() + (mapPanel.width() - cardWidth) / 2,
                    mapPanel.y() + PANEL_HEADER_HEIGHT + 7,
                    cardWidth,
                    cardHeight
            );
            drawMapCard(gui, displayed, resultCard, false,
                    false, false, !resolvingTie);
        } else {
            gui.drawCenteredString(font, "Результат недоступен",
                    mapPanel.x() + mapPanel.width() / 2,
                    mapPanel.y() + mapPanel.height() / 2,
                    GuideTheme.TEXT_MUTED);
        }

        gui.fill(footerPanel.x(), footerPanel.y(), footerPanel.right(),
                footerPanel.bottom(), GuideTheme.PANEL);
        gui.renderOutline(footerPanel.x(), footerPanel.y(),
                footerPanel.width(), footerPanel.height(), GuideTheme.BORDER_SOFT);
        gui.fill(footerPanel.x() + 1, footerPanel.y() + 1,
                footerPanel.right() - 1, footerPanel.y() + 3, headerAccent);

        if (resolvingTie) {
            int finalists = tiedMaps.size();
            gui.drawString(font, "ФИНАЛИСТОВ: " + finalists,
                    footerPanel.x() + 8, footerPanel.y() + 14,
                    GuideTheme.GOLD, false);
            gui.drawString(font, "Победитель определится автоматически",
                    footerPanel.x() + 8, footerPanel.y() + 28,
                    GuideTheme.TEXT_MUTED, false);

            float progress = Mth.clamp(elapsed / TIE_REVEAL_DURATION, 0.0f, 1.0f);
            int barX = footerPanel.x() + Math.max(8, footerPanel.width() / 2);
            int barWidth = Math.max(1, footerPanel.right() - barX - 8);
            gui.fill(barX, footerPanel.y() + 23, barX + barWidth,
                    footerPanel.y() + 26, 0xFF202830);
            gui.fill(barX, footerPanel.y() + 23,
                    barX + Math.max(1, Math.round(barWidth * progress)),
                    footerPanel.y() + 26, GuideTheme.GOLD);
        } else {
            MapData winner = MapRegistry.getMapById(winnerMapId);
            String winnerText = winner == null
                    ? "ПОБЕДИТЕЛЬ НЕ ОПРЕДЕЛЁН"
                    : "ПОБЕДИТЕЛЬ: " + winner.getName();
            gui.drawString(font, trim(winnerText, footerPanel.width() - 110),
                    footerPanel.x() + 8, footerPanel.y() + 14,
                    winner == null ? GuideTheme.TEXT_MUTED : GuideTheme.GREEN, false);
            gui.drawString(font, winner == null
                            ? "Нет данных о результате"
                            : voteCount.getOrDefault(winner.getId(), 0) + " голосов",
                    footerPanel.x() + 8, footerPanel.y() + 28,
                    GuideTheme.TEXT_MUTED, false);

            int seconds = Math.max(0,
                    (int) Math.ceil((RESULT_DISPLAY_DURATION - resultDisplayTime) / 20.0));
            String closing = "Закрытие через " + seconds;
            gui.drawString(font, closing,
                    footerPanel.right() - font.width(closing) - 9,
                    footerPanel.y() + 21, GuideTheme.TEXT_MUTED, false);
        }
    }

    private MapData currentTieCandidate(float elapsed) {
        if (tiedMaps.isEmpty()) {
            return MapRegistry.getMapById(winnerMapId);
        }
        float progress = Mth.clamp(elapsed / TIE_REVEAL_DURATION, 0.0f, 1.0f);
        float eased = 1.0f - (1.0f - progress) * (1.0f - progress);
        int winnerTieIndex = Math.max(0, tiedMaps.indexOf(winnerMapId));
        int totalSteps = tiedMaps.size() * 5 + winnerTieIndex;
        int step = Math.min(totalSteps, (int) Math.floor(eased * totalSteps));
        String id = tiedMaps.get(step % tiedMaps.size());
        return MapRegistry.getMapById(id);
    }

    private void drawPanel(GuiGraphics gui, Rect panel, String title, int accent) {
        gui.fill(panel.x(), panel.y(), panel.right(), panel.bottom(), GuideTheme.PANEL);
        gui.renderOutline(panel.x(), panel.y(), panel.width(), panel.height(),
                GuideTheme.BORDER_SOFT);
        gui.fill(panel.x() + 1, panel.y() + 1, panel.right() - 1,
                panel.y() + PANEL_HEADER_HEIGHT - 2, GuideTheme.HEADER);
        gui.drawCenteredString(font, trim(title, Math.max(1, panel.width() - 110)),
                panel.x() + panel.width() / 2, panel.y() + 5,
                GuideTheme.TEXT_SECONDARY);
        gui.fill(panel.x() + 1, panel.y() + PANEL_HEADER_HEIGHT - 2,
                panel.right() - 1, panel.y() + PANEL_HEADER_HEIGHT - 1, accent);
    }

    private void drawNavigationButton(GuiGraphics gui, Rect bounds, String label,
                                      boolean hovered, boolean active) {
        if (!active) {
            gui.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), 0xE5222930);
            gui.renderOutline(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                    GuideTheme.BORDER_SOFT);
            gui.drawCenteredString(font, label, bounds.x() + bounds.width() / 2,
                    bounds.y() + (bounds.height() - 8) / 2, GuideTheme.TEXT_MUTED);
            return;
        }
        GuideTheme.drawButton(gui, font, bounds.x(), bounds.y(),
                bounds.width(), bounds.height(), label, GuideTheme.RED, hovered, false);
    }

    private void drawConfirmButton(GuiGraphics gui, String label,
                                   boolean hovered, boolean active) {
        String clipped = trim(label, Math.max(1, confirmButton.width() - 8));
        if (!active) {
            gui.fill(confirmButton.x(), confirmButton.y(), confirmButton.right(),
                    confirmButton.bottom(), 0xE5222930);
            gui.renderOutline(confirmButton.x(), confirmButton.y(),
                    confirmButton.width(), confirmButton.height(),
                    votedIndex >= 0 ? GuideTheme.GREEN : GuideTheme.BORDER_SOFT);
            gui.drawCenteredString(font, clipped,
                    confirmButton.x() + confirmButton.width() / 2,
                    confirmButton.y() + (confirmButton.height() - 8) / 2,
                    votedIndex >= 0 ? GuideTheme.GREEN : GuideTheme.TEXT_MUTED);
            return;
        }

        int background = hovered
                ? GuideTheme.lerpColor(GuideTheme.RED, 0xFFFFFFFF, 0.12f)
                : GuideTheme.RED;
        int border = hovered ? GuideTheme.TEXT
                : GuideTheme.lerpColor(GuideTheme.RED, 0xFFFFFFFF, 0.28f);
        gui.fill(confirmButton.x(), confirmButton.y(), confirmButton.right(),
                confirmButton.bottom(), background);
        gui.renderOutline(confirmButton.x(), confirmButton.y(),
                confirmButton.width(), confirmButton.height(), border);
        gui.drawCenteredString(font, clipped,
                confirmButton.x() + confirmButton.width() / 2,
                confirmButton.y() + (confirmButton.height() - 8) / 2,
                GuideTheme.TEXT);
        if (hovered) {
            gui.fill(confirmButton.x() + 1, confirmButton.bottom() - 2,
                    confirmButton.right() - 1, confirmButton.bottom() - 1,
                    GuideTheme.TEXT);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showingResult || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (closeButton.contains(mouseX, mouseY)) {
            playClick(0.92f);
            onClose();
            return true;
        }
        if (leftArrow.contains(mouseX, mouseY) && scrollOffset > 0) {
            scrollOffset--;
            playClick(0.86f);
            return true;
        }
        if (rightArrow.contains(mouseX, mouseY) && scrollOffset < maxScrollOffset()) {
            scrollOffset++;
            playClick(1.06f);
            return true;
        }
        for (CardHit hit : cardHits) {
            if (hit.bounds().contains(mouseX, mouseY)) {
                if (selectedIndex == hit.index() && selectedIndex != votedIndex) {
                    selectedIndex = votedIndex;
                } else if (selectedIndex == hit.index() && votedIndex < 0) {
                    selectedIndex = -1;
                } else {
                    selectedIndex = hit.index();
                }
                playClick(selectedIndex == votedIndex ? 0.9f : 1.08f);
                return true;
            }
        }
        if (confirmButton.contains(mouseX, mouseY) && hasPendingVote()) {
            onSelectMap();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!showingResult && mapPanel.contains(mouseX, mouseY) && maxScrollOffset() > 0) {
            int next = scrollOffset + (delta < 0 ? 1 : -1);
            scrollOffset = Mth.clamp(next, 0, maxScrollOffset());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!showingResult && !maps.isEmpty()) {
            if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT) {
                int direction = keyCode == GLFW.GLFW_KEY_RIGHT ? 1 : -1;
                int start = selectedIndex >= 0 ? selectedIndex
                        : votedIndex >= 0 ? votedIndex : direction > 0 ? -1 : maps.size();
                selectedIndex = Mth.clamp(start + direction, 0, maps.size() - 1);
                ensureSelectionVisible();
                playClick(direction > 0 ? 1.04f : 0.88f);
                return true;
            }
            if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER
                    || keyCode == GLFW.GLFW_KEY_SPACE) && hasPendingVote()) {
                onSelectMap();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void onSelectMap() {
        MapData selected = mapAt(selectedIndex);
        if (selected == null || selectedIndex == votedIndex) {
            return;
        }

        ModNetworking.sendToServer(new PlayerVotePacket(selected.getId(), false));
        votedIndex = selectedIndex;
        playClick(1.16f);
    }

    private void ensureSelectionVisible() {
        if (selectedIndex < 0 || visibleCards <= 0) {
            return;
        }
        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= scrollOffset + visibleCards) {
            scrollOffset = selectedIndex - visibleCards + 1;
        }
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScrollOffset());
    }

    private boolean hasPendingVote() {
        return selectedIndex >= 0 && selectedIndex < maps.size()
                && selectedIndex != votedIndex;
    }

    private int maxScrollOffset() {
        return Math.max(0, maps.size() - visibleCards);
    }

    private int totalVotes() {
        int total = 0;
        for (int count : voteCount.values()) {
            total += Math.max(0, count);
        }
        return total;
    }

    private MapData mapAt(int index) {
        return index >= 0 && index < maps.size() ? maps.get(index) : null;
    }

    private String formatTime(int secondsRemaining) {
        int safeSeconds = Math.max(0, secondsRemaining);
        return String.format("%02d:%02d", safeSeconds / 60, safeSeconds % 60);
    }

    private String trim(String text, int maxWidth) {
        if (maxWidth <= 6 || font.width(text) <= maxWidth) {
            return text;
        }
        return font.plainSubstrByWidth(text,
                Math.max(1, maxWidth - font.width("…"))) + "…";
    }

    private void playClick(float pitch) {
        if (minecraft != null) {
            minecraft.getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (showingResult) {
            resultDisplayTime++;
            if (resultDisplayTime >= RESULT_DISPLAY_DURATION) {
                onClose();
            }
        }
    }

    public void updateVoting(int timeRemaining, Map<String, Integer> voteCount) {
        this.timeRemaining = timeRemaining;
        this.voteCount = new HashMap<>(voteCount);
    }

    public void showResult(String winnerMapId, Map<String, Integer> finalVoteCount) {
        this.winnerMapId = winnerMapId;
        this.voteCount = new HashMap<>(finalVoteCount);
        this.showingResult = true;
        this.resultDisplayTime = 0;

        if (finalVoteCount.isEmpty()) {
            tiedMaps = new ArrayList<>();
            for (MapData map : maps) {
                tiedMaps.add(map.getId());
            }
        } else {
            int maxVotes = Integer.MIN_VALUE;
            for (int count : finalVoteCount.values()) {
                maxVotes = Math.max(maxVotes, count);
            }
            tiedMaps = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : finalVoteCount.entrySet()) {
                if (entry.getValue() == maxVotes) {
                    tiedMaps.add(entry.getKey());
                }
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record CardHit(int index, Rect bounds) {}

    private record Rect(int x, int y, int width, int height) {
        private static final Rect EMPTY = new Rect(0, 0, 0, 0);

        int right() {
            return x + width;
        }

        int bottom() {
            return y + height;
        }

        boolean contains(double mouseX, double mouseY) {
            return width > 0 && height > 0
                    && mouseX >= x && mouseX < right()
                    && mouseY >= y && mouseY < bottom();
        }
    }
}
