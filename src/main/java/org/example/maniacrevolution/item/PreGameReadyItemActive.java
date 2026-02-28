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
 * Активное состояние (зелёный / с зачарованием).
 * ПКМ → снимает готовность, меняется обратно на PreGameReadyItem.
 */
public class PreGameReadyItemActive extends Item {

    public PreGameReadyItemActive(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide) {
            net.minecraft.server.level.ServerPlayer serverPlayer =
                    (net.minecraft.server.level.ServerPlayer) player;

            PreGameReadyManager.setPlayerReady(serverPlayer, false);
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("§aГотов!");
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        // Визуальный эффект зачарования — игрок видит что он готов
        return true;
    }
}
