package org.example.maniacrevolution.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.event.PenaltySlotManager;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public class PenaltyHudOverlay {

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.CROSSHAIR.type()) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null || player.isCreative() || player.isSpectator()) return;

        GuiGraphics gui = event.getGuiGraphics();
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        // Показываем предупреждение, если игрок в пенальти-слоте
        if (PenaltySlotManager.isInPenaltySlot(player)) {
            String warning = "§c⚠ СЛОТ ТОЛЬКО ДЛЯ ХРАНЕНИЯ ⚠";
            int textWidth = mc.font.width(warning);
            int x = (screenW - textWidth) / 2;
            int y = screenH / 2 + 20;

            // Тень для текста
            //gui.drawString(mc.font, warning, x + 1, y + 1, 0xFF000000, false);
            // Основной текст
            gui.drawString(mc.font, warning, x, y, 0xFFFF0000, false);
        }

        // Показываем уровень замедления в правом верхнем углу
        int filledSlots = PenaltySlotManager.getFilledPenaltySlots(player);
        if (filledSlots > 0) {
            String slownessInfo = "§cЗамедление: §f" + getRomanNumeral(filledSlots);
            int x = screenW - mc.font.width(slownessInfo) - 5;
            int y = 5;

            gui.drawString(mc.font, slownessInfo, x, y, 0xFFFFFFFF, true);
        }
    }

    /**
     * Конвертирует число в римские цифры (I, II, III)
     */
    private static String getRomanNumeral(int number) {
        return switch (number) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> String.valueOf(number);
        };
    }
}