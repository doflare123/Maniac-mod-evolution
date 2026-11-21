package org.example.maniacrevolution.necromancer;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import org.example.maniacrevolution.character.CharacterType;
import org.example.maniacrevolution.character.survivor.NecromancerClass;
import org.example.maniacrevolution.game.GameManager;
import org.example.maniacrevolution.util.PlayerModeUtil;

public final class NecromancerResurrection {
    private NecromancerResurrection() {}

    public static boolean canCast(ServerPlayer player) {
        if (GameManager.getCurrentPhase() == null || !player.isAlive() || player.hasDisconnected()
                || !PlayerModeUtil.isSurvivalOrAdventure(player) || !isSurvivor(player)) {
            return false;
        }

        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard.getObjective(CharacterType.SURVIVOR.getScoreboardName());
        String playerName = player.getScoreboardName();
        return objective != null && scoreboard.hasPlayerScore(playerName, objective)
                && scoreboard.getOrCreatePlayerScore(playerName, objective).getScore() == NecromancerClass.SCOREBOARD_ID;
    }

    public static boolean isEligibleTarget(ServerPlayer player) {
        return !player.hasDisconnected() && player.isSpectator() && isSurvivor(player);
    }

    public static void clearPending(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.getCapability(NecromancerProvider.NECROMANCER).ifPresent(NecromancerData::clearPendingResurrection);
        }
    }

    private static boolean isSurvivor(ServerPlayer player) {
        Team team = player.getTeam();
        return team != null && "survivors".equalsIgnoreCase(team.getName());
    }
}
