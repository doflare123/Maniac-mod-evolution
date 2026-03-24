package org.example.maniacrevolution.perk.perks.maniac;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import org.example.maniacrevolution.perk.*;
import org.example.maniacrevolution.util.SelectiveGlowingEffect;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Подсветка (Пассивный) (Маньяк)
 * При взломе компа подсвечивает N случайных выживших только для маньяков с перком.
 * N = количество маньяков с этим перком.
 */
public class HighlightPerk extends Perk {

    // ── Настройки ─────────────────────────────────────────────────────────
    private static final int GLOW_DURATION_TICKS = 120; // 6 секунд

    // Маньяки с активным перком: uuid -> игрок кэш
    private static final Set<UUID> activeManiacs =
            Collections.synchronizedSet(new HashSet<>());

    public HighlightPerk() {
        super(new Builder("highlight")
                .type(PerkType.PASSIVE)
                .team(PerkTeam.MANIAC)
                .phases(PerkPhase.ANY)
        );
    }

    // ── Описание ──────────────────────────────────────────────────────────

    @Override
    public Component getDescription() {
        return Component.translatable("perk.maniacrev.highlight.desc", GLOW_DURATION_TICKS / 20);
    }

    // ── Пассивный эффект ──────────────────────────────────────────────────

    @Override
    public void applyPassiveEffect(ServerPlayer player) {
        PerkTeam team = PerkTeam.fromPlayer(player);
        if (team == PerkTeam.MANIAC) {
            activeManiacs.add(player.getUUID());
        }
    }

    @Override
    public void removePassiveEffect(ServerPlayer player) {
        activeManiacs.remove(player.getUUID());
    }

    // ── Вызывается из HackManager.onComputerHacked ────────────────────────

    /**
     * Подсвечивает N случайных выживших только для маньяков с перком.
     * N = количество маньяков с этим перком онлайн.
     * Вызывается из HackManager.onComputerHacked().
     */
    public static void onComputerHacked(MinecraftServer server) {
        if (server == null) return;

        // Актуализируем список маньяков
        List<ServerPlayer> maniacs = activeManiacs.stream()
                .map(uuid -> server.getPlayerList().getPlayer(uuid))
                .filter(p -> p != null && PerkTeam.fromPlayer(p) == PerkTeam.MANIAC)
                .collect(Collectors.toList());

        // Обновляем set — убираем отключившихся
        activeManiacs.retainAll(maniacs.stream()
                .map(ServerPlayer::getUUID)
                .collect(Collectors.toSet()));

        if (maniacs.isEmpty()) return;

        // Получаем выживших в Adventure
        List<ServerPlayer> survivors = server.getPlayerList().getPlayers().stream()
                .filter(p -> {
                    PerkTeam team = PerkTeam.fromPlayer(p);
                    if (team != PerkTeam.SURVIVOR) return false;
                    return p.gameMode.getGameModeForPlayer() == GameType.ADVENTURE;
                })
                .collect(Collectors.toList());

        if (survivors.isEmpty()) return;

        // N = количество маньяков с перком, но не больше числа выживших
        int count = Math.min(maniacs.size(), survivors.size());

        // Перемешиваем и берём первых N
        List<ServerPlayer> shuffled = new ArrayList<>(survivors);
        Collections.shuffle(shuffled);
        List<ServerPlayer> targets = shuffled.subList(0, count);

        // Подсвечиваем каждому маньяку с перком
        for (ServerPlayer maniac : maniacs) {
            for (ServerPlayer target : targets) {
                SelectiveGlowingEffect.addGlowing(target, maniac, GLOW_DURATION_TICKS);
            }
        }
    }

    public static int getActiveCount() {
        return activeManiacs.size();
    }
}