package org.example.maniacrevolution.perk.perks.maniac;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import org.example.maniacrevolution.bud.BudDispatcherManager;
import org.example.maniacrevolution.perk.Perk;
import org.example.maniacrevolution.perk.PerkPhase;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.perk.PerkType;

/**
 * Показывает владельцу цветочную маркировку компьютеров и открывает сводку
 * их визуальных стадий увядания.
 */
public class BudDispatcherPerk extends Perk {
    public static final String ID = "bud_dispatcher";
    public static final int COMPUTER_COUNT = 9;
    public static final int FLOWER_VARIANT_COUNT = 9;
    public static final int WILT_STAGE_COUNT = 6;
    public static final int WILT_STAGE_PERCENT = 20;
    public static final int MAX_PROGRESS_PERCENT = 100;
    public static final float MENU_MANA_COST = 3.0F;

    private static final float PROGRESS_BOUNDARY_EPSILON = 0.0001F;

    public BudDispatcherPerk() {
        super(new Builder(ID)
                .type(PerkType.HYBRID)
                .team(PerkTeam.MANIAC)
                .phases(PerkPhase.HUNT, PerkPhase.MIDGAME)
                .manaCost(MENU_MANA_COST));
    }

    @Override
    public Component getDescription() {
        return Component.translatable(
                "perk.maniacrev." + ID + ".desc",
                COMPUTER_COUNT,
                WILT_STAGE_PERCENT,
                (int) MENU_MANA_COST
        );
    }

    @Override
    public void onGameStart(ServerPlayer player) {
        BudDispatcherManager.startMatch(player.server);
    }

    @Override
    public void applyPassiveEffect(ServerPlayer player) {
        BudDispatcherManager.syncFullState(player, false);
    }

    @Override
    public void onActivate(ServerPlayer player) {
        BudDispatcherManager.syncFullState(player, true);
    }

    @Override
    public void onTick(ServerPlayer player) {
        BudDispatcherManager.ensurePlayerSynchronized(player);
    }

    public static int getWiltStage(float normalizedProgress) {
        float progress = Mth.clamp(normalizedProgress, 0.0F, 1.0F);
        int percent = Mth.floor(progress * MAX_PROGRESS_PERCENT + PROGRESS_BOUNDARY_EPSILON);
        if (percent >= MAX_PROGRESS_PERCENT) {
            return WILT_STAGE_COUNT - 1;
        }
        return Mth.clamp(percent / WILT_STAGE_PERCENT, 0, WILT_STAGE_COUNT - 2);
    }
}
