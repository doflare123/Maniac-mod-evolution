package org.example.maniacrevolution.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.readiness.ReadinessManager;

/**
 * Предмет "Готово" в активном состоянии (зелёная кнопка)
 */
public class ReadyItemActive extends Item {

    public ReadyItemActive(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            ReadinessManager.setPlayerReady((net.minecraft.server.level.ServerPlayer) player, false);

            // Меняем обратно на красную кнопку
            ItemStack newStack = new ItemStack(ModItems.READY_ITEM.get());
            player.setItemInHand(hand, newStack);
        }

        return InteractionResultHolder.success(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("§aГотов!");
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // Всегда светится
    }
}
