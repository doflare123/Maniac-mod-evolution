package org.example.maniacrevolution.events;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.hud.CustomHud;
import org.example.maniacrevolution.hud.GuideUpdateIndicatorHud;
import org.example.maniacrevolution.hud.RoseColoredGlassesOverlay;
import org.example.maniacrevolution.hud.PinkOrchidRecordingOverlay;
import org.example.maniacrevolution.hud.SberSproutOverlay;
import org.example.maniacrevolution.hud.ColorRouletteOverlay;
import org.example.maniacrevolution.hud.PreGameReadyOverlay;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class HudEvents {

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("custom_hud", CustomHud.INSTANCE);
        event.registerAboveAll("pregame_ready", PreGameReadyOverlay.INSTANCE);
        event.registerAboveAll("color_roulette", ColorRouletteOverlay.INSTANCE);
        event.registerAboveAll("guide_update_indicator", new GuideUpdateIndicatorHud());
        event.registerAboveAll("pink_orchid_recording", PinkOrchidRecordingOverlay.INSTANCE);
        event.registerAboveAll("sber_sprout", SberSproutOverlay.INSTANCE);
        event.registerAboveAll("rose_colored_glasses_filter", RoseColoredGlassesOverlay.INSTANCE);
    }
}
