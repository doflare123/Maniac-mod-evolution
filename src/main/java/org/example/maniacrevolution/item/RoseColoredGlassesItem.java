package org.example.maniacrevolution.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.client.renderer.RoseColoredGlassesItemRenderer;
import org.example.maniacrevolution.perk.perks.survivor.RoseColoredGlassesPerk;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public final class RoseColoredGlassesItem extends Item {
    private static final String OWNER_TAG = "RoseGlassesOwner";

    public RoseColoredGlassesItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .durability(RoseColoredGlassesPerk.MAX_DURABILITY)
                .setNoRepair()
                .rarity(Rarity.EPIC));
    }

    public static ItemStack createFor(ServerPlayer owner) {
        ItemStack stack = new ItemStack(ModItems.ROSE_COLORED_GLASSES.get());
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
        return tag != null && tag.hasUUID(OWNER_TAG) ? tag.getUUID(OWNER_TAG) : null;
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
        int remaining = Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
        tooltip.add(Component.translatable("item.maniacrev.rose_colored_glasses.durability",
                        remaining, RoseColoredGlassesPerk.MAX_DURABILITY)
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("item.maniacrev.rose_colored_glasses.offhand")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.maniacrev.rose_colored_glasses.bound")
                .withStyle(ChatFormatting.DARK_PURPLE));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private final BlockEntityWithoutLevelRenderer renderer =
                    new RoseColoredGlassesItemRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        });
    }
}
