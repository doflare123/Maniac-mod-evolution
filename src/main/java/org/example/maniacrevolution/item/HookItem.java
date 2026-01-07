package org.example.maniacrevolution.item;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.example.maniacrevolution.entity.HookEntity;
import org.example.maniacrevolution.util.ManaUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HookItem extends Item {

    private static final float MANA_COST = 20.0f;
    private static final int COOLDOWN_TICKS = 20 * 1; // 20 секунд

    // Хранение кулдаунов по игрокам
    private static final Map<UUID, Long> cooldowns = new HashMap<>();

    public HookItem(Properties properties) {
        super(properties.stacksTo(1)); // Не стакается
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }

        // Проверяем кулдаун
        UUID playerUUID = player.getUUID();
        long currentTime = level.getGameTime();

        if (cooldowns.containsKey(playerUUID)) {
            long cooldownEnd = cooldowns.get(playerUUID);
            if (currentTime < cooldownEnd) {
                long ticksLeft = cooldownEnd - currentTime;
                int secondsLeft = (int) (ticksLeft / 20);

                player.displayClientMessage(
                        Component.literal("§cHook on cooldown: " + secondsLeft + "s"),
                        true
                );
                return InteractionResultHolder.fail(stack);
            }
        }

        // Проверяем ману
        if (!ManaUtil.hasMana(player, MANA_COST)) {
            player.displayClientMessage(
                    Component.literal("§9Not enough mana! (" + MANA_COST + " required)"),
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

        // Устанавливаем кулдаун
        cooldowns.put(playerUUID, currentTime + COOLDOWN_TICKS);
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);

        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7Launches a hook that pulls enemies"));
        tooltip.add(Component.literal("§7Max range: §e7 blocks"));
        tooltip.add(Component.literal("§9Costs: §b20 mana"));
        tooltip.add(Component.literal("§7Cooldown: §e20 seconds"));
    }

    // Статический метод для очистки кулдаунов (можно вызвать при выходе игрока)
    public static void clearCooldown(UUID playerUUID) {
        cooldowns.remove(playerUUID);
    }
}