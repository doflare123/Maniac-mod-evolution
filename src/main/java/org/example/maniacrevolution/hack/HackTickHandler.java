package org.example.maniacrevolution.hack;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HackTickHandler {

    private static int tickCounter = 0;
    /** Сохраняем прогресс каждые 5 минут (6000 тиков) */
    private static final int AUTOSAVE_INTERVAL = 6000;

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        HackManager.reset();
        HackManager.get().load(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        HackManager.get().save(event.getServer());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.getServer() == null) return;

        tickCounter++;

        // Тик хак-менеджера раз в секунду
        if (tickCounter % 20 == 0) {
            HackManager.get().tick(event.getServer());
        }

        // Автосохранение
        if (tickCounter % AUTOSAVE_INTERVAL == 0) {
            HackManager.get().save(event.getServer());
        }
    }
}