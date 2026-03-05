package org.example.maniacrevolution.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Душа предков (предмет шамана).
 *
 * ПКМ — восстанавливает HP по формуле:
 *   HP = BASE_HEAL + BONUS_PER_SPECTATOR × count
 *   count = игроки в team "survivors" И в режиме spectator (по всему серверу)
 *
 * Регистрация в ModItems:
 *   ITEMS.register("ancestor_soul", () -> new AncestorSoulItem(
 *       new Item.Properties().stacksTo(16)));
 */
public class AncestorSoulItem extends Item {

    public static final float  BASE_HEAL           = 2.0f;
    public static final float  BONUS_PER_SPECTATOR = 2.0f;
    private static final String SURVIVORS_TEAM     = "survivors";

    public AncestorSoulItem(Properties props) { super(props); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.success(stack);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.fail(stack);

        int spectatorCount = countSpectatorSurvivors(sp);
        float heal = BASE_HEAL + BONUS_PER_SPECTATOR * spectatorCount;

        player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + heal));

        sp.displayClientMessage(Component.literal(String.format(
                "§d✦ Душа предков: §f+%.0f HP §7(выживших-зрителей: §f%d§7)",
                heal, spectatorCount)), true);

        if (!sp.isCreative()) stack.shrink(1);
        return InteractionResultHolder.consume(stack);
    }

    private static int countSpectatorSurvivors(ServerPlayer self) {
        int count = 0;
        for (ServerPlayer p : self.getServer().getPlayerList().getPlayers()) {
            if (p == self) continue;
            if (p.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) continue;
            var team = p.getTeam();
            if (team != null && SURVIVORS_TEAM.equalsIgnoreCase(team.getName())) count++;
        }
        return count;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("§dДуша предков").withStyle(ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Базовое лечение: §f" + (int) BASE_HEAL + " HP")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(
                        "  §f+" + (int) BONUS_PER_SPECTATOR + " HP §7за каждого выжившего в spectator")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(
                        "  §8Формула: " + (int) BASE_HEAL + " + " + (int) BONUS_PER_SPECTATOR + " × кол-во")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}