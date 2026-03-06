package org.example.maniacrevolution.hack;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;

/**
 * Серверный тик-обработчик для HackManager.
 *
 * Регистрация: автоматическая через @Mod.EventBusSubscriber.
 * НЕ нужно добавлять в MinecraftForge.EVENT_BUS.register().
 *
 * HackManager.tick() вызывается каждые 20 тиков (1 раз в секунду).
 */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HackTickHandler {

    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.getServer() == null) return;

        tickCounter++;

        // Раз в секунду
        if (tickCounter % 20 == 0) {
            HackManager.get().tick(event.getServer());
        }
    }
}