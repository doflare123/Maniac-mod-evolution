package org.example.maniacrevolution.event;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.config.HudConfig;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public class VanillaHudCanceller {

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Pre event) {
        // Отключаем ванильные элементы HUD только если кастомный HUD включен
        if (!HudConfig.isCustomHudEnabled()) {
            return;
        }

        // Отключаем ванильные элементы HUD
        if (event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id()) ||
                event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id()) ||
                event.getOverlay().id().equals(VanillaGuiOverlay.FOOD_LEVEL.id()) ||
                event.getOverlay().id().equals(VanillaGuiOverlay.ARMOR_LEVEL.id()) ||
                event.getOverlay().id().equals(VanillaGuiOverlay.AIR_LEVEL.id()) ||
                event.getOverlay().id().equals(VanillaGuiOverlay.EXPERIENCE_BAR.id())) {
            event.setCanceled(true);
        }
    }
}