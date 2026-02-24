package org.example.maniacrevolution.command;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.downed.DownedCapability;
import org.example.maniacrevolution.downed.DownedData;
import org.example.maniacrevolution.downed.DownedEventHandler;
import org.example.maniacrevolution.downed.DownedState;

import java.util.Collection;

public class DownedCommand {

    /**
     * Регистрирует ветку .then(Commands.literal("downed") ...) внутри /maniacrev
     * Вызывается из ModCommands.register()
     */
    public static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("downed")

                // /maniacrev downed revive <targets>
                // Моментально поднимает одного игрока или @a
                .then(Commands.literal("revive")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(DownedCommand::executeRevive)))

                // /maniacrev downed reset <targets>
                // Откатывает состояние — игрок снова может «упасть»
                .then(Commands.literal("reset")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(DownedCommand::executeReset)));
    }

    // ── revive ────────────────────────────────────────────────────────────

    private static int executeRevive(CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {

        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        int count = 0;

        for (ServerPlayer player : targets) {
            DownedData data = DownedCapability.get(player);

            if (data.getState() != DownedState.DOWNED) {
                ctx.getSource().sendFailure(
                        Component.literal("§e" + player.getName().getString() +
                                " §cне лежит — revive пропущен"));
                continue;
            }

            DownedEventHandler.instantRevive(player);
            count++;
        }

        int finalCount = count;
        if (finalCount > 0) {
            ctx.getSource().sendSuccess(
                    () -> Component.literal("§aМоментально поднято: §e" + finalCount + " §aигроков"),
                    true
            );
        }
        return finalCount;
    }

    // ── reset ─────────────────────────────────────────────────────────────

    private static int executeReset(CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {

        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");

        for (ServerPlayer player : targets) {
            DownedEventHandler.resetPlayer(player);
        }

        int count = targets.size();
        ctx.getSource().sendSuccess(
                () -> Component.literal("§aСброшено состояние лежания у §e" + count + " §aигроков"),
                true
        );
        return count;
    }
}
