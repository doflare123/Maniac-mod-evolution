package org.example.maniacrevolution.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.example.maniacrevolution.gui.pages.GuidePage;

/** Shared visual language for every page of the in-game guide. */
public final class GuideTheme {
    public static final int PANEL = 0xF01B222A;
    public static final int HEADER = 0xF02A333D;
    public static final int SURFACE = 0xE52A333D;
    public static final int SURFACE_HOVER = 0xF0384551;
    public static final int SURFACE_SELECTED = 0xF03D4955;
    public static final int BORDER = 0xFF788592;
    public static final int BORDER_SOFT = 0xFF4C5864;
    public static final int TEXT = 0xFFF7F9FB;
    public static final int TEXT_SECONDARY = 0xFFC5CDD5;
    public static final int TEXT_MUTED = 0xFF929DA8;

    public static final int GOLD = 0xFFFFC857;
    public static final int GREEN = 0xFF70E28A;
    public static final int PURPLE = 0xFFC28BFF;
    public static final int RED = 0xFFFF7B70;
    public static final int BLUE = 0xFF65BCE8;

    private GuideTheme() {}

    public static int accent(GuidePage.PageType type) {
        return switch (type) {
            case MAIN, TUTORIAL -> GOLD;
            case PERKS -> GREEN;
            case MAPS -> RED;
            case CHARACTERS -> PURPLE;
        };
    }

    public static void drawButton(GuiGraphics gui, Font font, int x, int y, int width, int height,
                                  String label, int accent, boolean hovered, boolean selected) {
        int background = selected ? SURFACE_SELECTED : hovered ? SURFACE_HOVER : SURFACE;
        int border = hovered ? lerpColor(accent, 0xFF000000, 0.22f) : accent;
        gui.fill(x, y, x + width, y + height, background);
        gui.renderOutline(x, y, width, height, border);
        if (selected || hovered) {
            gui.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, border);
        }
        gui.drawCenteredString(font, label, x + width / 2, y + (height - 8) / 2,
                selected || hovered ? TEXT : TEXT_SECONDARY);
    }

    public static void drawBackButton(GuiGraphics gui, Font font, int x, int y, int width,
                                      String label, int accent, int mouseX, int mouseY) {
        drawButton(gui, font, x, y, width, 18, label, accent,
                inside(mouseX, mouseY, x, y, width, 18), false);
    }

    public static void drawCard(GuiGraphics gui, int x, int y, int width, int height,
                                int accent, boolean hovered) {
        int border = hovered ? lerpColor(accent, 0xFF000000, 0.22f) : accent;
        gui.fill(x, y, x + width, y + height, hovered ? SURFACE_HOVER : SURFACE);
        gui.renderOutline(x, y, width, height, border);
        gui.fill(x, y, x + (hovered ? 3 : 2), y + height, border);
    }

    public static void drawPageTitle(GuiGraphics gui, Font font, String title, String subtitle,
                                     int centerX, int y, int accent) {
        gui.drawCenteredString(font, title, centerX, y, TEXT);
        if (subtitle != null && !subtitle.isBlank()) {
            gui.drawCenteredString(font, subtitle, centerX, y + 13, TEXT_MUTED);
        }
        int lineWidth = Math.min(110, Math.max(38, font.width(title) / 2));
        gui.fill(centerX - lineWidth / 2, y + (subtitle == null ? 13 : 26),
                centerX + lineWidth / 2, y + (subtitle == null ? 14 : 27), accent);
    }

    public static void drawScrollHint(GuiGraphics gui, Font font, int right, int bottom) {
        String label = "↑↓  Колесо мыши";
        int width = font.width(label) + 12;
        gui.fill(right - width, bottom - 15, right, bottom, 0xE52A333D);
        gui.renderOutline(right - width, bottom - 15, width, 15, BORDER_SOFT);
        gui.drawString(font, label, right - width + 6, bottom - 12, TEXT_MUTED, false);
    }

    public static boolean inside(double mouseX, double mouseY,
                                 int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public static int lerpColor(int from, int to, float amount) {
        float value = Mth.clamp(amount, 0.0f, 1.0f);
        int a = Math.round(((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * value);
        int r = Math.round(((from >>> 16) & 0xFF) + (((to >>> 16) & 0xFF) - ((from >>> 16) & 0xFF)) * value);
        int g = Math.round(((from >>> 8) & 0xFF) + (((to >>> 8) & 0xFF) - ((from >>> 8) & 0xFF)) * value);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * value);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
