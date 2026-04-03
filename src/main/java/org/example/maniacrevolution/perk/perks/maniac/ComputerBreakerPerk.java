package org.example.maniacrevolution.perk.perks.maniac;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.hack.HackConfig;
import org.example.maniacrevolution.hack.HackManager;
import org.example.maniacrevolution.perk.*;

/**
 * Ломание компа (Активный) (Маньяк, Мидгейм)
 * Отнимает ROLLBACK_PERCENT от макс. прогресса у самого заряженного
 * незавершённого компьютера. Прогресс не уходит ниже 0.
 */
public class ComputerBreakerPerk extends Perk {

    // ── Настройки ─────────────────────────────────────────────────────────
    private static final int   COOLDOWN_SECONDS = 180;
    private static final float ROLLBACK_PERCENT = 0.5f; // 50% от макс. значения
    private static final float MANA_COST        = 10f;

    public ComputerBreakerPerk() {
        super(new Builder("computer_breaker")
                .type(PerkType.ACTIVE)
                .team(PerkTeam.MANIAC)
                .phases(PerkPhase.MIDGAME)
                .manaCost(MANA_COST)
                .cooldown(COOLDOWN_SECONDS)
        );
    }

    // ── Описание (RU + EN через локализационный ключ не используем —
    //    значения берутся из кода напрямую) ────────────────────────────────

    @Override
    public Component getDescription() {
        return Component.translatable(
                "perk.maniacrev.computer_breaker.desc",
                (int)(ROLLBACK_PERCENT * 100),
                COOLDOWN_SECONDS,
                (int) MANA_COST
        );
    }

    // ── Активация ─────────────────────────────────────────────────────────

    @Override
    public void onActivate(ServerPlayer player) {
        // Находим самый заряженный незавершённый комп
        int targetId = HackManager.get().getMostProgressedComputer();

        if (targetId == -1) {
            player.displayClientMessage(
                    Component.literal("Нет компьютеров с прогрессом!")
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return;
        }

        float progressBefore = HackManager.get().getProgress(targetId);
        boolean success = HackManager.get().rollbackComputerById(player, targetId, ROLLBACK_PERCENT);

        if (success) {
            float progressAfter = HackManager.get().getProgress(targetId);
            float maxPts = HackConfig.HACK_POINTS_REQUIRED;

            player.displayClientMessage(
                    Component.literal("💥 Комп #" + targetId + ": ")
                            .withStyle(ChatFormatting.DARK_RED)
                            .append(Component.literal(
                                            String.format("%.0f%%", progressBefore / maxPts * 100)
                                                    + " → "
                                                    + String.format("%.0f%%", progressAfter / maxPts * 100))
                                    .withStyle(ChatFormatting.RED)),
                    true
            );
        }
    }
}