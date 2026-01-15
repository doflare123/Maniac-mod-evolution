package org.example.maniacrevolution.event;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.gui.MedicTabletScreen;
import org.example.maniacrevolution.item.MedicTabletItem;
import org.example.maniacrevolution.util.MedicTabletTracker;
import org.example.maniacrevolution.util.SelectiveGlowingEffect;

/**
 * Обработчик событий для системы медика
 */
@Mod.EventBusSubscriber
public class MedicEventHandler {

    /**
     * Открытие GUI планшета при правом клике (только на клиенте)
     */
    @Mod.EventBusSubscriber(value = Dist.CLIENT)
    public static class ClientEvents {

        @SubscribeEvent
        public static void onRightClick(InputEvent.InteractionKeyMappingTriggered event) {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;

            if (player == null || event.getHand() != InteractionHand.MAIN_HAND) {
                return;
            }

            // Проверяем, держит ли игрок планшет
            if (player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof MedicTabletItem) {
                // Открываем GUI планшета
                mc.setScreen(new MedicTabletScreen());
                event.setCanceled(true);
            }
        }
    }

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