package org.example.maniacrevolution.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.fleshheap.FleshHeapData;

import java.util.Collection;

public class ClearAttributesCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("maniacrev")
                        // Очистка атрибутов
                        .then(Commands.literal("clear_attributes")
                                .requires(source -> source.hasPermission(2))
                                .executes(ClearAttributesCommand::clearSelf) // Для себя
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ClearAttributesCommand::clearTargets) // Для указанных игроков
                                )
                        )
                        // Добавление стаков
                        .then(Commands.literal("add_fleshheap")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1000))
                                                .executes(ClearAttributesCommand::addFleshHeap)
                                        )
                                )
                        )
                        // Установка стаков
                        .then(Commands.literal("set_fleshheap")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0, 1000))
                                                .executes(ClearAttributesCommand::setFleshHeap)
                                        )
                                )
                        )
        );
    }

    /**
     * Очистка атрибутов для себя (если команду выполняет игрок)
     */
    private static int clearSelf(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (source.getEntity() instanceof ServerPlayer player) {
            int stacks = FleshHeapData.getStacks(player);
            FleshHeapData.clearStacks(player);

            source.sendSuccess(() -> Component.literal(
                    "§eВсе атрибуты сброшены! §7(Потеряно: " + stacks + " Flesh Heap)"
            ), false);
            return 1;
        } else {
            // Если команду выполняет командный блок без таргета
            source.sendFailure(Component.literal("§cУкажите игрока: /maniacrev clear_attributes @a"));
            return 0;
        }
    }

    /**
     * Очистка атрибутов для указанных игроков
     */
    private static int clearTargets(CommandContext<CommandSourceStack> context) {
        try {
            Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
            int count = 0;

            for (ServerPlayer player : targets) {
                int stacks = FleshHeapData.getStacks(player);
                FleshHeapData.clearStacks(player);

                player.sendSystemMessage(Component.literal(
                        "§cВаши Flesh Heap были сброшены! §7(Потеряно: " + stacks + ")"
                ));
                count++;
            }

            int finalCount = count;
            context.getSource().sendSuccess(() -> Component.literal(
                    "§aСброшены атрибуты у " + finalCount + " игрок(ов)"
            ), true);

            return count;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cОшибка при выполнении команды"));
            return 0;
        }
    }

    /**
     * Добавление стаков Flesh Heap
     */
    private static int addFleshHeap(CommandContext<CommandSourceStack> context) {
        try {
            Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
            int amount = IntegerArgumentType.getInteger(context, "amount");
            int count = 0;

            for (ServerPlayer player : targets) {
                int oldStacks = FleshHeapData.getStacks(player);
                FleshHeapData.addStacks(player, amount);
                int newStacks = FleshHeapData.getStacks(player);

                player.sendSystemMessage(Component.literal(
                        "§a+§f" + amount + " §aFlesh Heap! §7(" + oldStacks + " → " + newStacks + ")"
                ));
                count++;
            }

            int finalCount = count;
            int finalAmount = amount;
            context.getSource().sendSuccess(() -> Component.literal(
                    "§aДобавлено " + finalAmount + " Flesh Heap для " + finalCount + " игрок(ов)"
            ), true);

            return count;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cОшибка при выполнении команды"));
            return 0;
        }
    }

    /**
     * Установка точного количества стаков
     */
    private static int setFleshHeap(CommandContext<CommandSourceStack> context) {
        try {
            Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
            int amount = IntegerArgumentType.getInteger(context, "amount");
            int count = 0;

            for (ServerPlayer player : targets) {
                FleshHeapData.setStacks(player, amount);

                player.sendSystemMessage(Component.literal(
                        "§eFlesh Heap установлено на: §f" + amount
                ));
                count++;
            }

            int finalCount = count;
            int finalAmount = amount;
            context.getSource().sendSuccess(() -> Component.literal(
                    "§aУстановлено " + finalAmount + " Flesh Heap для " + finalCount + " игрок(ов)"
            ), true);

            return count;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cОшибка при выполнении команды"));
            return 0;
        }
    }
}