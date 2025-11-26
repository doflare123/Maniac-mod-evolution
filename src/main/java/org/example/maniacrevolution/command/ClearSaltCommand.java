package org.example.maniacrevolution.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import org.example.maniacrevolution.util.SaltTracker;

public class ClearSaltCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("clearsalt")
                .requires(source -> source.hasPermission(2)) // Требуется уровень ОП 2
                .executes(ClearSaltCommand::execute));
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        SaltTracker tracker = SaltTracker.get(level);
        int saltCount = tracker.getSaltPositions().size();

        tracker.clearAllSalt(level);

        source.sendSuccess(
                () -> Component.literal("Удалено блоков соли: " + saltCount),
                true
        );

        return saltCount;
    }
}