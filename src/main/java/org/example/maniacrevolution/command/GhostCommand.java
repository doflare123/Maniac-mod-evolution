package org.example.maniacrevolution.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.ghost.GhostLoadoutManager;
import org.example.maniacrevolution.ghost.GhostPossessionManager;

import java.util.Collection;

public class GhostCommand {

    private GhostCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("maniacrev")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("ghost")
                        .then(Commands.literal("possess")
                                .then(Commands.argument("controller", EntityArgument.player())
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .executes(ctx -> {
                                                    ServerPlayer controller = EntityArgument.getPlayer(ctx, "controller");
                                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                                    boolean success = GhostPossessionManager.startPossession(controller, target, true);
                                                    if (!success) {
                                                        ctx.getSource().sendFailure(Component.literal("Не удалось запустить тестовое вселение."));
                                                        return 0;
                                                    }

                                                    ctx.getSource().sendSuccess(() ->
                                                            Component.literal("§dПризрак " + controller.getName().getString()
                                                                    + " захватил тело " + target.getName().getString()), true);
                                                    return 1;
                                                }))))
                        .then(Commands.literal("release")
                                .then(Commands.argument("controller", EntityArgument.player())
                                        .executes(ctx -> {
                                            ServerPlayer controller = EntityArgument.getPlayer(ctx, "controller");
                                            boolean success = GhostPossessionManager.releasePossession(controller, "принудительное завершение");
                                            if (!success) {
                                                ctx.getSource().sendFailure(Component.literal("У этого игрока нет активного вселения."));
                                                return 0;
                                            }

                                            ctx.getSource().sendSuccess(() ->
                                                    Component.literal("§7Вселение игрока " + controller.getName().getString() + " завершено"), true);
                                            return 1;
                                        })))
                        .then(Commands.literal("status")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> {
                                            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                                            ctx.getSource().sendSuccess(() ->
                                                    Component.literal("§7Статус Призрака для " + player.getName().getString()
                                                            + ": " + GhostPossessionManager.getStatus(player)), false);
                                            return 1;
                                        })))
                        .then(Commands.literal("refresh")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> {
                                            Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                                            for (ServerPlayer target : targets) {
                                                GhostLoadoutManager.refreshGhostLoadout(target);
                                            }
                                            ctx.getSource().sendSuccess(() ->
                                                    Component.literal("§dСнаряжение и способности Призрака обновлены для " + targets.size() + " игрок(ов)."), true);
                                            return targets.size();
                                        })))));
    }
}
