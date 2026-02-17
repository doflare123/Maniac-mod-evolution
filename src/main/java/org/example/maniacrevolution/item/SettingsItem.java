package org.example.maniacrevolution.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.OpenSettingsMenuPacket;
import org.jetbrains.annotations.NotNull;

public class SettingsItem extends Item {
    public SettingsItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            // Открываем меню настроек
            ModNetworking.sendToPlayer(new OpenSettingsMenuPacket(), serverPlayer);
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return true; // Эффект enchanted для красоты
    }
}