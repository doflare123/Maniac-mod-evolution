package org.example.maniacrevolution.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.forgetmenot.ForgetMeNotManager;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/** Личный предмет незабудки, который устанавливает безопасную точку телепортации. */
public final class ForgetMeNotItem extends Item {
    private static final String OWNER_TAG = "ForgetMeNotOwner";
    private static final int HIDE_CAN_PLACE_TOOLTIP_FLAG = 16;

    public ForgetMeNotItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    public static ItemStack createFor(ServerPlayer owner) {
        ItemStack stack = new ItemStack(ModItems.FORGET_ME_NOT.get());
        CompoundTag tag = stack.getOrCreateTag();
        tag.putUUID(OWNER_TAG, owner.getUUID());

        // Adventure mode checks CanPlaceOn before Item#useOn; placement safety is
        // still enforced by ForgetMeNotManager after this broad permission passes.
        ListTag canPlaceOn = new ListTag();
        BuiltInRegistries.BLOCK.keySet().stream()
                .sorted(java.util.Comparator.comparing(Object::toString))
                .forEach(id -> canPlaceOn.add(StringTag.valueOf(id.toString())));
        tag.put("CanPlaceOn", canPlaceOn);
        tag.putInt("HideFlags", tag.getInt("HideFlags") | HIDE_CAN_PLACE_TOOLTIP_FLAG);
        return stack;
    }

    public static boolean belongsTo(ItemStack stack, UUID ownerId) {
        CompoundTag tag = stack.getTag();
        return stack.is(ModItems.FORGET_ME_NOT.get())
                && tag != null && tag.hasUUID(OWNER_TAG)
                && ownerId.equals(tag.getUUID(OWNER_TAG));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (!(player instanceof ServerPlayer owner)) {
            return player == null ? InteractionResult.PASS : InteractionResult.SUCCESS;
        }

        ItemStack stack = context.getItemInHand();
        if (!belongsTo(stack, owner.getUUID())
                || !ForgetMeNotManager.canPlace(owner, context.getClickedPos())) {
            owner.displayClientMessage(
                    Component.translatable("message.maniacrev.forget_me_not.cannot_place"), true);
            return InteractionResult.FAIL;
        }

        if (!ForgetMeNotManager.place(owner, context.getClickedPos())) {
            return InteractionResult.FAIL;
        }
        stack.shrink(1);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.maniacrev.forget_me_not.place")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.maniacrev.forget_me_not.bound")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
