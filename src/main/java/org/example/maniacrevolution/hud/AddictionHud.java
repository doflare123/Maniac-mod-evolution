package org.example.maniacrevolution.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.example.maniacrevolution.client.ClientAddictionData;

/**
 * Вертикальная шкала зависимости.
 *
 * ═══════════════════════════════════════════════════════════════════
 *  КАК ВСТРОИТЬ В CustomHud.renderMainPanel():
 * ═══════════════════════════════════════════════════════════════════
 *
 *  В конец метода renderMainPanel(), ПОСЛЕ строки:
 *      renderManaBar(gui, currentX, barsY + BAR_HEIGHT + 4);
 *
 *  Добавьте:
 *      // Вертикальная шкала зависимости справа от баров
 *      int addX = currentX + BAR_WIDTH + 6;
 *      int addH = BAR_HEIGHT * 2 + 4;   // ровно высота двух баров
 *      AddictionHud.render(gui, addX, barsY, addH);
 *
 *  Импорт:
 *      import org.example.maniacrevolution.hud.AddictionHud;
 * ═══════════════════════════════════════════════════════════════════
 */
public class AddictionHud {

    private static final int BAR_W = 12;     // ширина вертикальной шкалы

    // Цвета по стадиям
    private static final int[] STAGE_COLOR = {
            0xFF3AB03A,  // 0 — зелёный
            0xFFCCCC00,  // 1 — жёлтый
            0xFFCC6600,  // 2 — оранжевый
            0xFF990000,  // 3 — тёмно-красный
    };
    private static final int BG_COLOR     = 0xFF111111;
    private static final int BORDER_COLOR = 0xFF555555;
    private static final int DIVIDER_COLOR = 0x88FFFFFF;

    /**
     * @param gui    GuiGraphics
     * @param x      левый край шкалы
     * @param y      верхний край шкалы
     * @param height высота шкалы (= BAR_HEIGHT*2+4 из CustomHud)
     */
    public static void render(GuiGraphics gui, int x, int y, int height) {
        if (!ClientAddictionData.isVisible()) return;

        float progress = ClientAddictionData.getProgress(); // 0..1
        int   stage    = ClientAddictionData.getStage();    // 0..3

        // ── Фон ──────────────────────────────────────────────────────────────
        gui.fill(x, y, x + BAR_W, y + height, BG_COLOR);
        gui.renderOutline(x, y, BAR_W, height, BORDER_COLOR);

        // ── Заполненная часть (снизу → вверх) ────────────────────────────────
        int filledH = (int) (height * progress);
        int fillTop = y + height - filledH;
        if (filledH > 0) {
            gui.fill(x + 1, fillTop, x + BAR_W - 1, y + height,
                    STAGE_COLOR[Math.min(stage, 3)]);
        }

        // ── Пульс на 3 стадии ─────────────────────────────────────────────────
        if (stage == 3) {
            long time = System.currentTimeMillis();
            float pulse = (float)(Math.sin(time / 300.0) * 0.5 + 0.5);
            int alpha = (int)(pulse * 100) + 30;
            gui.fill(x + 1, fillTop, x + BAR_W - 1, y + height, (alpha << 24) | 0xFF2200);
        }

        // ── Разделители стадий (25 / 50 / 75%) ───────────────────────────────
        for (float thr : new float[]{0.25f, 0.50f, 0.75f}) {
            int lineY = y + height - (int)(height * thr);
            gui.fill(x, lineY, x + BAR_W, lineY + 1, DIVIDER_COLOR);
        }

        // ── Номер стадии (если > 0) ───────────────────────────────────────────
        if (stage > 0) {
            Minecraft mc = Minecraft.getInstance();
            String txt = "§c" + stage;
            int tx = x + (BAR_W - mc.font.width(txt)) / 2;
            gui.drawString(mc.font, txt, tx, y + 2, 0xFFFFFF, true);
        }
    }
}