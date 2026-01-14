package org.example.maniacrevolution.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.character.CharacterType;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.OpenCharacterMenuPacket;
import org.example.maniacrevolution.readiness.ReadinessManager;

import java.util.Collection;

/**
 * Команды для управления выбором классов и готовностью
 */
public class CharacterMenuCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("maniacrev")
                .requires(source -> source.hasPermission(2))

                // Открытие меню выбора класса для выживших
                .then(Commands.literal("select_survivor")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> openSurvivorMenu(context))))

                // Открытие меню выбора класса для маньяка
                .then(Commands.literal("select_maniac")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> openManiacMenu(context))))

                // Сброс готовности всех игроков
                .then(Commands.literal("reset_ready")
                        .executes(context -> resetAllReadiness(context))
                        // Или конкретного игрока
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> resetPlayerReadiness(context))))
        );
    }

    private static int openSurvivorMenu(CommandContext<CommandSourceStack> context) {
        try {
            Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");

            for (ServerPlayer player : players) {
                ModNetworking.sendToPlayer(
                        new OpenCharacterMenuPacket(CharacterType.SURVIVOR),
                        player
                );

                context.getSource().sendSuccess(
                        () -> Component.literal("§aОткрыто меню выбора выжившего для " +
                                player.getName().getString()),
                        true
                );
            }

            return players.size();
        } catch (Exception e) {
            context.getSource().sendFailure(
                    Component.literal("§cОшибка открытия меню: " + e.getMessage())
            );
            return 0;
        }
    }

    private static int openManiacMenu(CommandContext<CommandSourceStack> context) {
        try {
            Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");

            for (ServerPlayer player : players) {
                ModNetworking.sendToPlayer(
                        new OpenCharacterMenuPacket(CharacterType.MANIAC),
                        player
                );

                context.getSource().sendSuccess(
                        () -> Component.literal("§aОткрыто меню выбора маньяка для " +
                                player.getName().getString()),
                        true
                );
            }

            return players.size();
        } catch (Exception e) {
            context.getSource().sendFailure(
                    Component.literal("§cОшибка открытия меню: " + e.getMessage())
            );
            return 0;
        }
    }

    private static int resetAllReadiness(CommandContext<CommandSourceStack> context) {
        ReadinessManager.resetReadiness(null);

        context.getSource().sendSuccess(
                () -> Component.literal("§aГотовность всех игроков сброшена"),
                true
        );

        return 1;
    }

    private static int resetPlayerReadiness(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "target");
            ReadinessManager.resetReadiness(player);

            context.getSource().sendSuccess(
                    () -> Component.literal("§aГотовность игрока " +
                            player.getName().getString() + " сброшена"),
                    true
            );

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(
                    Component.literal("§cОшибка сброса готовности: " + e.getMessage())
            );
            return 0;
        }
    }
}