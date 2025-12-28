package org.example.maniacrevolution.event;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public class HungerEvents {

    @SubscribeEvent
    public static void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (!event.player.level().isClientSide()) {
            // Устанавливаем голод на максимум, чтобы он не терялся
            event.player.getFoodData().setFoodLevel(20);
            event.player.getFoodData().setSaturation(20.0f);
        }
    }
}