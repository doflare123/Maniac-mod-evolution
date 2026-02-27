package org.example.maniacrevolution.downed;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class DownedHudClient {

    private static int     role           = DownedHudPacket.ROLE_CLEAR;
    private static String  downedName     = "";
    private static int     remainingTicks = 0;
    private static float   reviveProgress = 0f;
    private static boolean isPaused       = false;

    public static void update(DownedHudPacket pkt) {
        role           = pkt.role;
        downedName     = pkt.downedName;
        remainingTicks = pkt.remainingTicks;
        reviveProgress = pkt.reviveProgress;
        isPaused       = pkt.isPaused;
    }

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

    private static void renderHud(GuiGraphics gui, Font font, int sw, int sh) {
        // Центр экрана — под прицелом
        int cx = sw / 2;
        // Первая строка начинается на 12px ниже центра (чуть под прицелом)
        int y = sh / 2 + 12;

        switch (role) {
            case DownedHudPacket.ROLE_SELF  -> renderSelf(gui, font, cx, y);
            case DownedHudPacket.ROLE_ALLY  -> renderAlly(gui, font, cx, y);
            case DownedHudPacket.ROLE_ENEMY -> renderEnemy(gui, font, cx, y);
        }
    }

    // ── Лежачий — видит только таймер или прогресс подъёма ───────────────

    private static void renderSelf(GuiGraphics gui, Font font, int cx, int y) {
        if (isPaused) {
            int pct = (int)(reviveProgress * 100);
            drawCentered(gui, font, cx, y,     "§aПодъём: " + pct + "%");
            drawCentered(gui, font, cx, y + 10, "§7таймер на паузе");
        } else {
            int sec = remainingTicks / 20;
            String col = sec > 20 ? "§e" : sec > 10 ? "§6" : "§c";
            drawCentered(gui, font, cx, y, "§c☠ " + col + sec + "с §7до смерти");
        }
    }

    // ── Союзник — подсказка + прогресс если поднимает ────────────────────

    private static void renderAlly(GuiGraphics gui, Font font, int cx, int y) {
        if (reviveProgress > 0f) {
            // Этот игрок поднимает
            int pct = (int)(reviveProgress * 100);
            drawCentered(gui, font, cx, y,      "§aПодъём " + downedName + ": " + pct + "%");
            drawCentered(gui, font, cx, y + 10, "§7Удерживайте §aПКМ");
        } else if (isPaused) {
            // Кто-то другой поднимает
            drawCentered(gui, font, cx, y, "§e" + downedName + " §7поднимают...");
        } else {
            // Никто не поднимает
            int sec = remainingTicks / 20;
            drawCentered(gui, font, cx, y,      "§c" + downedName + " §7упал  §e" + sec + "с");
            drawCentered(gui, font, cx, y + 10, "§7Удерживайте §aПКМ у ног");
        }
    }

    // ── Враг — подсказка про тащение ─────────────────────────────────────

    private static void renderEnemy(GuiGraphics gui, Font font, int cx, int y) {
        drawCentered(gui, font, cx, y, "§6" + downedName + " §7лежит");
        drawCentered(gui, font, cx, y + 10, "§7Удерживайте §6ПКМ §7чтобы тащить");
    }

    // ── Утилита ───────────────────────────────────────────────────────────

    private static void drawCentered(GuiGraphics gui, Font font, int cx, int y, String text) {
        // Полупрозрачная подложка под текстом для читаемости
        int w = font.width(net.minecraft.network.chat.Component.literal(text).getString());
        // Рисуем тень через drawString с shadow=true — этого достаточно
        gui.drawCenteredString(font, text, cx, y, 0xFFFFFF);
    }
}
