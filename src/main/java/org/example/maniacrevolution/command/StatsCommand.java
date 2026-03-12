package org.example.maniacrevolution.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.stats.StatsManager;

/**
 * Команды статистики.
 *
 *   /maniacrev sendstats <0|1>
 *       0 = победили выжившие
 *       1 = победили маньяки
 *       Требует прав оператора (level 2)
 *
 *   /maniacrev offsendstats
 *       Любой игрок может отказаться от сбора статистики
 *
 * Регистрация (в ModCommands.register или HackCommands.register):
 *   StatsCommand.register(dispatcher);
 */
public class StatsCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
                Commands.literal("maniacrev")

                        // /maniacrev sendstats <0|1> — только оператор
                        .then(Commands.literal("sendstats")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.argument("winner", IntegerArgumentType.integer(0, 1))
                                        .executes(ctx -> {
                                            int winner = IntegerArgumentType.getInteger(ctx, "winner");
                                            return sendStats(ctx.getSource(), winner);
                                        })))

                        // /maniacrev offsendstats — любой игрок
                        .then(Commands.literal("offsendstats")
                                .executes(ctx -> optOut(ctx.getSource())))
        );
    }

    // ── /maniacrev sendstats <0|1> ────────────────────────────────────────

    private static int sendStats(CommandSourceStack source, int winner) {
        var server = source.getServer();

        source.sendSuccess(() -> Component.literal(
                "§e[Stats] §fСбор статистики игры... (победили "
                        + (winner == 0 ? "§aвыжившие" : "§cманьяки") + "§f)"), false);

        StatsManager.sendStats(server, winner).thenAccept(gameId -> {
            // Ответ приходит в async потоке — планируем сообщение на серверный тик
            server.execute(() -> {
                if (gameId > 0) {
                    source.sendSuccess(() -> Component.literal(
                            "§a[Stats] §fСтатистика сохранена! ID игры: §e#" + gameId), false);
                } else {
                    source.sendFailure(Component.literal(
                            "§c[Stats] Ошибка при сохранении статистики. Проверьте логи сервера."));
                }
            });
        });

        return 1;
    }

    // ── /maniacrev offsendstats ───────────────────────────────────────────

    private static int optOut(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Эту команду может использовать только игрок."));
            return 0;
        }

        StatsManager.optOut(player);

        source.sendSuccess(() -> Component.literal(
                "§7[Stats] §fВы отказались от сбора статистики. Ваши данные больше не будут собираться."), false);
        return 1;
    }
}