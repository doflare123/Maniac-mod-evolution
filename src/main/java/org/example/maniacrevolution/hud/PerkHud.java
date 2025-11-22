package org.example.maniacrevolution.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import com.mojang.blaze3d.systems.RenderSystem;
import org.example.maniacrevolution.data.ClientPlayerData;
import org.example.maniacrevolution.keybind.ModKeybinds;
import org.example.maniacrevolution.perk.PerkType;

import java.util.List;

public class PerkHud {
    private static final int ICON_SIZE = 24;
    private static final int SPACING = 4;

    public static void render(GuiGraphics gui, int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        List<ClientPlayerData.ClientPerkData> perks = ClientPlayerData.getSelectedPerks();

        if (perks.isEmpty()) return;

        int activeIndex = ClientPlayerData.getActivePerkIndex();

          //Фон панели
//        int panelWidth = perks.size() * (ICON_SIZE + SPACING) + 10;
//        gui.fill(x, y, x + panelWidth + 50, y + 65, 0x80000000);

        // Рендерим каждый перк
        for (int i = 0; i < perks.size(); i++) {
            ClientPlayerData.ClientPerkData perk = perks.get(i);
            int perkX = x + 5 + i * (ICON_SIZE + SPACING);
            int perkY = y + 5;

            // Рамка (жёлтая если выбран)
            int borderColor = (i == activeIndex) ? 0xFFFFFF00 : 0xFF888888;
            gui.renderOutline(perkX - 1, perkY - 1, ICON_SIZE + 2, ICON_SIZE + 2, borderColor);

            // Стрелка над выбранным перком
            if (i == activeIndex) {
                gui.drawString(mc.font, "§e▼", perkX + ICON_SIZE / 2 - 3, perkY - 10, 0xFFFFFF, false);
            }

            // Фон иконки
            int bgColor = getTypeColor(perk.type());
            gui.fill(perkX, perkY, perkX + ICON_SIZE, perkY + ICON_SIZE, bgColor);

            // Иконка перка (заглушка - первая буква названия)
            String initial = perk.id().substring(0, 1).toUpperCase();
            gui.drawString(mc.font, initial, perkX + 8, perkY + 8, 0xFFFFFF, true);

            // Оверлей кулдауна
            if (perk.isOnCooldown()) {
                float cdProgress = perk.getCooldownProgress();
                int cdHeight = (int) (ICON_SIZE * cdProgress);
                gui.fill(perkX, perkY + ICON_SIZE - cdHeight, perkX + ICON_SIZE, perkY + ICON_SIZE, 0xAA000000);

                // Текст кулдауна
                String cdText = perk.getCooldownSeconds() + "с";
                gui.drawString(mc.font, cdText, perkX + (ICON_SIZE - mc.font.width(cdText)) / 2,
                        perkY + ICON_SIZE / 2 - 3, 0xFFFFFF, true);
            }

            // Тип перка под иконкой
            String typeText = getTypeShort(perk.type());
            int typeColor = getTypeFontColor(perk.type());
            gui.drawString(mc.font, typeText, perkX + (ICON_SIZE - mc.font.width(typeText)) / 2,
                    perkY + ICON_SIZE + 2, typeColor, false);
        }

        // Подсказки по клавишам
        int hintY = y + 45;
        String activateKey = ModKeybinds.ACTIVATE_PERK.getTranslatedKeyMessage().getString();
        String switchKey = ModKeybinds.SWITCH_PERK.getTranslatedKeyMessage().getString();

        gui.drawString(mc.font, "§7[" + activateKey + "] Активация", x + 5, hintY, 0xAAAAAA, false);
        gui.drawString(mc.font, "§7[" + switchKey + "] Сменить", x + 5, hintY + 10, 0xAAAAAA, false);
    }

    private static int getTypeColor(PerkType type) {
        return switch (type) {
            case PASSIVE -> 0xFF3355FF;  // Синий
            case ACTIVE -> 0xFFFF5533;   // Красный
            case HYBRID -> 0xFFAA55FF;   // Фиолетовый
        };
    }

    private static int getTypeFontColor(PerkType type) {
        return switch (type) {
            case PASSIVE -> 0x5555FF;
            case ACTIVE -> 0xFF5555;
            case HYBRID -> 0xFF55FF;
        };
    }

    private static String getTypeShort(PerkType type) {
        return switch (type) {
            case PASSIVE -> "ПАС";
            case ACTIVE -> "АКТ";
            case HYBRID -> "ГИБ";
        };
    }
}
