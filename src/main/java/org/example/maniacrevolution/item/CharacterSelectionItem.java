package org.example.maniacrevolution.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.example.maniacrevolution.character.CharacterType;
import org.example.maniacrevolution.util.ClientOnlyExecutor;

public class CharacterSelectionItem extends Item {
    private final CharacterType type;

    public CharacterSelectionItem(Properties properties, CharacterType type) {
        super(properties);
        this.type = type;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            ClientOnlyExecutor.openCharacterSelectionScreen(this.type);
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    @Override
    public Component getName(ItemStack stack) {
        return type == CharacterType.SURVIVOR
                ? Component.literal("§aВыбор выжившего")
                : Component.literal("§cВыбор маньяка");
    }
}
