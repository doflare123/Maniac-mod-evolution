package org.example.maniacrevolution.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.Agent47PurchasePacket;
import org.example.maniacrevolution.network.packets.Agent47RequestDataPacket;
import org.example.maniacrevolution.system.Agent47ShopConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI планшета Агента 47
 * Показывает текущую цель и магазин
 */
@OnlyIn(Dist.CLIENT)
public class Agent47TabletScreen extends Screen {

    private static final int GUI_WIDTH = 280;
    private static final int GUI_HEIGHT = 220;

    private int guiLeft;
    private int guiTop;

    // Данные, полученные с сервера
    private String currentTarget = "Нет цели";
    private int playerMoney = 0;
    private List<Agent47ShopConfig.ShopItem> shopItems = new ArrayList<>();

    // Режимы отображения
    private enum DisplayMode {
        TARGET_INFO,
        SHOP
    }

    private DisplayMode currentMode = DisplayMode.TARGET_INFO;
    private int shopScrollOffset = 0;
    private static final int ITEMS_PER_PAGE = 4;

    public Agent47TabletScreen() {
        super(Component.literal("Планшет Агента 47"));
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = (this.width - GUI_WIDTH) / 2;
        this.guiTop = (this.height - GUI_HEIGHT) / 2;

        // Запрашиваем данные с сервера
        ModNetworking.CHANNEL.sendToServer(new Agent47RequestDataPacket());

        // Кнопка переключения режима
        int buttonWidth = 120;
        int buttonHeight = 20;

        this.addRenderableWidget(Button.builder(
                Component.literal(currentMode == DisplayMode.TARGET_INFO ? "Магазин" : "Цель"),
                button -> toggleMode()
        ).bounds(guiLeft + GUI_WIDTH / 2 - buttonWidth / 2, guiTop + GUI_HEIGHT - 30, buttonWidth, buttonHeight).build());
    }

    /**
     * Переключает режим отображения
     */
    private void toggleMode() {
        if (currentMode == DisplayMode.TARGET_INFO) {
            currentMode = DisplayMode.SHOP;
        } else {
            currentMode = DisplayMode.TARGET_INFO;
        }
        this.clearWidgets();
        this.init();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        // Фон GUI
        graphics.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xE0000000);

        // Рамка
        graphics.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + 2, 0xFFFF0000);
        graphics.fill(guiLeft, guiTop + GUI_HEIGHT - 2, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xFFFF0000);
        graphics.fill(guiLeft, guiTop, guiLeft + 2, guiTop + GUI_HEIGHT, 0xFFFF0000);
        graphics.fill(guiLeft + GUI_WIDTH - 2, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xFFFF0000);

        // Заголовок
        graphics.drawString(this.font, "§c§lАГЕНТ 47",
                guiLeft + 10, guiTop + 10, 0xFFFFFF, false);

        // Баланс
        graphics.drawString(this.font, "§eМонеты: §f" + playerMoney,
                guiLeft + GUI_WIDTH - 80, guiTop + 10, 0xFFFFFF, false);

        // Отображаем контент в зависимости от режима
        if (currentMode == DisplayMode.TARGET_INFO) {
            renderTargetInfo(graphics);
        } else {
            renderShop(graphics, mouseX, mouseY);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /**
     * Отображает информацию о цели
     */
    private void renderTargetInfo(GuiGraphics graphics) {
        int yOffset = 40;

        graphics.drawString(this.font, "§l§nТЕКУЩАЯ ЦЕЛЬ:",
                guiLeft + 10, guiTop + yOffset, 0xFFFFFF, false);

        yOffset += 20;

        // Информация о цели
        graphics.drawString(this.font, "§e" + currentTarget,
                guiLeft + 20, guiTop + yOffset, 0xFFFFFF, false);

        yOffset += 20;

        // Инструкции
        graphics.drawString(this.font, "§7Убейте цель, чтобы получить",
                guiLeft + 20, guiTop + yOffset, 0xAAAAAA, false);

        yOffset += 12;

        graphics.drawString(this.font, "§7награду в " + Agent47ShopConfig.KILL_TARGET_REWARD + " монет.",
                guiLeft + 20, guiTop + yOffset, 0xAAAAAA, false);

        yOffset += 25;

        // Дополнительная информация
        graphics.fill(guiLeft + 10, guiTop + yOffset, guiLeft + GUI_WIDTH - 10, guiTop + yOffset + 1, 0xFF444444);

        yOffset += 10;

        graphics.drawString(this.font, "§7При убийстве цели:",
                guiLeft + 20, guiTop + yOffset, 0xAAAAAA, false);

        yOffset += 12;

        graphics.drawString(this.font, "§8• §7Вы получаете монеты",
                guiLeft + 30, guiTop + yOffset, 0xAAAAAA, false);

        yOffset += 12;

        graphics.drawString(this.font, "§8• §7Автоматически назначается",
                guiLeft + 30, guiTop + yOffset, 0xAAAAAA, false);

        yOffset += 12;

        graphics.drawString(this.font, "§8  §7новая цель",
                guiLeft + 30, guiTop + yOffset, 0xAAAAAA, false);
    }

    /**
     * Отображает магазин
     */
    private void renderShop(GuiGraphics graphics, int mouseX, int mouseY) {
        int yOffset = 40;

        graphics.drawString(this.font, "§l§nМАГАЗИН:",
                guiLeft + 10, guiTop + yOffset, 0xFFFFFF, false);

        yOffset += 20;

        // Отображаем товары
        int startIndex = shopScrollOffset;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, shopItems.size());

        for (int i = startIndex; i < endIndex; i++) {
            Agent47ShopConfig.ShopItem item = shopItems.get(i);
            boolean canAfford = playerMoney >= item.price;
            boolean hovered = isMouseOverItem(mouseX, mouseY, yOffset);

            // Фон товара
            int bgColor = hovered ? 0x80444444 : 0x60222222;
            graphics.fill(guiLeft + 10, guiTop + yOffset, guiLeft + GUI_WIDTH - 10, guiTop + yOffset + 35, bgColor);

            // Название
            graphics.drawString(this.font, item.name,
                    guiLeft + 15, guiTop + yOffset + 5, 0xFFFFFF, false);

            // Описание
            graphics.drawString(this.font, item.description,
                    guiLeft + 15, guiTop + yOffset + 17, 0xAAAAAA, false);

            // Цена
            String priceColor = canAfford ? "§a" : "§c";
            graphics.drawString(this.font, priceColor + item.price + " монет",
                    guiLeft + GUI_WIDTH - 90, guiTop + yOffset + 10, 0xFFFFFF, false);

            yOffset += 40;
        }

        // Индикатор прокрутки
        if (shopItems.size() > ITEMS_PER_PAGE) {
            int scrollBarHeight = 100;
            int scrollBarY = guiTop + 60;
            int scrollBarX = guiLeft + GUI_WIDTH - 8;

            // Фон полосы прокрутки
            graphics.fill(scrollBarX, scrollBarY, scrollBarX + 4, scrollBarY + scrollBarHeight, 0x80FFFFFF);

            // Ползунок
            float scrollPercentage = (float) shopScrollOffset / Math.max(1, shopItems.size() - ITEMS_PER_PAGE);
            int thumbY = scrollBarY + (int) (scrollPercentage * (scrollBarHeight - 20));
            graphics.fill(scrollBarX, thumbY, scrollBarX + 4, thumbY + 20, 0xFFFFFFFF);
        }
    }

    /**
     * Проверяет, находится ли курсор над товаром
     */
    private boolean isMouseOverItem(int mouseX, int mouseY, int itemY) {
        return mouseX >= guiLeft + 10 && mouseX <= guiLeft + GUI_WIDTH - 10 &&
                mouseY >= guiTop + itemY && mouseY <= guiTop + itemY + 35;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (currentMode != DisplayMode.SHOP) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        // Обработка клика по товару
        int yOffset = 60;
        int startIndex = shopScrollOffset;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, shopItems.size());

        for (int i = startIndex; i < endIndex; i++) {
            if (isMouseOverItem((int) mouseX, (int) mouseY, yOffset)) {
                Agent47ShopConfig.ShopItem item = shopItems.get(i);

                // Отправляем пакет на сервер для покупки
                ModNetworking.CHANNEL.sendToServer(new Agent47PurchasePacket(item.id));

                // Обновляем данные
                ModNetworking.CHANNEL.sendToServer(new Agent47RequestDataPacket());

                return true;
            }
            yOffset += 40;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (currentMode == DisplayMode.SHOP && shopItems.size() > ITEMS_PER_PAGE) {
            shopScrollOffset -= (int) delta;
            shopScrollOffset = Math.max(0, Math.min(shopScrollOffset, shopItems.size() - ITEMS_PER_PAGE));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * Обновляет данные с сервера (вызывается из сетевого пакета)
     */
    public static void updateData(String target, int money, List<Agent47ShopConfig.ShopItem> items) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof Agent47TabletScreen screen) {
            screen.currentTarget = target;
            screen.playerMoney = money;
            screen.shopItems = items;
        }
    }

    /**
     * Обновляет только баланс
     */
    public static void updateMoney(int money) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof Agent47TabletScreen screen) {
            screen.playerMoney = money;
        }
    }
}