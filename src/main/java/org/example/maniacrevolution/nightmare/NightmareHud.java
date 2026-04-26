package org.example.maniacrevolution.nightmare;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.Maniacrev;

public final class NightmareHud {
    private static final int BAR_WIDTH = 120;
    private static final int BAR_HEIGHT = 10;
    private static final ResourceLocation SCREAMER_TEXTURE =
            Maniacrev.loc("textures/gui/abilities/screamer.jpg");

    private NightmareHud() {}

    public static void render(GuiGraphics gui, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (ClientNightmareData.isVisible()) {
            int x = 8;
            int y = screenHeight - 110;
            gui.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0xAA170A24);
            int filled = (int) (BAR_WIDTH * ClientNightmareData.getSanityPercent());
            gui.fill(x, y, x + filled, y + BAR_HEIGHT, 0xFFD65BFF);
            gui.renderOutline(x, y, BAR_WIDTH, BAR_HEIGHT, 0xFF4B245F);
            gui.drawString(mc.font, "Рассудок", x, y - 11, 0xFFE8C8FF, true);
        }

        if (ClientNightmareData.getTrialType() != NightmareTrialType.NONE) {
            String text = trialName(ClientNightmareData.getTrialType()) + ": " + ClientNightmareData.getTrialSecondsLeft() + "с";
            int x = screenWidth / 2 - mc.font.width(text) / 2;
            gui.drawString(mc.font, text, x, 20, 0xFFFFD5FF, true);
        }

        if (ClientNightmareData.shouldShowScreamer()) {
            RenderSystem.enableBlend();
            RenderSystem.setShaderTexture(0, SCREAMER_TEXTURE);
            gui.blit(SCREAMER_TEXTURE, 0, 0, 0, 0, screenWidth, screenHeight, screenWidth, screenHeight);
            RenderSystem.disableBlend();
        }
    }

    private static String trialName(NightmareTrialType type) {
        return switch (type) {
            case MAZE -> "Лабиринт";
            case ARENA -> "Арена";
            case FEAR_RACE -> "Гонка";
            case NONE -> "";
        };
    }
}
