package org.example.maniacrevolution.helsing;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.example.maniacrevolution.keybind.ModKeybinds;
import org.example.maniacrevolution.network.ModNetworking;
import org.lwjgl.glfw.GLFW;

/** Hold the binding, point at a sector, release to confirm. Esc cancels. */
public final class HelsingBoltWheelScreen extends Screen {
    private final InteractionHand hand;
    private int selected;
    private double lastMouseX = Double.NaN, lastMouseY = Double.NaN;
    private static final int[] COLORS = {0xA3B4CC, 0xBA8DF0, 0xE5F6FF, 0xF46C87};
    public HelsingBoltWheelScreen(InteractionHand hand) {
        super(Component.translatable("screen.maniacrev.helsing.bolts"));
        this.hand = hand;
        var player = net.minecraft.client.Minecraft.getInstance().player;
        selected = player == null ? 0 : HelsingCrossbowItem.selected(player.getItemInHand(hand)).ordinal();
    }
    @Override public boolean isPauseScreen() { return false; }
    private int radius() { return Math.max(58, Math.min(112, Math.min(width / 3, (height - 100) / 2))); }
    private void hover(double x, double y) {
        int hovered = HelsingBoltWheelMath.sector(x - width / 2.0, y - height / 2.0, radius() * 0.43);
        if (hovered >= 0) selected = hovered;
        lastMouseX = x; lastMouseY = y;
    }
    @Override public void mouseMoved(double x, double y) { hover(x, y); }
    private int count(HelsingBoltItem.Kind kind) {
        int count = 0;
        if (minecraft.player != null)
            for (int i = 0; i < minecraft.player.getInventory().getContainerSize(); i++) {
                ItemStack stack = minecraft.player.getInventory().getItem(i);
                if (stack.is(kind.item())) count += stack.getCount();
            }
        return count;
    }
    @Override public void tick() {
        if (minecraft.player == null || !HelsingManager.isSurvivor(minecraft.player)
                || !(minecraft.player.getItemInHand(hand).getItem() instanceof HelsingCrossbowItem)) onClose();
    }
    @Override public void render(GuiGraphics gui, int mouseX, int mouseY, float partial) {
        gui.fill(0, 0, width, height, 0x9A080B12);
        int cx = width / 2, cy = height / 2, radius = radius();
        double inner = radius * 0.43;
        if (mouseX != lastMouseX || mouseY != lastMouseY) {
            hover(mouseX, mouseY);
        }
        gui.drawCenteredString(font, title, cx, cy - radius - 25, 0xFFF0E6D4);
        gui.flush();
        for (int i = 0; i < 4; i++) {
            int color = COLORS[i];
            wedge(gui, cx, cy, inner, radius, i, i == selected ? 0xD8000000 | color : 0xDE202632);
            wedge(gui, cx, cy, radius - 3, radius, i, i == selected ? 0xFF000000 | color : 0xFF485163);
        }
        for (int i = 0; i < 4; i++) {
            var kind = HelsingBoltItem.Kind.values()[i];
            double angle = i * Math.PI / 2;
            int x = cx + (int) (Math.sin(angle) * radius * 0.72);
            int y = cy - (int) (Math.cos(angle) * radius * 0.72);
            gui.pose().pushPose(); gui.pose().translate(x - 12, y - 16, 0); gui.pose().scale(1.5F, 1.5F, 1.5F);
            gui.renderItem(new ItemStack(kind.item()), 0, 0);
            gui.pose().popPose();
            int count = count(kind);
            gui.drawString(font, Integer.toString(count), x + 10, y - 16, count > 0 ? 0xFFFFFFFF : 0xFFFF8793);
            int textY = y + 12;
            for (var line : font.split(Component.translatable(kind.key()), Math.max(48, radius - 18))) {
                gui.drawCenteredString(font, line, x, textY, count > 0 ? 0xFFE8EDF7 : 0xFF949AAA); textY += 9;
            }
        }
        var chosen = HelsingBoltItem.Kind.values()[selected];
        int centerY = cy - 10;
        for (var line : font.split(Component.translatable(chosen.key()), (int) inner * 2 - 10)) {
            gui.drawCenteredString(font, line, cx, centerY, 0xFF000000 | COLORS[selected]); centerY += 10;
        }
        gui.drawCenteredString(font, Component.translatable("screen.maniacrev.helsing.confirm", ModKeybinds.HELSING_SWITCH_BOLT.getTranslatedKeyMessage()),
                cx, cy + radius + 16, 0xFFD5DCE8);
        if (minecraft.player != null) {
            ItemStack bow = minecraft.player.getItemInHand(hand);
            if (HelsingCrossbowItem.loaded(bow)) {
                int lineY = cy + radius + 29;
                for (var line : font.split(Component.translatable("screen.maniacrev.helsing.next_load",
                        Component.translatable(HelsingCrossbowItem.loadedKind(bow).key())), width - 24)) {
                    gui.drawCenteredString(font, line, cx, lineY, 0xFFE7B9C7); lineY += 9;
                }
            }
        }
    }
    private static void wedge(GuiGraphics gui, int cx, int cy, double inner, double outer, int sector, int color) {
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        var matrix = gui.pose().last().pose();
        var buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        double start = sector * Math.PI / 2 - Math.PI / 4 + 0.035;
        double end = sector * Math.PI / 2 + Math.PI / 4 - 0.035;
        int r = color >> 16 & 255, g = color >> 8 & 255, b = color & 255, a = color >>> 24;
        for (int step = 0; step <= 24; step++) {
            double angle = start + (end - start) * step / 24;
            for (double radius : new double[]{outer, inner})
                buffer.vertex(matrix, (float) (cx + Math.sin(angle) * radius), (float) (cy - Math.cos(angle) * radius), 0)
                        .color(r, g, b, a).endVertex();
        }
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
    private void confirm() {
        if (minecraft.player != null) ModNetworking.sendToServer(new SelectHelsingBoltPacket(selected, hand));
        minecraft.setScreen(null);
    }
    @Override public boolean keyReleased(int key, int scan, int modifiers) {
        if (ModKeybinds.HELSING_SWITCH_BOLT.matches(key, scan)) { confirm(); return true; }
        return super.keyReleased(key, scan, modifiers);
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) { hover(x, y); confirm(); return true; }
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) { onClose(); return true; }
        return super.mouseClicked(x, y, button);
    }
    @Override public boolean mouseReleased(double x, double y, int button) {
        if (ModKeybinds.HELSING_SWITCH_BOLT.matchesMouse(button)) { hover(x, y); confirm(); return true; }
        return super.mouseReleased(x, y, button);
    }
    @Override public boolean mouseScrolled(double x, double y, double delta) {
        selected = Math.floorMod(selected + (delta > 0 ? -1 : 1), 4); return true;
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_DOWN) { selected = (selected + 1) % 4; return true; }
        if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_UP) { selected = Math.floorMod(selected - 1, 4); return true; }
        if (key == GLFW.GLFW_KEY_ENTER) { confirm(); return true; }
        return super.keyPressed(key, scan, modifiers);
    }
}
