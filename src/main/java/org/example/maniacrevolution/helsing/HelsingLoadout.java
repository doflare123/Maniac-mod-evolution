package org.example.maniacrevolution.helsing;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.item.armor.ArmorAbilityCooldownManager;

public final class HelsingLoadout {
    /** Called by the map's existing refresh_abilities command; never grants ammunition. */
    public static void equip(ServerPlayer player) {
        if (!HelsingManager.isHelsing(player)) return;
        ItemStack previous = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!previous.isEmpty() && !previous.is(ModItems.HELSING_HAT.get())) {
            if (!player.getInventory().add(previous.copy())) player.drop(previous.copy(), false);
        }
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.HELSING_HAT.get()));
        boolean hasCrossbow = player.getInventory().items.stream().anyMatch(stack -> stack.is(ModItems.HELSING_CROSSBOW.get()))
                || player.getOffhandItem().is(ModItems.HELSING_CROSSBOW.get());
        if (!hasCrossbow) {
            ItemStack bow = new ItemStack(ModItems.HELSING_CROSSBOW.get());
            if (!player.getInventory().add(bow)) player.drop(bow, false);
        }
        HelsingManager.clear(player);
        ArmorAbilityCooldownManager.clearCooldown(player, ModItems.HELSING_HAT.get());
        ArmorAbilityCooldownManager.syncToClient(player, ModItems.HELSING_HAT.get(), 0);
        player.inventoryMenu.broadcastChanges();
    }
    private HelsingLoadout() {}
}
