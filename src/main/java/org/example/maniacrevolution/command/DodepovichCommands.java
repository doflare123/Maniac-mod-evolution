package org.example.maniacrevolution.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.dodepovich.DodepovichCasinoManager;

import java.util.Collection;

public class DodepovichCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("maniacrev")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("dodepovich")
                        .then(Commands.literal("reset_coin_history")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> resetHistory(context.getSource(),
                                                EntityArgument.getPlayers(context, "targets")))))
                        .then(Commands.literal("reset_casino_chances")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> resetCasinoChances(context.getSource(),
                                                EntityArgument.getPlayers(context, "targets")))))
                        .then(Commands.literal("reset_all")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> resetAll(context.getSource(),
                                                EntityArgument.getPlayers(context, "targets")))))));
    }

    private static int resetHistory(CommandSourceStack source, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            DodepovichCasinoManager.clearCoinHistory(player);
        }

        source.sendSuccess(() -> Component.literal("§aИстория монеток очищена у игроков: " + players.size()), true);
        return players.size();
    }

    private static int resetCasinoChances(CommandSourceStack source, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            DodepovichCasinoManager.resetCasinoChances(player);
        }

        source.sendSuccess(() -> Component.literal("§aШансы казино очищены у игроков: " + players.size()), true);
        return players.size();
    }

    private static int resetAll(CommandSourceStack source, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            DodepovichCasinoManager.clearCoinHistory(player);
            DodepovichCasinoManager.resetCasinoChances(player);
        }

        source.sendSuccess(() -> Component.literal("§aВсе данные казино Додеповича очищены у игроков: " + players.size()), true);
        return players.size();
    }
}
