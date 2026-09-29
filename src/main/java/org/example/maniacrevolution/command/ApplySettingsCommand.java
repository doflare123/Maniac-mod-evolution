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
import org.example.maniacrevolution.game.GameManager;
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
        applySettings(server, suppressOutput, true);
    }

    public static void applySettings(MinecraftServer server, boolean suppressOutput, boolean applyMap) {
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

        // The datapack reads these scores before assigning teams and starting the timer.
        setScore(server, "game", "maniacCount", settings.getManiacCount());
        setScore(server, "timerMax", "Game", settings.getGameTime() * 60);
        // Extra health is an exact HP attribute modifier; leave only class health
        // effects to the legacy datapack, avoiding a second bonus from hp_boost.
        setScore(server, "game", "hpBoost", 0);
        if (applyMap && (settings.getSelectedMap() != 0 || GameManager.getPhaseValue() == 0)) {
            setScore(server, "map", "Game", settings.getSelectedMap());
        }

        GameManager.applyConfiguredDuration(settings.getGameTime() * 60);
        HpBoostCommand.applyHpBoost(server);
    }

    private static void setScore(MinecraftServer server, String objectiveName, String holder, int value) {
        Scoreboard scoreboard = server.getScoreboard();
        Objective objective = scoreboard.getObjective(objectiveName);
        if (objective == null) {
            objective = scoreboard.addObjective(objectiveName, ObjectiveCriteria.DUMMY,
                    Component.literal(objectiveName), ObjectiveCriteria.RenderType.INTEGER);
        }
        scoreboard.getOrCreatePlayerScore(holder, objective).setScore(value);
    }
}
