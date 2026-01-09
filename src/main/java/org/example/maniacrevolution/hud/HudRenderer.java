package org.example.maniacrevolution.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public class HudRenderer {

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        // Рендерим после любого оверлея, но только один раз за кадр
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        GuiGraphics gui = event.getGuiGraphics();
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        // Рендерим худы без проверки на креатив/спектатор
        // (если нужна проверка, добавь её внутри самих худов)
        LevelHud.render(gui, 5, 5);
        PerkHud.render(gui, 5, screenH - 70);
        TimerHud.render(gui, screenW / 2, 5);
    }

    // Перехватываем рендер названия предмета и сдвигаем его выше
    @SubscribeEvent
    public static void onRenderItemName(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() == VanillaGuiOverlay.ITEM_NAME.type()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.player.isCreative() || mc.player.isSpectator()) {
                return;
            }

            // Отменяем стандартный рендер
            event.setCanceled(true);

            // Рендерим название выше
            renderCustomItemName(event.getGuiGraphics(), mc);
        }
    }

    private static void renderCustomItemName(GuiGraphics gui, Minecraft mc) {
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        ItemStack itemStack = mc.player.getInventory().getSelected();
        if (itemStack.isEmpty()) return;

        int duration = mc.gui.getGuiTicks();
        if (duration <= 0) return;

        int opacity = (int) ((float) duration * 256.0F / 10.0F);
        if (opacity > 255) opacity = 255;

        Component name = itemStack.getHoverName();
        int textWidth = mc.font.width(name);
        int x = (screenW - textWidth) / 2;

        // Меняем Y координату - сдвигаем выше твоего HUD
        int y = screenH - 90;

        gui.drawString(mc.font, name, x, y, 0xFFFFFF | (opacity << 24));
    }
}