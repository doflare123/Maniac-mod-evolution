package org.example.maniacrevolution.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.example.maniacrevolution.hack.HackConfig;
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

        HackConfig.HACK_POINTS_REQUIRED = settings.getHackPointsRequired();
        HackConfig.POINTS_PER_PLAYER_PER_SECOND = settings.getPointsPerPlayer();
        HackConfig.POINTS_PER_SPECIALIST_PER_SECOND = settings.getPointsPerSpecialist();
        HackConfig.HACKER_RADIUS = settings.getHackerRadius();
        HackConfig.SUPPORT_RADIUS = settings.getSupportRadius();
        HackConfig.QTE_SUCCESS_BONUS = settings.getQteSuccessBonus();
        HackConfig.QTE_CRIT_BONUS = settings.getQteCritBonus();
        HackConfig.COMPUTERS_NEEDED_FOR_WIN = settings.getComputersNeededForWin();
        HackConfig.QTE_INTERVAL_MIN_SECONDS = settings.getQteIntervalMin();
        HackConfig.QTE_INTERVAL_MAX_SECONDS = settings.getQteIntervalMax();
        HackConfig.MAX_BONUS_PLAYERS = settings.getMaxBonusPlayers();

        
        // Применяем все настройки через команды
        
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
