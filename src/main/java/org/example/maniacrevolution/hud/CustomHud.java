package org.example.maniacrevolution.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import org.example.maniacrevolution.config.HudConfig;
import org.example.maniacrevolution.data.ClientPlayerData;
import org.example.maniacrevolution.fleshheap.ClientFleshHeapData;
import org.example.maniacrevolution.item.IItemWithAbility;
import org.example.maniacrevolution.item.ITimedAbility;
import org.example.maniacrevolution.item.armor.MedicalMaskItem;
import org.example.maniacrevolution.item.armor.NecromancerArmorItem;
import org.example.maniacrevolution.keybind.ModKeybinds;
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
    private static final int PENALTY_SLOT_SIZE = 16;
    private static final int BAR_HEIGHT = 18;
    private static final int BAR_WIDTH = 200;

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
    private static final int PENALTY_SLOT_BG = 0xFF330000;
    private static final int PENALTY_SLOT_BORDER = 0xFFAA0000;
    private static final int SELECTED_SLOT_BORDER = 0xFFFFFFFF;
    private static final int SELECTED_PERK_BORDER = 0xFFFFFF00;

    // Для отображения названия предмета
    private static ItemStack lastSelectedItem = ItemStack.EMPTY;
    private static long itemNameShowTime = 0;
    private static final long ITEM_NAME_DURATION = 5000; // 5 секунд

    @Override
    public void render(net.minecraftforge.client.gui.overlay.ForgeGui gui, GuiGraphics guiGraphics,
                       float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;

        Player player = mc.player;

        // ВАЖНО: Проверяем режим игрока И настройки HUD
        if (player.isCreative() || player.isSpectator()) {
            // В креативе/наблюдателе показываем только ванильный HUD
            return;
        }

        // Проверяем, включен ли кастомный HUD
        if (!HudConfig.isCustomHudEnabled()) {
            // Кастомный HUD выключен - не рендерим
            return;
        }

        float scale = calculateScale(screenWidth);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(scale, scale, 1.0f);

        int scaledWidth = (int) (screenWidth / scale);
        int scaledHeight = (int) (screenHeight / scale);

        int mainX = (scaledWidth - MAIN_PANEL_WIDTH) / 2;
        int mainY = scaledHeight - MAIN_PANEL_HEIGHT - 5;

        // Рендерим Flesh Heap ВЫШЕ главной панели
        renderFleshHeap(guiGraphics, scaledWidth / 2, mainY - 10);

        renderMainPanel(guiGraphics, mainX, mainY, player);
        LevelHud.render(guiGraphics, 5, 5);
        TimerHud.render(guiGraphics, screenWidth / 2, 5);

        int hotbarY = mainY + (MAIN_PANEL_HEIGHT - (HOTBAR_SLOT_SIZE * 2 + 4)) / 2;
        renderHotbar(guiGraphics, mainX + MAIN_PANEL_WIDTH + 5, hotbarY, player);

        int penaltyX = mainX + MAIN_PANEL_WIDTH + 5 + (HOTBAR_SLOT_SIZE + 4) * 3 + 8;
        int penaltyY = mainY + (MAIN_PANEL_HEIGHT - PENALTY_SLOT_SIZE * 3 - 8) / 2;
        renderPenaltySlots(guiGraphics, penaltyX, penaltyY, player);

        // НОВОЕ: Подсказки по клавишам слева от HUD
        renderPerkKeybindHints(guiGraphics, mainX - 125, mainY + 20);

        // Отображаем название предмета НАД кастомным HUD
        renderItemName(guiGraphics, player, scaledWidth, mainY);

        guiGraphics.pose().popPose();
    }

    /**
     * Отображает название выбранного предмета
     */
    private void renderItemName(GuiGraphics gui, Player player, int screenWidth, int hudTopY) {
        Minecraft mc = Minecraft.getInstance();
        ItemStack currentItem = player.getInventory().getSelected();
        long currentTime = System.currentTimeMillis();

        // Обновляем время показа при смене предмета
        if (!ItemStack.matches(currentItem, lastSelectedItem)) {
            lastSelectedItem = currentItem.copy();
            if (!currentItem.isEmpty()) {
                itemNameShowTime = currentTime;
            }
        }

        // Показываем название только если прошло меньше 5 секунд
        if (!currentItem.isEmpty() && (currentTime - itemNameShowTime) < ITEM_NAME_DURATION) {
            String itemName = currentItem.getHoverName().getString();
            int textWidth = mc.font.width(itemName);
            int textX = (screenWidth - textWidth) / 2;
            int textY = hudTopY - 25; // НАД HUD

            // Название предмета
            gui.drawString(mc.font, itemName, textX, textY, 0xFFFFFFFF, true);
        }
    }

    /**
     * Рендер индикатора Flesh Heap
     */
    private void renderFleshHeap(GuiGraphics gui, int centerX, int centerY) {
        int stacks = ClientFleshHeapData.getStacks();
        if (stacks <= 0) return;

        Minecraft mc = Minecraft.getInstance();
        final int ICON_SIZE = 16;

        int x = centerX - ICON_SIZE / 2;
        int y = centerY - ICON_SIZE / 2;

        gui.pose().pushPose();

        ResourceLocation texture = new ResourceLocation("maniacrev", "textures/gui/flesh_heap.png");
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.enableBlend();

        gui.blit(texture, x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);

        RenderSystem.disableBlend();

        // Количество стаков НА иконке
        String stackText = String.valueOf(stacks);
        int textWidth = mc.font.width(stackText);
        int textX = centerX - textWidth / 2;
        int textY = centerY - 4;

        // Черная обводка
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0) {
                    gui.drawString(mc.font, stackText, textX + dx, textY + dy, 0xFF000000, false);
                }
            }
        }

        gui.drawString(mc.font, "§l" + stackText, textX, textY, 0xFFFFFFFF, false);

        gui.pose().popPose();
    }

    private float calculateScale(int screenWidth) {
        if (screenWidth < MIN_SCREEN_WIDTH) {
            return (float) screenWidth / MIN_SCREEN_WIDTH;
        }
        return 1.0f;
    }

    private void renderMainPanel(GuiGraphics gui, int x, int y, Player player) {
        Minecraft mc = Minecraft.getInstance();

        gui.fill(x, y, x + MAIN_PANEL_WIDTH, y + MAIN_PANEL_HEIGHT, PANEL_BG);
        gui.renderOutline(x, y, MAIN_PANEL_WIDTH, MAIN_PANEL_HEIGHT, PANEL_BORDER);

        int contentHeight = PERK_ICON_SIZE;
        int verticalOffset = (MAIN_PANEL_HEIGHT - contentHeight) / 2;

        int currentX = x + 8;
        int currentY = y + verticalOffset;

        List<ClientPlayerData.ClientPerkData> perks = ClientPlayerData.getSelectedPerks();
        int activeIndex = ClientPlayerData.getActivePerkIndex();

        for (int i = 0; i < Math.min(2, perks.size()); i++) {
            ClientPlayerData.ClientPerkData perk = perks.get(i);
            renderPerkSlot(gui, perk, currentX, currentY, i == activeIndex);
            currentX += PERK_ICON_SIZE + 4;
        }

        // НОВОЕ: Рендер способности предмета
        IItemWithAbility itemAbility = findItemWithAbility(player);
        if (itemAbility != null) {
            renderAbilitySlot(gui, currentX, currentY, itemAbility, player);
        }
        currentX += ABILITY_ICON_SIZE + 15;

        int barsY = y + (MAIN_PANEL_HEIGHT - (BAR_HEIGHT * 2 + 4)) / 2;
        renderHealthBar(gui, player, currentX, barsY);
        renderManaBar(gui, currentX, barsY + BAR_HEIGHT + 4);
    }

    /**
     * НОВОЕ: Поиск предмета со способностью у игрока
     */
    /**
     * ИСПРАВЛЕНО: Поиск предмета со способностью с учетом условий
     */
    private IItemWithAbility findItemWithAbility(Player player) {
        // Проверяем основную руку
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof IItemWithAbility ability) {
            return ability;
        }

        // Проверяем броню с УСЛОВИЯМИ
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.ARMOR) continue;

            ItemStack armorPiece = player.getItemBySlot(slot);
            if (armorPiece.isEmpty()) continue;

            // ИСПРАВЛЕНО: Проверяем условия для брони
            if (armorPiece.getItem() instanceof IItemWithAbility ability) {
                // Для брони некроманта - нужен полный сет
                if (armorPiece.getItem() instanceof NecromancerArmorItem) {
                    if (!hasFullNecromancerSet(player)) {
                        continue; // Пропускаем, если нет полного сета
                    }
                }

                // Для медицинской маски - должна быть в слоте шлема
                if (armorPiece.getItem() instanceof MedicalMaskItem) {
                    if (slot != EquipmentSlot.HEAD) {
                        continue;
                    }
                }

                return ability;
            }
        }

        return null;
    }

    /**
     * НОВОЕ: Проверка полного сета некроманта
     */
    private boolean hasFullNecromancerSet(Player player) {
        for (ItemStack armorSlot : player.getArmorSlots()) {
            if (!(armorSlot.getItem() instanceof NecromancerArmorItem)) {
                return false;
            }
        }
        return true;
    }


    private void renderPerkSlot(GuiGraphics gui, ClientPlayerData.ClientPerkData perk, int x, int y, boolean selected) {
        Minecraft mc = Minecraft.getInstance();

        int bgColor = getTypeColor(perk.type());
        gui.fill(x, y, x + PERK_ICON_SIZE, y + PERK_ICON_SIZE, bgColor);

        int borderColor = selected ? SELECTED_PERK_BORDER : 0xFF666666;

        if (selected) {
            gui.renderOutline(x - 1, y - 1, PERK_ICON_SIZE + 2, PERK_ICON_SIZE + 2, borderColor);
        }
        gui.renderOutline(x, y, PERK_ICON_SIZE, PERK_ICON_SIZE, borderColor);

        if (selected) {
            String arrow = "▼";
            int arrowX = x + (PERK_ICON_SIZE - mc.font.width(arrow)) / 2;
            gui.drawString(mc.font, "§e" + arrow, arrowX, y - 10, 0xFFFFFF, false);
        }

        renderPerkIcon(gui, perk, x, y, PERK_ICON_SIZE);

        if (perk.isOnCooldown()) {
            float cdProgress = perk.getCooldownProgress();
            int cdHeight = (int) (PERK_ICON_SIZE * cdProgress);
            gui.fill(x, y + PERK_ICON_SIZE - cdHeight, x + PERK_ICON_SIZE, y + PERK_ICON_SIZE, 0xBB000000);

            String cdText = perk.getCooldownSeconds() + "с";
            int textX = x + (PERK_ICON_SIZE - mc.font.width(cdText)) / 2;
            int textY = y + PERK_ICON_SIZE / 2 - 4;
            gui.drawString(mc.font, cdText, textX, textY, 0xFFFFFF, true);
        }

        String typeText = getTypeShort(perk.type());
        int typeColor = getTypeFontColor(perk.type());
        int typeX = x + (PERK_ICON_SIZE - mc.font.width(typeText)) / 2;
        int typeY = y + PERK_ICON_SIZE + 2;
        gui.drawString(mc.font, typeText, typeX, typeY, typeColor, false);
    }

    private void renderAbilitySlot(GuiGraphics gui, int x, int y, IItemWithAbility ability, Player player) {
        Minecraft mc = Minecraft.getInstance();

        // Фон
        gui.fill(x, y, x + ABILITY_ICON_SIZE, y + ABILITY_ICON_SIZE, SLOT_BG);
        gui.renderOutline(x, y, ABILITY_ICON_SIZE, ABILITY_ICON_SIZE, SLOT_BORDER);

        // Иконка способности
        ResourceLocation icon = ability.getAbilityIcon();
        try {
            RenderSystem.setShaderTexture(0, icon);
            RenderSystem.enableBlend();
            gui.blit(icon, x, y, 0, 0, ABILITY_ICON_SIZE, ABILITY_ICON_SIZE, ABILITY_ICON_SIZE, ABILITY_ICON_SIZE);
            RenderSystem.disableBlend();
        } catch (Exception e) {
            gui.drawString(mc.font, "?", x + ABILITY_ICON_SIZE / 2 - 3, y + ABILITY_ICON_SIZE / 2 - 4, 0xFFFFFF, false);
        }

        // ИСПРАВЛЕНО: Индикатор активности с прогресс-баром
        if (ability instanceof ITimedAbility timedAbility && timedAbility.isAbilityActive(player)) {
            float durationProgress = timedAbility.getDurationProgress(player);

            // Круговой прогресс-бар (ободок)
            renderCircularProgress(gui, x, y, ABILITY_ICON_SIZE, durationProgress, 0xFF55FF55);

            // Опционально: оставшееся время
            int remainingSec = timedAbility.getRemainingDurationSeconds(player);
            if (remainingSec > 0) {
                String timeText = remainingSec + "с";
                int textX = x + (ABILITY_ICON_SIZE - mc.font.width(timeText)) / 2;
                int textY = y + ABILITY_ICON_SIZE + 2;
                gui.drawString(mc.font, "§a" + timeText, textX, textY, 0xFFFFFF, true);
            }
        }
        // Кулдаун (только если способность НЕ активна)
        else if (ability.isOnCooldown(player)) {
            float cdProgress = ability.getCooldownProgress(player);
            int cdHeight = (int) (ABILITY_ICON_SIZE * cdProgress);

            // Затемнение
            gui.fill(x, y + ABILITY_ICON_SIZE - cdHeight, x + ABILITY_ICON_SIZE, y + ABILITY_ICON_SIZE, 0xBB000000);

            // Текст кулдауна
            int cooldownSec = ability.getCooldownSeconds(player);
            String cdText = cooldownSec + "с";
            int textX = x + (ABILITY_ICON_SIZE - mc.font.width(cdText)) / 2;
            int textY = y + ABILITY_ICON_SIZE / 2 - 4;
            gui.drawString(mc.font, "§c" + cdText, textX, textY, 0xFFFFFF, true);
        }

        // Стоимость маны
        float manaCost = ability.getManaCost();
        if (manaCost > 0 && !ability.isOnCooldown(player)) {
            String costText = String.format("%.0f", manaCost);
            int costX = x + ABILITY_ICON_SIZE - mc.font.width(costText) - 2;
            int costY = y + ABILITY_ICON_SIZE - 10;

            gui.fill(costX - 1, costY - 1, costX + mc.font.width(costText) + 1, costY + 9, 0xAA000000);

            float currentMana = ClientManaData.getMana();
            int color = currentMana >= manaCost ? 0xFF00AAFF : 0xFFFF5555;
            gui.drawString(mc.font, costText, costX, costY, color, false);
        }
    }

    /**
     * НОВОЕ: Круговой прогресс-бар (ободок вокруг иконки)
     */
    private void renderCircularProgress(GuiGraphics gui, int x, int y, int size, float progress, int color) {
        int borderWidth = 3;

        // Рисуем 4 стороны с учетом прогресса
        float totalLength = (size * 4) - 4; // Периметр минус углы
        float currentLength = totalLength * (1.0f - progress); // Инвертируем для убывания

        // Верх (слева направо)
        if (currentLength > 0) {
            int topLength = Math.min((int)currentLength, size);
            gui.fill(x, y - borderWidth, x + topLength, y, color);
            currentLength -= size;
        }

        // Право (сверху вниз)
        if (currentLength > 0) {
            int rightLength = Math.min((int)currentLength, size);
            gui.fill(x + size, y, x + size + borderWidth, y + rightLength, color);
            currentLength -= size;
        }

        // Низ (справа налево)
        if (currentLength > 0) {
            int bottomLength = Math.min((int)currentLength, size);
            gui.fill(x + size - bottomLength, y + size, x + size, y + size + borderWidth, color);
            currentLength -= size;
        }

        // Лево (снизу вверх)
        if (currentLength > 0) {
            int leftLength = Math.min((int)currentLength, size);
            gui.fill(x - borderWidth, y + size - leftLength, x, y + size, color);
        }
    }

    /**
     * НОВОЕ: Пульсирующая рамка для активной способности
     */
    private void drawPulsingBorder(GuiGraphics gui, int x, int y, int size) {
        long time = System.currentTimeMillis();
        float pulse = (float) Math.sin(time / 200.0) * 0.3f + 0.7f; // 0.4 - 1.0

        int alpha = (int) (pulse * 255);
        int color = (alpha << 24) | 0x00FF00;

        // Верх и низ
        gui.fill(x - 1, y - 2, x + size + 1, y - 1, color);
        gui.fill(x - 1, y + size + 1, x + size + 1, y + size + 2, color);

        // Лево и право
        gui.fill(x - 2, y - 1, x - 1, y + size + 1, color);
        gui.fill(x + size + 1, y - 1, x + size + 2, y + size + 1, color);
    }

    private void renderHealthBar(GuiGraphics gui, Player player, int x, int y) {
        Minecraft mc = Minecraft.getInstance();

        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        float healthPercent = health / maxHealth;

        gui.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, HP_BG);

        int filledWidth = (int) (BAR_WIDTH * healthPercent);
        gui.fill(x, y, x + filledWidth, y + BAR_HEIGHT, HP_COLOR);

        gui.renderOutline(x, y, BAR_WIDTH, BAR_HEIGHT, PANEL_BORDER);

        String hpText = String.format("%.0f / %.0f", health, maxHealth);
        int textX = x + (BAR_WIDTH - mc.font.width(hpText)) / 2;
        int textY = y + (BAR_HEIGHT - 8) / 2;
        gui.drawString(mc.font, hpText, textX, textY, 0xFFFFFFFF, true);

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

        gui.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, MANA_BG);

        int filledWidth = (int) (BAR_WIDTH * manaPercent);
        gui.fill(x, y, x + filledWidth, y + BAR_HEIGHT, MANA_COLOR);

        gui.renderOutline(x, y, BAR_WIDTH, BAR_HEIGHT, PANEL_BORDER);

        String manaText = String.format("%.0f / %.0f", mana, maxMana);
        int textX = x + (BAR_WIDTH - mc.font.width(manaText)) / 2;
        int textY = y + (BAR_HEIGHT - 8) / 2;
        gui.drawString(mc.font, manaText, textX, textY, 0xFFFFFFFF, true);

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

        for (int i = 0; i < 6; i++) {
            int slotX = x + (i % 3) * (HOTBAR_SLOT_SIZE + 4);
            int slotY = y + (i / 3) * (HOTBAR_SLOT_SIZE + 4);

            gui.fill(slotX, slotY, slotX + HOTBAR_SLOT_SIZE, slotY + HOTBAR_SLOT_SIZE, SLOT_BG);

            boolean isSelected = (i == selectedSlot);
            int borderColor = isSelected ? SELECTED_SLOT_BORDER : SLOT_BORDER;
            gui.renderOutline(slotX, slotY, HOTBAR_SLOT_SIZE, HOTBAR_SLOT_SIZE, borderColor);

            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                gui.renderItem(stack, slotX + 8, slotY + 8);
                gui.renderItemDecorations(mc.font, stack, slotX + 8, slotY + 8);
            }

            String slotNum = String.valueOf(i + 1);
            gui.drawString(mc.font, slotNum, slotX + 2, slotY + 2, 0xFFAAAAAA, true);
        }
    }

    private void renderPenaltySlots(GuiGraphics gui, int x, int y, Player player) {
        Minecraft mc = Minecraft.getInstance();
        int selectedSlot = player.getInventory().selected;

        for (int i = 0; i < 3; i++) {
            int slotIndex = 6 + i;
            int slotX = x;
            int slotY = y + i * (PENALTY_SLOT_SIZE + 4);

            gui.fill(slotX, slotY, slotX + PENALTY_SLOT_SIZE, slotY + PENALTY_SLOT_SIZE, PENALTY_SLOT_BG);

            boolean isSelected = (slotIndex == selectedSlot);
            int borderColor = isSelected ? SELECTED_SLOT_BORDER : PENALTY_SLOT_BORDER;
            gui.renderOutline(slotX, slotY, PENALTY_SLOT_SIZE, PENALTY_SLOT_SIZE, borderColor);

            ItemStack stack = player.getInventory().getItem(slotIndex);
            if (!stack.isEmpty()) {
                gui.pose().pushPose();
                gui.pose().translate(slotX, slotY, 0);
                gui.pose().scale(0.9f, 0.9f, 0.9f);
                gui.renderItem(stack, 0, 0);
                gui.renderItemDecorations(mc.font, stack, 0, 0);
                gui.pose().popPose();
            }

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

    /**
     * НОВОЕ: Отображение подсказок по клавишам управления перками
     */
    private void renderPerkKeybindHints(GuiGraphics gui, int x, int y) {
        Minecraft mc = Minecraft.getInstance();

        // Получаем названия клавиш
        String activateKey = ModKeybinds.ACTIVATE_PERK.getTranslatedKeyMessage().getString();
        String switchKey = ModKeybinds.SWITCH_PERK.getTranslatedKeyMessage().getString();

        int hintY = y + (MAIN_PANEL_HEIGHT / 2) - 10;

        // Формируем строки подсказок
        String activateText = "§7[" + activateKey + "] Активация";
        String switchText = "§7[" + switchKey + "] Сменить";

        // Вычисляем X координаты с учетом ширины текста (выравнивание по правой стороне)
        int activateX = x + 120 - mc.font.width(activateText);
        int switchX = x + 120 - mc.font.width(switchText);

        // Подсказка активации
        gui.drawString(mc.font, activateText, activateX, hintY, 0xFFAAAAAA, false);

        // Подсказка смены перка
        gui.drawString(mc.font, switchText, switchX, hintY + 12, 0xFFAAAAAA, false);
    }


    private int getTypeColor(PerkType type) {
        return switch (type) {
            case PASSIVE -> 0xFF3355FF;
            case ACTIVE -> 0xFFFF5533;
            case HYBRID -> 0xFFAA55FF;
            case PASSIVE_COOLDOWN -> 0xFF3355FF;
        };
    }

    /**
     * НОВОЕ: Получение цвета текста для типа перка
     */
    private int getTypeFontColor(PerkType type) {
        return switch (type) {
            case PASSIVE -> 0xFF5555FF;
            case ACTIVE -> 0xFFFF5555;
            case HYBRID -> 0xFFFF55FF;
            case PASSIVE_COOLDOWN -> 0xFF5555FF;
        };
    }

    /**
     * НОВОЕ: Получение короткого названия типа перка
     */
    private String getTypeShort(PerkType type) {
        return switch (type) {
            case PASSIVE -> "П";
            case ACTIVE -> "А";
            case HYBRID -> "Г";
            case PASSIVE_COOLDOWN -> "ПК";
        };
    }
}