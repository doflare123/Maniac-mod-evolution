package org.example.maniacrevolution.forester;

import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public final class PepperCanisterItem extends Item {
    public PepperCanisterItem() { super(new Properties().stacksTo(16)); }
    @Override public int getUseDuration(ItemStack stack) { return 72000; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.NONE; }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!ForesterManager.canUse(player)) return InteractionResultHolder.fail(stack);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }
    @Override public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
        if (level.isClientSide || !(entity instanceof Player player) || !ForesterManager.canUse(player)) return;
        var canister = new PepperCanisterEntity(level, player);
        level.addFreshEntity(canister);
        stack.shrink(1);
        player.getCooldowns().addCooldown(this, 10);
        level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.SNOWBALL_THROW, player.getSoundSource(), .6F, .75F);
    }
}
