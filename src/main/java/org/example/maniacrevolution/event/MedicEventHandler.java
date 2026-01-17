package org.example.maniacrevolution.event;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.item.MedicTabletItem;
import org.example.maniacrevolution.util.MedicTabletTracker;
import org.example.maniacrevolution.util.SelectiveGlowingEffect;

/**
 * Обработчик событий для системы медика
 */
@Mod.EventBusSubscriber
public class MedicEventHandler {

    /**
     * Очистка данных при выходе игрока
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            // Очищаем подсветки
            SelectiveGlowingEffect.onPlayerLogout(serverPlayer);

            // Очищаем отслеживание
            MedicTabletTracker.onPlayerLogout(serverPlayer);

            // Очищаем кулдауны планшета
            MedicTabletItem.onPlayerLogout(serverPlayer.getUUID());
        }
    }
}