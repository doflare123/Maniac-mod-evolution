package org.example.maniacrevolution.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.SyncSettingsPacket;
import org.example.maniacrevolution.settings.GameSettings;

public class SettingsCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("maniacrev")
                .then(Commands.literal("settings")
                        .requires(source -> source.hasPermission(2))
                        .executes(SettingsCommand::giveSettingsToOps)
                )
                .then(Commands.literal("settings_all")
                        .requires(source -> source.hasPermission(2))
                        .executes(SettingsCommand::giveSettingsToAll)
                )
        );
    }

    private static int giveSettingsToOps(CommandContext<CommandSourceStack> context) {
        int issuedCount = 0;

        for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
            if (!player.hasPermissions(2)) {
                continue;
            }

            giveSettingsItem(player);
            issuedCount++;
        }

        context.getSource().sendSuccess(() ->
                Component.literal("§aПредмет настроек выдан всем игрокам с опкой!"), true);
        return issuedCount;
    }

    private static int giveSettingsToAll(CommandContext<CommandSourceStack> context) {
        context.getSource().getServer().getPlayerList().getPlayers().forEach(player -> {
            giveSettingsItem(player);
        });

        context.getSource().sendSuccess(() ->
                Component.literal("§aПредмет настроек выдан всем игрокам!"), true);
        return 1;
    }

    private static void giveSettingsItem(ServerPlayer player) {
        if (hasItem(player, ModItems.SETTINGS_ITEM.get())) {
            syncSettingsToPlayer(player);
            return;
        }

        ItemStack settingsItem = new ItemStack(ModItems.SETTINGS_ITEM.get());
        settingsItem.setHoverName(Component.literal("§6§lНастройки"));

        if (!player.getInventory().add(settingsItem)) {
            player.drop(settingsItem, false);
        }

        syncSettingsToPlayer(player);
    }

    private static void syncSettingsToPlayer(ServerPlayer player) {
        GameSettings settings = GameSettings.get(player.server);
        ModNetworking.sendToPlayer(SyncSettingsPacket.from(settings), player);
    }

    private static boolean hasItem(ServerPlayer player, Item item) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).is(item)) {
                return true;
            }
        }
        return false;
    }
}
