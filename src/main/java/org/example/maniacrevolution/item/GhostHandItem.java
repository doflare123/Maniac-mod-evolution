package org.example.maniacrevolution.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.client.ClientAbilityData;
import org.example.maniacrevolution.ghost.GhostPossessionManager;

import javax.annotation.Nullable;
import java.util.List;

public class GhostHandItem extends Item implements IItemWithAbility {
    public static final float MANA_COST = 10.0f;

    public GhostHandItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable("message.maniacrev.ghost_hand.hint"), true);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.maniacrev.ghost_hand.title").withStyle(ChatFormatting.BOLD));
        tooltip.add(Component.translatable("tooltip.maniacrev.ghost_hand.possess",
                GhostPossessionManager.POSSESSION_DURATION_TICKS / 20));
        tooltip.add(Component.translatable("tooltip.maniacrev.ghost_hand.exit"));
        tooltip.add(Component.translatable("tooltip.maniacrev.ghost_hand.speed"));
        tooltip.add(Component.translatable("tooltip.maniacrev.ghost_hand.cooldown", getMaxCooldownSeconds()));
    }

    @Override
    public ResourceLocation getAbilityIcon() {
        return new ResourceLocation(Maniacrev.MODID, "textures/item/ghost_hand.png");
    }

    @Override
    public String getAbilityName() {
        return Component.translatable("ability.maniacrev.ghost_hand.name").getString();
    }

    @Override
    public String getAbilityDescription() {
        return Component.translatable("ability.maniacrev.ghost_hand.desc").getString();
    }

    @Override
    public float getManaCost() {
        return MANA_COST;
    }

    @Override
    public int getCooldownSeconds(Player player) {
        if (player.level().isClientSide) {
            return ClientAbilityData.getCooldownSeconds(this);
        }
        return GhostPossessionManager.getCooldownSeconds(player);
    }

    @Override
    public int getMaxCooldownSeconds() {
        return GhostPossessionManager.POSSESSION_COOLDOWN_TICKS / 20;
    }
}
