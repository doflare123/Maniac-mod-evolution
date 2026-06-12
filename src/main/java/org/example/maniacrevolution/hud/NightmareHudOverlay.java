package org.example.maniacrevolution.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.example.maniacrevolution.nightmare.NightmareHud;

public class NightmareHudOverlay implements IGuiOverlay {
    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) {
            return;
        }

        NightmareHud.render(guiGraphics, screenWidth, screenHeight);
    }
}
