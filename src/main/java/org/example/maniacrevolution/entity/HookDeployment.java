package org.example.maniacrevolution.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Authoritative cast ownership plus per-stack render data, independent of cooldown length. */
public final class HookDeployment {
    private static final String TAG = "maniacrevHookFlight";
    private static final Map<UUID, Flight> ACTIVE = new HashMap<>();
    private record Flight(Player owner, HookEntity hook, ItemStack stack) {}
    private HookDeployment() {}

    public static void launched(Player owner, ItemStack stack, HookEntity hook) {
        if (owner.level().isClientSide()) return;
        var tag = new CompoundTag();
        tag.putUUID("Entity", hook.getUUID());
        tag.putInt("EntityId", hook.getId());
        stack.getOrCreateTag().put(TAG, tag);
        ACTIVE.put(owner.getUUID(), new Flight(owner, hook, stack));
    }

    @Nullable
    public static HookEntity activeHook(Player player) {
        if (player.level().isClientSide()) {
            var inventory = player.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                HookEntity hook = deployedEntity(inventory.getItem(i), player.level());
                if (hook != null && hook.getOwner() == player) return hook;
            }
            for (var hook : player.level().getEntitiesOfClass(HookEntity.class,
                    player.getBoundingBox().inflate(64), hook -> !hook.isRemoved() && hook.getOwner() == player))
                return hook;
            return null;
        }
        Flight flight = ACTIVE.get(player.getUUID());
        if (flight == null) return null;
        if (flight.hook.isRemoved() || flight.hook.level() != player.level()
                || !player.isAlive() || flight.hook.getOwner() != player) {
            finished(flight.hook);
            return null;
        }
        return flight.hook;
    }

    public static boolean hasActiveHook(Player player) {
        return activeHook(player) != null;
    }

    public static boolean isDeployed(ItemStack stack, @Nullable Level level) {
        return deployedEntity(stack, level) != null;
    }

    @Nullable
    private static HookEntity deployedEntity(ItemStack stack, @Nullable Level level) {
        if (level == null || !stack.hasTag() || !stack.getTag().contains(TAG)) return null;
        CompoundTag tag = stack.getTag().getCompound(TAG);
        if (!tag.hasUUID("Entity")) return null;
        UUID uuid = tag.getUUID("Entity");
        Entity entity = level instanceof ServerLevel server ? server.getEntity(uuid) : level.getEntity(tag.getInt("EntityId"));
        return entity instanceof HookEntity hook && !entity.isRemoved() && entity.getUUID().equals(uuid) ? hook : null;
    }

    private static void clear(ItemStack stack, UUID expected) {
        if (stack.hasTag() && stack.getTag().contains(TAG)) {
            var tag = stack.getTag().getCompound(TAG);
            if (tag.hasUUID("Entity") && tag.getUUID("Entity").equals(expected)) {
                stack.getTag().remove(TAG);
                if (stack.getTag().isEmpty()) stack.setTag(null);
            }
        }
    }

    public static void clearStale(ItemStack stack, Level level) {
        if (level.isClientSide() || !stack.hasTag() || !stack.getTag().contains(TAG) || isDeployed(stack, level)) return;
        stack.getTag().remove(TAG);
        if (stack.getTag().isEmpty()) stack.setTag(null);
    }

    public static void finished(HookEntity hook) {
        if (hook.level().isClientSide()) return;
        Flight flight = ACTIVE.values().stream().filter(value -> value.hook == hook).findFirst().orElse(null);
        if (flight == null) return;
        ACTIVE.remove(flight.owner.getUUID());
        clear(flight.stack, hook.getUUID());
        var inventory = flight.owner.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) clear(inventory.getItem(i), hook.getUUID());
    }

    public static void clearAll() {
        for (Flight flight : java.util.List.copyOf(ACTIVE.values())) finished(flight.hook);
    }
}
