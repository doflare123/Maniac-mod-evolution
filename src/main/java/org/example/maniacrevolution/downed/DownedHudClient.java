package org.example.maniacrevolution.downed;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;

/**
 * Клиентский HUD — рисует в правой части экрана (зелёная зона).
 *
 * ВАЖНО: рендер оверлея регистрируется через RegisterGuiOverlaysEvent на MOD BUS.
 * Данные получаем из DownedHudPacket через статический update().
 */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class DownedHudClient {

    // ── Текущее состояние (обновляется пакетом с сервера) ─────────────────
    private static int     role           = DownedHudPacket.ROLE_CLEAR;
    private static String  downedName     = "";
    private static int     remainingTicks = 0;
    private static float   reviveProgress = 0f;
    private static boolean isPaused       = false;

    /** Вызывается из DownedHudPacket.handle() на клиенте */
    public static void update(DownedHudPacket pkt) {
        role           = pkt.role;
        downedName     = pkt.downedName;
        remainingTicks = pkt.remainingTicks;
        reviveProgress = pkt.reviveProgress;
        isPaused       = pkt.isPaused;
    }

    // ── Регистрация оверлея (MOD BUS) ────────────────────────────────────

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        // Рисуем поверх хотбара
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(),
                "downed_hud",
                (gui, graphics, partialTick, screenWidth, screenHeight) -> {
                    if (role == DownedHudPacket.ROLE_CLEAR) return;
                    renderHud(graphics, screenWidth, screenHeight);
                });
    }

    // ── Рендер ────────────────────────────────────────────────────────────

    private static void renderHud(GuiGraphics gui, int screenW, int screenH) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;
        Font font = mc.font;

        int blockW = 160;
        int x = screenW - blockW - 10;
        int y = screenH / 2 - 20;

        switch (role) {
            case DownedHudPacket.ROLE_SELF  -> renderSelf(gui, font, x, y, blockW);
            case DownedHudPacket.ROLE_ALLY  -> renderAlly(gui, font, x, y, blockW);
            case DownedHudPacket.ROLE_ENEMY -> renderEnemy(gui, font, x, y, blockW);
        }
    }

    // ── Лежачий ───────────────────────────────────────────────────────────

    private static void renderSelf(GuiGraphics gui, Font font, int x, int y, int w) {
        int remainSec = remainingTicks / 20;

        int bgHeight = isPaused ? 46 : 46;
        gui.fill(x - 6, y - 6, x + w + 6, y + bgHeight, 0xAA000000);
        // Рамка
        gui.fill(x - 6, y - 6, x + w + 6, y - 5, 0xFFCC2222);
        gui.fill(x - 6, y + bgHeight - 1, x + w + 6, y + bgHeight, 0xFFCC2222);

        String title = isPaused ? "§e⬆ Вас поднимают..." : "§c☠ Вы упали!";
        gui.drawString(font, title, x, y, 0xFFFFFF, true);
        y += 13;

        if (isPaused) {
            gui.drawString(font, "§7Прогресс подъёма:", x, y, 0xAAAAAA, false);
            y += 11;
            drawBar(gui, x, y, w, reviveProgress, 0xFF22CC44, 0xFF444444);
            y += 12;
            gui.drawString(font, "§a" + (int)(reviveProgress * 100) + "%  §7таймер на паузе", x, y, 0xFFFFFF, false);
        } else {
            gui.drawString(font, "§7Осталось: §e" + remainSec + " §7сек", x, y, 0xFFFFFF, false);
            y += 11;
            float p = (float) remainingTicks / DownedData.DOWNED_TIMEOUT_TICKS;
            int col = p > 0.5f ? 0xFFCC2222 : p > 0.25f ? 0xFFDD8800 : 0xFFFF3333;
            drawBar(gui, x, y, w, p, col, 0xFF444444);
            y += 12;
            gui.drawString(font, "§7Зовите союзников!", x, y, 0xAAAAAA, false);
        }
    }

    // ── Союзник ───────────────────────────────────────────────────────────

    private static void renderAlly(GuiGraphics gui, Font font, int x, int y, int w) {
        int remainSec = remainingTicks / 20;

        gui.fill(x - 6, y - 6, x + w + 6, y + 46, 0xAA000000);
        gui.fill(x - 6, y - 6, x + w + 6, y - 5, 0xFF22AA44);
        gui.fill(x - 6, y + 45, x + w + 6, y + 46, 0xFF22AA44);

        gui.drawString(font, "§c☠ §f" + downedName, x, y, 0xFFFFFF, true);
        y += 13;

        if (reviveProgress > 0f) {
            // Идёт подъём этим игроком
            gui.drawString(font, "§aПодъём: §f" + (int)(reviveProgress * 100) + "%", x, y, 0xFFFFFF, false);
            y += 11;
            drawBar(gui, x, y, w, reviveProgress, 0xFF22CC44, 0xFF444444);
            y += 12;
            gui.drawString(font, "§7Держите §aПКМ §7у ног!", x, y, 0xAAAAAA, false);
        } else if (isPaused) {
            // Кто-то другой поднимает
            gui.drawString(font, "§eКто-то поднимает...", x, y, 0xFFFFFF, false);
            y += 11;
            drawBar(gui, x, y, w, 0f, 0xFF22CC44, 0xFF444444);
            y += 12;
            gui.drawString(font, "§7Осталось: §e" + remainSec + " §7сек", x, y, 0xAAAAAA, false);
        } else {
            // Никто не поднимает
            float p = (float) remainingTicks / DownedData.DOWNED_TIMEOUT_TICKS;
            int col = p > 0.5f ? 0xFFCC2222 : p > 0.25f ? 0xFFDD8800 : 0xFFFF3333;
            gui.drawString(font, "§7Осталось: §e" + remainSec + " §7сек", x, y, 0xFFFFFF, false);
            y += 11;
            drawBar(gui, x, y, w, p, col, 0xFF444444);
            y += 12;
            gui.drawString(font, "§a[ПКМ у ног] §7Поднять", x, y, 0xAAAAAA, false);
        }
    }

    // ── Враг ──────────────────────────────────────────────────────────────

    private static void renderEnemy(GuiGraphics gui, Font font, int x, int y, int w) {
        gui.fill(x - 6, y - 6, x + w + 6, y + 32, 0xAA000000);
        gui.fill(x - 6, y - 6, x + w + 6, y - 5, 0xFFDD8800);
        gui.fill(x - 6, y + 31, x + w + 6, y + 32, 0xFFDD8800);

        gui.drawString(font, "§e☠ §f" + downedName, x, y, 0xFFFFFF, true);
        y += 13;
        gui.drawString(font, "§6[ПКМ у ног] §7Тащить", x, y, 0xAAAAAA, false);
        y += 13;
        gui.drawString(font, "§7(замедление при тащении)", x, y, 0x888888, false);
    }

    // ── Полоска прогресса ─────────────────────────────────────────────────

    private static void drawBar(GuiGraphics gui, int x, int y, int w,
                                float progress, int filledColor, int emptyColor) {
        int fw = (int)(w * Math.max(0f, Math.min(1f, progress)));
        gui.fill(x, y, x + w, y + 7, emptyColor);
        if (fw > 0) gui.fill(x, y, x + fw, y + 7, filledColor);
        // Рамка
        gui.fill(x - 1, y - 1, x + w + 1, y,     0xFF000000);
        gui.fill(x - 1, y + 7, x + w + 1, y + 8, 0xFF000000);
        gui.fill(x - 1, y - 1, x,         y + 8, 0xFF000000);
        gui.fill(x + w, y - 1, x + w + 1, y + 8, 0xFF000000);
    }
}
