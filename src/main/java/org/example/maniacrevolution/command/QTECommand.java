package org.example.maniacrevolution.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.Config;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.StartQTEPacket;
import org.example.maniacrevolution.network.packets.StopQTEPacket;

import java.util.Collection;

public class QTECommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("maniacrev")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("start_game")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("number", IntegerArgumentType.integer(1))
                                        .executes(context -> startGame(context)))))
                .then(Commands.literal("stop_game")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> stopGame(context))))
                .then(Commands.literal("hackQTE")
                    .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                            .executes(QTECommand::setHackQTEReward)
                    )
                            .executes(QTECommand::getHackQTEReward))
        );
    }

    private static int startGame(CommandContext<CommandSourceStack> context) {
        try {
            Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
            int generatorNumber = IntegerArgumentType.getInteger(context, "number");

            for (ServerPlayer player : players) {
                boolean hasQuickReflexes = org.example.maniacrevolution.perk.perks.survivor.QuickReflexesPerk.hasQuickReflexes(player);
                ModNetworking.CHANNEL.sendTo(
                        new StartQTEPacket(generatorNumber, hasQuickReflexes),
                        player.connection.connection,
                        net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
                );

                context.getSource().sendSuccess(
                        () -> Component.literal("Started QTE game for " + player.getName().getString() +
                                " with generator #" + generatorNumber),
                        true
                );
            }

            return players.size();
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error starting game: " + e.getMessage()));
            return 0;
        }
    }

    private static int stopGame(CommandContext<CommandSourceStack> context) {
        try {
            Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");

            for (ServerPlayer player : players) {
                ModNetworking.CHANNEL.sendTo(
                        new StopQTEPacket(),
                        player.connection.connection,
                        net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
                );

                context.getSource().sendSuccess(
                        () -> Component.literal("Stopped QTE game for " + player.getName().getString()),
                        true
                );
            }

            return players.size();
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error stopping game: " + e.getMessage()));
            return 0;
        }
    }

    private static int setHackQTEReward(CommandContext<CommandSourceStack> context) {
        int amount = IntegerArgumentType.getInteger(context, "amount");
        Config.setHackQTEReward(amount);

        context.getSource().sendSuccess(
                () -> Component.literal("§aHack QTE reward set to: §e" + amount),
                true
        );

        return amount;
    }

    private static int getHackQTEReward(CommandContext<CommandSourceStack> context) {
        int current = Config.getHackQTEReward();

        context.getSource().sendSuccess(
                () -> Component.literal("§aCurrent Hack QTE reward: §e" + current),
                false
        );

        return current;
    }
}