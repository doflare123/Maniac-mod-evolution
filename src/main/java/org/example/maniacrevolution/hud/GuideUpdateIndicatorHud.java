package org.example.maniacrevolution.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.guide.GuideProgressClient;
import org.example.maniacrevolution.util.PlayerModeUtil;

public class GuideUpdateIndicatorHud implements IGuiOverlay {
    private static final ResourceLocation ICON = Maniacrev.loc("textures/gui/guide/icon/upd_guide.png");
    private static final int ICON_SIZE = 24;

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.screen != null) {
            return;
        }
        if (!PlayerModeUtil.isSurvivalOrAdventure(mc.player)) {
            return;
        }
        if (!GuideProgressClient.shouldShowUpdateIndicator()) {
            return;
        }

        float time = mc.player.tickCount + partialTick;
        int x = screenWidth / 2 - ICON_SIZE / 2;
        int y = screenHeight - 104 + Math.round((float) Math.sin(time * 0.25F) * 4.0F);

        RenderSystem.enableBlend();
        guiGraphics.blit(ICON, x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        RenderSystem.disableBlend();
    }
}
