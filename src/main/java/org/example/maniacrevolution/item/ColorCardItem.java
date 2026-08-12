package org.example.maniacrevolution.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.example.maniacrevolution.colorroulette.ColorCard;
import org.example.maniacrevolution.colorroulette.ColorRouletteManager;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/** Personal, expiring result card produced by Color Roulette. */
public final class ColorCardItem extends Item {
    private static final String OWNER_TAG = "ColorCardOwner";
    private static final String REMAINING_TAG = "ColorCardRemaining";
    private final ColorCard card;

    public ColorCardItem(ColorCard card) {
        super(new Item.Properties().stacksTo(1).setNoRepair().rarity(Rarity.EPIC));
        this.card = card;
    }

    public ColorCard card() {
        return card;
    }

    public static ItemStack createFor(ServerPlayer owner, ColorCard card) {
        ItemStack stack = new ItemStack(card.item());
        CompoundTag tag = stack.getOrCreateTag();
        tag.putUUID(OWNER_TAG, owner.getUUID());
        tag.putInt(REMAINING_TAG, ColorRouletteManager.CARD_LIFETIME_TICKS);
        return stack;
    }

    @Nullable
    public static UUID getOwner(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID(OWNER_TAG) ? tag.getUUID(OWNER_TAG) : null;
    }

    public static boolean belongsTo(ItemStack stack, UUID playerId) {
        UUID owner = getOwner(stack);
        return owner != null && owner.equals(playerId);
    }

    public static int getRemainingTicks(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : Math.max(0, tag.getInt(REMAINING_TAG));
    }

    public static void setRemainingTicks(ItemStack stack, int ticks) {
        stack.getOrCreateTag().putInt(REMAINING_TAG, Math.max(0, ticks));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(stack);
        }
        return ColorRouletteManager.useAirCard(serverPlayer, card, stack)
                ? InteractionResultHolder.consume(stack)
                : InteractionResultHolder.fail(stack);
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        return false;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack,
                                               boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getRemainingTicks(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getRemainingTicks(stack)
                / ColorRouletteManager.CARD_LIFETIME_TICKS);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return card.color();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        float seconds = getRemainingTicks(stack) / 20.0F;
        tooltip.add(Component.translatable("item.maniacrev.color_card.time", String.format("%.1f", seconds))
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("item.maniacrev.color_card.bound")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.maniacrev.color_card." + card.name().toLowerCase() + ".use")
                .withStyle(ChatFormatting.GRAY));
    }
}
