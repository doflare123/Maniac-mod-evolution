package org.example.maniacrevolution.perk.perks.maniac;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.paint.PaintPuddleManager;
import org.example.maniacrevolution.perk.Perk;
import org.example.maniacrevolution.perk.PerkPhase;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.perk.PerkType;

public class ThePaintThickensPerk extends Perk {
    public static final String ID = "the_paint_thickens";

    public static final int PAINT_CAN_USES = 3;
    public static final double PUDDLE_TRIGGER_RADIUS_BLOCKS = 0.5D;
    public static final int QTE_ATTEMPTS = 3;
    public static final int QTE_SCORE_MISS = 0;
    public static final int QTE_SCORE_NEAR = 1;
    public static final int QTE_SCORE_HIT = 2;
    public static final int QTE_TOTAL_DURATION_MILLISECONDS = 2000;
    public static final double QTE_ATTEMPT_DURATION_MILLISECONDS =
            QTE_TOTAL_DURATION_MILLISECONDS / (double) QTE_ATTEMPTS;
    public static final int MISS_ZONE_PERCENT = 30;
    public static final int NEAR_ZONE_PERCENT = 50;
    public static final int HIT_ZONE_PERCENT = 20;
    public static final int MISS_MAX_SCORE = 2;
    public static final int NEAR_MAX_SCORE = 4;
    public static final int NEAR_MIN_SCORE = MISS_MAX_SCORE + 1;
    public static final int HIT_MIN_SCORE = NEAR_MAX_SCORE + 1;
    public static final int MAX_QTE_SCORE = QTE_ATTEMPTS * QTE_SCORE_HIT;
    public static final int STUN_DURATION_SECONDS = 2;
    public static final int SLOWDOWN_PERCENT = 10;
    public static final int SLOWDOWN_DURATION_SECONDS = 1;
    public static final int PROTECTION_DURATION_SECONDS = 30;
    public static final int SCREEN_PAINT_DURATION_SECONDS = 5;

    public static final int TICKS_PER_SECOND = 20;
    public static final int STUN_DURATION_TICKS = STUN_DURATION_SECONDS * TICKS_PER_SECOND;
    public static final int SLOWDOWN_DURATION_TICKS = SLOWDOWN_DURATION_SECONDS * TICKS_PER_SECOND;
    public static final int PROTECTION_DURATION_TICKS = PROTECTION_DURATION_SECONDS * TICKS_PER_SECOND;
    public static final int SCREEN_PAINT_DURATION_TICKS = SCREEN_PAINT_DURATION_SECONDS * TICKS_PER_SECOND;
    public static final int QTE_TOTAL_DURATION_TICKS =
            QTE_TOTAL_DURATION_MILLISECONDS * TICKS_PER_SECOND / 1000;

    public ThePaintThickensPerk() {
        super(new Builder(ID)
                .type(PerkType.PASSIVE)
                .team(PerkTeam.ALL)
                .phases(PerkPhase.ANY));
    }

    @Override
    public Component getDescription() {
        return Component.translatable(
                "perk.maniacrev." + ID + ".desc",
                PAINT_CAN_USES,
                PUDDLE_TRIGGER_RADIUS_BLOCKS,
                QTE_ATTEMPTS,
                QTE_TOTAL_DURATION_MILLISECONDS / 1000.0D,
                MISS_ZONE_PERCENT,
                NEAR_ZONE_PERCENT,
                HIT_ZONE_PERCENT,
                MISS_MAX_SCORE,
                STUN_DURATION_SECONDS,
                NEAR_MIN_SCORE,
                NEAR_MAX_SCORE,
                SLOWDOWN_PERCENT,
                SLOWDOWN_DURATION_SECONDS,
                HIT_MIN_SCORE,
                MAX_QTE_SCORE,
                SCREEN_PAINT_DURATION_SECONDS,
                PROTECTION_DURATION_SECONDS
        );
    }

    @Override
    public void onGameStart(ServerPlayer player) {
        PaintPuddleManager.beginForOwner(player);
    }

    @Override
    public void removePassiveEffect(ServerPlayer player) {
        PaintPuddleManager.removeOwner(player);
    }
}
