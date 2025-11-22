package org.example.maniacrevolution.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.example.maniacrevolution.data.ClientPlayerData;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.PurchaseItemPacket;
import org.example.maniacrevolution.shop.ShopCategory;
import org.example.maniacrevolution.shop.ShopItem;
import org.example.maniacrevolution.shop.ShopRegistry;

import java.util.*;

public class ShopScreen extends Screen {
    private int guiLeft, guiTop;
    private static final int GUI_WIDTH = 300;
    private static final int GUI_HEIGHT = 220;

    private ShopCategory currentCategory = ShopCategory.COSMETICS;
    private int scrollOffset = 0;
    private ShopItem hoveredItem = null;

    public ShopScreen() {
        super(Component.literal("Магазин"));
    }

    @Override
    protected void init() {
        guiLeft = (width - GUI_WIDTH) / 2;
        guiTop = (height - GUI_HEIGHT) / 2;

        // Категории
        int btnX = guiLeft + 5;
        int btnY = guiTop + 25;

        for (ShopCategory cat : ShopCategory.values()) {
            final ShopCategory category = cat;
            String name = switch (cat) {
                case COSMETICS -> "Косметика";
                case BOOSTS -> "Усиления";
                case UPGRADES -> "Улучшения";
            };
            addRenderableWidget(Button.builder(Component.literal(name), b -> switchCategory(category))
                    .pos(btnX, btnY).size(60, 16).build());
            btnX += 65;
        }

        // Кнопка "Моя косметика"
        addRenderableWidget(Button.builder(Component.literal("§d✦ Мои эффекты"),
                        b -> Minecraft.getInstance().setScreen(new CosmeticScreen()))
                .pos(guiLeft + GUI_WIDTH - 95, guiTop + 25).size(90, 16).build());

        // Кнопка закрытия
        addRenderableWidget(Button.builder(Component.literal("X"), b -> onClose())
                .pos(guiLeft + GUI_WIDTH - 20, guiTop + 5).size(15, 15).build());
    }

    private void switchCategory(ShopCategory category) {
        currentCategory = category;
        scrollOffset = 0;
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);

        // Фон
        gui.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xEE1a1a1a);
        gui.renderOutline(guiLeft, guiTop, GUI_WIDTH, GUI_HEIGHT, 0xFF888888);

        // Заголовок
        gui.drawCenteredString(font, "§6✦ Магазин ✦", guiLeft + GUI_WIDTH / 2, guiTop + 7, 0xFFFFFF);

        // Баланс
        int coins = ClientPlayerData.getCoins();
        String balance = "§eМонеты: §f" + coins;
        gui.drawString(font, balance, guiLeft + 10, guiTop + 7, 0xFFFFFF, false);

        // Список товаров
        List<ShopItem> items = ShopRegistry.getItemsByCategory(currentCategory);

        int startY = guiTop + 48;
        int itemHeight = 38;
        hoveredItem = null;

        gui.enableScissor(guiLeft + 5, startY, guiLeft + GUI_WIDTH - 5, guiTop + GUI_HEIGHT - 10);

        int y = startY - scrollOffset;
        for (ShopItem item : items) {
            if (y + itemHeight > startY - itemHeight && y < guiTop + GUI_HEIGHT) {
                boolean hovered = mouseX >= guiLeft + 10 && mouseX < guiLeft + GUI_WIDTH - 10
                        && mouseY >= y && mouseY < y + itemHeight - 2
                        && mouseY >= startY;

                if (hovered) hoveredItem = item;

                renderShopItem(gui, item, guiLeft + 10, y, GUI_WIDTH - 20, itemHeight - 4, hovered, mouseX, mouseY);
            }
            y += itemHeight;
        }

        gui.disableScissor();

        super.render(gui, mouseX, mouseY, partialTick);

        // Тултип
        if (hoveredItem != null) {
            renderItemTooltip(gui, mouseX, mouseY);
        }
    }

    private void renderShopItem(GuiGraphics gui, ShopItem item, int x, int y, int width, int height,
                                boolean hovered, int mouseX, int mouseY) {
        // Фон
        int bgColor = hovered ? 0xFF3a3a3a : 0xFF2a2a2a;
        gui.fill(x, y, x + width, y + height, bgColor);
        gui.renderOutline(x, y, width, height, 0xFF555555);

        // Иконка
        int iconSize = height - 8;
        gui.fill(x + 4, y + 4, x + 4 + iconSize, y + 4 + iconSize, getCategoryColor(item.getCategory()));
        String initial = item.getId().substring(0, 1).toUpperCase();
        gui.drawCenteredString(font, initial, x + 4 + iconSize / 2, y + 4 + iconSize / 2 - 4, 0xFFFFFF);

        // Название
        gui.drawString(font, item.getName(), x + iconSize + 10, y + 5, 0xFFFFFF, false);

        // Цена / статус
        int coins = ClientPlayerData.getCoins();
        boolean canBuy = coins >= item.getPrice() && canPurchaseClient(item);

        if (!canPurchaseClient(item)) {
            gui.drawString(font, "§a✓ Куплено", x + iconSize + 10, y + 17, 0x55FF55, false);
        } else {
            String priceText = item.getPrice() + " монет";
            int priceColor = canBuy ? 0xFFAA00 : 0xFF5555;
            gui.drawString(font, priceText, x + iconSize + 10, y + 17, priceColor, false);
        }

        // Кнопка покупки
        if (canPurchaseClient(item)) {
            int btnX = x + width - 60;
            int btnY = y + (height - 16) / 2;

            boolean btnHovered = mouseX >= btnX && mouseX < btnX + 55 && mouseY >= btnY && mouseY < btnY + 16;
            int btnColor = canBuy ? (btnHovered ? 0xFF44AA44 : 0xFF338833) : 0xFF555555;
            gui.fill(btnX, btnY, btnX + 55, btnY + 16, btnColor);
            gui.renderOutline(btnX, btnY, 55, 16, canBuy ? 0xFF55FF55 : 0xFF888888);

            String btnText = canBuy ? "Купить" : "Мало";
            gui.drawCenteredString(font, btnText, btnX + 27, btnY + 4, 0xFFFFFF);
        }
    }

    private boolean canPurchaseClient(ShopItem item) {
        // Проверка на клиенте - для косметики проверяем через CosmeticData
        if (item.getCategory() == ShopCategory.COSMETICS) {
            return !ClientPlayerData.getCosmeticData().hasPurchased(item.getId());
        }
        // Для слотов пресетов
        if (item.getId().equals("extra_preset_slot")) {
            return ClientPlayerData.getMaxPresets() < 10;
        }
        return true; // Остальное можно покупать
    }

    private int getCategoryColor(ShopCategory cat) {
        return switch (cat) {
            case COSMETICS -> 0xFF9955FF;
            case BOOSTS -> 0xFF55FF55;
            case UPGRADES -> 0xFFFFAA00;
        };
    }

    private void renderItemTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(hoveredItem.getName().copy().withStyle(net.minecraft.ChatFormatting.WHITE));
        tooltip.add(hoveredItem.getDescription().copy().withStyle(net.minecraft.ChatFormatting.GRAY));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Категория: ").withStyle(net.minecraft.ChatFormatting.DARK_GRAY)
                .append(hoveredItem.getCategory().getDisplayName()));

        if (canPurchaseClient(hoveredItem)) {
            tooltip.add(Component.literal("Цена: " + hoveredItem.getPrice() + " монет")
                    .withStyle(net.minecraft.ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.literal("✓ Куплено").withStyle(net.minecraft.ChatFormatting.GREEN));
        }

        gui.renderComponentTooltip(font, tooltip, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && hoveredItem != null && canPurchaseClient(hoveredItem)) {
            int coins = ClientPlayerData.getCoins();
            if (coins >= hoveredItem.getPrice()) {
                ModNetworking.CHANNEL.sendToServer(new PurchaseItemPacket(hoveredItem.getId()));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        List<ShopItem> items = ShopRegistry.getItemsByCategory(currentCategory);
        int maxScroll = Math.max(0, items.size() * 38 - 150);
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - delta * 25));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}