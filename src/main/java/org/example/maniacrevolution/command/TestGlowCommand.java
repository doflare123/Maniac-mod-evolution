package org.example.maniacrevolution.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.example.maniacrevolution.util.SelectiveGlowingEffect;

public class TestGlowCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("testglow")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("target", EntityArgument.entity())
                        .then(Commands.argument("duration", IntegerArgumentType.integer(1, 600))
                                .then(Commands.argument("forMe", BoolArgumentType.bool())
                                        .executes(context -> {
                                            Entity target = EntityArgument.getEntity(context, "target");
                                            int duration = IntegerArgumentType.getInteger(context, "duration");
                                            boolean forMe = BoolArgumentType.getBool(context, "forMe");

                                            if (!(context.getSource().getEntity() instanceof ServerPlayer viewer)) {
                                                context.getSource().sendFailure(
                                                        net.minecraft.network.chat.Component.literal("§cТолько для игроков!")
                                                );
                                                return 0;
                                            }

                                            if (forMe) {
                                                // Подсвечиваем цель для того, кто активировал команду
                                                SelectiveGlowingEffect.addGlowing(target, viewer, duration * 20);

                                                context.getSource().sendSuccess(() ->
                                                        net.minecraft.network.chat.Component.literal(
                                                                "§a✓ Подсветка применена на §e" + target.getName().getString() +
                                                                        " §7[ID:" + target.getId() + "]§a для §bВАС§a на §e" + duration + "§a сек!"
                                                        ), false
                                                );
                                            } else {
                                                // Подсвечиваем активатора для цели (работает только если цель - игрок)
                                                if (target instanceof ServerPlayer targetPlayer) {
                                                    SelectiveGlowingEffect.addGlowing(viewer, targetPlayer, duration * 20);

                                                    context.getSource().sendSuccess(() ->
                                                            net.minecraft.network.chat.Component.literal(
                                                                    "§a✓ Подсветка применена на §bВАС§a для §e" +
                                                                            targetPlayer.getName().getString() + "§a на §e" + duration + "§a сек!"
                                                            ), false
                                                    );
                                                } else {
                                                    context.getSource().sendFailure(
                                                            net.minecraft.network.chat.Component.literal(
                                                                    "§cЦель должна быть игроком для режима forMe=false!"
                                                            )
                                                    );
                                                    return 0;
                                                }
                                            }

                                            return 1;
                                        })
                                )
                                // Вариант без флага (по умолчанию forMe = true)
                                .executes(context -> {
                                    Entity target = EntityArgument.getEntity(context, "target");
                                    int duration = IntegerArgumentType.getInteger(context, "duration");

                                    if (!(context.getSource().getEntity() instanceof ServerPlayer viewer)) {
                                        context.getSource().sendFailure(
                                                net.minecraft.network.chat.Component.literal("§cТолько для игроков!")
                                        );
                                        return 0;
                                    }

                                    SelectiveGlowingEffect.addGlowing(target, viewer, duration * 20);

                                    context.getSource().sendSuccess(() ->
                                            net.minecraft.network.chat.Component.literal(
                                                    "§a✓ Подсветка применена на §e" + target.getName().getString() +
                                                            " §7[ID:" + target.getId() + "]§a для §bВАС§a на §e" + duration + "§a сек!"
                                            ), false
                                    );

                                    return 1;
                                })
                        )
                )
                // Команда для снятия подсветки
                .then(Commands.literal("remove")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(context -> {
                                    Entity target = EntityArgument.getEntity(context, "target");

                                    if (!(context.getSource().getEntity() instanceof ServerPlayer viewer)) {
                                        context.getSource().sendFailure(
                                                net.minecraft.network.chat.Component.literal("§cТолько для игроков!")
                                        );
                                        return 0;
                                    }

                                    SelectiveGlowingEffect.removeGlowing(target, viewer);

                                    context.getSource().sendSuccess(() ->
                                            net.minecraft.network.chat.Component.literal(
                                                    "§c✗ Подсветка убрана с §e" + target.getName().getString() +
                                                            " §7[ID:" + target.getId() + "]"
                                            ), false
                                    );

                                    return 1;
                                })
                        )
                )
                // Команда для проверки статуса
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