package org.example.maniacrevolution.event;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.SyncSettingsPacket;
import org.example.maniacrevolution.settings.GameSettings;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LoginItemHandler {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        givePreGameReadyItem(player);

        if (player.hasPermissions(2)) {
            giveSettingsItem(player);
            syncSettingsToPlayer(player);
        }
    }

    private static void givePreGameReadyItem(ServerPlayer player) {
        if (hasAny(player, ModItems.PRE_GAME_READY_ITEM.get(), ModItems.PRE_GAME_READY_ITEM_ACTIVE.get())) {
            return;
        }

        ItemStack readyItem = new ItemStack(ModItems.PRE_GAME_READY_ITEM.get());
        if (!player.getInventory().add(readyItem)) {
            player.drop(readyItem, false);
        }
    }

    private static void giveSettingsItem(ServerPlayer player) {
        if (hasAny(player, ModItems.SETTINGS_ITEM.get())) {
            return;
        }

        ItemStack settingsItem = new ItemStack(ModItems.SETTINGS_ITEM.get());
        settingsItem.setHoverName(Component.literal("§6§lНастройки"));

        if (!player.getInventory().add(settingsItem)) {
            player.drop(settingsItem, false);
        }
    }

    private static void syncSettingsToPlayer(ServerPlayer player) {
        GameSettings settings = GameSettings.get(player.server);
        ModNetworking.sendToPlayer(SyncSettingsPacket.from(settings), player);
    }

    private static boolean hasAny(ServerPlayer player, Item... items) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (matchesAny(stack, items)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesAny(ItemStack stack, Item... items) {
        for (Item item : items) {
            if (stack.is(item)) {
                return true;
            }
        }
        return false;
    }
}
