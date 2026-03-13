package org.example.maniacrevolution.perk.perks.survivor;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.perk.*;

/**
 * Голландский Штурвал (Пассивный) (Выжившие)
 * За каждого союзника заряжающего тот же компьютер — +BONUS_PER_PLAYER% к скорости.
 * Работает только у одного владельца перка на компьютер (первый найденный).
 * Не считает самого владельца.
 */
public class DutchHelmPerk extends Perk {

    // ── Настройки ─────────────────────────────────────────────────────────
    // Процент бонуса за каждого союзника (0.04 = 4%)
    public static final float BONUS_PER_PLAYER = 1.04f;

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
                        .withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" Работает только у одного владельца перка на компьютер.")
                        .withStyle(ChatFormatting.GRAY));
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