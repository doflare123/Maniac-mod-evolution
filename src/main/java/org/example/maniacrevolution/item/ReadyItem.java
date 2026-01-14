package org.example.maniacrevolution.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.ReadyStatusPacket;
import org.example.maniacrevolution.readiness.ReadinessManager;

/**
 * Предмет "Готово" - ПКМ для переключения готовности
 */
public class ReadyItem extends Item {

    public ReadyItem(Properties properties) {
        super(properties.stacksTo(1)); // Только 1 в стаке
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            boolean currentReady = ReadinessManager.isPlayerReady(player);
            boolean newReady = !currentReady;

            ReadinessManager.setPlayerReady((net.minecraft.server.level.ServerPlayer) player, newReady);

            // Подсчёт готовых игроков
            int totalPlayers = level.getServer().getPlayerList().getPlayerCount();
            int readyPlayers = 0;
            for (net.minecraft.server.level.ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
                if (ReadinessManager.isPlayerReady(p)) {
                    readyPlayers++;
                }
            }

            // Сообщение всем игрокам
            String statusColor = newReady ? "§a" : "§c";
            String statusText = newReady ? "готов" : "отменил готовность";
            Component message = Component.literal(statusColor + player.getName().getString() + " " + statusText +
                    " §7(" + readyPlayers + "/" + totalPlayers + ")");

            for (net.minecraft.server.level.ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
                p.sendSystemMessage(message);
            }
        }

        return InteractionResultHolder.success(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("§6Готово");
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        // Свечение если игрок готов (проверяется на клиенте)
        if (net.minecraft.client.Minecraft.getInstance().player != null) {
            return ReadinessManager.isPlayerReady(net.minecraft.client.Minecraft.getInstance().player);
        }
        return false;
    }
}