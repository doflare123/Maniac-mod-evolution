package org.example.maniacrevolution.hud;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.example.maniacrevolution.block.entity.FNAFGeneratorBlockEntity;

public final class GeneratorChargeHud {
    public static final int WIDTH = 104;
    public static final int HEIGHT = 19;
    private static float displayedCharge = -1.0f;

    private GeneratorChargeHud() {
    }

    public static boolean render(GuiGraphics gui, int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return false;

        FNAFGeneratorBlockEntity generator = FNAFGeneratorBlockEntity.getInstance();
        if (generator == null) return false;

        float charge = Mth.clamp(generator.getChargePercentage(), 0.0f, 1.0f);
        displayedCharge = displayedCharge < 0.0f
                ? charge
                : Mth.lerp(mc.isPaused() ? 0.0f : 0.14f, displayedCharge, charge);
        int color = getChargeColor(charge);
        boolean criticalPulse = charge <= 0.2f && (Util.getMillis() / 350L) % 2L == 0L;

        gui.fill(x, y, x + WIDTH, y + HEIGHT, criticalPulse ? 0xC53A1719 : 0xB5101216);
        gui.renderOutline(x, y, WIDTH, HEIGHT, criticalPulse ? 0xFFE05252 : 0xCC59616C);

        String title = "Генератор";
        String value = Math.round(charge * 100.0f) + "%";
        gui.drawString(mc.font, title, x + 5, y + 3, 0xFFC7CDD4, false);
        gui.drawString(mc.font, value, x + WIDTH - mc.font.width(value) - 5, y + 3, color, true);

        gui.fill(x + 1, y + HEIGHT - 3, x + WIDTH - 1, y + HEIGHT - 1, 0xFF272C32);
        gui.fill(x + 1, y + HEIGHT - 3,
                x + 1 + Math.round((WIDTH - 2) * displayedCharge), y + HEIGHT - 1, color);
        return true;
    }

    private static int getChargeColor(float charge) {
        if (charge > 0.6f) return 0xFF70E28A;
        if (charge > 0.3f) return 0xFFFFC857;
        return 0xFFE05252;
    }
}
