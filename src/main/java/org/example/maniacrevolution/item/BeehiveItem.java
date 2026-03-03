package org.example.maniacrevolution.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.example.maniacrevolution.entity.RageBeeEntity;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Улей — еда.
 * Поедается 3 секунды, можно есть при любой сытости.
 * При поедании: резистенс 100% на 10 сек + 5 разъярённых пчёл.
 * Регистрация: ITEMS.register("beehive_food", () -> new BeehiveItem(
 *     new Item.Properties().food(BeehiveItem.FOOD).stacksTo(16)));
 */
public class BeehiveItem extends Item {

    public static final int BEE_COUNT   = 5;
    public static final int RAGE_TICKS  = 200; // 10 сек
    public static final int EAT_TICKS   = 60;  // 3 сек

    public static final FoodProperties FOOD = new FoodProperties.Builder()
            .nutrition(2).saturationMod(0f).alwaysEat().build();

    public BeehiveItem(Properties props) { super(props); }

    @Override public int     getUseDuration(ItemStack stack) { return EAT_TICKS; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.EAT; }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level,
                                     net.minecraft.world.entity.LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide() && entity instanceof ServerPlayer sp) {
            applyRage(sp, (ServerLevel) level);
        }
        return result;
    }

    private void applyRage(ServerPlayer player, ServerLevel level) {
        // Resistance 4 = полная защита от урона
        player.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_RESISTANCE, RAGE_TICKS, 4, false, true, true));
        player.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SPEED, RAGE_TICKS, 1, false, true, true));

        for (int i = 0; i < BEE_COUNT; i++) {
            double angle = (2 * Math.PI / BEE_COUNT) * i;
            RageBeeEntity bee = new RageBeeEntity(level, player);
            bee.setPos(
                    player.getX() + Math.cos(angle) * 1.5,
                    player.getY() + 1,
                    player.getZ() + Math.sin(angle) * 1.5
            );
            level.addFreshEntity(bee);
        }
        player.displayClientMessage(
                Component.literal("§4§lЯРОСТЬ! §cПчёлы вышли на охоту!"), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("§6Улей").withStyle(ChatFormatting.BOLD));
        tooltip.add(Component.literal("  §cЯрость §7на " + RAGE_TICKS/20 + " сек").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Резистенс 100% к урону").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Спавнит §e" + BEE_COUNT + " §7разъярённых пчёл").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Пчёлы атакуют выживших в радиусе §f10 §7блоков").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Поедание: §f3 сек §7| Любая сытость").withStyle(ChatFormatting.GRAY));
    }
}