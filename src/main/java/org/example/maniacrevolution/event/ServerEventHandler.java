package org.example.maniacrevolution.event;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.item.DeathScytheItem;
import org.example.maniacrevolution.item.HookItem;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public class ServerEventHandler {

    private static int cleanupTicker = 0;
    private static final int CLEANUP_INTERVAL = 20 * 60; // Раз в минуту

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        cleanupTicker++;

        // Очистка истекших кулдаунов раз в минуту
        if (cleanupTicker >= CLEANUP_INTERVAL) {
            HookItem.cleanupExpiredCooldowns();
            cleanupTicker = 0;
        }
    }
}