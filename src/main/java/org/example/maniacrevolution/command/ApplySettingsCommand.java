package org.example.maniacrevolution.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.example.maniacrevolution.settings.GameSettings;

public class ApplySettingsCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("maniacrev")
            .then(Commands.literal("apply_settings")
                .requires(source -> source.hasPermission(2))
                .executes(ApplySettingsCommand::applySettings)
            )
        );
    }

    private static int applySettings(CommandContext<CommandSourceStack> context) {
        MinecraftServer server = context.getSource().getServer();
        GameSettings settings = GameSettings.get(server);
        
        // Применяем все настройки через команды
        
        // 1. Количество компьютеров
        server.getCommands().performPrefixedCommand(
            server.createCommandSourceStack(),
            "scoreboard players set Game allGoal " + settings.getComputerCount()
        );
        
        // 2. Очки для хака
        server.getCommands().performPrefixedCommand(
            server.createCommandSourceStack(),
            "scoreboard players set Game hackGoal " + settings.getHackPoints()
        );
        
        // 3. Количество маньяков
        server.getCommands().performPrefixedCommand(
            server.createCommandSourceStack(),
            "scoreboard players set maniacCount game " + settings.getManiacCount()
        );
        
        // 4. Время игры (конвертируем минуты в секунды)
        int timeInSeconds = settings.getGameTime() * 60;
        server.getCommands().performPrefixedCommand(
            server.createCommandSourceStack(),
            "maniacrev timer maxtime " + timeInSeconds
        );
        
        // 5. HP Boost (применяем через нашу команду)
        server.getCommands().performPrefixedCommand(
            server.createCommandSourceStack(),
            "maniacrev hp_boost"
        );
        
        // 6. Карта (устанавливаем в scoreboard)
        server.getCommands().performPrefixedCommand(
            server.createCommandSourceStack(),
            "scoreboard players set selectedMap game " + settings.getSelectedMap()
        );
        
        context.getSource().sendSuccess(() -> 
            Component.literal("§aВсе настройки применены!"), true);
        
        return 1;
    }
}
