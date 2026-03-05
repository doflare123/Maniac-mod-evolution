package org.example.maniacrevolution.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.example.maniacrevolution.entity.TotemEntity;

/**
 * Команды шамана. Вызываются из ModCommands.register().
*/
public class ShamanCommands {

    /**
     * Регистрирует ветку "totem" в существующий /maniacrev dispatcher.
     *
     * Вызвать из ModCommands.register(dispatcher) так:
     *   ShamanCommands.register(dispatcher);
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Регистрируем как отдельный top-level литерал maniacrev.
        // Brigadier автоматически мержит одинаковые литералы —
        // ветка "totem" просто добавится к уже существующему /maniacrev.
        dispatcher.register(
                Commands.literal("maniacrev")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("totem")
                                .then(Commands.literal("killall")
                                        .executes(ctx -> killAllTotems(ctx.getSource()))
                                )
                        )
        );
    }

    private static int killAllTotems(CommandSourceStack src) {
        if (src.getServer() == null) return 0;
        int killed = 0;
        for (ServerLevel level : src.getServer().getAllLevels()) {
            for (Entity e : level.getAllEntities()) {
                if (e instanceof TotemEntity totem) {
                    totem.forceKill(); // discard() обходит isInvulnerableTo
                    killed++;
                }
            }
        }
        final int n = killed;
        src.sendSuccess(() -> Component.literal("§aУбито тотемов: §f" + n), true);
        return n;
    }
}