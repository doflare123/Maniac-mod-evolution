package org.example.maniacrevolution.cosmetic.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;

/**
 * Обработчик очистки косметических эффектов
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class CosmeticCleanupHandler {

    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        // Очищаем кэш косметики при выходе из мира
        ClientCosmeticCache.clear();
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        // Удаляем игрока из кэша при выходе
        ClientCosmeticCache.removePlayer(event.getEntity().getUUID());
    }
}