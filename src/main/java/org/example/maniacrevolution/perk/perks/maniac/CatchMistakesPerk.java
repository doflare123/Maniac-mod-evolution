package org.example.maniacrevolution.perk.perks.maniac;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.perk.*;
import org.example.maniacrevolution.util.SelectiveGlowingEffect;

import java.util.*;

/**
 * Ловля на ошибках (Пассивный с КД) (Маньяк)
 * При промахе выжившего в QTE — подсвечивает его только для владельца перка.
 */
public class CatchMistakesPerk extends Perk {

    // ── Настройки ─────────────────────────────────────────────────────────
    private static final int GLOW_DURATION_TICKS = 100; // 5 секунд
    private static final int COOLDOWN_SECONDS     = 30;

    // Маньяки с готовым перком (не на кулдауне)
    private static final Map<UUID, CatchMistakesPerk> readyPlayers = new HashMap<>();

    // Отложенные триггеры для запуска кулдауна
    private static final Map<UUID, Boolean> pendingTriggers = new HashMap<>();

    public CatchMistakesPerk() {
        super(new Builder("catch_mistakes")
                .type(PerkType.PASSIVE_COOLDOWN)
                .team(PerkTeam.MANIAC)
                .phases(PerkPhase.ANY)
                .cooldown(COOLDOWN_SECONDS)
        );
    }

    // ── Описание ──────────────────────────────────────────────────────────

    @Override
    public Component getDescription() {
        return Component.translatable("perk.maniacrev.catch_mistakes.desc",
                GLOW_DURATION_TICKS / 20,
                COOLDOWN_SECONDS);
    }

    // ── Пассивный эффект ──────────────────────────────────────────────────

    @Override
    public void applyPassiveEffect(ServerPlayer player) {
        if (PerkTeam.fromPlayer(player) == PerkTeam.MANIAC) {
            readyPlayers.put(player.getUUID(), this);
        }
    }

    @Override
    public void removePassiveEffect(ServerPlayer player) {
        readyPlayers.remove(player.getUUID());
    }

    // ── Срабатывание при промахе ──────────────────────────────────────────

    /**
     * Вызывается из QTEKeyPressPacket когда выживший промахнулся.
     * Находит первого готового маньяка с перком и подсвечивает промахнувшегося
     * только для него через SelectiveGlowingEffect.
     */
    public static boolean onQTEFailed(ServerPlayer failedPlayer) {
        // Только выжившие могут промахнуться и дать бонус
        PerkTeam failedTeam = PerkTeam.fromPlayer(failedPlayer);
        if (failedTeam != PerkTeam.SURVIVOR) return false;

        if (readyPlayers.isEmpty()) return false;

        // Берём первого готового маньяка
        UUID perkOwnerUUID = readyPlayers.keySet().stream()
                .sorted()
                .findFirst()
                .orElse(null);
        if (perkOwnerUUID == null) return false;

        if (failedPlayer.getServer() == null) return false;
        ServerPlayer perkOwner = failedPlayer.getServer().getPlayerList().getPlayer(perkOwnerUUID);

        if (perkOwner == null) {
            readyPlayers.remove(perkOwnerUUID);
            return false;
        }

        // Подсвечиваем промахнувшегося только для владельца перка
        SelectiveGlowingEffect.addGlowing(failedPlayer, perkOwner, GLOW_DURATION_TICKS);

        // Помечаем для запуска кулдауна
        pendingTriggers.put(perkOwnerUUID, true);

        perkOwner.displayClientMessage(
                Component.literal("👁 Ловля на ошибках: выживший подсвечен!")
                        .withStyle(net.minecraft.ChatFormatting.YELLOW),
                true
        );

        return true;
    }

    // ── PASSIVE_COOLDOWN система ──────────────────────────────────────────

    @Override
    public boolean shouldTrigger(ServerPlayer player) {
        boolean trigger = pendingTriggers.getOrDefault(player.getUUID(), false);
        if (trigger) pendingTriggers.remove(player.getUUID());
        return trigger;
    }

    @Override
    public void onTrigger(ServerPlayer player) {
        // Кулдаун запускается автоматически через PerkInstance
    }
}