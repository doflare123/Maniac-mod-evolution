package org.example.maniacrevolution.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.example.maniacrevolution.util.ClientOnlyExecutor;

public class RecipeBookItem extends Item {

    public RecipeBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            ClientOnlyExecutor.openRecipeBookScreen();
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
}
