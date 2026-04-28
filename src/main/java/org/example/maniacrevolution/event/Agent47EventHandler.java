package org.example.maniacrevolution.event;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.gui.Agent47TabletScreen;
import org.example.maniacrevolution.command.Agent47MoneyCommand;
import org.example.maniacrevolution.item.Agent47TabletItem;
import org.example.maniacrevolution.system.Agent47MoneyManager;
import org.example.maniacrevolution.system.Agent47TargetManager;

/**
 * Обработчик событий для системы Агента 47
 */
@Mod.EventBusSubscriber
public class Agent47EventHandler {

    /**
     * Регистрация команд
     */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        Agent47MoneyCommand.register(event.getDispatcher());
    }

    /**
     * Загрузка денег при входе игрока
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            Agent47MoneyManager.onPlayerLogin(player);
        }
    }

    /**
     * Сохранение данных при выходе игрока
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            Agent47MoneyManager.onPlayerLogout(player.getUUID());
        }
    }

    /**
     * Клиентские события
     */
    @Mod.EventBusSubscriber(value = Dist.CLIENT)
    public static class ClientEvents {

        @SubscribeEvent
        public static void onRightClick(InputEvent.InteractionKeyMappingTriggered event) {
            Minecraft mc = Minecraft.getInstance();
            var player = mc.player;

            if (player == null || event.getHand() != InteractionHand.MAIN_HAND) {
                return;
            }

            // Проверяем, держит ли игрок планшет агента
            if (player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof Agent47TabletItem) {
                // Открываем GUI планшета
                mc.setScreen(new Agent47TabletScreen());
                event.setCanceled(true);
            }
        }
    }
}