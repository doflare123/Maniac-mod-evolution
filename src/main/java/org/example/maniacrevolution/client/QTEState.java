package org.example.maniacrevolution.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.example.maniacrevolution.keybind.ModKeybinds;

public class QTEState {
    private static final int QTE_DURATION = 2000; // 2 секунды на реакцию
    private static final int BOX_SIZE = 100;
    private static final int GREEN_ZONE_SIZE = 20;

    private final int requiredKey; // Какую кнопку нужно нажать (0-3)
    private final long startTime;
    private boolean finished = false;

    private float shrinkProgress = 0f;

    public QTEState(int requiredKey) {
        this.requiredKey = requiredKey;
        this.startTime = System.currentTimeMillis();
    }

    public void update() {
        long elapsed = System.currentTimeMillis() - startTime;
        shrinkProgress = Math.min(1f, (float) elapsed / QTE_DURATION);

        if (shrinkProgress >= 1f) {
            finished = true;
        }
    }

    public void render(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // Позиция над хотбаром
        int centerX = screenWidth / 2;
        int centerY = screenHeight - 90; // Над хотбаром

        int boxX = centerX - BOX_SIZE / 2;
        int boxY = centerY - BOX_SIZE / 2;

        // Рендерим внешний квадрат (темный фон)
        guiGraphics.fill(boxX, boxY, boxX + BOX_SIZE, boxY + BOX_SIZE, 0xAA000000);

        // Рендерим рамку квадрата
        guiGraphics.fill(boxX, boxY, boxX + BOX_SIZE, boxY + 2, 0xFFFFFFFF); // Верх
        guiGraphics.fill(boxX, boxY + BOX_SIZE - 2, boxX + BOX_SIZE, boxY + BOX_SIZE, 0xFFFFFFFF); // Низ
        guiGraphics.fill(boxX, boxY, boxX + 2, boxY + BOX_SIZE, 0xFFFFFFFF); // Лево
        guiGraphics.fill(boxX + BOX_SIZE - 2, boxY, boxX + BOX_SIZE, boxY + BOX_SIZE, 0xFFFFFFFF); // Право

        // Зеленая зона в центре
        int greenX = centerX - GREEN_ZONE_SIZE / 2;
        int greenY = centerY - GREEN_ZONE_SIZE / 2;
        guiGraphics.fill(greenX, greenY, greenX + GREEN_ZONE_SIZE, greenY + GREEN_ZONE_SIZE, 0xFF00FF00);

        // Сужающаяся область (красная)
        int currentSize = (int) (BOX_SIZE * (1f - shrinkProgress));
        int shrinkX = centerX - currentSize / 2;
        int shrinkY = centerY - currentSize / 2;

        // Рендерим сужающуюся рамку
        int thickness = 4;
        guiGraphics.fill(shrinkX, shrinkY, shrinkX + currentSize, shrinkY + thickness, 0xFFFF0000); // Верх
        guiGraphics.fill(shrinkX, shrinkY + currentSize - thickness, shrinkX + currentSize, shrinkY + currentSize, 0xFFFF0000); // Низ
        guiGraphics.fill(shrinkX, shrinkY, shrinkX + thickness, shrinkY + currentSize, 0xFFFF0000); // Лево
        guiGraphics.fill(shrinkX + currentSize - thickness, shrinkY, shrinkX + currentSize, shrinkY + currentSize, 0xFFFF0000); // Право

        // Отображаем какую кнопку нужно нажать
        String keyName = getKeyName(requiredKey);
        Component keyText = Component.literal("Press: " + keyName);
        int textWidth = mc.font.width(keyText);
        guiGraphics.drawString(mc.font, keyText, centerX - textWidth / 2, boxY - 15, 0xFFFFFF);
    }

    private String getKeyName(int keyIndex) {
        return switch (keyIndex) {
            case 0 -> ModKeybinds.QTE_KEY_1.getTranslatedKeyMessage().getString();
            case 1 -> ModKeybinds.QTE_KEY_2.getTranslatedKeyMessage().getString();
            case 2 -> ModKeybinds.QTE_KEY_3.getTranslatedKeyMessage().getString();
            case 3 -> ModKeybinds.QTE_KEY_4.getTranslatedKeyMessage().getString();
            default -> "?";
        };
    }

    public boolean checkSuccess(int pressedKey) {
        if (pressedKey != requiredKey) {
            return false; // Неправильная кнопка
        }

        // Проверяем, находится ли сужающаяся область внутри зеленой зоны
        int currentSize = (int) (BOX_SIZE * (1f - shrinkProgress));

        // Успех если текущий размер примерно равен зеленой зоне (±15 пикселей)
        return Math.abs(currentSize - GREEN_ZONE_SIZE) <= 15;
    }

    public boolean isFinished() {
        return finished;
    }
}