package org.example.maniacrevolution.event;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.dodepovich.DodepovichCasinoManager;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public class DodepovichServerTickHandler {
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        DodepovichCasinoManager.tickDelayedActions();
    }
}
