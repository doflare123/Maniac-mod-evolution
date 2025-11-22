package org.example.maniacrevolution.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.example.maniacrevolution.cosmetic.CosmeticEffect;
import org.example.maniacrevolution.cosmetic.CosmeticRegistry;
import org.example.maniacrevolution.cosmetic.CosmeticType;
import org.example.maniacrevolution.data.ClientPlayerData;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.ToggleCosmeticPacket;

import java.util.*;

public class CosmeticScreen extends Screen {
    private int guiLeft, guiTop;
    private static final int GUI_WIDTH = 300;
    private static final int GUI_HEIGHT = 220;

    private CosmeticType currentCategory = CosmeticType.PARTICLE;
    private int scrollOffset = 0;
    private CosmeticEffect hoveredEffect = null;

    public CosmeticScreen() {
        super(Component.literal("Косметика"));
    }

    @Override
    protected void init() {
        guiLeft = (width - GUI_WIDTH) / 2;
        guiTop = (height - GUI_HEIGHT) / 2;

        // Кнопки категорий
        int btnX = guiLeft + 5;
        int btnY = guiTop + 25;

        for (CosmeticType type : CosmeticType.values()) {
            final CosmeticType cat = type;
            String name = switch (type) {
                case PARTICLE -> "Частицы";
                case WEAPON_EFFECT -> "Оружие";
                case PERK_SKIN -> "Перки";
                case TRAIL -> "Следы";
            };
            addRenderableWidget(Button.builder(Component.literal(name), b -> switchCategory(cat))
                    .pos(btnX, btnY).size(55, 16).build());
            btnX += 60;
        }

        // Кнопка закрытия
        addRenderableWidget(Button.builder(Component.literal("X"), b -> onClose())
                .pos(guiLeft + GUI_WIDTH - 20, guiTop + 5).size(15, 15).build());
    }

    private void switchCategory(CosmeticType type) {
        currentCategory = type;
        scrollOffset = 0;
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);

        // Фон
        gui.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xEE1a1a1a);
        gui.renderOutline(guiLeft, guiTop, GUI_WIDTH, GUI_HEIGHT, 0xFF888888);

        // Заголовок
        gui.drawCenteredString(font, "§d✦ Моя косметика ✦", guiLeft + GUI_WIDTH / 2, guiTop + 7, 0xFFFFFF);

        // Описание категории
        gui.drawCenteredString(font, "§7" + currentCategory.getDescription(),
                guiLeft + GUI_WIDTH / 2, guiTop + 45, 0xAAAAAA);

        // Список эффектов
        renderEffectsList(gui, mouseX, mouseY);

        super.render(gui, mouseX, mouseY, partialTick);

        // Тултип
        if (hoveredEffect != null) {
            renderEffectTooltip(gui, mouseX, mouseY);
        }
    }

    private void renderEffectsList(GuiGraphics gui, int mouseX, int mouseY) {
        List<CosmeticEffect> effects = CosmeticRegistry.getEffectsByType(currentCategory);
        Set<String> purchased = ClientPlayerData.getCosmeticData().getPurchasedCosmetics();
        Set<String> enabled = ClientPlayerData.getCosmeticData().getEnabledCosmetics();

        // Фильтруем только купленные
        List<CosmeticEffect> ownedEffects = new ArrayList<>();
        for (CosmeticEffect effect : effects) {
            if (purchased.contains(effect.getId())) {
                ownedEffects.add(effect);
            }
        }

        if (ownedEffects.isEmpty()) {
            gui.drawCenteredString(font, "§7У вас нет косметики этого типа",
                    guiLeft + GUI_WIDTH / 2, guiTop + 100, 0x888888);
            gui.drawCenteredString(font, "§8Купите в магазине!",
                    guiLeft + GUI_WIDTH / 2, guiTop + 115, 0x666666);
            return;
        }

        int startY = guiTop + 60;
        int itemHeight = 35;

        hoveredEffect = null;

        gui.enableScissor(guiLeft + 5, startY, guiLeft + GUI_WIDTH - 5, guiTop + GUI_HEIGHT - 10);

        int y = startY - scrollOffset;
        for (CosmeticEffect effect : ownedEffects) {
            if (y + itemHeight > startY - itemHeight && y < guiTop + GUI_HEIGHT) {
                boolean hovered = mouseX >= guiLeft + 10 && mouseX < guiLeft + GUI_WIDTH - 10
                        && mouseY >= y && mouseY < y + itemHeight - 2
                        && mouseY >= startY && mouseY < guiTop + GUI_HEIGHT - 10;

                if (hovered) hoveredEffect = effect;

                boolean isEnabled = enabled.contains(effect.getId());
                renderEffectEntry(gui, effect, guiLeft + 10, y, GUI_WIDTH - 20, itemHeight - 3,
                        hovered, isEnabled, mouseX, mouseY);
            }
            y += itemHeight;
        }

        gui.disableScissor();
    }

    private void renderEffectEntry(GuiGraphics gui, CosmeticEffect effect, int x, int y,
                                   int width, int height, boolean hovered, boolean enabled, int mouseX, int mouseY) {
        // Фон
        int bgColor = hovered ? 0xFF3a3a3a : 0xFF2a2a2a;
        gui.fill(x, y, x + width, y + height, bgColor);

        // Рамка (зелёная если включено)
        int borderColor = enabled ? 0xFF55FF55 : 0xFF555555;
        gui.renderOutline(x, y, width, height, borderColor);

        // Иконка статуса
        int iconColor = enabled ? 0xFF00FF00 : 0xFF666666;
        gui.fill(x + 4, y + 4, x + 24, y + 24, iconColor);
        String statusIcon = enabled ? "✓" : "✗";
        gui.drawCenteredString(font, statusIcon, x + 14, y + 10, 0xFFFFFF);

        // Название
        String name = effect.getName().getString();
        int nameColor = enabled ? 0xFFFFFF : 0xAAAAAA;
        gui.drawString(font, name, x + 30, y + 6, nameColor, false);

        // Тип
        gui.drawString(font, "§7" + effect.getType().getDisplayName().getString(),
                x + 30, y + 18, 0x888888, false);

        // Кнопка вкл/выкл
        int btnX = x + width - 70;
        int btnY = y + (height - 18) / 2;

        boolean btnHovered = mouseX >= btnX && mouseX < btnX + 65 && mouseY >= btnY && mouseY < btnY + 18;
        int btnColor = enabled
                ? (btnHovered ? 0xFF884444 : 0xFF663333) // Красный для выкл
                : (btnHovered ? 0xFF448844 : 0xFF336633); // Зелёный для вкл

        gui.fill(btnX, btnY, btnX + 65, btnY + 18, btnColor);
        gui.renderOutline(btnX, btnY, 65, 18, enabled ? 0xFFFF5555 : 0xFF55FF55);

        String btnText = enabled ? "Выключить" : "Включить";
        gui.drawCenteredString(font, btnText, btnX + 32, btnY + 5, 0xFFFFFF);
    }

    private void renderEffectTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(hoveredEffect.getName().copy().withStyle(ChatFormatting.GOLD));
        tooltip.add(hoveredEffect.getDescription().copy().withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Тип: ").withStyle(ChatFormatting.DARK_GRAY)
                .append(hoveredEffect.getType().getDisplayName()));

        // Показать на какие предметы действует
        if (hoveredEffect.getType() == CosmeticType.WEAPON_EFFECT) {
            tooltip.add(Component.empty());
            tooltip.add(Component.literal("§eДействует на:").withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.literal("§7Определённое оружие (топоры/мечи)"));
        }

        gui.renderComponentTooltip(font, tooltip, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && hoveredEffect != null) {
            // Проверяем клик на кнопку
            int x = guiLeft + 10;
            int width = GUI_WIDTH - 20;
            int btnX = x + width - 70;

            // Примерная проверка клика на кнопку
            if (mouseX >= btnX && mouseX < btnX + 65) {
                // Отправляем пакет на сервер
                ModNetworking.CHANNEL.sendToServer(new ToggleCosmeticPacket(hoveredEffect.getId()));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        List<CosmeticEffect> effects = CosmeticRegistry.getEffectsByType(currentCategory);
        Set<String> purchased = ClientPlayerData.getCosmeticData().getPurchasedCosmetics();

        int ownedCount = (int) effects.stream().filter(e -> purchased.contains(e.getId())).count();
        int maxScroll = Math.max(0, ownedCount * 35 - 130);
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - delta * 25));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

