package org.example.maniacrevolution.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.example.maniacrevolution.pregame.PreGameReadyManager;

/**
 * Предмет "Готово" для прелобби.
 * Неактивное состояние (красный / обычный вид).
 * ПКМ → помечает игрока как готового, меняется на PreGameReadyItemActive.
 */
public class PreGameReadyItem extends Item {

    public PreGameReadyItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide) {
            net.minecraft.server.level.ServerPlayer serverPlayer =
                    (net.minecraft.server.level.ServerPlayer) player;

            PreGameReadyManager.setPlayerReady(serverPlayer, true);
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("§cГотово");
    }
}
