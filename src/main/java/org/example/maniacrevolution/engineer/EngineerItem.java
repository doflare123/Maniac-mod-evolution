package org.example.maniacrevolution.engineer;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.chat.Component;
import java.util.List;

public final class EngineerItem extends Item {
    public enum Kind { TABLET, REMOTE, BARRICADE, PANEL, SCRAP }
    public final Kind kind;
    public EngineerItem(Kind kind) { super(new Properties().stacksTo(kind == Kind.SCRAP ? 64 : kind == Kind.BARRICADE || kind == Kind.PANEL ? 16 : 1)); this.kind = kind; }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (kind == Kind.SCRAP) {
            if (player.isShiftKeyDown()) {
                if (player instanceof ServerPlayer p) EngineerManager.depositScrap(p, stack);
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
            if (player instanceof ServerPlayer p && !EngineerManager.canEatScrap(p)) return InteractionResultHolder.fail(stack);
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        if (level.isClientSide) org.example.maniacrevolution.engineer.client.EngineerClient.useHeld(kind, hand);
        else if (player instanceof ServerPlayer p) {
            EngineerActionPacket.Action action = kind == Kind.TABLET ? EngineerActionPacket.Action.OPEN_WORKSHOP
                    : kind == Kind.REMOTE ? EngineerActionPacket.Action.OPEN_REMOTE : EngineerActionPacket.Action.USE_HELD;
            var pos = EngineerManager.placement(p);
            EngineerManager.action(p, new EngineerActionPacket(action, pos == null ? net.minecraft.core.BlockPos.ZERO : pos, hand.ordinal()));
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        return context.getPlayer() == null ? InteractionResult.PASS : use(context.getLevel(), context.getPlayer(), context.getHand()).getResult();
    }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return kind == Kind.SCRAP ? UseAnim.EAT : UseAnim.NONE; }
    @Override public int getUseDuration(ItemStack stack) { return EngineerRules.PLATE_TICKS; }
    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (kind == Kind.SCRAP && user instanceof ServerPlayer p && EngineerManager.eatScrap(p) && !p.isCreative()) stack.shrink(1);
        return stack;
    }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.maniacrev.engineer_" + kind.name().toLowerCase(java.util.Locale.ROOT) + ".hint"));
    }
}
