package org.example.maniacrevolution.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.example.maniacrevolution.client.ColorRouletteClientHandler;
import org.example.maniacrevolution.colorroulette.ColorCard;
import org.example.maniacrevolution.colorroulette.ColorRouletteManager;
import org.example.maniacrevolution.config.HudConfig;
import org.example.maniacrevolution.perk.perks.common.ColorRoulettePerk;
import org.example.maniacrevolution.util.PlayerModeUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Bright three-card carousel rendered above the regular HUD. */
public final class ColorRouletteOverlay implements IGuiOverlay {
    public static final ColorRouletteOverlay INSTANCE = new ColorRouletteOverlay();
    private static final int[] ROLE_SEQUENCE = {0, -1, 1};
    private static final float CARD_RENDER_SCALE = 4.0F;
    private static final float CARD_HORIZONTAL_SPACING = 68.0F;
    private static final float SIDE_CARD_VERTICAL_OFFSET = 14.0F;
    private static final int CARD_HALF_HEIGHT = 32;
    private static final int CARD_GAP_ABOVE_HUD = 4;

    private ColorRouletteOverlay() {}

    @Override
    public void render(ForgeGui forgeGui, GuiGraphics gui, float partialTick,
                       int screenWidth, int screenHeight) {
        if (!ColorRouletteClientHandler.isVisible()) return;
        if (ColorRouletteClientHandler.isRolling()) {
            renderCarousel(gui, partialTick, screenWidth, screenHeight);
        } else {
            renderResult(gui, partialTick, screenWidth, screenHeight);
        }
    }

    private void renderCarousel(GuiGraphics gui, float partialTick,
                                int screenWidth, int screenHeight) {
        double phase = ColorRouletteClientHandler.phase(partialTick);
        int whole = (int) Math.floor(phase);
        float fraction = smooth((float) (phase - whole));
        float age = ColorRouletteClientHandler.age(partialTick);
        float intro = Mth.clamp(age / ColorRoulettePerk.INTRO_TICKS, 0.0F, 1.0F);
        float arrive = easeOutBack(intro);
        int targetY = getCarouselCenterY(screenHeight);
        int originX = CustomHud.getActivePerkCenterX(screenWidth);
        int originY = CustomHud.getActivePerkCenterY(screenHeight);
        int centerX = Math.round(Mth.lerp(arrive, originX, screenWidth / 2.0F));
        int centerY = Math.round(Mth.lerp(arrive, originY, targetY));

        List<CardPose> poses = new ArrayList<>(3);
        int initial = ColorRouletteClientHandler.initial().ordinal();
        for (int offset = 0; offset < 3; offset++) {
            int fromRole = ROLE_SEQUENCE[Math.floorMod(offset + whole, 3)];
            int toRole = ROLE_SEQUENCE[Math.floorMod(offset + whole + 1, 3)];
            float role = Mth.lerp(fraction, fromRole, toRole);
            float centerWeight = 1.0F - Math.min(1.0F, Math.abs(role));
            float x = centerX + role * CARD_HORIZONTAL_SPACING * arrive;
            float y = centerY + (1.0F - centerWeight) * SIDE_CARD_VERTICAL_OFFSET * arrive;
            float scale = (0.70F + 0.30F * centerWeight) * (0.02F + 0.98F * arrive);
            ColorCard card = ColorCard.byOrdinal((initial + offset) % 3);
            poses.add(new CardPose(card, x, y, scale, centerWeight));
        }
        poses.sort(Comparator.comparingDouble(CardPose::depth));

        RenderSystem.enableBlend();
        for (CardPose pose : poses) {
            renderGlow(gui, pose);
            renderCard(gui, pose.card(), pose.x(), pose.y(), pose.scale(),
                    (float) phase * 0.33F);
        }
        RenderSystem.disableBlend();

        if (intro >= 1.0F) {
            String key = org.example.maniacrevolution.keybind.ModKeybinds.ACTIVATE_PERK
                    .getTranslatedKeyMessage().getString();
            String text = net.minecraft.network.chat.Component.translatable(
                    "hud.maniacrev.color_roulette.stop", key).getString();
            int width = Minecraft.getInstance().font.width(text);
            gui.fill(screenWidth / 2 - width / 2 - 5, centerY + 38,
                    screenWidth / 2 + width / 2 + 5, centerY + 51, 0xB014101F);
            gui.drawString(Minecraft.getInstance().font, text,
                    screenWidth / 2 - width / 2, centerY + 41, 0xFFFFE76B, true);
        }
    }

    private void renderResult(GuiGraphics gui, float partialTick,
                              int screenWidth, int screenHeight) {
        float age = ColorRouletteClientHandler.resultAge(partialTick);
        float progress = Mth.clamp(age / ColorRouletteManager.RESULT_ANIMATION_TICKS,
                0.0F, 1.0F);
        int centerX = screenWidth / 2;
        int centerY = getCarouselCenterY(screenHeight);
        float pop = progress < 0.35F
                ? Mth.lerp(easeOutBack(progress / 0.35F), 1.0F, 1.42F)
                : Mth.lerp((progress - 0.35F) / 0.65F, 1.42F, 1.08F);
        float fade = progress < 0.72F ? 1.0F : (1.0F - progress) / 0.28F;

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, Mth.clamp(fade, 0.0F, 1.0F));
        renderCard(gui, ColorRouletteClientHandler.result(), centerX, centerY,
                pop, age * 0.02F);
        renderShards(gui, centerX, centerY, progress,
                ColorRouletteClientHandler.result());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private void renderCard(GuiGraphics gui, ColorCard card, float x, float y,
                            float scale, float wobble) {
        gui.pose().pushPose();
        gui.pose().translate(x, y, 200.0F);
        gui.pose().scale(CARD_RENDER_SCALE * scale, CARD_RENDER_SCALE * scale, 1.0F);
        gui.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(
                Mth.sin(wobble) * 2.5F));
        gui.renderItem(new ItemStack(card.item()), -8, -8);
        gui.pose().popPose();
    }

    private void renderGlow(GuiGraphics gui, CardPose pose) {
        int alpha = 32 + Math.round(pose.depth() * 62.0F);
        int color = (alpha << 24) | pose.card().color();
        int halfWidth = Math.round(20.0F * pose.scale());
        int halfHeight = Math.round(28.0F * pose.scale());
        gui.fill(Math.round(pose.x()) - halfWidth, Math.round(pose.y()) - halfHeight,
                Math.round(pose.x()) + halfWidth, Math.round(pose.y()) + halfHeight, color);
    }

    private void renderShards(GuiGraphics gui, int x, int y, float progress,
                              ColorCard selected) {
        ColorCard[] colors = ColorCard.values();
        int shard = 0;
        for (ColorCard color : colors) {
            if (color == selected) continue;
            for (int i = 0; i < 9; i++) {
                float angle = (float) (shard * 1.7D + i * Math.PI * 2.0D / 9.0D);
                float distance = 24.0F + progress * (42.0F + i * 3.0F);
                int sx = Math.round(x + Mth.cos(angle) * distance);
                int sy = Math.round(y + Mth.sin(angle) * distance * 0.65F
                        + progress * progress * 24.0F);
                int size = 2 + i % 3;
                int alpha = Math.round(210.0F * (1.0F - progress));
                gui.fill(sx, sy, sx + size, sy + size, (alpha << 24) | color.color());
            }
            shard++;
        }
    }

    private static float smooth(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    private static float easeOutBack(float value) {
        float c1 = 1.70158F;
        float c3 = c1 + 1.0F;
        float shifted = value - 1.0F;
        return 1.0F + c3 * shifted * shifted * shifted + c1 * shifted * shifted;
    }

    private static int getCarouselCenterY(int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        int hudTop = screenHeight - 71;
        if (minecraft.player != null
                && PlayerModeUtil.isSurvivalOrAdventure(minecraft.player)
                && HudConfig.isCustomHudEnabled()) {
            hudTop = screenHeight - CustomHud.getBottomContentTopOffset(
                    minecraft.player, minecraft.screen instanceof ChatScreen);
        }
        return Math.max(CARD_HALF_HEIGHT + CARD_GAP_ABOVE_HUD,
                hudTop - CARD_HALF_HEIGHT - CARD_GAP_ABOVE_HUD);
    }

    private record CardPose(ColorCard card, float x, float y, float scale, float depth) {}
}
