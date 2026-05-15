package org.example.maniacrevolution.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.example.maniacrevolution.hack.HackConfig;
import org.example.maniacrevolution.settings.GameSettings;

public class ApplySettingsCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("maniacrev")
            .then(Commands.literal("apply_settings")
                .requires(source -> source.hasPermission(2))
                .executes(ApplySettingsCommand::applySettingsCommand)
            )
        );
    }

    private static int applySettingsCommand(CommandContext<CommandSourceStack> context) {
        applySettings(context.getSource().getServer(), false);

        context.getSource().sendSuccess(() ->
            Component.literal("§aВсе настройки применены!"), true);

        return 1;
    }

    public static void applySettings(MinecraftServer server, boolean suppressOutput) {
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

        CommandSourceStack source = suppressOutput
                ? server.createCommandSourceStack().withSuppressedOutput().withMaximumPermission(4)
                : server.createCommandSourceStack();

        // Применяем все настройки через команды

        // 3. Количество маньяков
        server.getCommands().performPrefixedCommand(
            source,
            "scoreboard players set maniacCount game " + settings.getManiacCount()
        );

        // 4. Время игры (конвертируем минуты в секунды)
        int timeInSeconds = settings.getGameTime() * 60;
        server.getCommands().performPrefixedCommand(
            source,
            "maniacrev timer maxtime " + timeInSeconds
        );

        // 5. HP Boost (применяем через нашу команду)
        server.getCommands().performPrefixedCommand(
            source,
            "maniacrev hp_boost"
        );

        // 6. Карта (устанавливаем туда же, откуда её читает игровая логика)
        Scoreboard scoreboard = server.getScoreboard();
        Objective mapObjective = scoreboard.getObjective("map");
        if (mapObjective == null) {
            mapObjective = scoreboard.addObjective(
                    "map",
                    ObjectiveCriteria.DUMMY,
                    Component.literal("Map"),
                    ObjectiveCriteria.RenderType.INTEGER
            );
        }
        scoreboard.getOrCreatePlayerScore("Game", mapObjective).setScore(settings.getSelectedMap());
    }
}
