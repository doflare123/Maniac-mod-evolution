package org.example.maniacrevolution.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.example.maniacrevolution.config.HudConfig;
import org.example.maniacrevolution.data.ClientPlayerData;
import org.example.maniacrevolution.mana.ClientManaData;
import org.example.maniacrevolution.perk.PerkType;

import java.util.List;

public class CustomHud implements IGuiOverlay {

    // Размеры элементов
    private static final int MAIN_PANEL_WIDTH = 338;
    private static final int MAIN_PANEL_HEIGHT = 70;
    private static final int PERK_ICON_SIZE = 32;
    private static final int ABILITY_ICON_SIZE = 32;
    private static final int HOTBAR_SLOT_SIZE = 32;
    private static final int PENALTY_SLOT_SIZE = 16; // Маленькие слоты для пенальти
    private static final int BAR_HEIGHT = 18;
    private static final int BAR_WIDTH = 200;

    // Минимальная ширина экрана для полного отображения
    private static final int MIN_SCREEN_WIDTH = MAIN_PANEL_WIDTH + 120;

    // Цвета
    private static final int PANEL_BG = 0xCC000000;
    private static final int PANEL_BORDER = 0xFF444444;
    private static final int HP_COLOR = 0xFFFF0000;
    private static final int HP_BG = 0xFF550000;
    private static final int MANA_COLOR = 0xFF0099FF;
    private static final int MANA_BG = 0xFF003355;
    private static final int SLOT_BG = 0xFF222222;
    private static final int SLOT_BORDER = 0xFF666666;
    private static final int PENALTY_SLOT_BG = 0xFF330000; // Красноватый фон для пенальти-слотов
    private static final int PENALTY_SLOT_BORDER = 0xFFAA0000; // Красная рамка
    private static final int SELECTED_SLOT_BORDER = 0xFFFFFFFF;
    private static final int SELECTED_PERK_BORDER = 0xFFFFFF00;

    @Override
    public void render(net.minecraftforge.client.gui.overlay.ForgeGui gui, GuiGraphics guiGraphics,
                       float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;

        Player player = mc.player;

        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        // Вычисляем масштаб для адаптивности
        float scale = calculateScale(screenWidth);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(scale, scale, 1.0f);

        // Скорректированные размеры экрана с учетом масштаба
        int scaledWidth = (int) (screenWidth / scale);
        int scaledHeight = (int) (screenHeight / scale);

        // Позиция главной панели (внизу по центру)
        int mainX = (scaledWidth - MAIN_PANEL_WIDTH) / 2;
        int mainY = scaledHeight - MAIN_PANEL_HEIGHT - 5;

        // Рендерим главную панель
        renderMainPanel(guiGraphics, mainX, mainY, player);
        LevelHud.render(guiGraphics, 5, 5);
        TimerHud.render(guiGraphics, screenWidth / 2, 5);

        // Рендерим основной хотбар (6 слотов) справа
        int hotbarY = mainY + (MAIN_PANEL_HEIGHT - (HOTBAR_SLOT_SIZE * 2 + 4)) / 2;
        renderHotbar(guiGraphics, mainX + MAIN_PANEL_WIDTH + 5, hotbarY, player);

        // Рендерим пенальти-слоты (7-9) справа от основного хотбара
        int penaltyX = mainX + MAIN_PANEL_WIDTH + 5 + (HOTBAR_SLOT_SIZE + 4) * 3 + 8;
        int penaltyY = mainY + (MAIN_PANEL_HEIGHT - PENALTY_SLOT_SIZE * 3 - 8) / 2;
        renderPenaltySlots(guiGraphics, penaltyX, penaltyY, player);

        guiGraphics.pose().popPose();
    }

    /**
     * Вычисляет масштаб для адаптивного отображения HUD
     */
    private float calculateScale(int screenWidth) {
        if (screenWidth < MIN_SCREEN_WIDTH) {
            return (float) screenWidth / MIN_SCREEN_WIDTH;
        }
        return 1.0f;
    }

    private void renderMainPanel(GuiGraphics gui, int x, int y, Player player) {
        Minecraft mc = Minecraft.getInstance();

        // Фон главной панели
        gui.fill(x, y, x + MAIN_PANEL_WIDTH, y + MAIN_PANEL_HEIGHT, PANEL_BG);
        gui.renderOutline(x, y, MAIN_PANEL_WIDTH, MAIN_PANEL_HEIGHT, PANEL_BORDER);

        // Вертикальное центрирование контента
        int contentHeight = PERK_ICON_SIZE;
        int verticalOffset = (MAIN_PANEL_HEIGHT - contentHeight) / 2;

        int currentX = x + 8;
        int currentY = y + verticalOffset;

        // === СЕКЦИЯ 1: ПЕРКИ И СПОСОБНОСТЬ ===
        List<ClientPlayerData.ClientPerkData> perks = ClientPlayerData.getSelectedPerks();
        int activeIndex = ClientPlayerData.getActivePerkIndex();

        // Рендерим 2 перка
        for (int i = 0; i < Math.min(2, perks.size()); i++) {
            ClientPlayerData.ClientPerkData perk = perks.get(i);
            renderPerkSlot(gui, perk, currentX, currentY, i == activeIndex);
            currentX += PERK_ICON_SIZE + 4;
        }

        // Слот под способность (пока пустой)
        renderAbilitySlot(gui, currentX, currentY);
        currentX += ABILITY_ICON_SIZE + 15;

        // === СЕКЦИЯ 2: ЗДОРОВЬЕ И МАНА ===
        int barsY = y + (MAIN_PANEL_HEIGHT - (BAR_HEIGHT * 2 + 4)) / 2;
        renderHealthBar(gui, player, currentX, barsY);
        renderManaBar(gui, currentX, barsY + BAR_HEIGHT + 4);
    }

    private void renderPerkSlot(GuiGraphics gui, ClientPlayerData.ClientPerkData perk, int x, int y, boolean selected) {
        Minecraft mc = Minecraft.getInstance();

        // Фон
        int bgColor = getTypeColor(perk.type());
        gui.fill(x, y, x + PERK_ICON_SIZE, y + PERK_ICON_SIZE, bgColor);

        // Рамка (ЖЁЛТАЯ если выбран, серая если нет)
        int borderColor = selected ? SELECTED_PERK_BORDER : 0xFF666666;

        // Рисуем более толстую рамку для выбранного перка
        if (selected) {
            gui.renderOutline(x - 1, y - 1, PERK_ICON_SIZE + 2, PERK_ICON_SIZE + 2, borderColor);
        }
        gui.renderOutline(x, y, PERK_ICON_SIZE, PERK_ICON_SIZE, borderColor);

        // Стрелка над выбранным перком
        if (selected) {
            String arrow = "▼";
            int arrowX = x + (PERK_ICON_SIZE - mc.font.width(arrow)) / 2;
            gui.drawString(mc.font, "§e" + arrow, arrowX, y - 10, 0xFFFFFF, false);
        }

        // Иконка перка
        renderPerkIcon(gui, perk, x, y, PERK_ICON_SIZE);

        // Кулдаун
        if (perk.isOnCooldown()) {
            float cdProgress = perk.getCooldownProgress();
            int cdHeight = (int) (PERK_ICON_SIZE * cdProgress);
            gui.fill(x, y + PERK_ICON_SIZE - cdHeight, x + PERK_ICON_SIZE, y + PERK_ICON_SIZE, 0xBB000000);

            String cdText = perk.getCooldownSeconds() + "с";
            int textX = x + (PERK_ICON_SIZE - mc.font.width(cdText)) / 2;
            int textY = y + PERK_ICON_SIZE / 2 - 4;
            gui.drawString(mc.font, cdText, textX, textY, 0xFFFFFF, true);
        }
    }

    private void renderAbilitySlot(GuiGraphics gui, int x, int y) {
        gui.fill(x, y, x + ABILITY_ICON_SIZE, y + ABILITY_ICON_SIZE, SLOT_BG);
        gui.renderOutline(x, y, ABILITY_ICON_SIZE, ABILITY_ICON_SIZE, SLOT_BORDER);

        Minecraft mc = Minecraft.getInstance();
        gui.drawString(mc.font, "?", x + ABILITY_ICON_SIZE / 2 - 3, y + ABILITY_ICON_SIZE / 2 - 4, 0xFF666666, false);
    }

    private void renderHealthBar(GuiGraphics gui, Player player, int x, int y) {
        Minecraft mc = Minecraft.getInstance();

        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        float healthPercent = health / maxHealth;

        // Фон бара
        gui.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, HP_BG);

        // Заполненная часть
        int filledWidth = (int) (BAR_WIDTH * healthPercent);
        gui.fill(x, y, x + filledWidth, y + BAR_HEIGHT, HP_COLOR);

        // Рамка
        gui.renderOutline(x, y, BAR_WIDTH, BAR_HEIGHT, PANEL_BORDER);

        // Текст здоровья
        String hpText = String.format("%.0f / %.0f", health, maxHealth);
        int textX = x + (BAR_WIDTH - mc.font.width(hpText)) / 2;
        int textY = y + (BAR_HEIGHT - 8) / 2;
        gui.drawString(mc.font, hpText, textX, textY, 0xFFFFFFFF, true);

        // Регенерация HP (в правом нижнем углу)
        float hpRegen = ClientHealthData.getHealthRegen();
        if (Math.abs(hpRegen) > 0.01f) {
            String regenText = String.format("%+.1f", hpRegen);
            int regenX = x + BAR_WIDTH - mc.font.width(regenText) - 3;
            int regenY = y + BAR_HEIGHT - 9;

            int regenColor = hpRegen > 0 ? 0xFF55FF55 : 0xFFFF5555;
            gui.drawString(mc.font, regenText, regenX, regenY, regenColor, false);
        }
    }

    private void renderManaBar(GuiGraphics gui, int x, int y) {
        Minecraft mc = Minecraft.getInstance();

        float mana = ClientManaData.getMana();
        float maxMana = ClientManaData.getMaxMana();
        float manaPercent = ClientManaData.getManaPercentage();
        float regenRate = ClientManaData.getRegenRate();

        // Фон бара
        gui.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, MANA_BG);

        // Заполненная часть
        int filledWidth = (int) (BAR_WIDTH * manaPercent);
        gui.fill(x, y, x + filledWidth, y + BAR_HEIGHT, MANA_COLOR);

        // Рамка
        gui.renderOutline(x, y, BAR_WIDTH, BAR_HEIGHT, PANEL_BORDER);

        // Текст маны
        String manaText = String.format("%.0f / %.0f", mana, maxMana);
        int textX = x + (BAR_WIDTH - mc.font.width(manaText)) / 2;
        int textY = y + (BAR_HEIGHT - 8) / 2;
        gui.drawString(mc.font, manaText, textX, textY, 0xFFFFFFFF, true);

        // Регенерация маны (показываем только если > 0)
        if (regenRate > 0.01f) {
            String regenText = String.format("+%.1f", regenRate);
            int regenX = x + BAR_WIDTH - mc.font.width(regenText) - 3;
            int regenY = y + BAR_HEIGHT - 9;
            gui.drawString(mc.font, regenText, regenX, regenY, 0xFF55AAFF, false);
        }
    }

    private void renderHotbar(GuiGraphics gui, int x, int y, Player player) {
        Minecraft mc = Minecraft.getInstance();
        int selectedSlot = player.getInventory().selected;

        // 6 слотов в 2 ряда (3x2)
        for (int i = 0; i < 6; i++) {
            int slotX = x + (i % 3) * (HOTBAR_SLOT_SIZE + 4);
            int slotY = y + (i / 3) * (HOTBAR_SLOT_SIZE + 4);

            // Фон слота
            gui.fill(slotX, slotY, slotX + HOTBAR_SLOT_SIZE, slotY + HOTBAR_SLOT_SIZE, SLOT_BG);

            // Рамка (белая если выбран)
            boolean isSelected = (i == selectedSlot);
            int borderColor = isSelected ? SELECTED_SLOT_BORDER : SLOT_BORDER;
            gui.renderOutline(slotX, slotY, HOTBAR_SLOT_SIZE, HOTBAR_SLOT_SIZE, borderColor);

            // Предмет
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                gui.renderItem(stack, slotX + 8, slotY + 8);
                gui.renderItemDecorations(mc.font, stack, slotX + 8, slotY + 8);
            }

            // Номер слота
            String slotNum = String.valueOf(i + 1);
            gui.drawString(mc.font, slotNum, slotX + 2, slotY + 2, 0xFFAAAAAA, true);
        }
    }

    /**
     * Рендерит пенальти-слоты (7-9) - маленькие слоты с красной рамкой
     */
    private void renderPenaltySlots(GuiGraphics gui, int x, int y, Player player) {
        Minecraft mc = Minecraft.getInstance();
        int selectedSlot = player.getInventory().selected;

        // 3 маленьких слота вертикально (слоты 6, 7, 8 в индексации)
        for (int i = 0; i < 3; i++) {
            int slotIndex = 6 + i; // Слоты 6, 7, 8
            int slotX = x;
            int slotY = y + i * (PENALTY_SLOT_SIZE + 4);

            // Фон слота (красноватый)
            gui.fill(slotX, slotY, slotX + PENALTY_SLOT_SIZE, slotY + PENALTY_SLOT_SIZE, PENALTY_SLOT_BG);

            // Рамка (белая если выбран, красная если нет)
            boolean isSelected = (slotIndex == selectedSlot);
            int borderColor = isSelected ? SELECTED_SLOT_BORDER : PENALTY_SLOT_BORDER;
            gui.renderOutline(slotX, slotY, PENALTY_SLOT_SIZE, PENALTY_SLOT_SIZE, borderColor);

            // Предмет (уменьшенный)
            ItemStack stack = player.getInventory().getItem(slotIndex);
            if (!stack.isEmpty()) {
                gui.pose().pushPose();
                gui.pose().translate(slotX, slotY, 0);
                gui.pose().scale(0.9f, 0.9f, 0.9f);
                gui.renderItem(stack, 0, 0);
                gui.renderItemDecorations(mc.font, stack, 0, 0);
                gui.pose().popPose();
            }

            // Номер слота
            String slotNum = String.valueOf(slotIndex + 1);
            gui.drawString(mc.font, "§c" + slotNum, slotX + 1, slotY + 1, 0xFFFFFF, true);
        }
    }

    private void renderPerkIcon(GuiGraphics gui, ClientPlayerData.ClientPerkData perk, int x, int y, int size) {
        Minecraft mc = Minecraft.getInstance();
        ResourceLocation texture = perk.getIcon();

        try {
            RenderSystem.setShaderTexture(0, texture);
            RenderSystem.enableBlend();
            gui.blit(texture, x, y, 0, 0, size, size, size, size);
            RenderSystem.disableBlend();
        } catch (Exception e) {
            String initial = perk.id().substring(0, 1).toUpperCase();
            gui.drawString(mc.font, initial, x + size / 2 - 3, y + size / 2 - 4, 0xFFFFFF, true);
        }
    }

    private int getTypeColor(PerkType type) {
        return switch (type) {
            case PASSIVE -> 0xFF3355FF;
            case ACTIVE -> 0xFFFF5533;
            case HYBRID -> 0xFFAA55FF;
            case PASSIVE_COOLDOWN -> 0xFF3355FF;
        };
    }
}