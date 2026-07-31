package org.example.maniacrevolution.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

/**
 * Reusable HUD-style boolean switch for settings screens.
 *
 * <p>The component owns its visual state and animation, while the supplied callback connects it
 * to any client-side temporary value. Calling {@link #setValue(boolean)} updates the switch
 * without invoking the callback, which is useful when a screen reloads or resets its values.</p>
 */
public class SettingsToggle extends AbstractButton {
    public static final int DEFAULT_WIDTH = 60;
    public static final int DEFAULT_HEIGHT = 22;
    public static final int DEFAULT_ACCENT = 0xFF70E28A;

    private static final int BACKGROUND = 0xFF28323C;
    private static final int BACKGROUND_ON = 0xFF263C32;
    private static final int BORDER = 0xFF4D5864;
    private static final int TEXT = 0xFFF7F9FB;
    private static final int TEXT_MUTED = 0xFF929CA7;
    private static final int KNOB_OFF = 0xFFC7CED6;

    private final OnValueChange onValueChange;
    private final int accentColor;
    private boolean value;
    private float animation;
    private long lastRenderNanos;

    public SettingsToggle(int x, int y, Component message, boolean initialValue,
                          OnValueChange onValueChange) {
        this(x, y, DEFAULT_WIDTH, DEFAULT_HEIGHT, message, initialValue,
                DEFAULT_ACCENT, onValueChange);
    }

    public SettingsToggle(int x, int y, int width, int height, Component message,
                          boolean initialValue, int accentColor,
                          OnValueChange onValueChange) {
        super(x, y, Math.max(42, width), Math.max(18, height), message);
        this.value = initialValue;
        this.animation = initialValue ? 1.0f : 0.0f;
        this.accentColor = accentColor;
        this.onValueChange = onValueChange == null ? (toggle, enabled) -> { } : onValueChange;
    }

    public boolean getValue() {
        return value;
    }

    public void setValue(boolean value) {
        this.value = value;
    }

    public void setValue(boolean value, boolean notifyListener) {
        if (this.value == value) {
            return;
        }
        this.value = value;
        if (notifyListener) {
            onValueChange.onValueChange(this, value);
        }
    }

    public void toggle() {
        setValue(!value, true);
    }

    @Override
    public void onPress() {
        toggle();
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics gui, int mouseX, int mouseY,
                                float partialTick) {
        updateAnimation();

        boolean highlighted = isHoveredOrFocused();
        int alpha = Math.round(this.alpha * 255.0f);
        int background = value ? BACKGROUND_ON : BACKGROUND;
        if (highlighted) {
            background = lerpColor(background, value ? accentColor : 0xFFFFFFFF, 0.13f);
        }
        int border = value ? accentColor : highlighted ? KNOB_OFF : BORDER;
        int x = getX();
        int y = getY();

        gui.fill(x + 2, y + 3, x + width + 2, y + height + 3,
                withAlpha(0xFF000000, Math.round(alpha * 0.32f)));
        gui.fill(x, y, x + width, y + height, withAlpha(background, alpha));
        gui.renderOutline(x, y, width, height, withAlpha(border, alpha));
        gui.fill(x + 1, y + 1, x + width - 1, y + 2,
                withAlpha(value ? accentColor : BORDER, Math.round(alpha * 0.65f)));

        int knobSize = Math.max(10, height - 8);
        int knobY = y + (height - knobSize) / 2;
        int knobLeft = x + 4;
        int knobRight = x + width - knobSize - 4;
        int knobX = Math.round(Mth.lerp(animation, knobLeft, knobRight));
        int knobColor = active ? lerpColor(KNOB_OFF, accentColor, animation) : BORDER;

        gui.fill(knobX + 1, knobY + 2, knobX + knobSize + 1, knobY + knobSize + 2,
                withAlpha(0xFF000000, Math.round(alpha * 0.35f)));
        gui.fill(knobX, knobY, knobX + knobSize, knobY + knobSize,
                withAlpha(knobColor, alpha));
        gui.renderOutline(knobX, knobY, knobSize, knobSize,
                withAlpha(value ? TEXT : BORDER, alpha));

        String stateLabel = value ? "ВКЛ" : "ВЫКЛ";
        int labelCenterX = value
                ? (x + knobRight) / 2
                : knobLeft + knobSize + (x + width - knobLeft - knobSize) / 2;
        int labelColor = active ? (value ? TEXT : TEXT_MUTED) : BORDER;
        gui.drawCenteredString(Minecraft.getInstance().font, stateLabel,
                labelCenterX, y + (height - 8) / 2, withAlpha(labelColor, alpha));
    }

    private void updateAnimation() {
        long now = System.nanoTime();
        if (lastRenderNanos == 0L) {
            lastRenderNanos = now;
            animation = value ? 1.0f : 0.0f;
            return;
        }
        float deltaSeconds = Math.min(0.1f, (now - lastRenderNanos) / 1_000_000_000.0f);
        lastRenderNanos = now;
        float responsiveness = 1.0f - (float) Math.pow(0.0008, deltaSeconds);
        animation += ((value ? 1.0f : 0.0f) - animation) * responsiveness;
        if (Math.abs((value ? 1.0f : 0.0f) - animation) < 0.001f) {
            animation = value ? 1.0f : 0.0f;
        }
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationOutput) {
        defaultButtonNarrationText(narrationOutput);
    }

    private static int withAlpha(int color, int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (color & 0x00FFFFFF);
    }

    private static int lerpColor(int from, int to, float amount) {
        float value = Mth.clamp(amount, 0.0f, 1.0f);
        int a = Math.round(((from >>> 24) & 0xFF)
                + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * value);
        int r = Math.round(((from >>> 16) & 0xFF)
                + (((to >>> 16) & 0xFF) - ((from >>> 16) & 0xFF)) * value);
        int g = Math.round(((from >>> 8) & 0xFF)
                + (((to >>> 8) & 0xFF) - ((from >>> 8) & 0xFF)) * value);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * value);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @FunctionalInterface
    public interface OnValueChange {
        void onValueChange(SettingsToggle toggle, boolean value);
    }
}
