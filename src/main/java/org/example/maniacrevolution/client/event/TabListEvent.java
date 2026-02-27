package org.example.maniacrevolution.client.event;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.client.gui.CustomTabListRenderer;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class TabListEvent {

    @SubscribeEvent
    public static void onTabListPre(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() == VanillaGuiOverlay.PLAYER_LIST.type()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.options.keyPlayerList.isDown()) {
                // Отменяем ванильный таб
                event.setCanceled(true);

                // Рендерим свой таб прямо здесь
                int w = event.getWindow().getGuiScaledWidth();
                int h = event.getWindow().getGuiScaledHeight();
                CustomTabListRenderer.render(event.getGuiGraphics(), w, h);
            }
        }
    }
}