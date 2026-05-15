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

    public GhostHandItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            player.displayClientMessage(Component.literal("§7Удерживайте Shift и нажмите ПКМ по выжившему."), true);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("§dПризрачная рука").withStyle(ChatFormatting.BOLD));
        tooltip.add(Component.literal("  §7Shift + ПКМ по выжившему: вселение на 30 сек."));
        tooltip.add(Component.literal("  §7Во время контроля Shift + ПКМ: досрочно выйти."));
        tooltip.add(Component.literal("  §7Во время вселения цель получает §fSpeed I§7."));
        tooltip.add(Component.literal("  §7Кулдаун: §f" + getMaxCooldownSeconds() + " сек."));
    }

    @Override
    public ResourceLocation getAbilityIcon() {
        return new ResourceLocation(Maniacrev.MODID, "textures/item/hand.png");
    }

    @Override
    public String getAbilityName() {
        return "Вселение";
    }

    @Override
    public String getAbilityDescription() {
        return "Shift + ПКМ по выжившему захватывает его тело.";
    }

    @Override
    public float getManaCost() {
        return 0;
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
