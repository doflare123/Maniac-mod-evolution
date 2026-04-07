package org.example.maniacrevolution.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.example.maniacrevolution.keybind.ModKeybinds;

import java.util.Random;

public class QTEState {
    private static final int BASE_QTE_DURATION   = 750;
    private static final int BOX_SIZE            = 100;
    private static final int BASE_GREEN_ZONE_SIZE = 25;
    // Толерантность: рамка считается "в зелёной зоне" когда её размер
    // попадает в диапазон [greenZoneSize - tolerance, greenZoneSize + tolerance].
    // Рамка сжимается от BOX_SIZE до 0, значит она проходит через greenZoneSize
    // ровно один раз — это и есть момент успеха.
    private static final int BASE_SUCCESS_TOLERANCE = 13;
    /** Размер зоны критического успеха (фиолетовый квадрат внутри зелёного) */
    private static final int BASE_CRIT_ZONE_SIZE = 5;

    private final int requiredKey;
    private final long startTime;
    private boolean finished = false;
    private float shrinkProgress = 0f;

    private final int qteDuration;
    private final int greenZoneSize;
    private final int successTolerance;
    private final int critZoneSize;

    // Флаг "наказания": все 4 кнопки одинаковые → случайная клавиша
    private final boolean isPunishMode;
    private final int punishKey; // случайный keyCode если isPunishMode

    private static final Random RANDOM = new Random();

    public QTEState(int requiredKey, boolean hasQuickReflexes) {
        this.requiredKey = requiredKey;
        this.startTime = System.currentTimeMillis();

        if (hasQuickReflexes) {
            this.qteDuration      = BASE_QTE_DURATION + 450;
            this.greenZoneSize    = (int)(BASE_GREEN_ZONE_SIZE * 1.06f);
            this.successTolerance = (int)(BASE_SUCCESS_TOLERANCE * 1.05f);
            this.critZoneSize     = (int)(BASE_CRIT_ZONE_SIZE * 1.1f);
        } else {
            this.qteDuration      = BASE_QTE_DURATION;
            this.greenZoneSize    = BASE_GREEN_ZONE_SIZE;
            this.successTolerance = BASE_SUCCESS_TOLERANCE;
            this.critZoneSize     = BASE_CRIT_ZONE_SIZE;
        }

        // Проверяем — все 4 кнопки одинаковые?
        this.isPunishMode = areAllKeysSame();
        // Случайный keyCode из 26 букв A-Z (keyCode 65-90)
        this.punishKey = isPunishMode ? (65 + RANDOM.nextInt(26)) : -1;

        System.out.println("=== QTEState created ===");
        System.out.println("requiredKey=" + requiredKey + " punishMode=" + isPunishMode
                + (isPunishMode ? " punishKey=" + punishKey : ""));
        System.out.println("Duration=" + qteDuration + " GreenZone=" + greenZoneSize
                + " Tolerance=" + successTolerance);
        System.out.println("========================");
    }

    public void update() {
        long elapsed = System.currentTimeMillis() - startTime;
        shrinkProgress = Math.min(1f, (float) elapsed / qteDuration);
        if (shrinkProgress >= 1f) finished = true;
    }

    public void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        int cx = mc.getWindow().getGuiScaledWidth()  / 2;
        int cy = mc.getWindow().getGuiScaledHeight() - 135;

        int boxX = cx - BOX_SIZE / 2;
        int boxY = cy - BOX_SIZE / 2;

        // Фон
        g.fill(boxX, boxY, boxX + BOX_SIZE, boxY + BOX_SIZE, 0xAA000000);

        // Рамка коробки
        g.fill(boxX,               boxY,               boxX + BOX_SIZE, boxY + 2,            0xFFFFFFFF);
        g.fill(boxX,               boxY + BOX_SIZE - 2, boxX + BOX_SIZE, boxY + BOX_SIZE,     0xFFFFFFFF);
        g.fill(boxX,               boxY,               boxX + 2,        boxY + BOX_SIZE,      0xFFFFFFFF);
        g.fill(boxX + BOX_SIZE - 2, boxY,               boxX + BOX_SIZE, boxY + BOX_SIZE,     0xFFFFFFFF);

        // Зелёная зона (целевая область)
        int greenX = cx - greenZoneSize / 2;
        int greenY = cy - greenZoneSize / 2;
        g.fill(greenX, greenY, greenX + greenZoneSize, greenY + greenZoneSize, 0x8800FF00);
        // Рамка зелёной зоны чуть ярче
        g.fill(greenX,                    greenY,                    greenX + greenZoneSize, greenY + 2,              0xFF00FF00);
        g.fill(greenX,                    greenY + greenZoneSize - 2, greenX + greenZoneSize, greenY + greenZoneSize, 0xFF00FF00);
        g.fill(greenX,                    greenY,                    greenX + 2,             greenY + greenZoneSize,  0xFF00FF00);
        g.fill(greenX + greenZoneSize - 2, greenY,                    greenX + greenZoneSize, greenY + greenZoneSize, 0xFF00FF00);

        // Фиолетовая зона критического успеха (внутри зелёной, у самого конца)
        int critX = cx - critZoneSize / 2;
        int critY = cy - critZoneSize / 2;
        g.fill(critX, critY, critX + critZoneSize, critY + critZoneSize, 0x88AA00FF);
        // Рамка фиолетовой зоны
        g.fill(critX,                   critY,                   critX + critZoneSize, critY + 2,             0xFFAA00FF);
        g.fill(critX,                   critY + critZoneSize - 2, critX + critZoneSize, critY + critZoneSize, 0xFFAA00FF);
        g.fill(critX,                   critY,                   critX + 2,            critY + critZoneSize,  0xFFAA00FF);
        g.fill(critX + critZoneSize - 2, critY,                   critX + critZoneSize, critY + critZoneSize, 0xFFAA00FF);

        // Сжимающаяся красная рамка
        int currentSize = (int)(BOX_SIZE * (1f - shrinkProgress));
        int shrinkX = cx - currentSize / 2;
        int shrinkY = cy - currentSize / 2;
        int thickness = 4;
        // Цвет рамки: красный → жёлтый когда приближается к зелёной зоне
        int frameColor = getFrameColor(currentSize);
        g.fill(shrinkX,               shrinkY,                   shrinkX + currentSize, shrinkY + thickness,      frameColor);
        g.fill(shrinkX,               shrinkY + currentSize - thickness, shrinkX + currentSize, shrinkY + currentSize, frameColor);
        g.fill(shrinkX,               shrinkY,                   shrinkX + thickness,   shrinkY + currentSize,    frameColor);
        g.fill(shrinkX + currentSize - thickness, shrinkY,       shrinkX + currentSize, shrinkY + currentSize,    frameColor);

        // Текст: какую кнопку нажать
        String keyLabel = isPunishMode
                ? getEnglishKeyName(punishKey)
                : getEnglishKeybindName(requiredKey);
        String display = "Press: " + keyLabel;
        int textW = mc.font.width(display);
        g.drawString(mc.font, display, cx - textW / 2, boxY - 15, 0xFFFFFF);
    }

    /**
     * Проверяет нажатие и возвращает результат: FAIL / SUCCESS / CRIT.
     * Рамка сжимается от BOX_SIZE (100) до 0.
     * CRIT  — currentSize попадает в [critZoneSize ± critZoneSize/2]
     * SUCCESS — currentSize попадает в [greenZoneSize ± tolerance]
     */
    public QTEHitResult checkHit(int pressedKeyCode) {
        // Проверка кнопки
        if (isPunishMode) {
            if (pressedKeyCode != punishKey) {
                System.out.println("PUNISH FAIL: wrong key " + pressedKeyCode + " expected " + punishKey);
                return QTEHitResult.FAIL;
            }
        } else {
            if (pressedKeyCode != requiredKey) {
                System.out.println("FAIL: wrong key index");
                return QTEHitResult.FAIL;
            }
        }

        int currentSize = (int)(BOX_SIZE * (1f - shrinkProgress));
        int diffCrit  = Math.abs(currentSize - critZoneSize);
        int diffGreen = Math.abs(currentSize - greenZoneSize);
        int critTolerance = critZoneSize / 2;

        System.out.println("=== QTE Check ===");
        System.out.println("currentSize=" + currentSize);
        System.out.println("critZone=" + critZoneSize + " diffCrit=" + diffCrit + " critTol=" + critTolerance);
        System.out.println("greenZone=" + greenZoneSize + " diffGreen=" + diffGreen + " greenTol=" + successTolerance);

        if (diffCrit <= critTolerance) {
            System.out.println("Result=CRIT");
            System.out.println("=================");
            return QTEHitResult.CRIT;
        }
        if (diffGreen <= successTolerance) {
            System.out.println("Result=SUCCESS");
            System.out.println("=================");
            return QTEHitResult.SUCCESS;
        }
        System.out.println("Result=FAIL");
        System.out.println("=================");
        return QTEHitResult.FAIL;
    }

    /** Для совместимости со старым кодом */
    public boolean checkSuccess(int pressedKeyCode) {
        return checkHit(pressedKeyCode) != QTEHitResult.FAIL;
    }

    public enum QTEHitResult {
        FAIL, SUCCESS, CRIT;
        public boolean isSuccess() { return this != FAIL; }
    }

    public boolean isFinished()    { return finished; }
    public boolean isPunishMode()  { return isPunishMode; }
    public int     getPunishKey()  { return punishKey; }

    // ── Вспомогательные ───────────────────────────────────────────────────────

    /** Проверяет, все ли 4 привязанные кнопки одинаковы */
    private static boolean areAllKeysSame() {
        try {
            int k0 = ModKeybinds.QTE_KEY_1.getKey().getValue();
            int k1 = ModKeybinds.QTE_KEY_2.getKey().getValue();
            int k2 = ModKeybinds.QTE_KEY_3.getKey().getValue();
            int k3 = ModKeybinds.QTE_KEY_4.getKey().getValue();
            return k0 == k1 && k1 == k2 && k2 == k3;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Возвращает английское название привязки QTE по индексу (0..3).
     * Принудительно выводим английскую букву/название, игнорируя локаль.
     */
    private static String getEnglishKeybindName(int keyIndex) {
        com.mojang.blaze3d.platform.InputConstants.Key key = switch (keyIndex) {
            case 0 -> ModKeybinds.QTE_KEY_1.getKey();
            case 1 -> ModKeybinds.QTE_KEY_2.getKey();
            case 2 -> ModKeybinds.QTE_KEY_3.getKey();
            case 3 -> ModKeybinds.QTE_KEY_4.getKey();
            default -> null;
        };
        if (key == null) return "?";
        return getEnglishKeyName(key.getValue());
    }

    /**
     * Возвращает английское название клавиши по GLFW keyCode.
     * Не зависит от языка системы.
     */
    static String getEnglishKeyName(int keyCode) {
        // Буквы A-Z
        if (keyCode >= 65 && keyCode <= 90) {
            return String.valueOf((char) keyCode);
        }
        // Цифры 0-9
        if (keyCode >= 48 && keyCode <= 57) {
            return String.valueOf((char) keyCode);
        }
        // Цифровая панель
        if (keyCode >= 320 && keyCode <= 329) {
            return "NUM" + (keyCode - 320);
        }
        // F-клавиши
        if (keyCode >= 290 && keyCode <= 301) {
            return "F" + (keyCode - 289);
        }
        // Спецклавиши
        return switch (keyCode) {
            case 32  -> "Space";
            case 256 -> "Esc";
            case 257 -> "Enter";
            case 258 -> "Tab";
            case 259 -> "Backspace";
            case 260 -> "Insert";
            case 261 -> "Delete";
            case 262 -> "→";
            case 263 -> "←";
            case 264 -> "↓";
            case 265 -> "↑";
            case 280 -> "CapsLk";
            case 340 -> "Shift";
            case 341 -> "Ctrl";
            case 342 -> "Alt";
            case 344 -> "RShift";
            case 345 -> "RCtrl";
            case 346 -> "RAlt";
            case 44  -> ",";
            case 46  -> ".";
            case 47  -> "/";
            case 59  -> ";";
            case 39  -> "'";
            case 91  -> "[";
            case 93  -> "]";
            case 92  -> "\\";
            case 45  -> "-";
            case 61  -> "=";
            case 96  -> "`";
            default  -> "Key" + keyCode;
        };
    }

    /** Цвет сжимающейся рамки: красный → жёлтый по мере приближения к зелёной зоне */
    private int getFrameColor(int currentSize) {
        float distRatio = Math.abs(currentSize - greenZoneSize) / (float)(BOX_SIZE - greenZoneSize);
        distRatio = Math.min(1f, distRatio);
        // 1.0 = далеко = красный, 0.0 = близко = жёлтый
        int g = (int)(255 * (1f - distRatio));
        return 0xFF000000 | (255 << 16) | (g << 8);
    }
}