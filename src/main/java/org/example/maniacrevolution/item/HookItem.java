package org.example.maniacrevolution.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.example.maniacrevolution.Maniacrev;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.example.maniacrevolution.client.HookItemClientExtensions;
import org.example.maniacrevolution.entity.HookEntity;
import org.example.maniacrevolution.entity.HookDeployment;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.SyncAbilityCooldownPacket;
import org.example.maniacrevolution.util.ManaUtil;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public class HookItem extends SwordItem implements IItemWithAbility {

    private static final float MANA_COST = 5.0f;
    private static final int COOLDOWN_TICKS = 20 * 20; // 20 секунд.
    private static final int DAMAGE = 0;
    private static final float SPEED = -2.4F;

    public HookItem(Properties properties) {
        super(Tiers.NETHERITE, DAMAGE-1, SPEED, properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            return InteractionResultHolder.consume(stack);
        }
        if (HookDeployment.hasActiveHook(player)) return InteractionResultHolder.fail(stack);

        // The same native cooldown is used for validation and both HUD indicators.
        if (player.getCooldowns().isOnCooldown(this)) {
            int secondsLeft = getCooldownSeconds(player);
            player.displayClientMessage(
                    Component.literal("§cПерезарядка: " + secondsLeft + "с"),
                    true
            );
            return InteractionResultHolder.fail(stack);
        }

        // Проверяем ману
        if (!ManaUtil.hasMana(player, MANA_COST)) {
            player.displayClientMessage(
                    Component.literal("§9Недостаточно маны!"),
                    true
            );
            return InteractionResultHolder.fail(stack);
        }

        // The projectile is launched on this click. Player animation never delays gameplay.

        // Запускаем хук
        HookEntity hook = new HookEntity(level, player, hand);
        if (!level.addFreshEntity(hook)) return InteractionResultHolder.fail(stack);
        if (!ManaUtil.consumeMana(player, MANA_COST)) {
            hook.discard();
            return InteractionResultHolder.fail(stack);
        }
        HookDeployment.launched(player, stack, hook);

        // Звук
        level.playSound(null, player.blockPosition(),
                SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 1.0f, 1.0f);

        // One authoritative tick-based timer for activation, the slot overlay and the ability HUD.
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);

        // НОВОЕ: Синхронизируем с клиентом
        if (player instanceof ServerPlayer serverPlayer) {
            ModNetworking.sendToPlayer(
                    new SyncAbilityCooldownPacket(this, COOLDOWN_TICKS / 20, COOLDOWN_TICKS / 20, 0),
                    serverPlayer
            );
        }

        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new HookItemClientExtensions());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        HookDeployment.clearStale(stack, level);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(""));
        tooltip.add(Component.translatable("tooltip.maniacrev.ability", getAbilityName()).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal(getAbilityDescription()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.translatable("tooltip.maniacrev.mana_cost", (int) MANA_COST).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.maniacrev.cooldown", COOLDOWN_TICKS / 20).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.translatable("tooltip.maniacrev.hook_aim").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.maniacrev.hook_cancel").withStyle(ChatFormatting.DARK_GRAY));
    }

    // === Реализация IItemWithAbility ===

    @Override
    public ResourceLocation getAbilityIcon() {
        return new ResourceLocation(Maniacrev.MODID, "textures/gui/abilities/hook.png");
    }

    @Override
    public String getAbilityName() {
        return Component.translatable("ability.maniacrev.hook.name").getString();
    }

    @Override
    public String getAbilityDescription() {
        return Component.translatable("ability.maniacrev.hook.desc", (int) HookEntity.MAX_DISTANCE).getString();
    }

    @Override
    public float getManaCost() {
        return MANA_COST;
    }

    @Override
    public int getCooldownSeconds(Player player) {
        int remainingTicks = Math.round(getCooldownProgress(player) * COOLDOWN_TICKS);
        return (remainingTicks + 19) / 20;
    }

    @Override
    public float getCooldownProgress(Player player) {
        return player.getCooldowns().getCooldownPercent(this, 0);
    }

    @Override
    public int getMaxCooldownSeconds() {
        return COOLDOWN_TICKS / 20;
    }

}
