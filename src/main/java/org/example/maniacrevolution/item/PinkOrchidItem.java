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
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.pinkorchid.PinkOrchidManager;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/** Личная многоразовая орхидея, запускающая и останавливающая запись маршрута. */
public final class PinkOrchidItem extends Item {
    private static final String OWNER_TAG = "PinkOrchidOwner";

    public PinkOrchidItem() {
        super(new Item.Properties().stacksTo(1).setNoRepair().rarity(Rarity.EPIC));
    }

    public static ItemStack createFor(ServerPlayer owner) {
        ItemStack stack = new ItemStack(ModItems.PINK_ORCHID.get());
        stack.getOrCreateTag().putUUID(OWNER_TAG, owner.getUUID());
        return stack;
    }

    public static boolean belongsTo(ItemStack stack, UUID playerId) {
        UUID owner = getOwner(stack);
        return owner != null && owner.equals(playerId);
    }

    @Nullable
    public static UUID getOwner(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return stack.is(ModItems.PINK_ORCHID.get()) && tag != null && tag.hasUUID(OWNER_TAG)
                ? tag.getUUID(OWNER_TAG) : null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player,
                                                   InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(stack);
        }
        if (!belongsTo(stack, serverPlayer.getUUID())
                || !PinkOrchidManager.toggleRecording(serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public net.minecraft.world.InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return player == null ? net.minecraft.world.InteractionResult.PASS
                    : net.minecraft.world.InteractionResult.SUCCESS;
        }
        ItemStack stack = context.getItemInHand();
        return belongsTo(stack, serverPlayer.getUUID())
                && PinkOrchidManager.toggleRecording(serverPlayer)
                ? net.minecraft.world.InteractionResult.CONSUME
                : net.minecraft.world.InteractionResult.FAIL;
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        return false;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.maniacrev.pink_orchid.record")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("item.maniacrev.pink_orchid.bound")
                .withStyle(ChatFormatting.DARK_PURPLE));
    }
}
