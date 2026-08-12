package org.example.maniacrevolution.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.config.HudConfig;
import org.example.maniacrevolution.hud.CustomHud;
import org.example.maniacrevolution.keybind.ModKeybinds;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.PaintQtePressPacket;
import org.example.maniacrevolution.paint.PaintColor;
import org.example.maniacrevolution.perk.perks.maniac.ThePaintThickensPerk;
import org.example.maniacrevolution.util.PlayerModeUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class PaintQteClientHandler {
    private static final ResourceLocation[] SCREEN_SPLAT_TEXTURES = {
            new ResourceLocation(Maniacrev.MODID, "textures/paint/paint_screen_splat.png"),
            new ResourceLocation(Maniacrev.MODID, "textures/paint/paint_screen_smear.png"),
            new ResourceLocation(Maniacrev.MODID, "textures/paint/paint_screen_drops.png")
    };
    private static final int SCREEN_SPLAT_COUNT = 28;
    private static final int EDGE_SPLAT_COUNT = 14;
    private static final int PRIMARY_COLOR_PERCENT = 55;
    private static final int SECONDARY_COLOR_COUNT = 3;
    private static final float EDGE_SPLAT_MIN_SIZE = 0.65F;
    private static final float EDGE_SPLAT_MAX_SIZE = 0.90F;
    private static final float INNER_SPLAT_MIN_SIZE = 0.45F;
    private static final float INNER_SPLAT_MAX_SIZE = 0.70F;
    private static final float EDGE_CENTER_OFFSET = 0.06F;
    private static final float FULL_SCREEN_FRACTION = 1.0F;
    private static final float FULL_ROTATION_DEGREES = 360.0F;
    private static final int MIN_SPLAT_SIZE_PHYSICAL_PIXELS = 48;
    private static final int KEY_BOX_WIDTH = 26;
    private static final int BAR_WIDTH = 184;
    private static final int BAR_HEIGHT = 18;
    private static final int BAR_GAP = 5;
    private static final int TOTAL_WIDTH = KEY_BOX_WIDTH + BAR_GAP + BAR_WIDTH;
    private static final int QTE_BOTTOM_GAP = 7;
    private static final int BORDER = 2;
    private static final int POINTER_WIDTH = 3;
    private static final int POINTER_OVERHANG = 3;
    private static final int COLOR_GRAY = 0xE0575B62;
    private static final int COLOR_YELLOW = 0xFFFFD21F;
    private static final int COLOR_GREEN = 0xFF38F218;

    private static ActiveQte activeQte;
    private static PaintOverlay paintOverlay;

    private PaintQteClientHandler() {
    }

    public static void startQte(int sessionId, PaintColor color, int attempts,
                                int totalDurationMs, int missPercent,
                                int nearPercent, int hitPercent,
                                int overlayDurationTicks) {
        activeQte = new ActiveQte(sessionId, color, attempts, totalDurationMs,
                missPercent, nearPercent, hitPercent);
        paintOverlay = PaintOverlay.create(
                color,
                System.currentTimeMillis(),
                overlayDurationTicks * 50L
        );
    }

    public static void stopQte() {
        activeQte = null;
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (activeQte == null || event.getAction() != 1) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) return;
        if (ModKeybinds.PAINT_QTE_KEY.matches(event.getKey(), event.getScanCode())) {
            long now = System.currentTimeMillis();
            submitCurrentAttempt(activeQte.scoreAt(now), now);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void renderPaintOverlay(RenderGuiEvent.Post event) {
        if (paintOverlay == null) return;

        long now = System.currentTimeMillis();
        float progress = (now - paintOverlay.startedMs) / (float) paintOverlay.durationMs;
        if (progress >= 1.0F) {
            paintOverlay = null;
            return;
        }
        float alpha = progress < 0.12F
                ? Mth.clamp(progress / 0.12F, 0.0F, 1.0F)
                : Mth.clamp((1.0F - progress) / 0.46F, 0.0F, 1.0F);
        renderSplats(event.getGuiGraphics(), paintOverlay.splats, alpha * 0.94F);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void renderPaintQte(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.CROSSHAIR.type()
                || activeQte == null) return;

        long now = System.currentTimeMillis();
        while (activeQte != null && now - activeQte.attemptStartedMs
                >= activeQte.attemptDurationMs) {
            double scheduledNextAttempt = activeQte.attemptStartedMs
                    + activeQte.attemptDurationMs;
            submitCurrentAttempt(
                    ThePaintThickensPerk.QTE_SCORE_MISS,
                    scheduledNextAttempt
            );
        }
        if (activeQte != null) {
            renderQte(event.getGuiGraphics(), activeQte, now);
        }
    }

    @SubscribeEvent
    public static void onDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        activeQte = null;
        paintOverlay = null;
    }

    private static void submitCurrentAttempt(int score, double nextAttemptStartedMs) {
        ActiveQte qte = activeQte;
        if (qte == null) return;
        int attempt = qte.attemptIndex;
        ModNetworking.sendToServer(new PaintQtePressPacket(qte.sessionId, attempt, score));
        qte.attemptIndex++;
        if (qte.attemptIndex >= qte.attempts) {
            activeQte = null;
        } else {
            qte.attemptStartedMs = nextAttemptStartedMs;
        }
    }

    private static void renderQte(GuiGraphics gui, ActiveQte qte, long now) {
        Minecraft minecraft = Minecraft.getInstance();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int bottomOffset = 71;
        if (minecraft.player != null
                && PlayerModeUtil.isSurvivalOrAdventure(minecraft.player)
                && HudConfig.isCustomHudEnabled()) {
            bottomOffset = CustomHud.getBottomContentTopOffset(
                    minecraft.player, minecraft.screen instanceof ChatScreen);
        }

        int x = (screenWidth - TOTAL_WIDTH) / 2;
        int y = screenHeight - bottomOffset - BAR_HEIGHT - QTE_BOTTOM_GAP;
        float appearance = Mth.clamp((now - qte.startedMs) / 120.0F, 0.0F, 1.0F);
        float scale = 0.68F + 0.32F * easeOutBack(appearance);

        gui.pose().pushPose();
        gui.pose().translate(screenWidth / 2.0F, y + BAR_HEIGHT / 2.0F, 150.0F);
        gui.pose().scale(scale, scale, 1.0F);
        gui.pose().translate(-screenWidth / 2.0F, -(y + BAR_HEIGHT / 2.0F), 0.0F);

        int accent = 0xFF000000 | qte.color.getRgb();
        gui.fill(x - BORDER, y - BORDER,
                x + KEY_BOX_WIDTH + BORDER, y + BAR_HEIGHT + BORDER, accent);
        gui.fill(x, y, x + KEY_BOX_WIDTH, y + BAR_HEIGHT, 0xEA202229);
        String key = ModKeybinds.PAINT_QTE_KEY.getTranslatedKeyMessage().getString();
        gui.drawCenteredString(minecraft.font, key, x + KEY_BOX_WIDTH / 2,
                y + (BAR_HEIGHT - minecraft.font.lineHeight) / 2, 0xFFFFFFFF);

        int barX = x + KEY_BOX_WIDTH + BAR_GAP;
        gui.fill(barX - BORDER, y - BORDER,
                barX + BAR_WIDTH + BORDER, y + BAR_HEIGHT + BORDER, accent);
        int missSide = Math.round(BAR_WIDTH * (qte.missPercent / 200.0F));
        int nearSide = Math.round(BAR_WIDTH * (qte.nearPercent / 200.0F));
        int greenWidth = BAR_WIDTH - 2 * missSide - 2 * nearSide;
        int cursor = barX;
        gui.fill(cursor, y, cursor += missSide, y + BAR_HEIGHT, COLOR_GRAY);
        gui.fill(cursor, y, cursor += nearSide, y + BAR_HEIGHT, COLOR_YELLOW);
        gui.fill(cursor, y, cursor += greenWidth, y + BAR_HEIGHT, COLOR_GREEN);
        gui.fill(cursor, y, cursor += nearSide, y + BAR_HEIGHT, COLOR_YELLOW);
        gui.fill(cursor, y, barX + BAR_WIDTH, y + BAR_HEIGHT, COLOR_GRAY);

        float sweep = (float) Mth.clamp((now - qte.attemptStartedMs)
                / qte.attemptDurationMs, 0.0D, 1.0D);
        int pointerX = barX + Math.round(sweep * (BAR_WIDTH - POINTER_WIDTH));
        gui.fill(pointerX, y - POINTER_OVERHANG,
                pointerX + POINTER_WIDTH, y + BAR_HEIGHT + POINTER_OVERHANG,
                0xFFFFFFFF);
        gui.fill(pointerX + 1, y - POINTER_OVERHANG + 1,
                pointerX + POINTER_WIDTH - 1, y + BAR_HEIGHT + POINTER_OVERHANG - 1,
                accent);

        int pipY = y - 7;
        int pipStartX = barX + BAR_WIDTH - qte.attempts * 7;
        for (int index = 0; index < qte.attempts; index++) {
            int color = index < qte.attemptIndex ? accent : 0xAA6A6E75;
            gui.fill(pipStartX + index * 7, pipY,
                    pipStartX + index * 7 + 5, pipY + 3, color);
        }
        gui.pose().popPose();
    }

    private static void renderSplats(GuiGraphics gui, List<ScreenSplat> splats,
                                     float alpha) {
        Minecraft minecraft = Minecraft.getInstance();
        int physicalWidth = minecraft.getWindow().getWidth();
        int physicalHeight = minecraft.getWindow().getHeight();
        float physicalToGuiScale = (float) (1.0D
                / minecraft.getWindow().getGuiScale());

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        gui.pose().pushPose();
        gui.pose().scale(physicalToGuiScale, physicalToGuiScale, 1.0F);
        for (PaintColor color : PaintColor.values()) {
            boolean renderedColor = false;
            RenderSystem.setShaderColor(color.red(), color.green(), color.blue(), alpha);
            for (ScreenSplat splat : splats) {
                if (splat.color != color) continue;
                renderedColor = true;
                int centerX = Math.round(physicalWidth * splat.centerX);
                int centerY = Math.round(physicalHeight * splat.centerY);
                int size = Math.max(
                        MIN_SPLAT_SIZE_PHYSICAL_PIXELS,
                        Math.round(Math.min(physicalWidth, physicalHeight)
                                * splat.size)
                );
                gui.pose().pushPose();
                gui.pose().translate(centerX, centerY, 70.0F);
                gui.pose().mulPose(Axis.ZP.rotationDegrees(splat.rotationDegrees));
                gui.pose().scale(splat.mirrored ? -1.0F : 1.0F, 1.0F, 1.0F);
                gui.blit(splat.texture, -size / 2, -size / 2,
                        0, 0, size, size, size, size);
                gui.pose().popPose();
            }
            if (renderedColor) {
                gui.flush();
            }
        }
        gui.pose().popPose();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
    }

    private static float easeOutBack(float value) {
        float shifted = value - 1.0F;
        return 1.0F + 2.70158F * shifted * shifted * shifted
                + 1.70158F * shifted * shifted;
    }

    private static final class ActiveQte {
        private final int sessionId;
        private final PaintColor color;
        private final int attempts;
        private final double attemptDurationMs;
        private final int missPercent;
        private final int nearPercent;
        private final int hitPercent;
        private final long startedMs;
        private double attemptStartedMs;
        private int attemptIndex;

        private ActiveQte(int sessionId, PaintColor color, int attempts,
                          int totalDurationMs, int missPercent,
                          int nearPercent, int hitPercent) {
            this.sessionId = sessionId;
            this.color = color;
            this.attempts = attempts;
            this.attemptDurationMs = totalDurationMs / (double) attempts;
            this.missPercent = missPercent;
            this.nearPercent = nearPercent;
            this.hitPercent = hitPercent;
            this.startedMs = System.currentTimeMillis();
            this.attemptStartedMs = startedMs;
        }

        private int scoreAt(long now) {
            float progress = (float) Mth.clamp((now - attemptStartedMs)
                    / attemptDurationMs, 0.0D, 1.0D);
            float missSide = missPercent / 200.0F;
            float nearSide = nearPercent / 200.0F;
            if (progress < missSide || progress > 1.0F - missSide) {
                return ThePaintThickensPerk.QTE_SCORE_MISS;
            }
            if (progress < missSide + nearSide
                    || progress > 1.0F - missSide - nearSide) {
                return ThePaintThickensPerk.QTE_SCORE_NEAR;
            }
            return ThePaintThickensPerk.QTE_SCORE_HIT;
        }
    }

    private static final class PaintOverlay {
        private final long startedMs;
        private final long durationMs;
        private final List<ScreenSplat> splats;

        private PaintOverlay(long startedMs, long durationMs,
                             List<ScreenSplat> splats) {
            this.startedMs = startedMs;
            this.durationMs = durationMs;
            this.splats = splats;
        }

        private static PaintOverlay create(PaintColor primaryColor,
                                           long startedMs, long durationMs) {
            Random random = new Random(System.nanoTime() ^ startedMs);
            List<PaintColor> colors = buildColors(primaryColor, random);
            List<ScreenSplat> splats = new ArrayList<>(SCREEN_SPLAT_COUNT);
            for (int index = 0; index < SCREEN_SPLAT_COUNT; index++) {
                boolean edge = index < EDGE_SPLAT_COUNT;
                float centerX;
                float centerY;
                if (edge) {
                    int side = random.nextInt(4);
                    float alongEdge = random.nextFloat();
                    centerX = switch (side) {
                        case 0 -> -EDGE_CENTER_OFFSET;
                        case 1 -> FULL_SCREEN_FRACTION + EDGE_CENTER_OFFSET;
                        default -> alongEdge;
                    };
                    centerY = switch (side) {
                        case 2 -> -EDGE_CENTER_OFFSET;
                        case 3 -> FULL_SCREEN_FRACTION + EDGE_CENTER_OFFSET;
                        default -> alongEdge;
                    };
                } else {
                    centerX = random.nextFloat();
                    centerY = random.nextFloat();
                }

                float minSize = edge ? EDGE_SPLAT_MIN_SIZE : INNER_SPLAT_MIN_SIZE;
                float maxSize = edge ? EDGE_SPLAT_MAX_SIZE : INNER_SPLAT_MAX_SIZE;
                float size = Mth.lerp(random.nextFloat(), minSize, maxSize);
                splats.add(new ScreenSplat(
                        SCREEN_SPLAT_TEXTURES[index % SCREEN_SPLAT_TEXTURES.length],
                        colors.get(index),
                        centerX,
                        centerY,
                        size,
                        random.nextFloat() * FULL_ROTATION_DEGREES,
                        random.nextBoolean()
                ));
            }
            Collections.shuffle(splats, random);
            return new PaintOverlay(startedMs, durationMs, List.copyOf(splats));
        }

        private static List<PaintColor> buildColors(PaintColor primaryColor,
                                                    Random random) {
            int primaryCount = Math.round(
                    SCREEN_SPLAT_COUNT * PRIMARY_COLOR_PERCENT / 100.0F
            );
            List<PaintColor> secondaryPalette = new ArrayList<>(
                    List.of(PaintColor.values())
            );
            secondaryPalette.remove(primaryColor);
            secondaryPalette.sort(Comparator.comparingDouble(
                    color -> -colorDistanceSquared(primaryColor, color)
            ));
            secondaryPalette = new ArrayList<>(secondaryPalette.subList(
                    0,
                    Math.min(SECONDARY_COLOR_COUNT, secondaryPalette.size())
            ));
            Collections.shuffle(secondaryPalette, random);

            List<PaintColor> colors = new ArrayList<>(SCREEN_SPLAT_COUNT);
            for (int index = 0; index < SCREEN_SPLAT_COUNT; index++) {
                if (index < primaryCount) {
                    colors.add(primaryColor);
                } else {
                    colors.add(secondaryPalette.get(
                            (index - primaryCount) % secondaryPalette.size()
                    ));
                }
            }
            Collections.shuffle(colors, random);
            return colors;
        }

        private static double colorDistanceSquared(PaintColor first,
                                                   PaintColor second) {
            double red = first.red() - second.red();
            double green = first.green() - second.green();
            double blue = first.blue() - second.blue();
            return red * red + green * green + blue * blue;
        }
    }

    private record ScreenSplat(ResourceLocation texture, PaintColor color,
                               float centerX, float centerY, float size,
                               float rotationDegrees, boolean mirrored) {
    }
}
