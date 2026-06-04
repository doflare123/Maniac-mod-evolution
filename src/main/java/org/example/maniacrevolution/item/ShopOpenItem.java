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
import org.example.maniacrevolution.data.ClientGameState;
import org.example.maniacrevolution.game.GameManager;
import org.example.maniacrevolution.gui.ShopScreen;

import javax.annotation.Nullable;
import java.util.List;

public class ShopOpenItem extends Item {

    public ShopOpenItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            // Проверяем фазу на клиенте
            if (ClientGameState.getPhase() != 0) {
                player.displayClientMessage(
                        Component.literal("§cМагазин доступен только вне игры!"), true);
                return InteractionResultHolder.fail(stack);
            }
            openShopScreen();
            return InteractionResultHolder.success(stack);
        }

        // Проверка на сервере
        if (GameManager.getPhaseValue() != 0) {
            player.displayClientMessage(
                    Component.literal("§cМагазин доступен только вне игры!"), true);
            return InteractionResultHolder.fail(stack);
        }

        return InteractionResultHolder.success(stack);
    }

    @OnlyIn(Dist.CLIENT)
    private void openShopScreen() {
        Minecraft.getInstance().setScreen(new ShopScreen());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7ПКМ чтобы открыть магазин"));
        tooltip.add(Component.literal("§8Работает только вне игры"));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("§6✦ Токен Магазина ✦");
    }
}
