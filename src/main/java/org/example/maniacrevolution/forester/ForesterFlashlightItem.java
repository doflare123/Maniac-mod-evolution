package org.example.maniacrevolution.forester;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import org.example.maniacrevolution.block.ModBlocks;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.helsing.HelsingManager;
import java.util.List;

public final class ForesterFlashlightItem extends Item {
    public ForesterFlashlightItem() { super(new Properties().stacksTo(1)); }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override public boolean applyForgeHandTransform(com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.player.LocalPlayer player,
                    net.minecraft.world.entity.HumanoidArm arm, ItemStack stack, float partial, float equip, float swing) {
                // The model's lens faces -Z. Keep that direction both idle and during UseAnim.NONE.
                pose.translate(arm == net.minecraft.world.entity.HumanoidArm.RIGHT ? .45 : -.45, -.35 - equip * .4, -.7);
                return true;
            }
        });
    }
    /** Supply code may set ForesterUses; no automatic loadout is issued by this class. */
    public static int uses(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains("ForesterUses") ? Math.max(0, stack.getTag().getInt("ForesterUses")) : ForesterRules.DEFAULT_FLASH_USES;
    }
    @Override public int getUseDuration(ItemStack stack) { return ForesterRules.FLASH_DURATION; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.NONE; }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!ForesterManager.canUse(player) || uses(stack) == 0) return InteractionResultHolder.fail(stack);
        if (!level.isClientSide) stack.getOrCreateTag().putInt("ForesterUses", uses(stack) - 1);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }
    @Override public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remaining) {
        if (!(living instanceof ServerPlayer player)) return;
        if (!ForesterManager.canUse(player)) { player.stopUsingItem(); return; }
        Vec3 start = player.getEyePosition(), direction = player.getLookAngle();
        var wall = level.clip(new net.minecraft.world.level.ClipContext(start, start.add(direction.scale(ForesterRules.FLASH_RANGE)),
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, player));
        Vec3 end = wall.getLocation();
        double reach = start.distanceTo(end);
        var light = (ForesterLightBlock) ModBlocks.FORESTER_LIGHT.get();
        for (double distance = 0; distance < reach; distance += 2) light.illuminate(player.serverLevel(), net.minecraft.core.BlockPos.containing(start.add(direction.scale(distance))));
        for (ServerPlayer target : player.serverLevel().players()) {
            if (!HelsingManager.isManiac(target) || target.distanceToSqr(player) > ForesterRules.FLASH_RANGE * ForesterRules.FLASH_RANGE) continue;
            if (target.getBoundingBox().inflate(.15).clip(start, end).isPresent() && player.hasLineOfSight(target))
                target.addEffect(new MobEffectInstance(ModEffects.FORESTER_BLIND.get(), ForesterRules.FLASH_BLIND, 0, false, false, true));
        }
    }
    @Override public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
        if (entity instanceof Player player) player.getCooldowns().addCooldown(this, 8);
    }
    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        releaseUsing(stack, level, entity, 0); return stack;
    }
    @Override public boolean isBarVisible(ItemStack stack) { return uses(stack) < ForesterRules.DEFAULT_FLASH_USES; }
    @Override public int getBarWidth(ItemStack stack) { return Math.min(13, Math.round(13F * uses(stack) / ForesterRules.DEFAULT_FLASH_USES)); }
    @Override public int getBarColor(ItemStack stack) { return 0xF5D575; }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.maniacrev.forester_flashlight", uses(stack)));
    }
}
