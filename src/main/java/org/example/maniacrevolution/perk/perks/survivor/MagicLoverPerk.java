package org.example.maniacrevolution.perk.perks.survivor;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.downed.DownedCapability;
import org.example.maniacrevolution.downed.DownedData;
import org.example.maniacrevolution.downed.DownedState;
import org.example.maniacrevolution.mana.ManaProvider;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.SyncManaPacket;
import org.example.maniacrevolution.perk.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Любовник Магии (Пассивный) (Выжившие)
 * После подъёма союзника — получает MANA_REWARD ед. маны.
 * КД: COOLDOWN_SEC секунд.
 */
@Mod.EventBusSubscriber
public class MagicLoverPerk extends Perk {

    // ── Настройки ─────────────────────────────────────────────────────────
    private static final float MANA_REWARD  = 10f;
    private static final int   COOLDOWN_SEC = 90;

    private static final Set<UUID> activePlayers =
            Collections.synchronizedSet(new HashSet<>());

    // helper uuid -> target uuid (кого поднимает)
    private static final Map<UUID, UUID> pendingHelpers = new ConcurrentHashMap<>();

    public MagicLoverPerk() {
        super(new Builder("magic_lover")
                .type(PerkType.PASSIVE_COOLDOWN)
                .team(PerkTeam.SURVIVOR)
                .phases(PerkPhase.ANY)
                .cooldown(COOLDOWN_SEC)
        );
    }

    // ── Описание ──────────────────────────────────────────────────────────

    @Override
    public Component getDescription() {
        return Component.literal("После подъёма союзника получаешь ")
                .withStyle(ChatFormatting.WHITE)
                .append(Component.literal((int) MANA_REWARD + " ед. маны.")
                        .withStyle(ChatFormatting.AQUA))
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
        pendingHelpers.remove(player.getUUID());
    }

    // ── Отслеживаем начало подъёма ────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer helper)) return;
        if (!(event.getTarget() instanceof ServerPlayer target)) return;
        if (!activePlayers.contains(helper.getUUID())) return;

        DownedData targetData = DownedCapability.get(target);
        if (targetData == null || targetData.getState() != DownedState.DOWNED) return;

        // Запоминаем кого поднимает
        pendingHelpers.put(helper.getUUID(), target.getUUID());
    }

    // ── Тик: проверяем завершение подъёма ─────────────────────────────────

    @Override
    public void onTick(ServerPlayer player) {
        UUID uuid = player.getUUID();
        UUID targetUUID = pendingHelpers.get(uuid);
        if (targetUUID == null || player.getServer() == null) return;

        ServerPlayer target = player.getServer().getPlayerList().getPlayer(targetUUID);
        if (target == null) {
            pendingHelpers.remove(uuid);
            return;
        }

        DownedData data = DownedCapability.get(target);
        // Цель уже не в нокдауне — подъём завершён
        if (data == null || data.getState() != DownedState.DOWNED) {
            pendingHelpers.remove(uuid);
            grantMana(player);
        }
    }

    // ── Выдача маны ───────────────────────────────────────────────────────

    private static void grantMana(ServerPlayer player) {
        player.getCapability(ManaProvider.MANA).ifPresent(mana -> {
            mana.addMana(MANA_REWARD);

            ModNetworking.sendToPlayer(
                    new SyncManaPacket(mana.getMana(), mana.getMaxMana(), mana.getTotalRegenRate()),
                    player
            );
        });

        player.displayClientMessage(
                Component.literal("✨ Богиня Магии довольна! +" + (int) MANA_REWARD + " маны")
                        .withStyle(ChatFormatting.LIGHT_PURPLE),
                true
        );
    }
}
