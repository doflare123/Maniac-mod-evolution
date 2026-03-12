package org.example.maniacrevolution.perk.perks.survivor;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.perk.*;

/**
 * Голландский Штурвал (Пассивный) (Выжившие)
 * За каждого союзника заряжающего тот же компьютер — +BONUS_PER_PLAYER% к скорости.
 * Реализация: перк регистрирует игрока как "имеющего перк",
 * HackSession проверяет это при подсчёте очков.
 */
public class DutchHelmPerk extends Perk {

    // ── Настройки ─────────────────────────────────────────────────────────
    public static final float BONUS_PER_PLAYER = 0.04f; // +4% за каждого союзника

    // Множитель применяется в HackSession через hasThisPerk()
    private static final java.util.Set<java.util.UUID> activePlayers =
            java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    public DutchHelmPerk() {
        super(new Builder("dutch_helm")
                .type(PerkType.PASSIVE)
                .team(PerkTeam.SURVIVOR)
                .phases(PerkPhase.ANY)
        );
    }

    // ── Описание ──────────────────────────────────────────────────────────

    @Override
    public Component getDescription() {
        return Component.literal("За каждого союзника заряжающего тот же компьютер: ")
                .withStyle(ChatFormatting.WHITE)
                .append(Component.literal("+" + (int)(BONUS_PER_PLAYER * 100) + "% к скорости зарядки.")
                        .withStyle(ChatFormatting.GREEN));
    }

    // ── Пассивный эффект ──────────────────────────────────────────────────

    @Override
    public void applyPassiveEffect(ServerPlayer player) {
        activePlayers.add(player.getUUID());
    }

    @Override
    public void removePassiveEffect(ServerPlayer player) {
        activePlayers.remove(player.getUUID());
    }

    // ── Статический хелпер для HackSession ───────────────────────────────

    public static boolean hasThisPerk(ServerPlayer player) {
        return activePlayers.contains(player.getUUID());
    }
}
