package org.example.maniacrevolution.helsing;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.example.maniacrevolution.item.DeathScytheItem;
import org.example.maniacrevolution.item.HookItem;
import org.example.maniacrevolution.item.armor.ArmorAbilityCooldownManager;
import org.example.maniacrevolution.ghost.GhostPossessionManager;

/** Preserve existing timers, advancing only cooldown deadlines while the curse is active. */
public final class HelsingCooldowns {
    public static void pause(ServerPlayer player, long elapsedMillis) {
        ArmorAbilityCooldownManager.pauseCooldowns(player, elapsedMillis);
        DeathScytheItem.pauseCooldown(player.getUUID(), elapsedMillis);
        HookItem.pauseCooldown(player, elapsedMillis);
        GhostPossessionManager.pauseCooldown(player);
        for (ItemStack stack : player.getInventory().items) pauseStack(stack, player.level().getGameTime());
        pauseStack(player.getOffhandItem(), player.level().getGameTime());
    }
    private static void pauseStack(ItemStack stack, long tick) {
        if (!stack.hasTag()) return;
        var tag = stack.getTag();
        for (String key : new String[]{"BeastClawCooldown", "PlagueLanternCooldownTick", "MicCooldown", "BongCooldownTick"}) {
            if (tag.contains(key)) tag.putLong(key, HelsingTiming.pauseDeadline(tag.getLong(key), tick));
        }
    }
    public static int remainingSeconds(ServerPlayer player, ItemStack stack, org.example.maniacrevolution.item.IItemWithAbility ability) {
        if (stack.hasTag()) {
            var tag = stack.getTag();
            for (String key : new String[]{"BeastClawCooldown", "PlagueLanternCooldownTick", "MicCooldown", "BongCooldownTick"}) {
                if (tag.contains(key)) return (int) Math.max(0, (tag.getLong(key) - player.level().getGameTime() + 19) / 20);
            }
            if (tag.contains("netherswap_cooldown")) return (tag.getInt("netherswap_cooldown") + 19) / 20;
        }
        return ability.getCooldownSeconds(player);
    }
    private HelsingCooldowns() {}
}
