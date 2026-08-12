package org.example.maniacrevolution.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.sbersprout.client.SberSproutClientHandler;
import org.example.maniacrevolution.perk.perks.survivor.SberSproutPerk;
import org.example.maniacrevolution.util.PlayerModeUtil;

import java.util.Locale;

/** Компактная строка лично накопленного и сохраняемого прогресса. */
public final class SberSproutOverlay implements IGuiOverlay {
    public static final SberSproutOverlay INSTANCE = new SberSproutOverlay();

    private static final int PANEL_WIDTH = 100;
    private static final int PANEL_HEIGHT = 20;
    private static final int PANEL_GAP_ABOVE_HUD = 39;
    private static final int ICON_SIZE = 16;
    private static final int BACKGROUND_COLOR = 0xD314241D;
    private static final int GREEN = 0xFF38F27B;
    private static final int CYAN = 0xFF35E8FF;
    private static final int GOLD = 0xFFFFC53D;

    private SberSproutOverlay() {
    }

    @Override
    public void render(ForgeGui forgeGui, GuiGraphics graphics, float partialTick,
                       int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!SberSproutClientHandler.isSessionActive() || minecraft.options.hideGui
                || minecraft.player == null || minecraft.screen != null
                || !PlayerModeUtil.isSurvivalOrAdventure(minecraft.player)) return;

        int hudTop = screenHeight - CustomHud.getBottomContentTopOffset(
                minecraft.player, false);
        int x = screenWidth / 2 - PANEL_WIDTH / 2;
        int y = hudTop - PANEL_GAP_ABOVE_HUD - PANEL_HEIGHT;
        float pulse = 0.5F + 0.5F * Mth.sin(
                (minecraft.level == null ? 0.0F : minecraft.level.getGameTime())
                        * 0.22F + partialTick);
        String numberFormat = "%." + SberSproutPerk.DISPLAY_DECIMAL_PLACES + "f%%";
        String text = String.format(Locale.ROOT, numberFormat + " → " + numberFormat,
                SberSproutClientHandler.getCurrentPercent(),
                SberSproutClientHandler.getSavedPercent());

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 880.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.fill(x + 2, y, x + PANEL_WIDTH - 2, y + PANEL_HEIGHT,
                BACKGROUND_COLOR);
        graphics.fill(x, y + 2, x + PANEL_WIDTH, y + PANEL_HEIGHT - 2,
                BACKGROUND_COLOR);
        graphics.renderOutline(x, y, PANEL_WIDTH, PANEL_HEIGHT,
                pulse > 0.5F ? GREEN : CYAN);
        graphics.fill(x + 20, y + PANEL_HEIGHT - 3,
                x + 20 + Math.round((PANEL_WIDTH - 23) * pulse),
                y + PANEL_HEIGHT - 2, GOLD);
        graphics.renderItem(new ItemStack(ModItems.SBER_SPROUT.get()),
                x + 3, y + 2);
        int textX = x + 21 + (PANEL_WIDTH - 23 - minecraft.font.width(text)) / 2;
        graphics.drawString(minecraft.font, text, textX, y + 6,
                pulse > 0.5F ? CYAN : GREEN, true);
        RenderSystem.disableBlend();
        graphics.pose().popPose();
    }
}
