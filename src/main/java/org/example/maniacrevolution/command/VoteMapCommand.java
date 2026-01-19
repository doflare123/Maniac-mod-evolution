package org.example.maniacrevolution.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.example.maniacrevolution.map.MapVotingManager;

public class VoteMapCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("maniacrev")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("votemap")
                        .then(Commands.argument("duration", IntegerArgumentType.integer(10, 300))
                                .executes(context -> {
                                    int duration = IntegerArgumentType.getInteger(context, "duration");
                                    return startVoting(context.getSource(), duration);
                                }))
                        .executes(context -> startVoting(context.getSource(), 60)) // По умолчанию 60 секунд
                )
        );
    }

    private static int startVoting(CommandSourceStack source, int duration) {
        MapVotingManager.getInstance().startVoting(source.getServer(), duration);
        source.sendSuccess(() -> Component.literal("Голосование за карту запущено на " + duration + " секунд!"), true);
        return 1;
    }
}