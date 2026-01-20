package org.example.maniacrevolution.event;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.config.HudConfig;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public class VanillaHudCanceller {

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();

        // Если нет игрока - не трогаем ванильный HUD
        if (mc.player == null) {
            return;
        }

        // В КРЕАТИВЕ/НАБЛЮДАТЕЛЕ - ВСЕГДА показываем ванильный HUD
        if (mc.player.isCreative() || mc.player.isSpectator()) {
            return; // НЕ отменяем ванильный рендер
        }

        // Если кастомный HUD выключен - показываем ванильный HUD
        if (!HudConfig.isCustomHudEnabled()) {
            return;
        }

        // Только в ВЫЖИВАНИИ с ВКЛЮЧЕННЫМ кастомным HUD - отключаем ванильные элементы
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