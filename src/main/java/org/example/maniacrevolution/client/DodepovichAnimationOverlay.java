package org.example.maniacrevolution.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.dodepovich.DodepovichCoin;
import org.example.maniacrevolution.dodepovich.SlotMachineResult;

import java.util.List;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public class DodepovichAnimationOverlay {
    private static final long COIN_FLIP_MS = 500;
    private static final long COIN_HOLD_MS = 850;
    private static final long SLOT_DURATION_MS = 3200;
    private static final int COIN_GOOD_COLOR = 0xFF58D878;
    private static final int COIN_BAD_COLOR = 0xFFE14F4F;
    private static final List<ItemStack> FAKE_ICONS = List.of(
            new ItemStack(Items.APPLE),
            new ItemStack(Items.BLAZE_POWDER),
            new ItemStack(Items.ENDER_PEARL),
            new ItemStack(Items.GOLDEN_CARROT),
            new ItemStack(Items.REDSTONE),
            new ItemStack(Items.AMETHYST_SHARD),
            new ItemStack(Items.LAPIS_LAZULI)
    );

    private static CoinAnimation coinAnimation;
    private static SlotAnimation slotAnimation;

    public static void startCoinFlip(DodepovichCoin coin, boolean good) {
        coinAnimation = new CoinAnimation(coin, good, Util.getMillis());
    }

    public static void startSlotMachine(DodepovichCoin coin, SlotMachineResult result) {
        slotAnimation = new SlotAnimation(coin, result, Util.getMillis());
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.CROSSHAIR.type()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;

        long now = Util.getMillis();
        GuiGraphics graphics = event.getGuiGraphics();
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        if (slotAnimation != null) {
            if (now - slotAnimation.startedAt > SLOT_DURATION_MS) {
                slotAnimation = null;
            } else {
                renderSlotMachine(graphics, width, height, now);
            }
        }

        if (coinAnimation != null) {
            if (now - coinAnimation.startedAt > COIN_FLIP_MS + COIN_HOLD_MS) {
                coinAnimation = null;
            } else {
                renderCoinFlip(graphics, width, height, now);
            }
        }
    }

    private static void renderCoinFlip(GuiGraphics graphics, int width, int height, long now) {
        CoinAnimation animation = coinAnimation;
        long elapsed = now - animation.startedAt;
        float flipProgress = Math.min(1.0f, elapsed / (float) COIN_FLIP_MS);
        boolean settled = elapsed >= COIN_FLIP_MS;

        int centerX = width / 2;
        int baseY = height / 2 + 44;
        int arc = (int) (Math.sin(flipProgress * Math.PI) * 86.0);
        int y = settled ? baseY - 8 : baseY - arc;

        float phase = settled ? (animation.good ? 1.0f : 0.0f) : (float) ((elapsed / 65) % 2);
        boolean greenSide = settled ? animation.good : phase >= 1.0f;
        int color = greenSide ? COIN_GOOD_COLOR : COIN_BAD_COLOR;

        float flip = settled ? 1.0f : Math.abs((flipProgress * 10.0f) % 2.0f - 1.0f);
        int sizeX = Math.max(10, (int) (58 * flip));
        int sizeY = 58;

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 250);
        renderCoinItem(graphics, animation.coin, centerX, y, sizeX, sizeY, greenSide);
        graphics.pose().popPose();
    }

    private static void renderCoinItem(GuiGraphics graphics, DodepovichCoin coin, int centerX, int centerY,
                                       int sizeX, int sizeY, boolean goodSide) {
        ItemStack stack = getCoinStack(coin);
        float red = goodSide ? 0.62f : 1.0f;
        float green = goodSide ? 1.0f : 0.52f;
        float blue = goodSide ? 0.62f : 0.52f;

        graphics.fill(centerX - sizeX / 2 + 4, centerY - sizeY / 2 + 5,
                centerX + sizeX / 2 + 4, centerY + sizeY / 2 + 5, 0x55000000);
        graphics.pose().pushPose();
        graphics.pose().translate(centerX - sizeX / 2.0f, centerY - sizeY / 2.0f, 0);
        graphics.pose().scale(sizeX / 16.0f, sizeY / 16.0f, 1.0f);
        RenderSystem.setShaderColor(red, green, blue, 1.0f);
        graphics.renderItem(stack, 0, 0);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        graphics.pose().popPose();
    }

    private static void renderSlotMachine(GuiGraphics graphics, int width, int height, long now) {
        SlotAnimation animation = slotAnimation;
        long elapsed = now - animation.startedAt;

        int machineW = 230;
        int machineH = 158;
        int x = width / 2 - machineW / 2;
        int y = height / 2 - machineH / 2;

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 220);
        graphics.fill(x, y, x + machineW, y + machineH, 0xE01B1210);
        graphics.fill(x + 5, y + 5, x + machineW - 5, y + machineH - 5, 0xE05A241F);
        graphics.fill(x + 12, y + 28, x + machineW - 12, y + 116, 0xFF211614);
        graphics.renderOutline(x, y, machineW, machineH, 0xFFFFCC55);
        graphics.drawCenteredString(Minecraft.getInstance().font, "§6§lDODEPOVICH", width / 2, y + 13, 0xFFFFFFFF);

        int reelY = y + 34;
        int reelW = 56;
        int reelH = 78;
        int gap = 7;
        int startX = width / 2 - (reelW * 3 + gap * 2) / 2;

        for (int reel = 0; reel < 3; reel++) {
            int sx = startX + reel * (reelW + gap);
            renderReel(graphics, sx, reelY, reelW, reelH, reel, elapsed);
        }

        if (elapsed >= 2350) {
            int color = animation.result.getColor() & 0x00FFFFFF;
            graphics.drawCenteredString(Minecraft.getInstance().font, animation.result.getDisplayName(), width / 2, y + 126, color);
        }
        graphics.pose().popPose();
    }

    private static void renderReel(GuiGraphics graphics, int x, int y, int width, int height, int reel, long elapsed) {
        boolean stopped = isReelStopped(reel, elapsed);
        int innerX = x + 4;
        int innerY = y + 4;
        int innerW = width - 8;
        int innerH = height - 8;
        int centerY = innerY + innerH / 2;

        graphics.fill(x, y, x + width, y + height, 0xFF5B2B1F);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0xFFFFE2B4);
        graphics.fill(innerX, innerY, innerX + innerW, innerY + innerH, 0xFFF7E8C8);
        graphics.fill(innerX, innerY, innerX + innerW, innerY + 8, 0x55FFFFFF);
        graphics.fill(innerX, innerY + innerH - 9, innerX + innerW, innerY + innerH - 6, 0x18000000);
        graphics.fill(innerX, innerY + innerH - 6, innerX + innerW, innerY + innerH - 3, 0x26000000);
        graphics.fill(innerX, innerY + innerH - 3, innerX + innerW, innerY + innerH, 0x36000000);
        graphics.fill(innerX, centerY - 17, innerX + innerW, centerY + 17, 0x22FFFFFF);

        graphics.enableScissor(innerX, innerY, innerX + innerW, innerY + innerH);
        if (stopped) {
            int finalX = innerX + innerW / 2 - 8;
            renderAdjacentIcon(graphics, finalX, centerY - 44, reel + 1);
            renderFinalSlotIcon(graphics, finalX, centerY - 8, reel);
            renderAdjacentIcon(graphics, finalX, centerY + 28, reel + 3);
        } else {
            int step = 34;
            int offset = (int) ((elapsed * (0.42 + reel * 0.06)) % step);
            int iconX = innerX + innerW / 2 - 8;
            for (int row = -3; row <= 3; row++) {
                int iconY = centerY - 8 + row * step + offset;
                ItemStack icon = FAKE_ICONS.get((int) ((elapsed / 90 + reel * 2 + row + FAKE_ICONS.size()) % FAKE_ICONS.size()));
                graphics.renderItem(icon, iconX, iconY);
            }
        }
        graphics.disableScissor();

        graphics.fill(innerX, centerY - 18, innerX + innerW, centerY - 16, 0xAAC43E2A);
        graphics.fill(innerX, centerY + 16, innerX + innerW, centerY + 18, 0xAAC43E2A);
        graphics.renderOutline(x, y, width, height, stopped ? 0xFFFFCC55 : 0xFF7D4E35);
    }

    private static void renderFinalSlotIcon(GuiGraphics graphics, int x, int y, int reel) {
        SlotMachineResult result = slotAnimation.result;
        if (result == SlotMachineResult.JACKPOT) {
            graphics.drawCenteredString(Minecraft.getInstance().font, "§6§l7", x + 8, y + 4, 0xFFFFFFFF);
            return;
        }

        if (result == SlotMachineResult.DEATH) {
            renderDeathIcon(graphics, x, y);
            return;
        }

        ItemStack stack = result.isTriple()
                ? result.getIcon(slotAnimation.coin)
                : FAKE_ICONS.get((reel * 2 + 1) % FAKE_ICONS.size());

        if (result == SlotMachineResult.COIN_GOOD) {
            renderTintedItem(graphics, stack, x, y, 0.45f, 1.0f, 0.45f);
        } else if (result == SlotMachineResult.COIN_BAD) {
            renderTintedItem(graphics, stack, x, y, 1.0f, 0.35f, 0.35f);
        } else {
            graphics.renderItem(stack, x, y);
        }
    }

    private static void renderAdjacentIcon(GuiGraphics graphics, int x, int y, int seed) {
        graphics.renderItem(FAKE_ICONS.get(seed % FAKE_ICONS.size()), x, y);
    }

    private static void renderTintedItem(GuiGraphics graphics, ItemStack stack, int x, int y, float red, float green, float blue) {
        RenderSystem.setShaderColor(red, green, blue, 1.0f);
        graphics.renderItem(stack, x, y);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        int overlayColor = ((int) (red * 80) << 16) | ((int) (green * 80) << 8) | (int) (blue * 80) | 0x44000000;
        graphics.fill(x, y, x + 16, y + 16, overlayColor);
    }

    private static boolean isReelStopped(int reel, long elapsed) {
        return elapsed >= 1850 + reel * 430L;
    }

    private static ItemStack getCoinStack(DodepovichCoin coin) {
        return switch (coin) {
            case ELUSIVENESS -> new ItemStack(ModItems.COIN_ELUSIVENESS.get());
            case INSIGHT -> new ItemStack(ModItems.COIN_INSIGHT.get());
            case SHACKLES -> new ItemStack(ModItems.COIN_SHACKLES.get());
            case HEALTH -> new ItemStack(ModItems.COIN_HEALTH.get());
            case EAGLE -> new ItemStack(ModItems.COIN_EAGLE.get());
            case DEBT -> new ItemStack(ModItems.COIN_DEBT.get());
            case REROLL -> new ItemStack(ModItems.COIN_REROLL.get());
            case FATE -> new ItemStack(ModItems.COIN_FATE.get());
        };
    }

    private static void renderDeathIcon(GuiGraphics graphics, int x, int y) {
        graphics.fill(x + 3, y + 1, x + 13, y + 11, 0xFFEEE8D8);
        graphics.fill(x + 1, y + 4, x + 15, y + 10, 0xFFEEE8D8);
        graphics.fill(x + 5, y + 10, x + 11, y + 15, 0xFFEEE8D8);
        graphics.fill(x + 4, y + 5, x + 7, y + 8, 0xFF1B0B0B);
        graphics.fill(x + 9, y + 5, x + 12, y + 8, 0xFF1B0B0B);
        graphics.fill(x + 7, y + 9, x + 9, y + 11, 0xFF1B0B0B);
        graphics.fill(x + 4, y + 14, x + 5, y + 16, 0xFFEEE8D8);
        graphics.fill(x + 7, y + 14, x + 8, y + 16, 0xFFEEE8D8);
        graphics.fill(x + 10, y + 14, x + 11, y + 16, 0xFFEEE8D8);
    }

    private record CoinAnimation(DodepovichCoin coin, boolean good, long startedAt) {
    }

    private record SlotAnimation(DodepovichCoin coin, SlotMachineResult result, long startedAt) {
    }
}
