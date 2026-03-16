package org.example.maniacrevolution.perk.perks.maniac;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.hack.HackManager;
import org.example.maniacrevolution.hack.HackSession;
import org.example.maniacrevolution.perk.*;
import org.example.maniacrevolution.util.SelectiveGlowingEffect;

import java.util.List;
import java.util.Set;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;

/**
 * Семейный Раздор (Пассивный с КД) (Маньяк)
 * Когда 2+ выживших взламывают один компьютер — подсвечивает их для владельца перка.
 * Срабатывает с кулдауном.
 */
public class FamilyDiscordPerk extends Perk {

    // ── Настройки ─────────────────────────────────────────────────────────
    private static final int COOLDOWN_SEC  = 60;
    private static final int GLOW_DURATION = 5 * 20; // 5 секунд подсветки

    private static final Set<UUID> activePlayers =
            Collections.synchronizedSet(new HashSet<>());

    public FamilyDiscordPerk() {
        super(new Builder("family_discord")
                .type(PerkType.PASSIVE_COOLDOWN)
                .team(PerkTeam.MANIAC)
                .phases(PerkPhase.ANY)
                .cooldown(COOLDOWN_SEC)
        );
    }

    // ── Описание ──────────────────────────────────────────────────────────

    @Override
    public Component getDescription() {
        return Component.literal("Когда ")
                .withStyle(ChatFormatting.WHITE)
                .append(Component.literal("2+ выживших")
                        .withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" взламывают один компьютер — подсвечивает их для тебя на ")
                        .withStyle(ChatFormatting.WHITE))
                .append(Component.literal(GLOW_DURATION / 20 + " сек.")
                        .withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" КД: " + COOLDOWN_SEC + " сек.")
                        .withStyle(ChatFormatting.WHITE));
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

    // ── Проверка условия срабатывания ─────────────────────────────────────

    @Override
    public boolean shouldTrigger(ServerPlayer player) {
        // Ищем сессию с 2+ участниками
        for (HackSession session : HackManager.get().getActiveSessions().values()) {
            List<ServerPlayer> participants = session.getAllParticipants();
            if (participants.size() >= 2) {
                return true;
            }
        }
        return false;
    }

    // ── Срабатывание ──────────────────────────────────────────────────────

    @Override
    public void onTrigger(ServerPlayer player) {
        int highlighted = 0;

        for (HackSession session : HackManager.get().getActiveSessions().values()) {
            List<ServerPlayer> participants = session.getAllParticipants();
            if (participants.size() < 2) continue;

            // Подсвечиваем всех участников для владельца перка
            for (ServerPlayer target : participants) {
                if (target == player) continue;
                SelectiveGlowingEffect.addGlowing(target, player, GLOW_DURATION);
                highlighted++;
            }
        }

        if (highlighted > 0) {
            player.displayClientMessage(
                    Component.literal("👁 Семейный Раздор: подсвечено " + highlighted + " выживших!")
                            .withStyle(ChatFormatting.DARK_RED),
                    true
            );
        }
    }
}
