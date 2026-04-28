package org.example.maniacrevolution.item;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.example.maniacrevolution.gui.PerkSelectionScreen;

import javax.annotation.Nullable;
import java.util.List;

public class PerkOpenItem extends Item {
    public PerkOpenItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            openPerkScreen();
            return InteractionResultHolder.success(stack);
        }

        return InteractionResultHolder.success(stack);
    }

    @OnlyIn(Dist.CLIENT)
    private void openPerkScreen() {
        Minecraft.getInstance().setScreen(new PerkSelectionScreen());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7ПКМ чтобы выбор перков"));
        tooltip.add(Component.literal("§8Работает только на стадии планирования игры"));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("§6✦ Выбор перков ✦");
    }
}
