package org.example.maniacrevolution.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.example.maniacrevolution.block.entity.FNAFGeneratorBlockEntity;

/**
 * Рендер индикатора заряда FNAF генератора для интеграции в CustomHud
 */
public class GeneratorChargeHud {

    // ВЕРТИКАЛЬНАЯ батарейка
    private static final int BATTERY_WIDTH = 30;
    private static final int BATTERY_HEIGHT = 60;
    private static final int SEGMENT_WIDTH = 20;
    private static final int SEGMENT_HEIGHT = 8;
    private static final int SEGMENT_SPACING = 2;
    private static final int NUM_SEGMENTS = 5;

    private static int blinkTimer = 0;

    /**
     * Рендер индикатора заряда генератора
     * @param guiGraphics Графический контекст
     * @param x X координата (левый верхний угол)
     * @param y Y координата (левый верхний угол)
     * @return true если генератор найден и отрисован, false если генератора нет
     */
    public static boolean render(GuiGraphics guiGraphics, int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return false;

        // Ищем генератор в мире
        FNAFGeneratorBlockEntity generator = findGeneratorInWorld(mc.level);
        if (generator == null) return false;

        blinkTimer++;

        float chargePercent = generator.getChargePercentage();
        int activeSegments = Math.round(chargePercent * NUM_SEGMENTS);

        // Рисуем рамку батарейки
        drawBatteryFrame(guiGraphics, x, y);

        // Рисуем сегменты заряда
        if (chargePercent > 0) {
            drawChargeSegments(guiGraphics, x, y, activeSegments, chargePercent);
        }

        // Рисуем процент
        String percentText = String.format("%.0f%%", chargePercent * 100);
        int textX = x + BATTERY_WIDTH / 2 - mc.font.width(percentText) / 2;
        int textY = y + BATTERY_HEIGHT + 5;
        guiGraphics.drawString(mc.font, percentText, textX, textY, getColorForCharge(chargePercent), true);

        return true;
    }

    private static void drawBatteryFrame(GuiGraphics guiGraphics, int x, int y) {
        // Выступ батарейки (верх)
        int tipWidth = 10;
        int tipHeight = 4;
        int tipX = x + (BATTERY_WIDTH - tipWidth) / 2;
        guiGraphics.fill(tipX, y, tipX + tipWidth, y + tipHeight, 0xFF3F3F3F);

        // Основной корпус батарейки
        guiGraphics.fill(x, y + tipHeight, x + BATTERY_WIDTH, y + BATTERY_HEIGHT, 0xFF3F3F3F);
        guiGraphics.fill(x + 2, y + tipHeight + 2, x + BATTERY_WIDTH - 2, y + BATTERY_HEIGHT - 2, 0xFF1A1A1A);
    }

    private static void drawChargeSegments(GuiGraphics guiGraphics, int x, int y, int activeSegments, float chargePercent) {
        int startX = x + 5;
        int startY = y + 10; // Начинаем ниже выступа

        // Рисуем сегменты СНИЗУ ВВЕРХ (как заряд батарейки)
        for (int i = 0; i < NUM_SEGMENTS; i++) {
            // Индекс с конца (снизу вверх)
            int segmentIndex = NUM_SEGMENTS - 1 - i;
            int segY = startY + i * (SEGMENT_HEIGHT + SEGMENT_SPACING);

            if (segmentIndex < activeSegments) {
                // Активный сегмент
                int color = getColorForSegment(segmentIndex, chargePercent);
                guiGraphics.fill(startX, segY, startX + SEGMENT_WIDTH, segY + SEGMENT_HEIGHT, color);
            } else {
                // Неактивный сегмент (тёмно-серый)
                guiGraphics.fill(startX, segY, startX + SEGMENT_WIDTH, segY + SEGMENT_HEIGHT, 0xFF2A2A2A);
            }
        }
    }

    private static int getColorForSegment(int segmentIndex, float chargePercent) {
        // Цвет зависит от процента заряда
        if (chargePercent > 0.6f) {
            // Зелёный
            return 0xFF00FF00;
        } else if (chargePercent > 0.3f) {
            // Жёлто-оранжевый
            return 0xFFFFAA00;
        } else {
            // Красный
            return 0xFFFF0000;
        }
    }

    private static int getColorForCharge(float chargePercent) {
        if (chargePercent > 0.6f) {
            return 0x00FF00; // Зелёный
        } else if (chargePercent > 0.5f) {
            return 0xFFAA00; // Оранжевый
        } else if (chargePercent > 0.3f) {
            return 0xFF0000; // Красный
        } else {
            return 0xFFFF00; // Жёлтый (разряжен)
        }
    }

    private static FNAFGeneratorBlockEntity findGeneratorInWorld(Level level) {
        // Получаем инстанс генератора из статического поля
        return FNAFGeneratorBlockEntity.getInstance();
    }
}