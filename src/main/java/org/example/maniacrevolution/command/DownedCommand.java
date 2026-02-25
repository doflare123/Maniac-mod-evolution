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
import org.example.maniacrevolution.downed.DownedCapability;
import org.example.maniacrevolution.downed.DownedData;
import org.example.maniacrevolution.downed.DownedEventHandler;
import org.example.maniacrevolution.downed.DownedState;

public class DownedCommand {

    /**
     * Регистрирует ветку .then(Commands.literal("downed") ...)
     * Вызывается из ModCommands.register() внутри дерева /maniacrev
     */
    public static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("downed")

                // /maniacrev downed revive <targets>
                // Моментально поднимает лежачих игроков
                .then(Commands.literal("revive")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(DownedCommand::executeRevive)))

                // /maniacrev downed reset <targets>
                // Сбрасывает состояние — игрок снова может упасть
                .then(Commands.literal("reset")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(DownedCommand::executeReset)))

                // /maniacrev downed system enable|disable
                // Глобальное включение/выключение системы лежания
                .then(Commands.literal("system")
                        .then(Commands.literal("enable")
                                .executes(ctx -> executeSystem(ctx, true)))
                        .then(Commands.literal("disable")
                                .executes(ctx -> executeSystem(ctx, false))));
    }

    // ── revive ────────────────────────────────────────────────────────────

    private static int executeRevive(CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {

        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        int count = 0;

        for (ServerPlayer player : targets) {
            DownedData data = DownedCapability.get(player);
            if (data == null || data.getState() != DownedState.DOWNED) {
                ctx.getSource().sendFailure(
                        Component.literal("§e" + player.getName().getString() +
                                " §cне лежит — пропущен"));
                continue;
            }
            DownedEventHandler.instantRevive(player);
            count++;
        }

        int finalCount = count;
        if (finalCount > 0) {
            ctx.getSource().sendSuccess(
                    () -> Component.literal("§aМоментально поднято §e" + finalCount + " §aигроков"),
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

    // ── system enable/disable ─────────────────────────────────────────────

    private static int executeSystem(CommandContext<CommandSourceStack> ctx, boolean enable) {
        DownedEventHandler.SYSTEM_ENABLED = enable;

        if (enable) {
            ctx.getSource().sendSuccess(
                    () -> Component.literal("§a✔ Система лежания §2включена"),
                    true
            );
        } else {
            ctx.getSource().sendSuccess(
                    () -> Component.literal("§c✘ Система лежания §4отключена§c — все игроки будут умирать как обычно"),
                    true
            );
        }
        return 1;
    }
}