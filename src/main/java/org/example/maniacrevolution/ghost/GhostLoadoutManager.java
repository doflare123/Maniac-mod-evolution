package org.example.maniacrevolution.ghost;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.cosmetic.CosmeticSyncHandler;
import org.example.maniacrevolution.data.PlayerData;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.SyncAbilityCooldownPacket;

import java.util.HashSet;
import java.util.Set;

public final class GhostLoadoutManager {
    private static final String MANIAC_CLASS_OBJECTIVE = "ManiacClass";
    private static final String GHOST_LOADOUT_TAG = "GhostLoadoutItem";
    private static final String STORED_COSMETICS_TAG = "GhostStoredCosmetics";
    private static final int WHITE_LEATHER_COLOR = 0xF9FFFE;

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

        player.setItemSlot(EquipmentSlot.HEAD, createGhostArmor(Items.LEATHER_HELMET));
        player.setItemSlot(EquipmentSlot.LEGS, createGhostArmor(Items.LEATHER_LEGGINGS));
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
        restoreCosmetics(player);

        if (isGhostArmor(player.getItemBySlot(EquipmentSlot.HEAD))) {
            player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        }
        if (isGhostArmor(player.getItemBySlot(EquipmentSlot.LEGS))) {
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

    public static void suppressCosmetics(ServerPlayer player) {
        PlayerData data = PlayerDataManager.get(player);
        if (data == null) {
            return;
        }

        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(STORED_COSMETICS_TAG, Tag.TAG_LIST)) {
            ListTag stored = new ListTag();
            for (String cosmeticId : data.getCosmeticData().getEnabledCosmetics()) {
                stored.add(StringTag.valueOf(cosmeticId));
            }
            persistentData.put(STORED_COSMETICS_TAG, stored);
        }

        Set<String> enabled = new HashSet<>(data.getCosmeticData().getEnabledCosmetics());
        for (String cosmeticId : enabled) {
            data.getCosmeticData().setEnabled(cosmeticId, false);
        }

        PlayerDataManager.syncToClient(player);
        CosmeticSyncHandler.syncCosmeticsToAll(player);
    }

    public static void restoreCosmetics(ServerPlayer player) {
        PlayerData data = PlayerDataManager.get(player);
        if (data == null) {
            return;
        }

        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(STORED_COSMETICS_TAG, Tag.TAG_LIST)) {
            return;
        }

        ListTag stored = persistentData.getList(STORED_COSMETICS_TAG, Tag.TAG_STRING);
        for (int i = 0; i < stored.size(); i++) {
            data.getCosmeticData().setEnabled(stored.getString(i), true);
        }
        persistentData.remove(STORED_COSMETICS_TAG);

        PlayerDataManager.syncToClient(player);
        CosmeticSyncHandler.syncCosmeticsToAll(player);
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

    public static boolean isGhostArmor(ItemStack stack) {
        return !stack.isEmpty() && stack.getOrCreateTag().getBoolean(GHOST_LOADOUT_TAG);
    }

    private static ItemStack createGhostArmor(net.minecraft.world.item.Item item) {
        ItemStack stack = new ItemStack(item);
        stack.getOrCreateTag().putBoolean(GHOST_LOADOUT_TAG, true);
        if (stack.getItem() instanceof DyeableLeatherItem dyeable) {
            dyeable.setColor(stack, WHITE_LEATHER_COLOR);
        }
        return stack;
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
