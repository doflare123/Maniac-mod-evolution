package org.example.maniacrevolution.settings;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.client.screen.SettingsScreen;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;

/** Actual screen rendering and +/- interactions, without a world or sending Apply. */
public final class SettingsGraphicsProbe {
    private SettingsGraphicsProbe() {}

    public static void run() throws Exception {
        var mc = Minecraft.getInstance();
        ClientGameSettings.setGraphicsMode(GraphicsMode.NORMAL);
        var screen = new SettingsScreen();
        screen.init(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        screen.keyPressed(GLFW.GLFW_KEY_TAB, 0, 0);
        screen.keyPressed(GLFW.GLFW_KEY_TAB, 0, 0);
        // Freeze only animation timing; selection is exercised through the real input handler.
        field("openedAt").setLong(screen, System.currentTimeMillis() - 2000);
        field("categoryChangedAt").setLong(screen, System.currentTimeMillis() - 2000);
        Field category = field("currentCategory");
        var categoryY = SettingsScreen.class.getDeclaredMethod("categoryButtonY", category.getType());
        categoryY.setAccessible(true);
        field("categoryIndicatorY").setFloat(screen, (int) categoryY.invoke(screen, category.get(screen)));
        int left = field("contentLeft").getInt(screen), right = field("contentRight").getInt(screen);
        int y = field("rowsTop").getInt(screen) + 20;
        int plus = right - 50 + 8;
        int valueWidth = Math.max(54, Math.min(78, (right - left) / 5));
        int minus = right - 50 - valueWidth - 4 - 24 + 8;
        screen.mouseClicked(minus, y, GLFW.GLFW_MOUSE_BUTTON_LEFT);
        screen.mouseClicked(minus, y, GLFW.GLFW_MOUSE_BUTTON_LEFT);
        var previousProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        var sorting = RenderSystem.getVertexSorting();
        var view = RenderSystem.getModelViewStack(); view.pushPose();
        try {
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, screen.width, screen.height, 0, 1000, 21000), VertexSorting.ORTHOGRAPHIC_Z);
            view.setIdentity(); view.translate(0, 0, -10000); RenderSystem.applyModelViewMatrix();
            for (GraphicsMode mode : GraphicsMode.values()) {
                if (field("tempGraphicsMode").get(screen) != mode) throw new AssertionError("Graphics selector did not reach " + mode);
                mc.getMainRenderTarget().clear(Minecraft.ON_OSX); mc.getMainRenderTarget().bindWrite(true);
                var gui = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
                screen.render(gui, -1, -1, 0); gui.flush();
                try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                    image.writeToFile(mc.gameDirectory.toPath().resolve("graphics-menu-" + mode.name().toLowerCase(java.util.Locale.ROOT) + ".png"));
                }
                screen.mouseClicked(plus, y, GLFW.GLFW_MOUSE_BUTTON_LEFT);
            }
            if (field("tempGraphicsMode").get(screen) != GraphicsMode.HEAVY) throw new AssertionError("Selector wraps at HEAVY");
            screen.mouseClicked(right - 3, field("panelY").getInt(screen) + 54 + 20, GLFW.GLFW_MOUSE_BUTTON_LEFT);
            if (field("tempGraphicsMode").get(screen) != GraphicsMode.NORMAL) throw new AssertionError("Graphics section reset failed");
            if (ClientGameSettings.getGraphicsMode() != GraphicsMode.NORMAL) throw new AssertionError("Editing the menu changed accepted server settings before Apply");
            Maniacrev.LOGGER.info("Graphics menu probe PASSED: four modes, +/- boundaries, section reset, no premature client change");
        } finally {
            view.popPose(); RenderSystem.applyModelViewMatrix(); RenderSystem.setProjectionMatrix(previousProjection, sorting);
            mc.getMainRenderTarget().bindWrite(true);
        }
    }

    private static Field field(String name) throws ReflectiveOperationException {
        var field = SettingsScreen.class.getDeclaredField(name); field.setAccessible(true); return field;
    }
}
