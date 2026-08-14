package org.example.maniacrevolution.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.example.maniacrevolution.client.BudDispatcherClientData;
import org.example.maniacrevolution.client.BudDispatcherTextures;
import org.example.maniacrevolution.perk.perks.maniac.BudDispatcherPerk;

import java.util.List;

/** Цветочная сводка: намеренно не содержит ID, координат, расстояний и процентов. */
public class BudDispatcherScreen extends Screen {
    private static final int MAX_PANEL_WIDTH = 500;
    private static final int MAX_PANEL_HEIGHT = 356;
    private static final int SCREEN_MARGIN = 18;
    private static final int PANEL_PADDING = 14;
    private static final int HEADER_HEIGHT = 48;
    private static final int FOOTER_HEIGHT = 22;
    private static final int GRID_COLUMNS = 3;
    private static final int GRID_ROWS = 3;
    private static final int CARD_GAP = 8;
    private static final int CARD_INSET = 4;
    private static final int CLOSE_SIZE = 18;
    private static final int PETAL_COUNT = 18;
    private static final int OPEN_ANIMATION_MILLIS = 280;
    private static final float OPEN_START_SCALE = 0.72F;

    private static final int BACKDROP = 0xE80A0716;
    private static final int PANEL_SHADOW = 0xB0000000;
    private static final int PANEL_BACKGROUND = 0xF0181029;
    private static final int PANEL_INNER = 0xE8211933;
    private static final int BORDER_DARK = 0xFF49305E;
    private static final int BORDER_BRIGHT = 0xFFFF8DE5;
    private static final int TEXT_PRIMARY = 0xFFFFF3D8;
    private static final int TEXT_SECONDARY = 0xFFCDB9D9;

    private final long openedAtMillis = System.currentTimeMillis();

    public BudDispatcherScreen() {
        super(Component.translatable("perk.maniacrev." + BudDispatcherPerk.ID + ".name"));
    }

    @Override
    protected void init() {
        refreshMovementKeysFromKeyboard();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, BACKDROP);
        drawFloatingPetals(graphics);

        int panelWidth = Math.min(MAX_PANEL_WIDTH, Math.max(1, width - SCREEN_MARGIN * 2));
        int panelHeight = Math.min(MAX_PANEL_HEIGHT, Math.max(1, height - SCREEN_MARGIN * 2));
        int panelX = (width - panelWidth) / 2;
        int panelY = (height - panelHeight) / 2;

        float elapsed = (System.currentTimeMillis() - openedAtMillis)
                / (float) OPEN_ANIMATION_MILLIS;
        float animation = easeOutBack(Mth.clamp(elapsed, 0.0F, 1.0F));
        float scale = Mth.lerp(animation, OPEN_START_SCALE, 1.0F);

        graphics.pose().pushPose();
        graphics.pose().translate(width / 2.0F, height / 2.0F, 20.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.pose().translate(-width / 2.0F, -height / 2.0F, 0.0F);
        drawBoard(graphics, panelX, panelY, panelWidth, panelHeight, mouseX, mouseY);
        graphics.pose().popPose();

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawBoard(GuiGraphics graphics, int x, int y, int width, int height,
                           int mouseX, int mouseY) {
        graphics.fill(x + 5, y + 7, x + width + 5, y + height + 7, PANEL_SHADOW);
        graphics.fill(x, y, x + width, y + height, PANEL_BACKGROUND);
        graphics.renderOutline(x, y, width, height, BORDER_BRIGHT);
        graphics.renderOutline(x + 3, y + 3, width - 6, height - 6, BORDER_DARK);
        graphics.fill(x + 7, y + 7, x + width - 7, y + height - 7, PANEL_INNER);

        graphics.drawCenteredString(font, title, x + width / 2, y + 13, TEXT_PRIMARY);
        graphics.drawCenteredString(font,
                Component.translatable("screen.maniacrev.bud_dispatcher.subtitle"),
                x + width / 2, y + 28, TEXT_SECONDARY);

        int closeX = x + width - PANEL_PADDING - CLOSE_SIZE;
        int closeY = y + PANEL_PADDING - 3;
        boolean closeHovered = mouseX >= closeX && mouseX < closeX + CLOSE_SIZE
                && mouseY >= closeY && mouseY < closeY + CLOSE_SIZE;
        graphics.fill(closeX, closeY, closeX + CLOSE_SIZE, closeY + CLOSE_SIZE,
                closeHovered ? 0xFFF05C94 : 0xFF5C304F);
        graphics.renderOutline(closeX, closeY, CLOSE_SIZE, CLOSE_SIZE,
                closeHovered ? TEXT_PRIMARY : BORDER_BRIGHT);
        graphics.drawCenteredString(font, "×", closeX + CLOSE_SIZE / 2,
                closeY + 5, TEXT_PRIMARY);

        int gridX = x + PANEL_PADDING;
        int gridY = y + HEADER_HEIGHT;
        int gridWidth = width - PANEL_PADDING * 2;
        int gridHeight = height - HEADER_HEIGHT - FOOTER_HEIGHT - PANEL_PADDING;
        int cardWidth = (gridWidth - CARD_GAP * (GRID_COLUMNS - 1)) / GRID_COLUMNS;
        int cardHeight = (gridHeight - CARD_GAP * (GRID_ROWS - 1)) / GRID_ROWS;

        List<BudDispatcherClientData.FlowerState> states = BudDispatcherClientData.getStates();
        int count = Math.min(BudDispatcherPerk.COMPUTER_COUNT, states.size());
        for (int index = 0; index < count; index++) {
            int column = index % GRID_COLUMNS;
            int row = index / GRID_COLUMNS;
            int cardX = gridX + column * (cardWidth + CARD_GAP);
            int cardY = gridY + row * (cardHeight + CARD_GAP);
            drawFlowerCard(graphics, states.get(index), cardX, cardY, cardWidth, cardHeight);
        }

        graphics.drawCenteredString(font,
                Component.translatable("screen.maniacrev.bud_dispatcher.hint"),
                x + width / 2, y + height - FOOTER_HEIGHT + 5, TEXT_SECONDARY);
    }

    private void drawFlowerCard(GuiGraphics graphics,
                                BudDispatcherClientData.FlowerState state,
                                int x, int y, int width, int height) {
        int accent = BudDispatcherTextures.accentColor(state.flowerIndex());
        int fadedAccent = withAlpha(accent, 0x66);
        int darkenedAccent = darken(accent, 0.25F + state.stage() * 0.05F);

        graphics.fill(x + 2, y + 3, x + width + 2, y + height + 3, 0x70000000);
        graphics.fill(x, y, x + width, y + height, 0xD00D1620);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, fadedAccent);
        graphics.fill(x + CARD_INSET, y + CARD_INSET,
                x + width - CARD_INSET, y + height - CARD_INSET, 0xE0141020);
        graphics.renderOutline(x, y, width, height, darkenedAccent);

        int spriteSize = Math.max(1, Math.min(width - CARD_INSET * 4,
                height - CARD_INSET * 2));
        int spriteX = x + (width - spriteSize) / 2;
        int spriteY = y + height - CARD_INSET - spriteSize;
        BudDispatcherTextures.blit(graphics, state.flowerIndex(), state.stage(),
                spriteX, spriteY, spriteSize, spriteSize);
    }

    private void drawFloatingPetals(GuiGraphics graphics) {
        long time = System.currentTimeMillis();
        for (int index = 0; index < PETAL_COUNT; index++) {
            float speed = 0.010F + (index % 5) * 0.0025F;
            int x = Math.floorMod(index * 83 + (int) (time * speed), Math.max(1, width));
            double wave = Math.sin(time / 420.0D + index * 1.7D);
            int y = Math.floorMod(index * 61 + (int) (time * speed * 0.58F),
                    Math.max(1, height));
            x += Math.round((float) wave * 11.0F);
            int flower = index % BudDispatcherPerk.FLOWER_VARIANT_COUNT;
            int color = withAlpha(BudDispatcherTextures.accentColor(flower), 0xA8);
            int petalWidth = 2 + index % 3;
            int petalHeight = 3 + (index + 1) % 4;
            graphics.fill(x, y, x + petalWidth, y + petalHeight, color);
        }
    }

    private static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private static int darken(int color, float amount) {
        float multiplier = 1.0F - Mth.clamp(amount, 0.0F, 1.0F);
        int red = Math.round(((color >> 16) & 0xFF) * multiplier);
        int green = Math.round(((color >> 8) & 0xFF) * multiplier);
        int blue = Math.round((color & 0xFF) * multiplier);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private static float easeOutBack(float value) {
        float shifted = value - 1.0F;
        return 1.0F + 2.70158F * shifted * shifted * shifted
                + 1.70158F * shifted * shifted;
    }

    @Override
    public void tick() {
        refreshMovementKeysFromKeyboard();
        if (!BudDispatcherClientData.canSeeFlowers()) {
            onClose();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int panelWidth = Math.min(MAX_PANEL_WIDTH, Math.max(1, width - SCREEN_MARGIN * 2));
        int panelHeight = Math.min(MAX_PANEL_HEIGHT, Math.max(1, height - SCREEN_MARGIN * 2));
        int panelX = (width - panelWidth) / 2;
        int panelY = (height - panelHeight) / 2;
        int closeX = panelX + panelWidth - PANEL_PADDING - CLOSE_SIZE;
        int closeY = panelY + PANEL_PADDING - 3;
        if (button == 0 && mouseX >= closeX && mouseX < closeX + CLOSE_SIZE
                && mouseY >= closeY && mouseY < closeY + CLOSE_SIZE) {
            onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (setMovementKeyState(keyCode, scanCode, true)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (setMovementKeyState(keyCode, scanCode, false)) {
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    private boolean setMovementKeyState(int keyCode, int scanCode, boolean pressed) {
        if (minecraft == null) return false;
        boolean matched = false;
        for (KeyMapping key : movementKeys()) {
            if (key.matches(keyCode, scanCode)) {
                key.setDown(pressed);
                matched = true;
            }
        }
        return matched;
    }

    private void refreshMovementKeysFromKeyboard() {
        if (minecraft == null) return;
        long window = minecraft.getWindow().getWindow();
        for (KeyMapping keyMapping : movementKeys()) {
            InputConstants.Key key = keyMapping.getKey();
            if (key.getType() == InputConstants.Type.KEYSYM
                    && key.getValue() != InputConstants.UNKNOWN.getValue()) {
                boolean down = InputConstants.isKeyDown(window, key.getValue());
                KeyMapping.set(key, down);
                keyMapping.setDown(down);
            }
        }
    }

    private KeyMapping[] movementKeys() {
        return new KeyMapping[]{
                minecraft.options.keyUp,
                minecraft.options.keyDown,
                minecraft.options.keyLeft,
                minecraft.options.keyRight,
                minecraft.options.keyJump,
                minecraft.options.keyShift,
                minecraft.options.keySprint
        };
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
