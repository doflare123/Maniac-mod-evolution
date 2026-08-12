package org.example.maniacrevolution.perk.perks.maniac;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.flower.FlowerTrailManager;
import org.example.maniacrevolution.perk.Perk;
import org.example.maniacrevolution.perk.PerkPhase;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.perk.PerkType;

public class FlowersLeadInvestigationPerk extends Perk {
    public static final String ID = "flowers_lead_the_investigation";
    public static final int SPAWN_INTERVAL_SECONDS = 6;
    public static final int FLOWER_LIFETIME_SECONDS = 12;
    public static final int SURFACE_SEARCH_DEPTH_BLOCKS = 3;

    private static final int TICKS_PER_SECOND = 20;
    public static final int SPAWN_INTERVAL_TICKS = SPAWN_INTERVAL_SECONDS * TICKS_PER_SECOND;
    public static final int FLOWER_LIFETIME_TICKS = FLOWER_LIFETIME_SECONDS * TICKS_PER_SECOND;

    public FlowersLeadInvestigationPerk() {
        super(new Builder(ID)
                .type(PerkType.PASSIVE)
                .team(PerkTeam.MANIAC)
                .phases(PerkPhase.HUNT, PerkPhase.MIDGAME));
    }

    @Override
    public Component getDescription() {
        return Component.translatable(
                "perk.maniacrev." + ID + ".desc",
                SPAWN_INTERVAL_SECONDS,
                FLOWER_LIFETIME_SECONDS
        );
    }

    @Override
    public void onGameStart(ServerPlayer player) {
        FlowerTrailManager.startMatch(player.server);
    }

    @Override
    public void onPhaseChange(ServerPlayer player, PerkPhase newPhase) {
        if (newPhase == PerkPhase.HUNT) {
            FlowerTrailManager.startMatch(player.server);
        }
    }

    @Override
    public void applyPassiveEffect(ServerPlayer player) {
        FlowerTrailManager.showExistingTrailsTo(player);
    }

    @Override
    public void onTick(ServerPlayer player) {
        FlowerTrailManager.tick(player.server);
    }

    @Override
    public void removePassiveEffect(ServerPlayer player) {
        FlowerTrailManager.hideTrailsFrom(player);
    }
}
