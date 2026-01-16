package org.example.maniacrevolution.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.system.Agent47MoneyManager;

import java.util.Collection;

/**
 * Команды для управления деньгами Агента 47
 * /maniacrev agent_money <add|set|remove|clear> <игрок> [количество]
 */
public class Agent47MoneyCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("maniacrev")
                .then(Commands.literal("agent_money")
                        .requires(source -> source.hasPermission(2)) // Требуется OP уровень 2

                        // /maniacrev agent_money add <игрок> <количество>
                        .then(Commands.literal("add")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(context -> addMoney(context))
                                        )
                                )
                        )

                        // /maniacrev agent_money set <игрок> <количество>
                        .then(Commands.literal("set")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                .executes(context -> setMoney(context))
                                        )
                                )
                        )

                        // /maniacrev agent_money remove <игрок> <количество>
                        .then(Commands.literal("remove")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(context -> removeMoney(context))
                                        )
                                )
                        )

                        // /maniacrev agent_money clear <игрок>
                        .then(Commands.literal("clear")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> clearMoney(context))
                                )
                        )

                        // /maniacrev agent_money query <игрок>
                        .then(Commands.literal("query")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> queryMoney(context))
                                )
                        )
                )
        );
    }

    /**
     * Добавляет деньги игроку
     */
    private static int addMoney(CommandContext<CommandSourceStack> context) {
        try {
            Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
            int amount = IntegerArgumentType.getInteger(context, "amount");

            for (ServerPlayer player : targets) {
                int oldBalance = Agent47MoneyManager.getMoney(player);
                Agent47MoneyManager.addMoney(player, amount);
                int newBalance = Agent47MoneyManager.getMoney(player);

                // Сообщение игроку
                player.displayClientMessage(
                        Component.literal(String.format("§a+%d монет. Баланс: %d", amount, newBalance)),
                        false
                );
            }

            // Сообщение отправителю команды
            context.getSource().sendSuccess(
                    () -> Component.literal(String.format("§aДобавлено %d монет для %d игрок(ов)",
                            amount, targets.size())),
                    true
            );

            return targets.size();
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cОшибка выполнения команды: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Устанавливает баланс игрока
     */
    private static int setMoney(CommandContext<CommandSourceStack> context) {
        try {
            Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
            int amount = IntegerArgumentType.getInteger(context, "amount");

            for (ServerPlayer player : targets) {
                Agent47MoneyManager.setMoney(player, amount);

                // Сообщение игроку
                player.displayClientMessage(
                        Component.literal(String.format("§eБаланс установлен: %d монет", amount)),
                        false
                );
            }

            // Сообщение отправителю команды
            context.getSource().sendSuccess(
                    () -> Component.literal(String.format("§aБаланс установлен на %d для %d игрок(ов)",
                            amount, targets.size())),
                    true
            );

            return targets.size();
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cОшибка выполнения команды: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Убирает деньги у игрока
     */
    private static int removeMoney(CommandContext<CommandSourceStack> context) {
        try {
            Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
            int amount = IntegerArgumentType.getInteger(context, "amount");

            int successCount = 0;
            for (ServerPlayer player : targets) {
                boolean success = Agent47MoneyManager.removeMoney(player, amount);

                if (success) {
                    successCount++;
                    int newBalance = Agent47MoneyManager.getMoney(player);

                    // Сообщение игроку
                    player.displayClientMessage(
                            Component.literal(String.format("§c-%d монет. Баланс: %d", amount, newBalance)),
                            false
                    );
                } else {
                    // Недостаточно денег
                    player.displayClientMessage(
                            Component.literal("§cНедостаточно монет для списания!"),
                            false
                    );
                }
            }

            // Сообщение отправителю команды
            final int finalSuccessCount = successCount;
            final int finalAmount = amount;
            final int targetSize = targets.size();
            context.getSource().sendSuccess(
                    () -> Component.literal(String.format("§aУбрано %d монет у %d из %d игрок(ов)",
                            finalAmount, finalSuccessCount, targetSize)),
                    true
            );

            return successCount;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cОшибка выполнения команды: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Обнуляет баланс игрока
     */
    private static int clearMoney(CommandContext<CommandSourceStack> context) {
        try {
            Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");

            for (ServerPlayer player : targets) {
                Agent47MoneyManager.setMoney(player, 0);

                // Сообщение игроку
                player.displayClientMessage(
                        Component.literal("§cВаш баланс обнулен!"),
                        false
                );
            }

            // Сообщение отправителю команды
            context.getSource().sendSuccess(
                    () -> Component.literal(String.format("§aБаланс обнулен для %d игрок(ов)", targets.size())),
                    true
            );

            return targets.size();
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cОшибка выполнения команды: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Показывает баланс игрока
     */
    private static int queryMoney(CommandContext<CommandSourceStack> context) {
        try {
            Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");

            for (ServerPlayer player : targets) {
                int balance = Agent47MoneyManager.getMoney(player);

                context.getSource().sendSuccess(
                        () -> Component.literal(String.format("§e%s: §f%d монет",
                                player.getName().getString(), balance)),
                        false
                );
            }

            return targets.size();
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cОшибка выполнения команды: " + e.getMessage()));
            return 0;
        }
    }
}