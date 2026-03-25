package org.example.maniacrevolution.downed;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public class DownedHudClient {

    private static int     role           = DownedHudPacket.ROLE_CLEAR;
    private static String  downedName     = "";
    private static int     remainingTicks = 0;
    private static float   reviveProgress = 0f;
    private static boolean isPaused       = false;

    public static void update(DownedHudPacket pkt) {
        int oldRole = role;
        role           = pkt.role;
        downedName     = pkt.downedName;
        remainingTicks = pkt.remainingTicks;
        reviveProgress = pkt.reviveProgress;
        isPaused       = pkt.isPaused;

        // Снимаем форсированную позу когда нас подняли / умерли
        if (oldRole == DownedHudPacket.ROLE_SELF && role == DownedHudPacket.ROLE_CLEAR) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.setForcedPose(null);
            }
        }
    }

    // ── Клиентский тик — форсируем позу локально ─────────────────────────

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (role != DownedHudPacket.ROLE_SELF) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        // Форсируем позу на клиенте каждый тик — без этого клиент сбрасывает её локально
        player.setForcedPose(Pose.SLEEPING);

        // Блокируем прыжок на клиенте — обнуляем jumping флаг
        player.input.jumping = false;
    }

    // ── Регистрация HUD оверлея (MOD bus) ────────────────────────────────

    @Mod.EventBusSubscriber(modid = Maniacrev.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class Registration {
        @SubscribeEvent
        public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "downed_hud",
                    (gui, graphics, partialTick, screenWidth, screenHeight) -> {
                        if (role == DownedHudPacket.ROLE_CLEAR) return;
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.options.hideGui) return;
                        renderHud(graphics, mc.font, screenWidth, screenHeight);
                    });
        }
    }

    private static void renderHud(GuiGraphics gui, Font font, int sw, int sh) {
        int cx = sw / 2;
        int y  = sh / 2 + 12;

        switch (role) {
            case DownedHudPacket.ROLE_SELF -> renderSelf(gui, font, cx, y);
            case DownedHudPacket.ROLE_ALLY -> renderAlly(gui, font, cx, y);
        }
    }

    // ── Лежачий ───────────────────────────────────────────────────────────

    private static void renderSelf(GuiGraphics gui, Font font, int cx, int y) {
        if (isPaused) {
            int pct = (int)(reviveProgress * 100);
            drawCentered(gui, font, cx, y,      "§aПодъём: " + pct + "%");
            drawCentered(gui, font, cx, y + 10, "§7таймер на паузе");
        } else {
            int sec = remainingTicks / 20;
            String col = sec > 20 ? "§e" : sec > 10 ? "§6" : "§c";
            drawCentered(gui, font, cx, y, "§c☠ " + col + sec + "с §7до смерти");
        }
    }

    // ── Союзник ───────────────────────────────────────────────────────────

    private static void renderAlly(GuiGraphics gui, Font font, int cx, int y) {
        if (reviveProgress > 0f) {
            int pct = (int)(reviveProgress * 100);
            String bar = buildBar(reviveProgress, 8);
            drawCentered(gui, font, cx, y,      bar + " §f" + pct + "%");
            drawCentered(gui, font, cx, y + 10, "§7Поднимаете §e" + downedName);
        } else if (isPaused) {
            drawCentered(gui, font, cx, y, "§e" + downedName + " §7поднимают...");
        } else {
            int sec = remainingTicks / 20;
            String col = sec > 20 ? "§e" : sec > 10 ? "§6" : "§c";
            drawCentered(gui, font, cx, y,      "§c" + downedName + " §7упал  " + col + sec + "с");
            drawCentered(gui, font, cx, y + 10, "§7Удерживайте §aПКМ у тела");
        }
    }

    private static String buildBar(float progress, int len) {
        int filled = Math.round(progress * len);
        return "§a" + "█".repeat(filled) + "§8" + "█".repeat(len - filled);
    }

    // ── Утилита ───────────────────────────────────────────────────────────

    private static void drawCentered(GuiGraphics gui, Font font, int cx, int y, String text) {
        gui.drawCenteredString(font, text, cx, y, 0xFFFFFF);
    }
}
