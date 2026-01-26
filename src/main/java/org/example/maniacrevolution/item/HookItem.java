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
import org.example.maniacrevolution.client.ClientAbilityData;
import org.example.maniacrevolution.entity.HookEntity;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.SyncAbilityCooldownPacket;
import org.example.maniacrevolution.util.ManaUtil;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HookItem extends SwordItem implements IItemWithAbility {

    private static final float MANA_COST = 5.0f;
    private static final int COOLDOWN_TICKS = 20 * 20; // 20 секунд
    private static final int DAMAGE = 0;
    private static final float SPEED = -2.4F;

    // ИСПРАВЛЕНО: Используем timestamp вместо тиков
    private static final Map<UUID, CooldownData> cooldowns = new HashMap<>();

    private static class CooldownData {
        long timestamp;
        int durationTicks;

        CooldownData(long timestamp, int durationTicks) {
            this.timestamp = timestamp;
            this.durationTicks = durationTicks;
        }

        int getRemaining() {
            long currentTime = System.currentTimeMillis();
            long elapsed = currentTime - timestamp;
            long elapsedTicks = elapsed / 50;
            return Math.max(0, (int)(durationTicks - elapsedTicks));
        }

        boolean isExpired() {
            return getRemaining() <= 0;
        }
    }

    public HookItem(Properties properties) {
        super(Tiers.NETHERITE, DAMAGE-1, SPEED, properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }

        UUID playerUUID = player.getUUID();

        // ИСПРАВЛЕНО: Проверяем кулдаун через новую систему
        CooldownData cooldownData = cooldowns.get(playerUUID);
        if (cooldownData != null && !cooldownData.isExpired()) {
            int secondsLeft = cooldownData.getRemaining() / 20;
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

        // Тратим ману
        ManaUtil.consumeMana(player, MANA_COST);

        // Запускаем хук
        HookEntity hook = new HookEntity(level, player);
        level.addFreshEntity(hook);

        // Звук
        level.playSound(null, player.blockPosition(),
                SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 1.0f, 1.0f);

        // ИСПРАВЛЕНО: Устанавливаем кулдаун через timestamp
        long currentTime = System.currentTimeMillis();
        cooldowns.put(playerUUID, new CooldownData(currentTime, COOLDOWN_TICKS));
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
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§6Способность: §e" + getAbilityName()).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("§7" + getAbilityDescription()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§9Стоимость: §b" + (int)MANA_COST + " маны").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("§9Кулдаун: §b" + (COOLDOWN_TICKS / 20) + "с").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§8ПКМ для активации").withStyle(ChatFormatting.DARK_GRAY));
    }

    // === Реализация IItemWithAbility ===

    @Override
    public ResourceLocation getAbilityIcon() {
        return new ResourceLocation(Maniacrev.MODID, "textures/gui/abilities/hook.png");
    }

    @Override
    public String getAbilityName() {
        return "И куда собрался?";
    }

    @Override
    public String getAbilityDescription() {
        return "Киньте хук (дальность 12 блоков) и притяните к себе существо";
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

        CooldownData data = cooldowns.get(player.getUUID());
        return data != null ? data.getRemaining() / 20 : 0;
    }

    @Override
    public int getMaxCooldownSeconds() {
        return COOLDOWN_TICKS / 20;
    }

    /**
     * Очистка кулдауна при выходе игрока
     */
    public static void clearCooldown(UUID playerUUID) {
        CooldownData data = cooldowns.get(playerUUID);
        if (data != null && data.isExpired()) {
            cooldowns.remove(playerUUID);
        }
    }

    /**
     * НОВОЕ: Очистка всех истекших кулдаунов
     */
    public static void cleanupExpiredCooldowns() {
        cooldowns.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}