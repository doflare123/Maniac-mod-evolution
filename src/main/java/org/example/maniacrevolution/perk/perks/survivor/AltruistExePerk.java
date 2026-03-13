package org.example.maniacrevolution.perk.perks.survivor;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.downed.DownedCapability;
import org.example.maniacrevolution.downed.DownedData;
import org.example.maniacrevolution.downed.DownedState;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.mana.ManaProvider;
import org.example.maniacrevolution.perk.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Альтруист.exe (Пассивный) (Выжившие)
 * После подъёма союзника — накладывает AltruistBoostEffect на DURATION_SEC секунд.
 * Пока эффект активен — +HACK_BONUS% к скорости взлома.
 */
@Mod.EventBusSubscriber
public class AltruistExePerk extends Perk {

    // ── Настройки ─────────────────────────────────────────────────────────
    public static final float HACK_BONUS   = 0.05f;
    private static final int  DURATION_SEC = 45;
    private static final int  COOLDOWN_SEC = 90;
    private static final float MANA_COST   = 5f;

    private static final Set<UUID> activePlayers =
            Collections.synchronizedSet(new HashSet<>());

    private static final Map<UUID, UUID> pendingHelpers = new ConcurrentHashMap<>();

    public AltruistExePerk() {
        super(new Builder("altruist_exe")
                .type(PerkType.PASSIVE_COOLDOWN)
                .team(PerkTeam.SURVIVOR)
                .phases(PerkPhase.ANY)
                .cooldown(COOLDOWN_SEC)
                .manaCost(MANA_COST)
        );
    }

    @Override
    public Component getDescription() {
        return Component.literal("После подъёма союзника получаешь эффект ")
                .withStyle(ChatFormatting.WHITE)
                .append(Component.literal("Альтруист.exe")
                        .withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" на " + DURATION_SEC + " сек. — ")
                        .withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("+" + (int)(HACK_BONUS * 100) + "% к скорости взлома.")
                        .withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" КД: " + COOLDOWN_SEC + " сек. Стоимость: ")
                        .withStyle(ChatFormatting.WHITE))
                .append(Component.literal((int) MANA_COST + " маны.")
                        .withStyle(ChatFormatting.AQUA));
    }

    @Override
    public void applyPassiveEffect(ServerPlayer player) {
        activePlayers.add(player.getUUID());
    }

    @Override
    public void removePassiveEffect(ServerPlayer player) {
        activePlayers.remove(player.getUUID());
        pendingHelpers.remove(player.getUUID());
        player.removeEffect(ModEffects.ALTRUIST_BOOST.get());
    }

    @SubscribeEvent
    public static void onPlayerInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer helper)) return;
        if (!(event.getTarget() instanceof ServerPlayer target)) return;
        if (!activePlayers.contains(helper.getUUID())) return;

        DownedData targetData = DownedCapability.get(target);
        if (targetData == null || targetData.getState() != DownedState.DOWNED) return;

        pendingHelpers.put(helper.getUUID(), target.getUUID());
    }

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
        if (data == null || data.getState() != DownedState.DOWNED) {
            pendingHelpers.remove(uuid);
            grantBonus(player);
        }
    }

    private static void grantBonus(ServerPlayer player) {
        boolean hasMana = player.getCapability(ManaProvider.MANA)
                .map(m -> {
                    if (m.getMana() >= MANA_COST) {
                        m.consumeMana(MANA_COST);
                        return true;
                    }
                    return false;
                }).orElse(false);

        if (!hasMana) {
            player.displayClientMessage(
                    Component.literal("Альтруист.exe: недостаточно маны!")
                            .withStyle(ChatFormatting.RED), true);
            return;
        }

        player.addEffect(new MobEffectInstance(
                ModEffects.ALTRUIST_BOOST.get(),
                DURATION_SEC * 20,
                0,
                false, true, true
        ));

        player.displayClientMessage(
                Component.literal("Альтруист.exe: +5% к скорости взлома на " + DURATION_SEC + " сек!")
                        .withStyle(ChatFormatting.GREEN), true);
    }

    public static boolean hasActiveBonus(ServerPlayer player) {
        return player.hasEffect(ModEffects.ALTRUIST_BOOST.get());
    }
}