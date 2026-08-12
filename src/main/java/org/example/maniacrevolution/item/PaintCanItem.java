package org.example.maniacrevolution.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.core.Direction;
import org.example.maniacrevolution.game.GameManager;
import org.example.maniacrevolution.paint.PaintPuddleManager;
import org.example.maniacrevolution.perk.perks.maniac.ThePaintThickensPerk;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class PaintCanItem extends Item {
    private static final String OWNER_TAG = "PaintOwner";

    public PaintCanItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .durability(ThePaintThickensPerk.PAINT_CAN_USES)
                .setNoRepair()
                .rarity(Rarity.EPIC));
    }

    public static ItemStack createFor(ServerPlayer owner) {
        ItemStack stack = new ItemStack(org.example.maniacrevolution.ModItems.PAINT_CAN.get());
        stack.getOrCreateTag().putUUID(OWNER_TAG, owner.getUUID());
        return stack;
    }

    public static boolean belongsTo(ItemStack stack, UUID playerId) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID(OWNER_TAG) && playerId.equals(tag.getUUID(OWNER_TAG));
    }

    @Nullable
    public static UUID getOwner(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID(OWNER_TAG) ? tag.getUUID(OWNER_TAG) : null;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return player == null ? InteractionResult.PASS : InteractionResult.SUCCESS;
        }

        ItemStack stack = context.getItemInHand();
        if (context.getClickedFace() != Direction.UP
                || GameManager.getPhaseValue() < 1
                || GameManager.getPhaseValue() > 3
                || !belongsTo(stack, serverPlayer.getUUID())
                || !PaintPuddleManager.canOwnerPlace(serverPlayer)) {
            return InteractionResult.FAIL;
        }

        if (!PaintPuddleManager.placePuddle(serverPlayer, context.getClickLocation())) {
            return InteractionResult.FAIL;
        }

        stack.hurtAndBreak(1, serverPlayer,
                brokenPlayer -> brokenPlayer.broadcastBreakEvent(context.getHand()));
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        int remaining = Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
        tooltip.add(Component.translatable("item.maniacrev.paint_can.uses", remaining)
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("item.maniacrev.paint_can.bound")
                .withStyle(ChatFormatting.GRAY));
    }
}
