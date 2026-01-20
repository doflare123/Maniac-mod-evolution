package org.example.maniacrevolution.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.OpenVotingMenuPacket;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class VotingTicketItem extends Item {

    public VotingTicketItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            // На клиенте отправляем запрос на открытие меню
            ModNetworking.sendToServer(new OpenVotingMenuPacket());
        }

        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7ПКМ чтобы открыть голосование"));
        tooltip.add(Component.literal("§eИспользуется для голосования за карту"));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // Делаем предмет с эффектом зачарования
    }
}