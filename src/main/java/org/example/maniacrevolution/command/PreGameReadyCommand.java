package org.example.maniacrevolution.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.example.maniacrevolution.pregame.PreGameReadyManager;

/**
 * Команды для управления прелобби-готовностью:
 *   /maniacrev reset_pregame_ready  — сбросить готовность всем
 *   /maniacrev give_pregame_ready   — выдать предмет "Готово" всем
 */
public class PreGameReadyCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("maniacrev")
                .then(Commands.literal("reset_pregame_ready")
                    .requires(src -> src.hasPermission(2))
                    .executes(ctx -> {
                        PreGameReadyManager.resetAll(ctx.getSource().getServer());
                        ctx.getSource().sendSuccess(
                                () -> Component.literal("§aГотовность всех игроков сброшена."), true);
                        return 1;
                    })
                )
                .then(Commands.literal("give_pregame_ready")
                    .requires(src -> src.hasPermission(2))
                    .executes(ctx -> {
                        PreGameReadyManager.giveToAll(ctx.getSource().getServer());
                        ctx.getSource().sendSuccess(
                                () -> Component.literal("§aПредмет 'Готово' выдан всем игрокам."), true);
                        return 1;
                    })
                )
        );
    }
}
