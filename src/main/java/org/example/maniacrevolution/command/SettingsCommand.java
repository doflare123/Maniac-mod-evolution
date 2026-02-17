package org.example.maniacrevolution.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
                        .executes(SettingsCommand::giveSettingsItem)
                )
                .then(Commands.literal("settings_all")
                        .requires(source -> source.hasPermission(2))
                        .executes(SettingsCommand::giveSettingsToAll)
                )
        );
    }

    private static int giveSettingsItem(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            ItemStack settingsItem = new ItemStack(ModItems.SETTINGS_ITEM.get());
            settingsItem.setHoverName(Component.literal("§6§lНастройки"));

            if (!player.getInventory().add(settingsItem)) {
                player.drop(settingsItem, false);
            }

            // Синхронизируем настройки с клиентом
            syncSettingsToPlayer(player);

            player.sendSystemMessage(Component.literal("§aВам выдан предмет настроек!"));
            return 1;
        }
        return 0;
    }

    private static int giveSettingsToAll(CommandContext<CommandSourceStack> context) {
        context.getSource().getServer().getPlayerList().getPlayers().forEach(player -> {
            ItemStack settingsItem = new ItemStack(ModItems.SETTINGS_ITEM.get());
            settingsItem.setHoverName(Component.literal("§6§lНастройки"));

            if (!player.getInventory().add(settingsItem)) {
                player.drop(settingsItem, false);
            }

            // Синхронизируем настройки с каждым клиентом
            syncSettingsToPlayer(player);
        });

        context.getSource().sendSuccess(() ->
                Component.literal("§aПредмет настроек выдан всем игрокам!"), true);
        return 1;
    }

    private static void syncSettingsToPlayer(ServerPlayer player) {
        GameSettings settings = GameSettings.get(player.server);
        ModNetworking.sendToPlayer(new SyncSettingsPacket(
                settings.getComputerCount(),
                settings.getHackPoints(),
                settings.getHpBoost(),
                settings.getManiacCount(),
                settings.getGameTime(),
                settings.getSelectedMap()
        ), player);
    }
}