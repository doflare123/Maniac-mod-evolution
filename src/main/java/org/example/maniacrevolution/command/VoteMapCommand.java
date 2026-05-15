package org.example.maniacrevolution.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.example.maniacrevolution.map.MapData;
import org.example.maniacrevolution.map.MapRegistry;
import org.example.maniacrevolution.map.MapVotingManager;
import org.example.maniacrevolution.settings.GameSettings;

public class VoteMapCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("maniacrev")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("votemap")
                        .then(Commands.argument("duration", IntegerArgumentType.integer(10, 300))
                                .executes(context -> {
                                    int duration = IntegerArgumentType.getInteger(context, "duration");
                                    return startVoting(context.getSource(), duration);
                                }))
                        .executes(context -> startVoting(context.getSource(), 60)) // По умолчанию 60 секунд
                )
        );
    }

    private static int startVoting(CommandSourceStack source, int duration) {
        int selectedMap = GameSettings.get(source.getServer()).getSelectedMap();
        if (selectedMap != 0) {
            MapData map = MapRegistry.getMapByNumericId(selectedMap);
            if (map == null) {
                source.sendFailure(Component.literal("§cВыбранная карта не найдена: ID " + selectedMap));
                return 0;
            }

            Scoreboard scoreboard = source.getServer().getScoreboard();
            Objective mapObjective = scoreboard.getObjective("map");
            if (mapObjective == null) {
                mapObjective = scoreboard.addObjective(
                        "map",
                        ObjectiveCriteria.DUMMY,
                        Component.literal("Map"),
                        ObjectiveCriteria.RenderType.INTEGER
                );
            }

            // Ставим карту на следующий тик, чтобы значение не было затёрто
            // datаpack-командами, если votemap вызван внутри mcfunction.
            source.getServer().execute(() -> {
                Scoreboard delayedScoreboard = source.getServer().getScoreboard();
                Objective delayedMapObjective = delayedScoreboard.getObjective("map");
                if (delayedMapObjective == null) {
                    delayedMapObjective = delayedScoreboard.addObjective(
                            "map",
                            ObjectiveCriteria.DUMMY,
                            Component.literal("Map"),
                            ObjectiveCriteria.RenderType.INTEGER
                    );
                }
                delayedScoreboard.getOrCreatePlayerScore("Game", delayedMapObjective).setScore(map.getNumericId());
            });

            source.sendSuccess(() -> Component.literal(
                    "§aКарта выбрана из настроек: §e" + map.getName()), true);
            return 1;
        }

        MapVotingManager.getInstance().startVoting(source.getServer(), duration);
        source.sendSuccess(() -> Component.literal("Голосование за карту запущено на " + duration + " секунд!"), true);
        return 1;
    }
}
