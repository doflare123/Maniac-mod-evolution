package org.example.maniacrevolution.perk.perks.survivor;

import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import org.example.maniacrevolution.perk.*;
import org.example.maniacrevolution.util.SelectiveGlowingEffect;

import java.util.List;

/**
 * Ментальная Связь (Пассивный) (Выжившие)
 * Подсвечивает всех выживших в режиме Adventure в радиусе 30 блоков
 * только для владельца перка.
 */
public class MentalBondPerk extends Perk {

    // ── Настройки ─────────────────────────────────────────────────────────
    private static final double RADIUS         = 30.0;
    private static final int    GLOW_TICKS     = 40; // обновляем каждые 2 сек с запасом
    private static final int    UPDATE_INTERVAL = 20; // проверяем каждую секунду

    private int tickTimer = 0;

    public MentalBondPerk() {
        super(new Builder("mental_bond")
                .type(PerkType.PASSIVE)
                .team(PerkTeam.SURVIVOR)
                .phases(PerkPhase.ANY)
        );
    }

    // ── Описание ──────────────────────────────────────────────────────────

    @Override
    public Component getDescription() {
        return Component.literal("Подсвечивает союзников-выживших в радиусе ")
                .withStyle(ChatFormatting.WHITE)
                .append(Component.literal((int) RADIUS + " блоков")
                        .withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" только для тебя.")
                        .withStyle(ChatFormatting.WHITE));
    }

    // ── Тик ───────────────────────────────────────────────────────────────

    @Override
    public void onTick(ServerPlayer player) {
        tickTimer++;
        if (tickTimer < UPDATE_INTERVAL) return;
        tickTimer = 0;

        updateGlow(player);
    }

    // ── Применение пассивного эффекта ─────────────────────────────────────

    @Override
    public void applyPassiveEffect(ServerPlayer player) {
        tickTimer = 0;
        updateGlow(player);
    }

    // ── Снятие пассивного эффекта ─────────────────────────────────────────

    @Override
    public void removePassiveEffect(ServerPlayer player) {
        // Убираем подсветку со всех когда перк снимается
        if (player.getServer() == null) return;

        for (ServerPlayer other : player.getServer().getPlayerList().getPlayers()) {
            if (other == player) continue;
            SelectiveGlowingEffect.removeGlowing(other, player);
        }
    }

    // ── Логика обновления подсветки ───────────────────────────────────────

    private void updateGlow(ServerPlayer player) {
        if (player.getServer() == null) return;

        List<ServerPlayer> allPlayers = player.getServer().getPlayerList().getPlayers();

        for (ServerPlayer other : allPlayers) {
            if (other == player) continue;

            // Только выжившие (команда survivors)
            PerkTeam otherTeam = PerkTeam.fromPlayer(other);
            if (otherTeam != PerkTeam.SURVIVOR) continue;

            // Только в режиме Adventure
            if (other.gameMode.getGameModeForPlayer() != GameType.ADVENTURE) continue;

            double dist = player.distanceTo(other);

            if (dist <= RADIUS) {
                // В радиусе — подсвечиваем с запасом тиков
                SelectiveGlowingEffect.addGlowing(other, player, GLOW_TICKS);
            } else {
                // Вышел из радиуса — снимаем подсветку
                SelectiveGlowingEffect.removeGlowing(other, player);
            }
        }
    }
}
