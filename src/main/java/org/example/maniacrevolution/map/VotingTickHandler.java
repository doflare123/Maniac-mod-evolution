package org.example.maniacrevolution.map;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public class VotingTickHandler {
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            tickCounter++;

            // Каждую секунду (20 тиков)
            if (tickCounter >= 20) {
                tickCounter = 0;
                MapVotingManager.getInstance().tick();
            }
        }
    }
}