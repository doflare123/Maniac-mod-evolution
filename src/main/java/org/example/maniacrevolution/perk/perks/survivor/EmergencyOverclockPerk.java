package org.example.maniacrevolution.perk.perks.survivor;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.hack.HackConfig;
import org.example.maniacrevolution.hack.HackManager;
import org.example.maniacrevolution.perk.Perk;
import org.example.maniacrevolution.perk.PerkPhase;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.perk.PerkType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Аварийный разгон — активный перк выжившего для рискованного рывка во время взлома.
 * Пока разгон активен, владелец не даёт обычных пассивных очков, а результат его QTE
 * напрямую и значительно меняет прогресс текущего компьютера.
 */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public class EmergencyOverclockPerk extends Perk {
    public static final String ID = "emergency_overclock";
    public static final int DURATION_SECONDS = 8;
    public static final int SILENCE_DURATION_SECONDS = 5;
    public static final float SUCCESS_BONUS_FRACTION = 0.20F;
    public static final float CRITICAL_BONUS_FRACTION = 0.50F;
    public static final float FAILURE_PENALTY_POINTS = 0.8F;

    private static final int COOLDOWN_SECONDS = 75;
    private static final float MANA_COST = 15.0F;
    private static final Map<UUID, Long> ACTIVE_UNTIL_TICK = new ConcurrentHashMap<>();

    public EmergencyOverclockPerk() {
        super(new Builder(ID)
                .type(PerkType.ACTIVE)
                .team(PerkTeam.SURVIVOR)
                .phases(PerkPhase.ANY)
                .cooldown(COOLDOWN_SECONDS)
                .manaCost(MANA_COST));
    }

    @Override
    public Component getDescription() {
        return Component.translatable("perk.maniacrev.emergency_overclock.desc",
                DURATION_SECONDS,
                Math.round(SUCCESS_BONUS_FRACTION * 100.0F),
                Math.round(CRITICAL_BONUS_FRACTION * 100.0F),
                FAILURE_PENALTY_POINTS,
                SILENCE_DURATION_SECONDS);
    }

    @Override
    public boolean meetsActivationCondition(ServerPlayer player) {
        return HackManager.get().isParticipatingInActiveHack(player);
    }

    @Override
    public void onActivate(ServerPlayer player) {
        ACTIVE_UNTIL_TICK.put(
                player.getUUID(),
                player.level().getGameTime() + DURATION_SECONDS * 20L);

        player.displayClientMessage(
                Component.translatable("perk.maniacrev.emergency_overclock.activated", DURATION_SECONDS)
                        .withStyle(ChatFormatting.GOLD),
                true);
    }

    @Override
    public void onGameStart(ServerPlayer player) {
        ACTIVE_UNTIL_TICK.remove(player.getUUID());
    }

    /** Возвращает true только в пределах серверного восьмисекундного окна разгона. */
    public static boolean isActive(ServerPlayer player) {
        if (player == null) return false;

        UUID playerId = player.getUUID();
        Long activeUntil = ACTIVE_UNTIL_TICK.get(playerId);
        if (activeUntil == null) return false;

        if (player.level().getGameTime() >= activeUntil) {
            ACTIVE_UNTIL_TICK.remove(playerId, activeUntil);
            return false;
        }
        return true;
    }

    /** Увеличивает динамическую награду QTE из HackConfig на процент перка. */
    public static float getQteBonusPoints(boolean critical) {
        float baseBonus = critical
                ? HackConfig.QTE_CRIT_BONUS
                : HackConfig.QTE_SUCCESS_BONUS;
        float perkBonusFraction = critical
                ? CRITICAL_BONUS_FRACTION
                : SUCCESS_BONUS_FRACTION;
        return baseBonus * (1.0F + perkBonusFraction);
    }

    /**
     * Применяет риск проваленного перегруженного QTE. Возвращает false для устаревшего
     * клиентского результата, если игрок уже не участвует в активной сессии взлома.
     */
    public static boolean onQteFailed(ServerPlayer player) {
        if (!isActive(player)) return false;
        if (!HackManager.get().applyQTEPenalty(player, FAILURE_PENALTY_POINTS)) return false;

        player.addEffect(new MobEffectInstance(
                ModEffects.SILENCE.get(),
                SILENCE_DURATION_SECONDS * 20,
                0,
                false,
                true,
                true));
        player.displayClientMessage(
                Component.translatable(
                                "perk.maniacrev.emergency_overclock.failed",
                                FAILURE_PENALTY_POINTS,
                                SILENCE_DURATION_SECONDS)
                        .withStyle(ChatFormatting.RED),
                true);
        return true;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ACTIVE_UNTIL_TICK.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        ACTIVE_UNTIL_TICK.clear();
    }
}
