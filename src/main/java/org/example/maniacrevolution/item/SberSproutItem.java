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
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.hack.ComputerBlockEntity;
import org.example.maniacrevolution.sbersprout.SberSproutManager;
import org.example.maniacrevolution.perk.perks.survivor.SberSproutPerk;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Личный заполненный росток, хранящий округлённый процент прогресса. */
public final class SberSproutItem extends Item {
    private static final String OWNER_TAG = "SberSproutOwner";
    private static final String STORED_PERCENT_TAG = "SberSproutStoredPercent";

    public SberSproutItem() {
        super(new Item.Properties().stacksTo(1).setNoRepair().rarity(Rarity.EPIC));
    }

    public static ItemStack createFor(ServerPlayer owner, float storedPercent) {
        ItemStack stack = new ItemStack(ModItems.SBER_SPROUT.get());
        CompoundTag tag = stack.getOrCreateTag();
        tag.putUUID(OWNER_TAG, owner.getUUID());
        tag.putFloat(STORED_PERCENT_TAG, storedPercent);
        return stack;
    }

    public static boolean belongsTo(ItemStack stack, UUID playerId) {
        UUID owner = getOwner(stack);
        return owner != null && owner.equals(playerId);
    }

    @Nullable
    public static UUID getOwner(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return stack.is(ModItems.SBER_SPROUT.get()) && tag != null && tag.hasUUID(OWNER_TAG)
                ? tag.getUUID(OWNER_TAG) : null;
    }

    public static float getStoredPercent(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0.0F : Math.max(0.0F, tag.getFloat(STORED_PERCENT_TAG));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (!(player instanceof ServerPlayer serverPlayer)
                || !(context.getLevel().getBlockEntity(context.getClickedPos())
                instanceof ComputerBlockEntity computer)) {
            return player == null ? InteractionResult.PASS : InteractionResult.FAIL;
        }
        return SberSproutManager.tryPlant(serverPlayer, context.getClickedPos(),
                computer, context.getItemInHand())
                ? InteractionResult.CONSUME : InteractionResult.FAIL;
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
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        String percent = String.format(Locale.ROOT,
                "%." + SberSproutPerk.DISPLAY_DECIMAL_PLACES + "f",
                getStoredPercent(stack));
        tooltip.add(Component.translatable("item.maniacrev.sber_sprout.progress", percent)
                .withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable("item.maniacrev.sber_sprout.plant")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.maniacrev.sber_sprout.bound")
                .withStyle(ChatFormatting.DARK_GREEN));
    }
}
