package org.example.maniacrevolution.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.Maniacrev;

public class ResourcePackCommand {
    private static boolean resourcePackEnabled = true;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("maniacrev")
                .then(Commands.literal("resourcepack")
                        .then(Commands.literal("on")
                                .requires(cs -> cs.hasPermission(2)) // Только OP
                                .executes(ResourcePackCommand::enableResourcePack))
                        .then(Commands.literal("off")
                                .requires(cs -> cs.hasPermission(2)) // Только OP
                                .executes(ResourcePackCommand::disableResourcePack))
                        .then(Commands.literal("status")
                                .requires(cs -> cs.hasPermission(2)) // Только OP
                                .executes(ResourcePackCommand::getStatus))
                )
        );
    }

    private static int enableResourcePack(CommandContext<CommandSourceStack> context) {
        resourcePackEnabled = true;
        Component message = Component.literal("§a✓ Ресурс-пак §aВКЛЮЧЕН");
        context.getSource().sendSuccess(() -> message, true);
        Maniacrev.LOGGER.info("Resource pack enabled");
        return 1;
    }

    private static int disableResourcePack(CommandContext<CommandSourceStack> context) {
        resourcePackEnabled = false;
        Component message = Component.literal("§c✗ Ресурс-пак §cОТКЛЮЧЕН");
        context.getSource().sendSuccess(() -> message, true);
        Maniacrev.LOGGER.info("Resource pack disabled");
        return 1;
    }

    private static int getStatus(CommandContext<CommandSourceStack> context) {
        String status = resourcePackEnabled ? "§a✓ ВКЛЮЧЕН" : "§c✗ ОТКЛЮЧЕН";
        Component message = Component.literal("Статус ресурс-пака: " + status);
        context.getSource().sendSuccess(() -> message, true);
        return 1;
    }

    public static boolean isResourcePackEnabled() {
        return resourcePackEnabled;
    }
}