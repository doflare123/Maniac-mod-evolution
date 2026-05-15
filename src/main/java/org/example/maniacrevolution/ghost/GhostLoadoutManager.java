package org.example.maniacrevolution.ghost;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.SyncAbilityCooldownPacket;

public final class GhostLoadoutManager {
    private static final String MANIAC_CLASS_OBJECTIVE = "ManiacClass";

    private GhostLoadoutManager() {
    }

    public static boolean isGhostClass(ServerPlayer player) {
        if (player == null || player.getTeam() == null || !"maniac".equalsIgnoreCase(player.getTeam().getName())) {
            return false;
        }

        var objective = player.getScoreboard().getObjective(MANIAC_CLASS_OBJECTIVE);
        if (objective == null) {
            return false;
        }

        return player.getScoreboard()
                .getOrCreatePlayerScore(player.getScoreboardName(), objective)
                .getScore() == GhostPossessionManager.GHOST_CLASS_ID;
    }

    public static void refreshGhostLoadout(ServerPlayer player) {
        if (player == null) {
            return;
        }

        GhostStealthManager.resetGhostState(player);
        GhostPossessionManager.resetGhostState(player);

        removeGhostItems(player);
        clearStoredGhostArmor(player);

        placeInHotbar(player, new ItemStack(ModItems.TOY_KNIFE.get()), 0);
        placeInHotbar(player, new ItemStack(ModItems.GHOST_HAND.get()), 1);

        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.GHOST_HELMET.get()));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.GHOST_LEGGINGS.get()));
        player.getInventory().selected = 0;

        player.getCooldowns().removeCooldown(ModItems.TOY_KNIFE.get());
        player.getCooldowns().removeCooldown(ModItems.GHOST_HAND.get());
        ModNetworking.sendToPlayer(new SyncAbilityCooldownPacket(ModItems.TOY_KNIFE.get(), 0, GhostStealthManager.STEALTH_COOLDOWN_TICKS / 20, 0), player);
        ModNetworking.sendToPlayer(new SyncAbilityCooldownPacket(ModItems.GHOST_HAND.get(), 0, GhostPossessionManager.POSSESSION_COOLDOWN_TICKS / 20, 0), player);
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
    }

    public static void clearGhostLoadout(ServerPlayer player) {
        if (player == null) {
            return;
        }

        GhostStealthManager.resetGhostState(player);
        GhostPossessionManager.resetGhostState(player);
        removeGhostItems(player);
        clearStoredGhostArmor(player);

        if (player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.GHOST_HELMET.get())) {
            player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        }
        if (player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.GHOST_LEGGINGS.get())) {
            player.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
        }

        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
    }

    public static void clearStoredGhostArmor(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        data.remove(GhostStealthManager.NBT_HIDDEN_HELMET);
        data.remove(GhostStealthManager.NBT_HIDDEN_LEGGINGS);
    }

    private static void removeGhostItems(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(ModItems.TOY_KNIFE.get()) || stack.is(ModItems.GHOST_HAND.get())) {
                inventory.setItem(slot, ItemStack.EMPTY);
            }
        }
    }

    private static void placeInHotbar(ServerPlayer player, ItemStack stack, int preferredHotbarSlot) {
        Inventory inventory = player.getInventory();
        if (preferredHotbarSlot >= 0 && preferredHotbarSlot < 9) {
            ItemStack displaced = inventory.getItem(preferredHotbarSlot);
            if (!displaced.isEmpty() && !inventory.add(displaced.copy())) {
                player.drop(displaced.copy(), false);
            }
            inventory.setItem(preferredHotbarSlot, stack);
            return;
        }

        if (!inventory.add(stack)) {
            player.drop(stack, false);
        }
    }

    @Mod.EventBusSubscriber(modid = Maniacrev.MODID)
    public static class Events {
        @SubscribeEvent
        public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer player && isGhostClass(player)) {
                refreshGhostLoadout(player);
                player.displayClientMessage(Component.literal("§dСнаряжение Призрака восстановлено."), true);
            }
        }
    }
}
