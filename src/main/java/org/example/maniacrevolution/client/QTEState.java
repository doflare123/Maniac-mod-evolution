package org.example.maniacrevolution.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.example.maniacrevolution.keybind.ModKeybinds;

public class QTEState {
    private static final int BASE_QTE_DURATION = 750;
    private static final int BOX_SIZE = 100;
    private static final int BASE_GREEN_ZONE_SIZE = 25;
    private static final int BASE_SUCCESS_TOLERANCE = 15;

    private final int requiredKey;
    private final long startTime;
    private boolean finished = false;
    private float shrinkProgress = 0f;

    private final int qteDuration;
    private final int greenZoneSize;
    private final int successTolerance;

    public QTEState(int requiredKey, boolean hasQuickReflexes) {
        this.requiredKey = requiredKey;
        this.startTime = System.currentTimeMillis();

        // ОТЛАДКА
        System.out.println("=== QTEState created ===");
        System.out.println("hasQuickReflexes = " + hasQuickReflexes);

        if (hasQuickReflexes) {
            this.qteDuration = BASE_QTE_DURATION + 400;
            this.greenZoneSize = (int) (BASE_GREEN_ZONE_SIZE * 1.9f);
            this.successTolerance = (int) (BASE_SUCCESS_TOLERANCE * 1.1f);

            System.out.println("WITH PERK:");
        } else {
            this.qteDuration = BASE_QTE_DURATION;
            this.greenZoneSize = BASE_GREEN_ZONE_SIZE;
            this.successTolerance = BASE_SUCCESS_TOLERANCE;

            System.out.println("WITHOUT PERK:");
        }

        System.out.println("  Duration: " + qteDuration + " ms (base: " + BASE_QTE_DURATION + ")");
        System.out.println("  Green zone: " + greenZoneSize + " px (base: " + BASE_GREEN_ZONE_SIZE + ")");
        System.out.println("  Tolerance: " + successTolerance + " px (base: " + BASE_SUCCESS_TOLERANCE + ")");
        System.out.println("=======================");
    }

    public void update() {
        long elapsed = System.currentTimeMillis() - startTime;
        shrinkProgress = Math.min(1f, (float) elapsed / qteDuration);

        if (shrinkProgress >= 1f) {
            finished = true;
        }
    }

    public void render(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int centerX = screenWidth / 2;
        int centerY = screenHeight - 90;

        int boxX = centerX - BOX_SIZE / 2;
        int boxY = centerY - BOX_SIZE / 2;

        guiGraphics.fill(boxX, boxY, boxX + BOX_SIZE, boxY + BOX_SIZE, 0xAA000000);
        guiGraphics.fill(boxX, boxY, boxX + BOX_SIZE, boxY + 2, 0xFFFFFFFF);
        guiGraphics.fill(boxX, boxY + BOX_SIZE - 2, boxX + BOX_SIZE, boxY + BOX_SIZE, 0xFFFFFFFF);
        guiGraphics.fill(boxX, boxY, boxX + 2, boxY + BOX_SIZE, 0xFFFFFFFF);
        guiGraphics.fill(boxX + BOX_SIZE - 2, boxY, boxX + BOX_SIZE, boxY + BOX_SIZE, 0xFFFFFFFF);

        int greenX = centerX - greenZoneSize / 2;
        int greenY = centerY - greenZoneSize / 2;
        guiGraphics.fill(greenX, greenY, greenX + greenZoneSize, greenY + greenZoneSize, 0xFF00FF00);

        int currentSize = (int) (BOX_SIZE * (1f - shrinkProgress));
        int shrinkX = centerX - currentSize / 2;
        int shrinkY = centerY - currentSize / 2;

        int thickness = 4;
        guiGraphics.fill(shrinkX, shrinkY, shrinkX + currentSize, shrinkY + thickness, 0xFFFF0000);
        guiGraphics.fill(shrinkX, shrinkY + currentSize - thickness, shrinkX + currentSize, shrinkY + currentSize, 0xFFFF0000);
        guiGraphics.fill(shrinkX, shrinkY, shrinkX + thickness, shrinkY + currentSize, 0xFFFF0000);
        guiGraphics.fill(shrinkX + currentSize - thickness, shrinkY, shrinkX + currentSize, shrinkY + currentSize, 0xFFFF0000);

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
            System.out.println("FAIL: Wrong key!");
            return false;
        }

        int currentSize = (int) (BOX_SIZE * (1f - shrinkProgress));
        int diff = Math.abs(currentSize - greenZoneSize);
        boolean success = diff <= successTolerance;

        System.out.println("=== QTE Check ===");
        System.out.println("Current size: " + currentSize);
        System.out.println("Green zone: " + greenZoneSize);
        System.out.println("Difference: " + diff);
        System.out.println("Tolerance: " + successTolerance);
        System.out.println("Success: " + success);
        System.out.println("=================");

        return success;
    }

    public boolean isFinished() {
        return finished;
    }
}