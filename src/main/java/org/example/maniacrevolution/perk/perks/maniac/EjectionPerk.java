package org.example.maniacrevolution.perk.perks.maniac;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.downed.DownedCapability;
import org.example.maniacrevolution.downed.DownedData;
import org.example.maniacrevolution.downed.DownedState;
import org.example.maniacrevolution.hack.HackManager;
import org.example.maniacrevolution.mana.ManaProvider;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.SyncManaPacket;
import org.example.maniacrevolution.perk.*;

import java.util.Set;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;

/**
 * Выброс (Пассивный с КД) (Маньяк)
 * Когда маньяк кладёт выжившего в нокдаун или убивает —
 * ближайший компьютер откатывается на ROLLBACK_PERCENT.
 * Требует прогресс > 0 на компьютере.
 */
@Mod.EventBusSubscriber
public class EjectionPerk extends Perk {

    // ── Настройки ─────────────────────────────────────────────────────────
    private static final float ROLLBACK_PERCENT = 0.05f; // 5%
    private static final int   COOLDOWN_SEC     = 20;
    private static final float MANA_COST        = 2f;

    private static final Set<UUID> activePlayers =
            Collections.synchronizedSet(new HashSet<>());

    public EjectionPerk() {
        super(new Builder("ejection")
                .type(PerkType.PASSIVE_COOLDOWN)
                .team(PerkTeam.MANIAC)
                .phases(PerkPhase.ANY)
                .cooldown(COOLDOWN_SEC)
                .manaCost(MANA_COST)
        );
    }

    // ── Описание ──────────────────────────────────────────────────────────

    @Override
    public Component getDescription() {
        return Component.literal("Когда кладёшь выжившего или убиваешь — ")
                .withStyle(ChatFormatting.WHITE)
                .append(Component.literal("ближайший компьютер откатывается на "
                        + (int)(ROLLBACK_PERCENT * 100) + "%.")
                        .withStyle(ChatFormatting.RED))
                .append(Component.literal(" Только если прогресс > 0. КД: " + COOLDOWN_SEC + " сек. Стоимость: ")
                        .withStyle(ChatFormatting.WHITE))
                .append(Component.literal((int) MANA_COST + " маны.")
                        .withStyle(ChatFormatting.AQUA));
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

    // ── Обработчик: выживший упал в нокдаун ──────────────────────────────

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        if (event.isCanceled()) return;

        // Жертва должна быть выжившим
        PerkTeam victimTeam = PerkTeam.fromPlayer(victim);
        if (victimTeam != PerkTeam.SURVIVOR) return;

        // Атакующий должен быть маньяком с этим перком
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;
        if (!activePlayers.contains(attacker.getUUID())) return;

        // Проверяем что этот удар приведёт к нокдауну
        // (жертва получит летальный урон и у неё ещё не использован второй шанс)
        DownedData victimData = DownedCapability.get(victim);
        if (victimData == null) return;
        if (victimData.hasUsedSecondChance()) return; // уже использовал — не нокдаун
        if (victimData.getState() == DownedState.DOWNED) return; // уже лежит

        float health = victim.getHealth();
        float damage = event.getAmount();

        // Удар будет летальным — значит выживший упадёт
        if (health - damage <= 0) {
            pendingTrigger(attacker);
        }
    }

    // ── Обработчик: выживший умирает (не нокдаун) ────────────────────────

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;

        PerkTeam victimTeam = PerkTeam.fromPlayer(victim);
        if (victimTeam != PerkTeam.SURVIVOR) return;

        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;
        if (!activePlayers.contains(attacker.getUUID())) return;

        // Выживший умер (уже использовал второй шанс или последний)
        pendingTrigger(attacker);
    }

    // ── Отложенный триггер ────────────────────────────────────────────────

    private static final Set<UUID> pendingTriggers =
            Collections.synchronizedSet(new HashSet<>());

    private static void pendingTrigger(ServerPlayer attacker) {
        pendingTriggers.add(attacker.getUUID());
    }

    @Override
    public boolean shouldTrigger(ServerPlayer player) {
        if (pendingTriggers.contains(player.getUUID())) {
            pendingTriggers.remove(player.getUUID());
            return true;
        }
        return false;
    }

    @Override
    public void onTrigger(ServerPlayer player) {
        // Проверяем ману
        boolean hasMana = player.getCapability(ManaProvider.MANA).map(mana -> {
            if (mana.getMana() >= MANA_COST) {
                mana.consumeMana(MANA_COST);
                ModNetworking.sendToPlayer(
                        new SyncManaPacket(mana.getMana(), mana.getMaxMana(), mana.getTotalRegenRate()),
                        player);
                return true;
            }
            return false;
        }).orElse(false);

        if (!hasMana) {
            player.displayClientMessage(
                    Component.literal("Выброс: недостаточно маны!")
                            .withStyle(ChatFormatting.RED), true);
            return;
        }

        boolean success = HackManager.get().rollbackNearestComputer(player, ROLLBACK_PERCENT);

        if (success) {
            player.displayClientMessage(
                    Component.literal("💥 Выброс! Ближайший компьютер откатился на "
                            + (int)(ROLLBACK_PERCENT * 100) + "%!")
                            .withStyle(ChatFormatting.DARK_RED), true);

            // Частицы у маньяка
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                        player.getX(), player.getY() + 1.0, player.getZ(),
                        5, 0.3, 0.3, 0.3, 0.05);
            }

            player.level().playSound(null,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GENERIC_EXPLODE,
                    SoundSource.PLAYERS, 0.5f, 1.5f);
        }
        // Если нет компьютера с прогрессом > 0 — просто молчим
    }
}
