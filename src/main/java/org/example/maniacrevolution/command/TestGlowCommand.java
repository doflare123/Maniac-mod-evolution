package org.example.maniacrevolution.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.example.maniacrevolution.util.SelectiveGlowingEffect;

import java.util.Collection;

public class TestGlowCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("testglow")
                .requires(source -> source.hasPermission(2))

                // /testglow <цели> <длительность> <наблюдатели>
                .then(Commands.argument("targets", EntityArgument.entities())
                        .then(Commands.argument("duration", IntegerArgumentType.integer(1, 600))
                                .then(Commands.argument("viewers", EntityArgument.players())
                                        .executes(context -> {
                                            Collection<? extends Entity> targets = EntityArgument.getEntities(context, "targets");
                                            int duration = IntegerArgumentType.getInteger(context, "duration");
                                            Collection<ServerPlayer> viewers = EntityArgument.getPlayers(context, "viewers");

                                            // Подсвечиваем все цели для всех указанных наблюдателей
                                            for (Entity target : targets) {
                                                for (ServerPlayer viewer : viewers) {
                                                    SelectiveGlowingEffect.addGlowing(target, viewer, duration * 20);
                                                }
                                            }

                                            context.getSource().sendSuccess(() ->
                                                    net.minecraft.network.chat.Component.literal(
                                                            "§a✓ Подсветка применена на §e" + targets.size() +
                                                                    "§a сущност(ь/ей) для §e" + viewers.size() +
                                                                    "§a игрок(ов) на §e" + duration + "§a сек!"
                                                    ), true
                                            );

                                            return 1;
                                        })
                                )
                                // /testglow <цели> <длительность> (для себя)
                                .executes(context -> {
                                    Collection<? extends Entity> targets = EntityArgument.getEntities(context, "targets");
                                    int duration = IntegerArgumentType.getInteger(context, "duration");

                                    if (!(context.getSource().getEntity() instanceof ServerPlayer viewer)) {
                                        context.getSource().sendFailure(
                                                net.minecraft.network.chat.Component.literal("§cТолько для игроков!")
                                        );
                                        return 0;
                                    }

                                    for (Entity target : targets) {
                                        SelectiveGlowingEffect.addGlowing(target, viewer, duration * 20);
                                    }

                                    context.getSource().sendSuccess(() ->
                                            net.minecraft.network.chat.Component.literal(
                                                    "§a✓ Подсветка применена на §e" + targets.size() +
                                                            "§a сущност(ь/ей) для §bВАС§a на §e" + duration + "§a сек!"
                                            ), false
                                    );

                                    return 1;
                                })
                        )
                )

                // /testglow remove <цели> [наблюдатели]
                .then(Commands.literal("remove")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .then(Commands.argument("viewers", EntityArgument.players())
                                        .executes(context -> {
                                            Collection<? extends Entity> targets = EntityArgument.getEntities(context, "targets");
                                            Collection<ServerPlayer> viewers = EntityArgument.getPlayers(context, "viewers");

                                            for (Entity target : targets) {
                                                for (ServerPlayer viewer : viewers) {
                                                    SelectiveGlowingEffect.removeGlowing(target, viewer);
                                                }
                                            }

                                            context.getSource().sendSuccess(() ->
                                                    net.minecraft.network.chat.Component.literal(
                                                            "§c✗ Подсветка убрана с §e" + targets.size() +
                                                                    "§c сущност(ь/ей) для §e" + viewers.size() + "§c игрок(ов)"
                                                    ), true
                                            );

                                            return 1;
                                        })
                                )
                                .executes(context -> {
                                    Collection<? extends Entity> targets = EntityArgument.getEntities(context, "targets");

                                    if (!(context.getSource().getEntity() instanceof ServerPlayer viewer)) {
                                        context.getSource().sendFailure(
                                                net.minecraft.network.chat.Component.literal("§cТолько для игроков!")
                                        );
                                        return 0;
                                    }

                                    for (Entity target : targets) {
                                        SelectiveGlowingEffect.removeGlowing(target, viewer);
                                    }

                                    context.getSource().sendSuccess(() ->
                                            net.minecraft.network.chat.Component.literal(
                                                    "§c✗ Подсветка убрана с §e" + targets.size() + "§c сущност(ь/ей)"
                                            ), false
                                    );

                                    return 1;
                                })
                        )
                )

                // /testglow removeall <цели>
                .then(Commands.literal("removeall")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .executes(context -> {
                                    Collection<? extends Entity> targets = EntityArgument.getEntities(context, "targets");

                                    for (Entity target : targets) {
                                        SelectiveGlowingEffect.removeAllGlowing(target);
                                    }

                                    context.getSource().sendSuccess(() ->
                                            net.minecraft.network.chat.Component.literal(
                                                    "§c✗ Все подсветки убраны с §e" + targets.size() + "§c сущност(ь/ей)"
                                            ), true
                                    );

                                    return 1;
                                })
                        )
                )

                // /testglow check <цель> (одна сущность для проверки)
                .then(Commands.literal("check")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(context -> {
                                    Entity target = EntityArgument.getEntity(context, "target");

                                    if (!(context.getSource().getEntity() instanceof ServerPlayer viewer)) {
                                        context.getSource().sendFailure(
                                                net.minecraft.network.chat.Component.literal("§cТолько для игроков!")
                                        );
                                        return 0;
                                    }

                                    boolean isGlowing = SelectiveGlowingEffect.isGlowing(target, viewer);

                                    context.getSource().sendSuccess(() ->
                                            net.minecraft.network.chat.Component.literal(
                                                    isGlowing ?
                                                            "§a✓ Сущность §e" + target.getName().getString() +
                                                                    " §7[ID:" + target.getId() + "]§a подсвечена для вас!" :
                                                            "§c✗ Сущность §e" + target.getName().getString() +
                                                                    " §7[ID:" + target.getId() + "]§c НЕ подсвечена для вас."
                                            ), false
                                    );

                                    return 1;
                                })
                        )
                )
        );
    }
}