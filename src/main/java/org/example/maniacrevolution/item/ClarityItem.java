package org.example.maniacrevolution.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.example.maniacrevolution.effect.ModEffects;

import java.util.List;

public class ClarityItem extends Item {

    private static final int EFFECT_DURATION = 20 * 30; // 30 секунд
    private static final int EFFECT_AMPLIFIER = 1; // Уровень 1 (индекс 0)

    public ClarityItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        // Можно пить всегда (игнорируем сытость)
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(itemStack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player) {
            if (!level.isClientSide()) {
                // Даём эффект "Усиление манопотока"
                player.addEffect(new MobEffectInstance(
                        ModEffects.MANA_FLOW.get(),
                        EFFECT_DURATION,
                        EFFECT_AMPLIFIER,
                        false, // ambient
                        true,  // visible
                        true   // showIcon
                ));

                player.displayClientMessage(
                        Component.literal("§bMana flow enhanced!"),
                        true
                );
            }

            // Уменьшаем стак
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32; // Время использования (как у зелья)
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7Drink to enhance mana flow"));
        tooltip.add(Component.literal("§9+2.0 mana/sec for 30 seconds"));
        tooltip.add(Component.literal("§9Works even with passive regen disabled"));
    }
}