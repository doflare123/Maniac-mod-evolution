package org.example.maniacrevolution.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.example.maniacrevolution.character.CharacterType;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.OpenCharacterMenuPacket;

/**
 * Предмет для открытия меню выбора персонажа
 */
public class CharacterSelectionItem extends Item {
    private final CharacterType type;

    public CharacterSelectionItem(Properties properties, CharacterType type) {
        super(properties);
        this.type = type;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            // На клиенте открываем меню
            net.minecraft.client.Minecraft.getInstance().setScreen(
                    new org.example.maniacrevolution.client.screen.CharacterSelectionScreen(type)
            );
        }

        return InteractionResultHolder.success(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (type == CharacterType.SURVIVOR) {
            return Component.literal("§aВыбор выжившего");
        } else {
            return Component.literal("§cВыбор маньяка");
        }
    }
}