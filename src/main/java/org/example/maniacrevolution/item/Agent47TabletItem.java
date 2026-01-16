package org.example.maniacrevolution.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Планшет Агента 47
 * Позволяет просматривать текущую цель и покупать товары в магазине
 */
public class Agent47TabletItem extends Item {

    public Agent47TabletItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            // На клиенте открываем GUI планшета
            return InteractionResultHolder.success(player.getItemInHand(hand));
        }

        // На сервере просто возвращаем успех
        // GUI откроется на клиенте через событие
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    /**
     * Проверяет, является ли игрок агентом
     */
    public static boolean isAgent(ServerPlayer player) {
        var scoreboard = player.getScoreboard();
        if (scoreboard == null) return false;

        try {
            var objective = scoreboard.getObjective("ManiacClass");
            if (objective == null) return false;

            var scoreAccess = scoreboard.getOrCreatePlayerScore(player.getScoreboardName(), objective);

            return scoreAccess.getScore() == 4; // Агент 47 имеет класс 4
        } catch (Exception e) {
            return false;
        }
    }
}