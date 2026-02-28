package org.example.maniacrevolution.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.capability.PlagueCapability;
import org.example.maniacrevolution.capability.PlagueCapabilityProvider;

import java.util.Collection;

/**
 * Команда /clearplaguedata — очищает накопленные данные чумы у игроков.
 *
 * Использование:
 *   /clearplaguedata @a          — очистить всех
 *   /clearplaguedata @e[...]     — очистить по селектору
 *   /clearplaguedata PlayerName  — очистить конкретного игрока
 *
 * Работает в командных блоках (требует уровень 2).
 * Зарегистрирован через @Mod.EventBusSubscriber — дополнительной регистрации не нужно.
 */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClearPlagueCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("clearplaguedata")
                        // Уровень 2 — работает в командных блоках и у операторов
                        .requires(source -> source.hasPermission(2))

                        // /clearplaguedata <targets>
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> execute(ctx,
                                        EntityArgument.getPlayers(ctx, "targets")))
                        )

                        // /clearplaguedata — без аргументов очищает того, кто пишет команду
                        .executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();
                            // Проверяем что источник — игрок (не командный блок без цели)
                            ServerPlayer player = source.getPlayerOrException();
                            return execute(ctx, java.util.List.of(player));
                        })
        );
    }

    private static int execute(CommandContext<CommandSourceStack> ctx,
                               Collection<ServerPlayer> targets) {
        CommandSourceStack source = ctx.getSource();
        int successCount = 0;

        for (ServerPlayer player : targets) {
            PlagueCapability cap = PlagueCapabilityProvider.get(player);
            if (cap == null) continue;

            // Сбрасываем накопленные тики
            cap.setAccumulatedTicks(0);
            // Синхронизируем с клиентом (обнуляем полоску в HUD)
            cap.syncToClient(player);

            // Снимаем эффект чумы если висит
            player.removeEffect(
                    org.example.maniacrevolution.effect.ModEffects.PLAGUE.get()
            );

            successCount++;
        }

        // Сообщение в чат источнику команды
        if (successCount == 1) {
            source.sendSuccess(() -> Component.literal(
                    "§aДанные чумы очищены у §f" + targets.iterator().next().getName().getString()
            ), true);
        } else if (successCount > 1) {
            int finalSuccessCount = successCount;
            source.sendSuccess(() -> Component.literal(
                    "§aДанные чумы очищены у §f" + finalSuccessCount + " §aигроков"
            ), true);
        } else {
            source.sendFailure(Component.literal("§cНе найдено подходящих игроков"));
        }

        // Возвращаем количество затронутых игроков (стандарт для команд MC)
        return successCount;
    }
}