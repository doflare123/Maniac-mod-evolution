package org.example.maniacrevolution.perk.perks.maniac;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import org.example.maniacrevolution.data.PlayerData;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.downed.DownedCapability;
import org.example.maniacrevolution.downed.DownedData;
import org.example.maniacrevolution.downed.DownedState;
import org.example.maniacrevolution.game.GameManager;
import org.example.maniacrevolution.perk.Perk;
import org.example.maniacrevolution.perk.PerkInstance;
import org.example.maniacrevolution.perk.PerkPhase;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.perk.PerkType;
import org.example.maniacrevolution.scream.ScreamManager;

public class GoNextPerk extends Perk {
    public static final String ID = "go_next";
    public static final int HORIZONTAL_RADIUS_BLOCKS = 32;
    public static final int VERTICAL_RANGE_BLOCKS = 12;
    public static final int COOLDOWN_SECONDS = 45;

    private static final double HORIZONTAL_RADIUS_SQUARED =
            (double) HORIZONTAL_RADIUS_BLOCKS * HORIZONTAL_RADIUS_BLOCKS;

    public GoNextPerk() {
        super(new Builder(ID)
                .type(PerkType.PASSIVE_COOLDOWN)
                .team(PerkTeam.MANIAC)
                .phases(PerkPhase.HUNT, PerkPhase.MIDGAME)
                .cooldown(COOLDOWN_SECONDS));
    }

    @Override
    public Component getDescription() {
        return Component.translatable(
                "perk.maniacrev." + ID + ".desc",
                HORIZONTAL_RADIUS_BLOCKS,
                VERTICAL_RANGE_BLOCKS,
                ScreamManager.MARKER_DURATION_SECONDS,
                COOLDOWN_SECONDS
        );
    }

    public static void tryTrigger(ServerPlayer responsibleManiac, ServerPlayer downedSurvivor) {
        if (responsibleManiac == downedSurvivor
                || PerkTeam.fromPlayer(responsibleManiac) != PerkTeam.MANIAC
                || responsibleManiac.isSpectator()) {
            return;
        }

        PerkPhase currentPhase = GameManager.getCurrentPhase();
        if (currentPhase != PerkPhase.HUNT && currentPhase != PerkPhase.MIDGAME) {
            return;
        }

        PlayerData data = PlayerDataManager.get(responsibleManiac);
        PerkInstance instance = data.getPerkInstance(ID);
        if (instance == null || instance.isOnCooldown()) {
            return;
        }

        ServerPlayer screamTarget = findNearestActiveSurvivor(downedSurvivor);
        if (screamTarget == null) {
            return;
        }

        ScreamManager.trigger(screamTarget);
        instance.startCooldown();
        PlayerDataManager.syncToClient(responsibleManiac);
    }

    private static ServerPlayer findNearestActiveSurvivor(ServerPlayer downedSurvivor) {
        ServerPlayer nearest = null;
        double nearestDistanceSquared = Double.MAX_VALUE;

        for (ServerPlayer candidate : downedSurvivor.server.getPlayerList().getPlayers()) {
            if (!isEligibleTarget(candidate, downedSurvivor)) {
                continue;
            }

            double deltaX = candidate.getX() - downedSurvivor.getX();
            double deltaY = candidate.getY() - downedSurvivor.getY();
            double deltaZ = candidate.getZ() - downedSurvivor.getZ();
            double horizontalDistanceSquared = deltaX * deltaX + deltaZ * deltaZ;

            if (horizontalDistanceSquared > HORIZONTAL_RADIUS_SQUARED
                    || Math.abs(deltaY) > VERTICAL_RANGE_BLOCKS) {
                continue;
            }

            double distanceSquared = horizontalDistanceSquared + deltaY * deltaY;
            if (distanceSquared < nearestDistanceSquared) {
                nearestDistanceSquared = distanceSquared;
                nearest = candidate;
            }
        }

        return nearest;
    }

    private static boolean isEligibleTarget(ServerPlayer candidate, ServerPlayer downedSurvivor) {
        if (candidate == downedSurvivor
                || !candidate.level().dimension().equals(downedSurvivor.level().dimension())
                || !candidate.isAlive()
                || candidate.isRemoved()
                || candidate.gameMode.getGameModeForPlayer() != GameType.ADVENTURE
                || PerkTeam.fromPlayer(candidate) != PerkTeam.SURVIVOR) {
            return false;
        }

        DownedData downedData = DownedCapability.get(candidate);
        return downedData == null || downedData.getState() != DownedState.DOWNED;
    }
}
