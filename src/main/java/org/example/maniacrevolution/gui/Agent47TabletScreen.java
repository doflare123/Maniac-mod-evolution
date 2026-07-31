package org.example.maniacrevolution.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.Agent47PurchasePacket;
import org.example.maniacrevolution.network.packets.Agent47RequestDataPacket;
import org.example.maniacrevolution.system.Agent47ShopConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Responsive contract terminal for Agent 47. The graphite panels, restrained accent colors and
 * compact cards intentionally match the perk, character and settings screens.
 */
@OnlyIn(Dist.CLIENT)
public class Agent47TabletScreen extends Screen {
    private static final int GAP = 6;
    private static final int HEADER_HEIGHT = 31;
    private static final int NAV_HEIGHT = 23;
    private static final int PANEL_HEADER_HEIGHT = 18;
    private static final int SHOP_CARD_HEIGHT = 62;
    private static final int SHOP_CARD_GAP = 4;

    private static final int ACCENT = 0xFFE35B64;
    private static final int ACCENT_DARK = 0xFF7A2B32;
    private static final int TERMINAL_GREEN = 0xFF70E28A;

    private int pageX;
    private int pageY;
    private int pageWidth;
    private int pageHeight;

    private Rect targetTab = Rect.EMPTY;
    private Rect shopTab = Rect.EMPTY;
    private Rect closeButton = Rect.EMPTY;
    private Rect contentPanel = Rect.EMPTY;
    private Rect shopViewport = Rect.EMPTY;

    private String currentTarget = "Нет цели";
    private int playerMoney;
    private List<Agent47ShopConfig.ShopItem> shopItems = new ArrayList<>();

    private final List<ShopHit> shopHits = new ArrayList<>();
    private DisplayMode currentMode = DisplayMode.TARGET_INFO;
    private int shopScroll;
    private int shopScrollMax;

    private enum DisplayMode {
        TARGET_INFO,
        SHOP
    }

    public Agent47TabletScreen() {
        super(Component.literal("Планшет Агента 47"));
    }

    @Override
    protected void init() {
        super.init();
        calculateLayout();
        ModNetworking.CHANNEL.sendToServer(new Agent47RequestDataPacket());
    }

    private void calculateLayout() {
        int availableWidth = Math.max(1, width - 12);
        int availableHeight = Math.max(1, height - 12);
        int maxWidth = Math.min(660, availableWidth);
        int maxHeight = Math.min(390, availableHeight);
        int minWidth = Math.min(300, maxWidth);
        int minHeight = Math.min(220, maxHeight);

        pageWidth = Mth.clamp(Math.round(width * 0.78f), minWidth, maxWidth);
        pageHeight = Mth.clamp(Math.round(height * 0.84f), minHeight, maxHeight);
        pageX = (width - pageWidth) / 2;
        pageY = (height - pageHeight) / 2;

        closeButton = new Rect(pageX + pageWidth - 23, pageY + 6, 18, 18);

        int navX = pageX + 5;
        int navY = pageY + HEADER_HEIGHT;
        int navWidth = Math.max(1, pageWidth - 10);
        int tabWidth = Math.max(1, (navWidth - GAP) / 2);
        targetTab = new Rect(navX, navY, tabWidth, NAV_HEIGHT);
        shopTab = new Rect(targetTab.right() + GAP, navY,
                Math.max(1, navX + navWidth - targetTab.right() - GAP), NAV_HEIGHT);

        int contentY = navY + NAV_HEIGHT + GAP;
        contentPanel = new Rect(pageX, contentY, pageWidth,
                Math.max(1, pageY + pageHeight - contentY));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        drawBackdrop(graphics);
        drawHeader(graphics, mouseX, mouseY);
        drawNavigation(graphics, mouseX, mouseY);

        shopHits.clear();
        if (currentMode == DisplayMode.TARGET_INFO) {
            drawTargetDossier(graphics);
        } else {
            drawShop(graphics, mouseX, mouseY);
        }
    }

    private void drawBackdrop(GuiGraphics graphics) {
        graphics.fillGradient(0, 0, width, height, 0xEE0D1116, 0xFA030507);
        graphics.fill(pageX, pageY, pageX + pageWidth, pageY + pageHeight, 0xD00D1218);
        graphics.renderOutline(pageX, pageY, pageWidth, pageHeight, GuideTheme.BORDER_SOFT);

        for (int lineX = pageX + 32; lineX < pageX + pageWidth; lineX += 32) {
            graphics.fill(lineX, pageY + HEADER_HEIGHT, lineX + 1,
                    pageY + pageHeight, 0x12151D25);
        }
        for (int lineY = pageY + HEADER_HEIGHT + 24; lineY < pageY + pageHeight; lineY += 24) {
            graphics.fill(pageX + 1, lineY, pageX + pageWidth - 1,
                    lineY + 1, 0x10151D25);
        }

        drawCorner(graphics, pageX, pageY, 1, 1);
        drawCorner(graphics, pageX + pageWidth - 1, pageY, -1, 1);
        drawCorner(graphics, pageX, pageY + pageHeight - 1, 1, -1);
        drawCorner(graphics, pageX + pageWidth - 1, pageY + pageHeight - 1, -1, -1);
    }

    private void drawHeader(GuiGraphics graphics, int mouseX, int mouseY) {
        int badgeX = pageX + 6;
        int badgeY = pageY + 5;
        graphics.fill(badgeX, badgeY, badgeX + 21, badgeY + 21, 0xE525171B);
        graphics.renderOutline(badgeX, badgeY, 21, 21, ACCENT);
        graphics.drawCenteredString(font, "47", badgeX + 10, badgeY + 7, GuideTheme.TEXT);

        int balanceWidth = Mth.clamp(font.width(Integer.toString(playerMoney)) + 49, 72, 110);
        int balanceX = closeButton.x() - balanceWidth - 5;
        int titleX = badgeX + 27;
        int titleWidth = Math.max(1, balanceX - titleX - 6);
        graphics.drawString(font, trim("УПРАВЛЕНИЕ СПЕЦОПЕРАЦИЙ", titleWidth),
                titleX, pageY + 6, GuideTheme.TEXT, false);
        if (pageWidth >= 420) {
            graphics.drawString(font, trim("АГЕНТ 47  //  КАНАЛ ЗАШИФРОВАН", titleWidth),
                    titleX, pageY + 17, GuideTheme.TEXT_MUTED, false);
        } else {
            graphics.fill(titleX, pageY + 19, Math.min(balanceX - 5, titleX + 70),
                    pageY + 20, ACCENT);
        }

        graphics.fill(balanceX, pageY + 7, balanceX + balanceWidth, pageY + 24,
                GuideTheme.SURFACE);
        graphics.renderOutline(balanceX, pageY + 7, balanceWidth, 17, GuideTheme.GOLD);
        graphics.drawString(font, "МОНЕТЫ", balanceX + 5, pageY + 12,
                GuideTheme.TEXT_MUTED, false);
        String balance = Integer.toString(playerMoney);
        graphics.drawString(font, balance, balanceX + balanceWidth - font.width(balance) - 5,
                pageY + 12, GuideTheme.GOLD, false);

        drawButton(graphics, closeButton, "×", GuideTheme.RED,
                closeButton.contains(mouseX, mouseY), false);
    }

    private void drawNavigation(GuiGraphics graphics, int mouseX, int mouseY) {
        drawTab(graphics, targetTab, "ДОСЬЕ", currentMode == DisplayMode.TARGET_INFO,
                targetTab.contains(mouseX, mouseY));
        drawTab(graphics, shopTab, "АРСЕНАЛ  //  " + shopItems.size(), currentMode == DisplayMode.SHOP,
                shopTab.contains(mouseX, mouseY));
    }

    private void drawTab(GuiGraphics graphics, Rect bounds, String label,
                         boolean selected, boolean hovered) {
        int background = selected ? GuideTheme.SURFACE_SELECTED
                : hovered ? GuideTheme.SURFACE_HOVER : GuideTheme.SURFACE;
        int border = selected || hovered ? ACCENT : GuideTheme.BORDER_SOFT;
        graphics.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), background);
        graphics.renderOutline(bounds.x(), bounds.y(), bounds.width(), bounds.height(), border);
        if (selected) {
            graphics.fill(bounds.x() + 1, bounds.bottom() - 3,
                    bounds.right() - 1, bounds.bottom() - 1, ACCENT);
        }
        graphics.drawCenteredString(font, trim(label, Math.max(1, bounds.width() - 8)),
                bounds.x() + bounds.width() / 2, bounds.y() + 7,
                selected || hovered ? GuideTheme.TEXT : GuideTheme.TEXT_SECONDARY);
    }

    private void drawTargetDossier(GuiGraphics graphics) {
        drawPanel(graphics, contentPanel, "ЛИЧНОЕ ДЕЛО  //  ОБЪЕКТ ОПЕРАЦИИ", ACCENT);

        Rect dossier = new Rect(contentPanel.x() + 6,
                contentPanel.y() + PANEL_HEADER_HEIGHT + 6,
                Math.max(1, contentPanel.width() - 12),
                Math.max(1, contentPanel.height() - PANEL_HEADER_HEIGHT - 12));
        boolean hasTarget = hasTarget();
        int primaryHeight = Math.min(164, dossier.height());
        int photoWidth = Mth.clamp(dossier.width() * 30 / 100,
                Math.min(84, dossier.width()), 126);
        Rect photo = new Rect(dossier.x(), dossier.y(), photoWidth, primaryHeight);
        Rect information = new Rect(photo.right() + GAP, dossier.y(),
                Math.max(1, dossier.right() - photo.right() - GAP), primaryHeight);

        int directiveHeight = Math.min(45, Math.max(38, primaryHeight / 3));
        Rect personalData = new Rect(information.x(), information.y(), information.width(),
                Math.max(1, information.height() - directiveHeight - GAP));
        Rect directive = new Rect(information.x(), personalData.bottom() + GAP,
                information.width(), Math.max(1, information.bottom() - personalData.bottom() - GAP));

        drawSubjectPhoto(graphics, photo, hasTarget);
        drawPersonalData(graphics, personalData, hasTarget);
        drawDirective(graphics, directive, hasTarget);

        int briefingY = dossier.y() + primaryHeight + GAP;
        if (briefingY + 34 <= dossier.bottom()) {
            drawOperationalBriefing(graphics, new Rect(dossier.x(), briefingY,
                    dossier.width(), dossier.bottom() - briefingY), hasTarget);
        }
    }

    private void drawSubjectPhoto(GuiGraphics graphics, Rect bounds, boolean active) {
        graphics.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), 0xF0080B0F);
        graphics.renderOutline(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                active ? ACCENT : GuideTheme.BORDER_SOFT);
        graphics.fill(bounds.x() + 1, bounds.y() + 1, bounds.right() - 1,
                bounds.y() + 17, GuideTheme.HEADER);
        graphics.drawString(font, "ФОТО // LIVE", bounds.x() + 5, bounds.y() + 5,
                active ? TERMINAL_GREEN : GuideTheme.TEXT_MUTED, false);

        int imageSize = Math.max(16, Math.min(bounds.width() - 12, bounds.height() - 42));
        int imageX = bounds.x() + (bounds.width() - imageSize) / 2;
        int imageY = bounds.y() + 21;
        graphics.fill(imageX, imageY, imageX + imageSize, imageY + imageSize, 0xFF111820);
        graphics.renderOutline(imageX, imageY, imageSize, imageSize, GuideTheme.BORDER_SOFT);

        PlayerInfo playerInfo = active ? findTargetPlayerInfo() : null;
        if (playerInfo != null) {
            ResourceLocation skin = playerInfo.getSkinLocation();
            RenderSystem.enableBlend();
            graphics.blit(skin, imageX + 2, imageY + 2, imageSize - 4, imageSize - 4,
                    8, 8, 8, 8, 64, 64);
            graphics.blit(skin, imageX + 2, imageY + 2, imageSize - 4, imageSize - 4,
                    40, 8, 8, 8, 64, 64);
            RenderSystem.disableBlend();
            for (int scanY = imageY + 3; scanY < imageY + imageSize - 2; scanY += 4) {
                graphics.fill(imageX + 2, scanY, imageX + imageSize - 2,
                        scanY + 1, 0x16000000);
            }
        } else {
            int markSize = Math.max(12, imageSize - 14);
            drawTargetMark(graphics, imageX + (imageSize - markSize) / 2,
                    imageY + (imageSize - markSize) / 2, markSize,
                    active ? ACCENT : GuideTheme.TEXT_MUTED);
        }

        String fileNumber = active ? dossierNumber() : "MR-0000-00";
        graphics.drawCenteredString(font, trim(fileNumber, Math.max(1, bounds.width() - 8)),
                bounds.x() + bounds.width() / 2, bounds.bottom() - 14,
                active ? GuideTheme.TEXT_SECONDARY : GuideTheme.TEXT_MUTED);
    }

    private void drawPersonalData(GuiGraphics graphics, Rect bounds, boolean active) {
        graphics.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), GuideTheme.SURFACE);
        graphics.renderOutline(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                GuideTheme.BORDER_SOFT);
        graphics.fill(bounds.x() + 1, bounds.y() + 1, bounds.right() - 1,
                bounds.y() + 17, GuideTheme.HEADER);
        graphics.drawString(font, trim("АНКЕТА ОБЪЕКТА", Math.max(1, bounds.width() - 84)),
                bounds.x() + 6, bounds.y() + 5, GuideTheme.TEXT_SECONDARY, false);

        String stamp = "СЕКРЕТНО";
        int stampWidth = font.width(stamp) + 8;
        if (stampWidth + 12 < bounds.width()) {
            int stampX = bounds.right() - stampWidth - 5;
            graphics.renderOutline(stampX, bounds.y() + 3, stampWidth, 13, ACCENT);
            graphics.drawCenteredString(font, stamp, stampX + stampWidth / 2,
                    bounds.y() + 6, ACCENT);
        }

        int y = bounds.y() + 22;
        y = drawDossierField(graphics, bounds, "ИМЯ / ФАМИЛИЯ",
                active ? plain(currentTarget) : "НЕ УСТАНОВЛЕНО", y,
                active ? GuideTheme.TEXT : GuideTheme.TEXT_MUTED);
        if (y + 8 < bounds.bottom()) {
            y = drawDossierField(graphics, bounds, "СТАТУС",
                    active ? "ПОД НАБЛЮДЕНИЕМ" : "ОЖИДАНИЕ", y,
                    active ? TERMINAL_GREEN : GuideTheme.TEXT_MUTED);
        }
        if (y + 8 < bounds.bottom()) {
            y = drawDossierField(graphics, bounds, "СЕМ. ПОЛОЖЕНИЕ",
                    active ? "НЕ ЖЕНАТ" : "Н/Д", y, GuideTheme.TEXT_SECONDARY);
        }
        if (y + 8 < bounds.bottom()) {
            y = drawDossierField(graphics, bounds, "ХАРАКТЕР",
                    active ? "СКВЕРНЫЙ" : "Н/Д", y, active ? GuideTheme.GOLD : GuideTheme.TEXT_MUTED);
        }
        if (y + 8 < bounds.bottom()) {
            drawDossierField(graphics, bounds, "УРОВЕНЬ УГРОЗЫ",
                    active ? "ВЫСОКИЙ" : "НЕИЗВЕСТЕН", y,
                    active ? ACCENT : GuideTheme.TEXT_MUTED);
        }
    }

    private int drawDossierField(GuiGraphics graphics, Rect bounds, String label,
                                 String value, int y, int valueColor) {
        int innerX = bounds.x() + 6;
        int innerWidth = Math.max(1, bounds.width() - 12);
        int labelWidth = Mth.clamp(innerWidth * 44 / 100,
                Math.min(56, innerWidth), 94);
        graphics.drawString(font, trim(label, Math.max(1, labelWidth - 4)),
                innerX, y, GuideTheme.TEXT_MUTED, false);
        int valueX = innerX + labelWidth;
        graphics.drawString(font, trim(value, Math.max(1, bounds.right() - valueX - 6)),
                valueX, y, valueColor, false);
        graphics.fill(innerX, y + 9, bounds.right() - 6, y + 10, 0x394C5864);
        return y + 12;
    }

    private void drawDirective(GuiGraphics graphics, Rect bounds, boolean active) {
        graphics.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), 0xF0191215);
        graphics.renderOutline(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                active ? ACCENT : GuideTheme.BORDER_SOFT);
        int rewardWidth = Mth.clamp(bounds.width() * 30 / 100,
                Math.min(62, bounds.width()), 96);
        graphics.fill(bounds.x() + rewardWidth, bounds.y() + 4,
                bounds.x() + rewardWidth + 1, bounds.bottom() - 4, ACCENT_DARK);

        graphics.drawCenteredString(font, "ГОНОРАР", bounds.x() + rewardWidth / 2,
                bounds.y() + 6, GuideTheme.TEXT_MUTED);
        graphics.drawCenteredString(font, "+" + Agent47ShopConfig.KILL_TARGET_REWARD + " М",
                bounds.x() + rewardWidth / 2, bounds.y() + 19,
                active ? GuideTheme.GOLD : GuideTheme.TEXT_MUTED);

        int textX = bounds.x() + rewardWidth + 7;
        int textWidth = Math.max(1, bounds.right() - textX - 5);
        graphics.drawString(font, "ДИРЕКТИВА", textX, bounds.y() + 6,
                GuideTheme.TEXT_MUTED, false);
        graphics.drawString(font, trim(active ? "ЛИКВИДИРОВАТЬ ОБЪЕКТ" : "ОЖИДАТЬ НАЗНАЧЕНИЯ",
                        textWidth), textX, bounds.y() + 19,
                active ? ACCENT : GuideTheme.TEXT_SECONDARY, false);
    }

    private void drawOperationalBriefing(GuiGraphics graphics, Rect bounds, boolean active) {
        graphics.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), GuideTheme.SURFACE);
        graphics.renderOutline(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                GuideTheme.BORDER_SOFT);
        graphics.fill(bounds.x() + 1, bounds.y() + 1, bounds.right() - 1,
                bounds.y() + 17, GuideTheme.HEADER);
        graphics.drawString(font, "ОПЕРАТИВНАЯ СВОДКА", bounds.x() + 6, bounds.y() + 5,
                GuideTheme.TEXT_SECONDARY, false);

        int textX = bounds.x() + 7;
        int textY = bounds.y() + 23;
        int textWidth = Math.max(1, bounds.width() - 14);
        String briefing = active
                ? "Объект подтверждён и находится в зоне операции. Ликвидировать без привлечения внимания. После исполнения новая цель будет назначена автоматически."
                : "Канал открыт. Ожидайте назначения объекта; досье и оперативная директива обновятся автоматически.";
        textY = drawWrapped(graphics, briefing, textX, textY, textWidth,
                Math.max(1, (bounds.bottom() - textY - 15) / 10), GuideTheme.TEXT_SECONDARY);
        if (textY + 10 < bounds.bottom()) {
            graphics.drawString(font, trim(active ? "ПРОТОКОЛ: ЧИСТАЯ РАБОТА // СВИДЕТЕЛИ НЕЖЕЛАТЕЛЬНЫ"
                            : "ПРОТОКОЛ: СОХРАНЯТЬ РЕЖИМ ОЖИДАНИЯ", textWidth),
                    textX, bounds.bottom() - 13, active ? TERMINAL_GREEN : GuideTheme.TEXT_MUTED, false);
        }
    }

    private PlayerInfo findTargetPlayerInfo() {
        if (minecraft == null || minecraft.getConnection() == null) {
            return null;
        }
        String targetName = plain(currentTarget);
        for (PlayerInfo playerInfo : minecraft.getConnection().getListedOnlinePlayers()) {
            if (playerInfo.getProfile().getName().equalsIgnoreCase(targetName)) {
                return playerInfo;
            }
        }
        return null;
    }

    private String dossierNumber() {
        int hash = plain(currentTarget).toLowerCase(Locale.ROOT).hashCode();
        return String.format(Locale.ROOT, "MR-%04X-%02d",
                hash & 0xFFFF, Math.floorMod(hash, 100));
    }

    private void drawShop(GuiGraphics graphics, int mouseX, int mouseY) {
        drawPanel(graphics, contentPanel, "АРСЕНАЛ И ПОЛЕВОЕ СНАРЯЖЕНИЕ", ACCENT);
        String count = shopItems.size() + " поз.";
        graphics.drawString(font, count,
                contentPanel.right() - font.width(count) - 7,
                contentPanel.y() + 5, GuideTheme.TEXT_MUTED, false);

        shopViewport = new Rect(contentPanel.x() + 6,
                contentPanel.y() + PANEL_HEADER_HEIGHT + 6,
                Math.max(1, contentPanel.width() - 12),
                Math.max(1, contentPanel.height() - PANEL_HEADER_HEIGHT - 12));

        if (shopItems.isEmpty()) {
            graphics.drawCenteredString(font, "Каталог загружается…",
                    shopViewport.x() + shopViewport.width() / 2,
                    shopViewport.y() + shopViewport.height() / 2 - 4,
                    GuideTheme.TEXT_MUTED);
            shopScroll = 0;
            shopScrollMax = 0;
            return;
        }

        int columns = shopViewport.width() >= 390 ? 2 : 1;
        int cardWidth = Math.max(1,
                (shopViewport.width() - SHOP_CARD_GAP * (columns - 1)) / columns);
        int rows = (shopItems.size() + columns - 1) / columns;
        int contentHeight = rows * SHOP_CARD_HEIGHT + Math.max(0, rows - 1) * SHOP_CARD_GAP;
        shopScrollMax = Math.max(0, contentHeight - shopViewport.height());
        shopScroll = Mth.clamp(shopScroll, 0, shopScrollMax);

        graphics.enableScissor(shopViewport.x(), shopViewport.y(),
                shopViewport.right(), shopViewport.bottom());
        for (int index = 0; index < shopItems.size(); index++) {
            int row = index / columns;
            int column = index % columns;
            Rect card = new Rect(
                    shopViewport.x() + column * (cardWidth + SHOP_CARD_GAP),
                    shopViewport.y() + row * (SHOP_CARD_HEIGHT + SHOP_CARD_GAP) - shopScroll,
                    cardWidth - (shopScrollMax > 0 && column == columns - 1 ? 3 : 0),
                    SHOP_CARD_HEIGHT);
            if (card.bottom() <= shopViewport.y() || card.y() >= shopViewport.bottom()) {
                continue;
            }

            Agent47ShopConfig.ShopItem item = shopItems.get(index);
            boolean hovered = shopViewport.contains(mouseX, mouseY) && card.contains(mouseX, mouseY);
            drawShopCard(graphics, item, card, hovered);
            shopHits.add(new ShopHit(item, card));
        }
        graphics.disableScissor();

        drawScrollBar(graphics, shopViewport, shopScroll, shopScrollMax, contentHeight);
    }

    private void drawShopCard(GuiGraphics graphics, Agent47ShopConfig.ShopItem item,
                              Rect card, boolean hovered) {
        boolean canAfford = playerMoney >= item.price;
        int typeAccent = typeColor(item.type);
        int background = hovered ? GuideTheme.SURFACE_HOVER : GuideTheme.SURFACE;
        int border = hovered ? (canAfford ? typeAccent : GuideTheme.RED) : GuideTheme.BORDER_SOFT;

        graphics.fill(card.x(), card.y(), card.right(), card.bottom(), background);
        graphics.renderOutline(card.x(), card.y(), card.width(), card.height(), border);
        graphics.fill(card.x(), card.y(), card.x() + (hovered ? 3 : 2), card.bottom(), typeAccent);

        int iconSize = 32;
        int iconX = card.x() + 7;
        int iconY = card.y() + 7;
        graphics.fill(iconX, iconY, iconX + iconSize, iconY + iconSize, 0xFF10161C);
        graphics.renderOutline(iconX, iconY, iconSize, iconSize, typeAccent);
        drawShopIcon(graphics, item, iconX, iconY, iconSize, typeAccent);

        int textX = iconX + iconSize + 6;
        int textWidth = Math.max(1, card.right() - textX - 5);
        graphics.drawString(font, trim(plain(item.name), textWidth),
                textX, card.y() + 6, GuideTheme.TEXT, false);

        List<FormattedCharSequence> description = font.split(
                Component.literal(plain(item.description)), textWidth);
        for (int line = 0; line < Math.min(2, description.size()); line++) {
            graphics.drawString(font, description.get(line), textX, card.y() + 18 + line * 9,
                    GuideTheme.TEXT_MUTED, false);
        }

        String type = typeLabel(item);
        graphics.drawString(font, trim(type, Math.max(1, textWidth - 54)),
                textX, card.bottom() - 15, typeAccent, false);

        String price = item.price + " М";
        int priceWidth = Math.min(textWidth, Math.max(44, font.width(price) + 10));
        int priceX = card.right() - priceWidth - 5;
        int priceColor = canAfford ? TERMINAL_GREEN : GuideTheme.RED;
        graphics.fill(priceX, card.bottom() - 19, card.right() - 5, card.bottom() - 5,
                canAfford ? 0xD5193823 : 0xD53A1A1E);
        graphics.renderOutline(priceX, card.bottom() - 19, priceWidth, 14, priceColor);
        graphics.drawCenteredString(font, price, priceX + priceWidth / 2,
                card.bottom() - 16, priceColor);
    }

    private void drawShopIcon(GuiGraphics graphics, Agent47ShopConfig.ShopItem item,
                              int x, int y, int size, int color) {
        if (item.type == Agent47ShopConfig.ShopItemType.ITEM && item.data != null) {
            String itemId = item.data;
            int nbtStart = itemId.indexOf('{');
            if (nbtStart >= 0) {
                itemId = itemId.substring(0, nbtStart);
            }
            try {
                Item registryItem = BuiltInRegistries.ITEM.get(new ResourceLocation(itemId));
                if (registryItem != Items.AIR) {
                    graphics.renderItem(new ItemStack(registryItem), x + (size - 16) / 2,
                            y + (size - 16) / 2);
                    if (item.amount > 1) {
                        String amount = "×" + item.amount;
                        graphics.drawString(font, amount,
                                x + size - font.width(amount) - 2, y + size - 10,
                                GuideTheme.TEXT, true);
                    }
                    return;
                }
            } catch (RuntimeException ignored) {
                // A compact terminal glyph below is safer than a missing-texture icon.
            }
        }

        MobEffect effect = switch (item.type) {
            case DEBUFF_GLOW -> MobEffects.GLOWING;
            case DEBUFF_SLOW -> MobEffects.MOVEMENT_SLOWDOWN;
            case DEBUFF_WEAK -> MobEffects.WEAKNESS;
            default -> null;
        };
        if (effect != null) {
            TextureAtlasSprite sprite = Minecraft.getInstance().getMobEffectTextures().get(effect);
            int effectIconSize = Math.min(20, size - 8);
            int effectX = x + (size - effectIconSize) / 2;
            int effectY = y + (size - effectIconSize) / 2;
            RenderSystem.enableBlend();
            graphics.blit(effectX, effectY, 0, effectIconSize, effectIconSize, sprite);
            RenderSystem.disableBlend();
            return;
        }

        int centerX = x + size / 2;
        graphics.renderOutline(x + 8, y + 8, size - 16, size - 16, color);
        graphics.fill(centerX - 1, y + 6, centerX + 2, y + size - 6, color);
    }

    private void drawPanel(GuiGraphics graphics, Rect panel, String title, int accent) {
        graphics.fill(panel.x(), panel.y(), panel.right(), panel.bottom(), GuideTheme.PANEL);
        graphics.renderOutline(panel.x(), panel.y(), panel.width(), panel.height(),
                GuideTheme.BORDER_SOFT);
        graphics.fill(panel.x() + 1, panel.y() + 1, panel.right() - 1,
                panel.y() + PANEL_HEADER_HEIGHT - 2, GuideTheme.HEADER);
        graphics.drawCenteredString(font, trim(title, Math.max(1, panel.width() - 70)),
                panel.x() + panel.width() / 2, panel.y() + 5, GuideTheme.TEXT_SECONDARY);
        graphics.fill(panel.x() + 1, panel.y() + PANEL_HEADER_HEIGHT - 2,
                panel.right() - 1, panel.y() + PANEL_HEADER_HEIGHT - 1, accent);
    }

    private void drawButton(GuiGraphics graphics, Rect bounds, String label, int accent,
                            boolean hovered, boolean selected) {
        GuideTheme.drawButton(graphics, font, bounds.x(), bounds.y(), bounds.width(),
                bounds.height(), label, accent, hovered, selected);
    }

    private void drawScrollBar(GuiGraphics graphics, Rect viewport, int scroll,
                               int maxScroll, int contentHeight) {
        if (maxScroll <= 0 || viewport.height() <= 0 || contentHeight <= 0) {
            return;
        }
        int trackX = viewport.right() - 2;
        int thumbHeight = Math.max(12, viewport.height() * viewport.height() / contentHeight);
        int thumbTravel = Math.max(1, viewport.height() - thumbHeight);
        int thumbY = viewport.y() + Math.round(thumbTravel * (scroll / (float) maxScroll));
        graphics.fill(trackX, viewport.y(), trackX + 2, viewport.bottom(), 0x66374450);
        graphics.fill(trackX, thumbY, trackX + 2, thumbY + thumbHeight, ACCENT);
    }

    private void drawTargetMark(GuiGraphics graphics, int x, int y, int size, int color) {
        int centerX = x + size / 2;
        int centerY = y + size / 2;
        int arm = Math.max(4, size / 4);
        graphics.renderOutline(x, y, size, size, GuideTheme.BORDER_SOFT);
        graphics.fill(centerX - 1, y + 5, centerX + 2, centerY - 3, color);
        graphics.fill(centerX - 1, centerY + 4, centerX + 2, y + size - 5, color);
        graphics.fill(x + 5, centerY - 1, centerX - 3, centerY + 2, color);
        graphics.fill(centerX + 4, centerY - 1, x + size - 5, centerY + 2, color);
        graphics.renderOutline(centerX - arm / 2, centerY - arm / 2,
                arm + 1, arm + 1, color);
    }

    private void drawCorner(GuiGraphics graphics, int x, int y, int directionX, int directionY) {
        graphics.fill(x, y, x + directionX * 10, y + directionY, ACCENT_DARK);
        graphics.fill(x, y, x + directionX, y + directionY * 10, ACCENT_DARK);
    }

    private int drawWrapped(GuiGraphics graphics, String text, int x, int y,
                            int maxWidth, int maxLines, int color) {
        List<FormattedCharSequence> lines = font.split(Component.literal(text), maxWidth);
        int drawn = Math.min(maxLines, lines.size());
        for (int index = 0; index < drawn; index++) {
            graphics.drawString(font, lines.get(index), x, y + index * 10, color, false);
        }
        return y + drawn * 10;
    }

    private int typeColor(Agent47ShopConfig.ShopItemType type) {
        return switch (type) {
            case ITEM -> GuideTheme.GREEN;
            case DEBUFF_GLOW -> GuideTheme.GOLD;
            case DEBUFF_SLOW -> GuideTheme.TEAL;
            case DEBUFF_WEAK -> GuideTheme.RED;
            case CUSTOM -> GuideTheme.PURPLE;
        };
    }

    private String typeLabel(Agent47ShopConfig.ShopItem item) {
        return switch (item.type) {
            case ITEM -> item.amount > 1 ? "ПРЕДМЕТ ×" + item.amount : "ПРЕДМЕТ";
            case DEBUFF_GLOW -> "РАЗВЕДКА • " + item.duration + " С";
            case DEBUFF_SLOW -> "ПОМЕХА • " + item.duration + " С";
            case DEBUFF_WEAK -> "ДЕБАФ • " + item.duration + " С";
            case CUSTOM -> "ОСОБОЕ";
        };
    }

    private boolean hasTarget() {
        String target = plain(currentTarget).trim();
        return !target.isEmpty() && !target.equalsIgnoreCase("Нет цели");
    }

    private String plain(String text) {
        if (text == null) {
            return "";
        }
        String stripped = ChatFormatting.stripFormatting(text);
        return stripped == null ? text : stripped;
    }

    private String trim(String text, int maxWidth) {
        if (text == null || maxWidth <= 0) {
            return "";
        }
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String suffix = "…";
        int bodyWidth = Math.max(0, maxWidth - font.width(suffix));
        return font.plainSubstrByWidth(text, bodyWidth) + suffix;
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
        if (targetTab.contains(mouseX, mouseY)) {
            currentMode = DisplayMode.TARGET_INFO;
            return true;
        }
        if (shopTab.contains(mouseX, mouseY)) {
            currentMode = DisplayMode.SHOP;
            return true;
        }
        if (currentMode == DisplayMode.SHOP && shopViewport.contains(mouseX, mouseY)) {
            for (ShopHit hit : shopHits) {
                if (hit.bounds().contains(mouseX, mouseY)) {
                    ModNetworking.CHANNEL.sendToServer(new Agent47PurchasePacket(hit.item().id));
                    ModNetworking.CHANNEL.sendToServer(new Agent47RequestDataPacket());
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (currentMode == DisplayMode.SHOP && shopViewport.contains(mouseX, mouseY)
                && shopScrollMax > 0) {
            shopScroll = Mth.clamp(shopScroll - (int) Math.round(delta * 28.0),
                    0, shopScrollMax);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public static void updateData(String target, int money,
                                  List<Agent47ShopConfig.ShopItem> items) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof Agent47TabletScreen screen) {
            screen.currentTarget = target;
            screen.playerMoney = money;
            screen.shopItems = items == null ? new ArrayList<>() : new ArrayList<>(items);
        }
    }

    public static void updateMoney(int money) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof Agent47TabletScreen screen) {
            screen.playerMoney = money;
        }
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

    private record ShopHit(Agent47ShopConfig.ShopItem item, Rect bounds) {
    }
}
