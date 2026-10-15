package org.example.maniacrevolution.scp173.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Moving eyelids first; the status panel is explicitly drawn above them. */
public final class Scp173BlinkOverlay {
    private Scp173BlinkOverlay() {}
    public static void mask(GuiGraphics graphics, int width, int height, float closure) {
        if (closure <= 0) return;
        int covered = (int) Math.ceil(height * 0.5 * Mth.clamp(closure, 0, 1));
        graphics.fill(0, 0, width, covered, 0xFF000000);
        graphics.fill(0, height - covered, width, height, 0xFF000000);
        graphics.flush();
    }
    public static void panel(GuiGraphics graphics, int width, double meter, double pressure, Component key) {
        var mc = Minecraft.getInstance(); int x = width / 2 - 60;
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(0, 0, 500);
            graphics.fill(x - 7, 24, x + 127, 54, 0xD016171A);
            graphics.drawCenteredString(mc.font, Component.translatable("hud.maniacrev.scp173.blink", key), width / 2, 28, 0xE6D8B8);
            graphics.fill(x, 40, x + 120, 46, 0xFF333338);
            graphics.fill(x, 40, x + (int) Math.round(120 * Mth.clamp(meter, 0, 1)), 46, 0xFFD0C2A2);
            graphics.fill(x, 48, x + (int) Math.round(120 * Mth.clamp(pressure, 0, 1)), 50, 0xFFC66040);
            graphics.flush();
        } finally { graphics.pose().popPose(); }
    }
}
