package org.example.maniacrevolution.event;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.config.HudConfig;
import org.example.maniacrevolution.util.PlayerModeUtil;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class PenaltyHudOverlay {
    private static final long DISPLAY_DURATION_MS = 1400L;
    private static final long FADE_DURATION_MS = 350L;
    private static boolean wasInPenaltySlot;
    private static long visibleUntil;

    private PenaltyHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.CROSSHAIR.type()) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (!PlayerModeUtil.isSurvivalOrAdventure(player) || !HudConfig.isCustomHudEnabled()) return;

        boolean inPenaltySlot = PenaltySlotManager.isInPenaltySlot(player);
        long now = Util.getMillis();
        if (inPenaltySlot && !wasInPenaltySlot) visibleUntil = now + DISPLAY_DURATION_MS;
        wasInPenaltySlot = inPenaltySlot;
        if (now >= visibleUntil) return;

        float alpha = visibleUntil - now >= FADE_DURATION_MS
                ? 1.0f
                : (visibleUntil - now) / (float) FADE_DURATION_MS;
        alpha = Mth.clamp(alpha, 0.0f, 1.0f);

        GuiGraphics gui = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        String warning = "ШТРАФНОЙ СЛОТ: ТОЛЬКО ХРАНЕНИЕ";
        int textWidth = mc.font.width(warning);
        int width = textWidth + 12;
        int x = (screenWidth - width) / 2;
        int y = screenHeight - 104 + Math.round((1.0f - alpha) * 4.0f);
        int alphaByte = Math.round(alpha * 255.0f);

        gui.fill(x, y, x + width, y + 17, (Math.min(alphaByte, 205) << 24) | 0x00241115);
        gui.renderOutline(x, y, width, 17, (alphaByte << 24) | 0x00D54A54);
        gui.drawString(mc.font, warning, x + 6, y + 4,
                (alphaByte << 24) | 0x00FF8B92, true);
    }
}
