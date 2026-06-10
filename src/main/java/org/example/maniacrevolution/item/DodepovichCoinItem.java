package org.example.maniacrevolution.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.example.maniacrevolution.dodepovich.DodepovichCasinoManager;
import org.example.maniacrevolution.dodepovich.DodepovichCoin;
import org.example.maniacrevolution.util.ManaUtil;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DodepovichCoinItem extends Item {
    public static final float MANA_COST = 5.0f;
    private final DodepovichCoin coin;

    public DodepovichCoinItem(Properties properties, DodepovichCoin coin) {
        super(properties.stacksTo(16));
        this.coin = coin;
    }

    public DodepovichCoin getCoin() {
        return coin;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }

        if (!DodepovichCasinoManager.isDodepovich(serverPlayer)) {
            player.displayClientMessage(Component.literal("§cТолько Додепович умеет правильно подбрасывать эти монетки."), true);
            return InteractionResultHolder.fail(stack);
        }

        if (!ManaUtil.consumeMana(serverPlayer, MANA_COST)) {
            player.displayClientMessage(Component.literal("§bНедостаточно маны. Подброс монетки стоит 5 маны."), true);
            return InteractionResultHolder.fail(stack);
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        DodepovichCasinoManager.flipCoin(serverPlayer, coin);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("§6" + coin.getDisplayName());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.maniacrev.dodepovich_coin.roll").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.maniacrev.dodepovich_coin.good").withStyle(ChatFormatting.GREEN));
        tooltip.add(goodTooltip().withStyle(ChatFormatting.DARK_GREEN));
        tooltip.add(Component.translatable("tooltip.maniacrev.dodepovich_coin.bad").withStyle(ChatFormatting.RED));
        tooltip.add(badTooltip().withStyle(ChatFormatting.DARK_RED));
    }

    private MutableComponent goodTooltip() {
        return switch (coin) {
            case ELUSIVENESS -> Component.translatable("tooltip.maniacrev.coin_elusiveness.good",
                    DodepovichCasinoManager.ELUSIVENESS_GOOD_SECONDS);
            case INSIGHT -> Component.translatable("tooltip.maniacrev.coin_insight.good",
                    DodepovichCasinoManager.INSIGHT_GOOD_SECONDS);
            case SHACKLES -> Component.translatable("tooltip.maniacrev.coin_shackles.good",
                    DodepovichCasinoManager.SHACKLES_SECONDS);
            case HEALTH -> Component.translatable("tooltip.maniacrev.coin_health.good",
                    formatHp(DodepovichCasinoManager.HEALTH_AMOUNT));
            case EAGLE -> Component.translatable("tooltip.maniacrev.coin_eagle.good",
                    DodepovichCasinoManager.EAGLE_BLINDNESS_SECONDS);
            case DEBT -> Component.translatable("tooltip.maniacrev.coin_debt.good",
                    DodepovichCasinoManager.DEBT_EFFECT_SECONDS);
            case REROLL -> Component.translatable("tooltip.maniacrev.coin_reroll.good");
            case FATE -> Component.translatable("tooltip.maniacrev.coin_fate.good");
        };
    }

    private MutableComponent badTooltip() {
        return switch (coin) {
            case ELUSIVENESS -> Component.translatable("tooltip.maniacrev.coin_elusiveness.bad",
                    DodepovichCasinoManager.ELUSIVENESS_BAD_SLOW_SECONDS);
            case INSIGHT -> Component.translatable("tooltip.maniacrev.coin_insight.bad",
                    DodepovichCasinoManager.INSIGHT_BAD_SECONDS);
            case SHACKLES -> Component.translatable("tooltip.maniacrev.coin_shackles.bad",
                    DodepovichCasinoManager.SHACKLES_SECONDS);
            case HEALTH -> Component.translatable("tooltip.maniacrev.coin_health.bad",
                    formatHp(DodepovichCasinoManager.HEALTH_AMOUNT));
            case EAGLE -> Component.translatable("tooltip.maniacrev.coin_eagle.bad",
                    DodepovichCasinoManager.EAGLE_BLINDNESS_SECONDS);
            case DEBT -> Component.translatable("tooltip.maniacrev.coin_debt.bad",
                    DodepovichCasinoManager.DEBT_EFFECT_SECONDS);
            case REROLL -> Component.translatable("tooltip.maniacrev.coin_reroll.bad");
            case FATE -> Component.translatable("tooltip.maniacrev.coin_fate.bad",
                    formatHp(DodepovichCasinoManager.FATE_ALLY_DAMAGE));
        };
    }

    private static String formatHp(float value) {
        return value == (int) value ? Integer.toString((int) value) : Float.toString(value);
    }
}
