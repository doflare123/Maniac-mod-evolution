package org.example.maniacrevolution.perk.perks.maniac;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.hack.HackManager;
import org.example.maniacrevolution.perk.*;

import java.util.Set;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;

/**
 * Грифер (Пассивный с КД) (Маньяк)
 * После того как выжившие завершают взлом компьютера —
 * самый заряженный незаблокированный комп блокируется на BLOCK_SEC секунд.
 */
@Mod.EventBusSubscriber
public class GrieferPerk extends Perk {

    // ── Настройки ─────────────────────────────────────────────────────────
    private static final int BLOCK_SEC    = 30;  // время блокировки
    private static final int COOLDOWN_SEC = 150;

    private static final Set<UUID> activePlayers =
            Collections.synchronizedSet(new HashSet<>());

    // Ожидающие триггеры (после завершения хака)
    private static final Set<UUID> pendingTriggers =
            Collections.synchronizedSet(new HashSet<>());

    public GrieferPerk() {
        super(new Builder("griefer")
                .type(PerkType.PASSIVE_COOLDOWN)
                .team(PerkTeam.MANIAC)
                .phases(PerkPhase.ANY)
                .cooldown(COOLDOWN_SEC)
        );
    }

    // ── Описание ──────────────────────────────────────────────────────────

    @Override
    public Component getDescription() {
        return Component.literal("После взлома компьютера выжившими — ")
                .withStyle(ChatFormatting.WHITE)
                .append(Component.literal("самый заряженный компьютер")
                        .withStyle(ChatFormatting.RED))
                .append(Component.literal(" блокируется на ")
                        .withStyle(ChatFormatting.WHITE))
                .append(Component.literal(BLOCK_SEC + " сек.")
                        .withStyle(ChatFormatting.RED))
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
        pendingTriggers.remove(player.getUUID());
    }

    // ── Вызывается из HackManager когда компьютер взломан ────────────────

    /**
     * Помечает всех маньяков с этим перком для срабатывания.
     * Вызывается из HackManager.onComputerHacked().
     */
    public static void onComputerHacked(net.minecraft.server.MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (activePlayers.contains(player.getUUID())) {
                pendingTriggers.add(player.getUUID());
            }
        }
    }

    // ── Условие срабатывания ──────────────────────────────────────────────

    @Override
    public boolean shouldTrigger(ServerPlayer player) {
        if (pendingTriggers.contains(player.getUUID())) {
            pendingTriggers.remove(player.getUUID());
            return true;
        }
        return false;
    }

    // ── Срабатывание ──────────────────────────────────────────────────────

    @Override
    public void onTrigger(ServerPlayer player) {
        int targetId = HackManager.get().getMostProgressedComputer();

        if (targetId == -1) {
            // Нет подходящего компьютера
            return;
        }

        HackManager.get().blockComputer(targetId, BLOCK_SEC, player.getServer());

        player.displayClientMessage(
                Component.literal("🔒 Грифер: компьютер #" + targetId
                        + " заблокирован на " + BLOCK_SEC + " сек!")
                        .withStyle(ChatFormatting.DARK_RED),
                true
        );

        // Оповещаем всех выживших
        if (player.getServer() != null) {
            for (ServerPlayer other : player.getServer().getPlayerList().getPlayers()) {
                PerkTeam team = PerkTeam.fromPlayer(other);
                if (team == PerkTeam.SURVIVOR) {
                    other.displayClientMessage(
                            Component.literal("🔒 Компьютер заблокирован на " + BLOCK_SEC + " сек!")
                                    .withStyle(ChatFormatting.RED),
                            true
                    );
                }
            }
        }
    }
}
