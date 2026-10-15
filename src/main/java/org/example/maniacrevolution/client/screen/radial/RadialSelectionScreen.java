package org.example.maniacrevolution.client.screen.radial;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Shared hold-to-select wheel, with clockwise entries beginning at the top. */
public class RadialSelectionScreen extends Screen {
    private final RadialSelectionMenu menu;
    private final int count;
    private int selected;
    private boolean finished;
    private double lastMouseX = Double.NaN, lastMouseY = Double.NaN;

    public RadialSelectionScreen(RadialSelectionMenu menu) {
        super(menu.title());
        this.menu = menu;
        count = menu.entries().size();
        if (count < 1) throw new IllegalArgumentException("A wheel needs at least one entry");
        selected = Math.floorMod(menu.initialSelection(), count);
    }
    @Override public boolean isPauseScreen() { return false; }
    private int radius() { return Math.max(58, Math.min(112, Math.min(width / 3, (height - 100) / 2))); }
    private void hover(double x, double y) {
        int hovered = RadialSelectionMath.sector(x - width / 2.0, y - height / 2.0, radius() * 0.43, count);
        if (hovered >= 0) selected = hovered;
        lastMouseX = x; lastMouseY = y;
    }
    @Override public void mouseMoved(double x, double y) { hover(x, y); }
    @Override public void tick() { if (!menu.valid()) onClose(); }
    @Override public void render(GuiGraphics gui, int mouseX, int mouseY, float partial) {
        gui.fill(0, 0, width, height, 0x9A080B12);
        int cx = width / 2, cy = height / 2, radius = radius();
        double inner = radius * 0.43;
        if (mouseX != lastMouseX || mouseY != lastMouseY) hover(mouseX, mouseY);
        var entries = menu.entries();
        gui.drawCenteredString(font, title, cx, cy - radius - 25, 0xFFF0E6D4);
        gui.flush();
        for (int i = 0; i < count; i++) {
            int color = entries.get(i).color();
            wedge(gui, cx, cy, inner, radius, i, i == selected ? 0xD8000000 | color : 0xDE202632);
            wedge(gui, cx, cy, radius - 3, radius, i, i == selected ? 0xFF000000 | color : 0xFF485163);
        }
        for (int i = 0; i < count; i++) {
            var entry = entries.get(i);
            double angle = i * Math.PI * 2 / count;
            int x = cx + (int) (Math.sin(angle) * radius * 0.72);
            int y = cy - (int) (Math.cos(angle) * radius * 0.72);
            gui.pose().pushPose(); gui.pose().translate(x - 12, y - 16, 0); gui.pose().scale(1.5F, 1.5F, 1.5F);
            gui.renderItem(entry.icon(), 0, 0);
            gui.pose().popPose();
            gui.drawString(font, entry.badge(), x + 10, y - 16, entry.available() ? 0xFFFFFFFF : 0xFFFF8793);
            int textY = y + 12;
            for (var line : font.split(entry.label(), Math.max(48, radius - 18))) {
                gui.drawCenteredString(font, line, x, textY, entry.available() ? 0xFFE8EDF7 : 0xFF949AAA); textY += 9;
            }
        }
        int centerY = cy - 10;
        var chosen = entries.get(selected);
        for (var line : font.split(chosen.label(), (int) inner * 2 - 10)) {
            gui.drawCenteredString(font, line, cx, centerY, 0xFF000000 | chosen.color()); centerY += 10;
        }
        for (var detail : menu.details(selected))
            for (var line : font.split(detail, (int) inner * 2 - 10)) {
                gui.drawCenteredString(font, line, cx, centerY, 0xFFD5DCE8); centerY += 9;
            }
        gui.drawCenteredString(font, Component.translatable("screen.maniacrev.radial.confirm", menu.binding().getTranslatedKeyMessage()),
                cx, cy + radius + 16, 0xFFD5DCE8);
        int lineY = cy + radius + 29;
        for (var footer : menu.footer())
            for (var line : font.split(footer, width - 24)) {
                gui.drawCenteredString(font, line, cx, lineY, 0xFFE7B9C7); lineY += 9;
            }
    }
    private void wedge(GuiGraphics gui, int cx, int cy, double inner, double outer, int sector, int color) {
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest(); RenderSystem.setShader(GameRenderer::getPositionColorShader);
        var matrix = gui.pose().last().pose(); var buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        double stepAngle = Math.PI * 2 / count;
        double start = sector * stepAngle - stepAngle / 2 + 0.035;
        double end = sector * stepAngle + stepAngle / 2 - 0.035;
        int r = color >> 16 & 255, g = color >> 8 & 255, b = color & 255, a = color >>> 24;
        for (int step = 0; step <= 24; step++) {
            double angle = start + (end - start) * step / 24;
            for (double radius : new double[]{outer, inner})
                buffer.vertex(matrix, (float) (cx + Math.sin(angle) * radius), (float) (cy - Math.cos(angle) * radius), 0)
                        .color(r, g, b, a).endVertex();
        }
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.enableDepthTest(); RenderSystem.disableBlend();
    }
    private void confirm() {
        if (finished) return;
        finished = true;
        // Close before sending: a selection must never let the click reach the world.
        minecraft.setScreen(null);
        if (menu.valid()) menu.select(selected);
    }
    @Override public void onClose() { finished = true; super.onClose(); }
    @Override public boolean keyReleased(int key, int scan, int modifiers) {
        if (menu.binding().matches(key, scan)) { confirm(); return true; }
        return super.keyReleased(key, scan, modifiers);
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) { hover(x, y); confirm(); return true; }
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) { onClose(); return true; }
        return super.mouseClicked(x, y, button);
    }
    @Override public boolean mouseReleased(double x, double y, int button) {
        if (menu.binding().matchesMouse(button)) { hover(x, y); confirm(); return true; }
        return super.mouseReleased(x, y, button);
    }
    @Override public boolean mouseScrolled(double x, double y, double delta) {
        if (delta != 0) selected = Math.floorMod(selected + (delta > 0 ? -1 : 1), count);
        return true;
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_DOWN) { selected = (selected + 1) % count; return true; }
        if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_UP) { selected = Math.floorMod(selected - 1, count); return true; }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) { confirm(); return true; }
        return super.keyPressed(key, scan, modifiers);
    }
}
