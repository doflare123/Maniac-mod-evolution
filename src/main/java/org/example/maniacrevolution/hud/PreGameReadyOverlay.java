package org.example.maniacrevolution.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.example.maniacrevolution.client.ClientPreGameReadyData;
import org.example.maniacrevolution.util.PlayerModeUtil;

/** Temporary Counter-Strike-inspired readiness vote panel. */
public final class PreGameReadyOverlay implements IGuiOverlay {
    public static final PreGameReadyOverlay INSTANCE = new PreGameReadyOverlay();

    private static final int WIDTH = 196;
    private static final int HEIGHT = 88;
    private static final int BACKGROUND = 0xD91B222A;
    private static final int HEADER_BACKGROUND = 0xE02A333D;
    private static final int BORDER = 0xE064707D;
    private static final int TEXT = 0xFFF0F2F4;
    private static final int MUTED_TEXT = 0xFFB8C0C8;
    private static final int READY = 0xFF57C96B;
    private static final int NOT_READY = 0xFFEA5964;
    private static final long SLIDE_DURATION_NANOS = 280_000_000L;
    private static final ResourceLocation READY_ICON = new ResourceLocation(
            "maniacrev", "textures/gui/pregame_ready/bedrock_check.png");
    private static final ResourceLocation NOT_READY_ICON = new ResourceLocation(
            "maniacrev", "textures/gui/pregame_ready/bedrock_cancel.png");

    private float visibility;
    private long lastFrameNanos;

    private PreGameReadyOverlay() {}

    @Override
    public void render(net.minecraftforge.client.gui.overlay.ForgeGui forgeGui, GuiGraphics gui,
                       float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui
                || minecraft.player == null
                || !PlayerModeUtil.isSurvivalOrAdventure(minecraft.player)) {
            visibility = 0.0F;
            lastFrameNanos = System.nanoTime();
            return;
        }

        updateAnimation(ClientPreGameReadyData.isActive());
        if (visibility <= 0.001F) return;

        Font font = minecraft.font;
        float easedVisibility = visibility * visibility * (3.0F - 2.0F * visibility);
        int restingX = screenWidth - WIDTH - 8;
        int hiddenX = screenWidth + 4;
        int x = Math.round(Mth.lerp(easedVisibility, hiddenX, restingX));
        int y = 28;
        int ready = ClientPreGameReadyData.getReadyCount();
        int notReady = Math.max(0, ClientPreGameReadyData.getTotalCount() - ready);

        gui.fill(x, y, x + WIDTH, y + HEIGHT, BACKGROUND);
        gui.fill(x, y, x + WIDTH, y + 22, HEADER_BACKGROUND);
        gui.renderOutline(x, y, WIDTH, HEIGHT, BORDER);
        gui.fill(x, y + 21, x + WIDTH, y + 22, BORDER);

        Component title = Component.translatable(
                "hud.maniacrev.pregame_ready.started_by",
                ClientPreGameReadyData.getInitiatorName()
        );
        String fittedTitle = font.plainSubstrByWidth(title.getString(), WIDTH - 18);
        gui.drawString(font, fittedTitle, x + 9, y + 7, TEXT, true);

        gui.drawString(font, Component.translatable("hud.maniacrev.pregame_ready.question"),
                x + 9, y + 31, TEXT, true);

        int badgeX = x + WIDTH - 43;
        drawStatusBadge(gui, badgeX, y + 38, true, ready);
        drawStatusBadge(gui, badgeX, y + 54, false, notReady);

        Component voteLabel = Component.translatable("hud.maniacrev.pregame_ready.your_vote");
        Component vote = Component.translatable(ClientPreGameReadyData.isLocalReady()
                ? "hud.maniacrev.pregame_ready.ready"
                : "hud.maniacrev.pregame_ready.not_ready");
        int voteWidth = font.width(voteLabel) + font.width(vote);
        int voteX = x + WIDTH - 8 - voteWidth;
        gui.drawString(font, voteLabel, voteX, y + HEIGHT - 13, MUTED_TEXT, true);
        gui.drawString(font, vote, voteX + font.width(voteLabel), y + HEIGHT - 13,
                ClientPreGameReadyData.isLocalReady() ? READY : NOT_READY, true);
    }

    private void updateAnimation(boolean targetVisible) {
        long now = System.nanoTime();
        if (lastFrameNanos == 0L) {
            lastFrameNanos = now;
            return;
        }

        float step = (now - lastFrameNanos) / (float) SLIDE_DURATION_NANOS;
        lastFrameNanos = now;
        visibility = Mth.clamp(
                visibility + (targetVisible ? step : -step),
                0.0F,
                1.0F
        );
    }

    private static void drawStatusBadge(GuiGraphics gui, int x, int y,
                                        boolean positive, int count) {
        Font font = Minecraft.getInstance().font;
        int color = positive ? READY : NOT_READY;
        ResourceLocation icon = positive ? READY_ICON : NOT_READY_ICON;

        RenderSystem.enableBlend();
        if (positive) {
            gui.pose().pushPose();
            gui.pose().translate(x + 1, y + 2, 0.0F);
            gui.pose().scale(0.68F, 0.68F, 1.0F);
            gui.blit(icon, 0, 0, 0, 0, 22, 16, 22, 16);
            gui.pose().popPose();
        } else {
            gui.blit(icon, x + 2, y + 1, 0, 0, 13, 13, 13, 13);
        }
        RenderSystem.disableBlend();

        gui.drawString(font, Integer.toString(count), x + 22, y + 3, color, true);
    }
}
