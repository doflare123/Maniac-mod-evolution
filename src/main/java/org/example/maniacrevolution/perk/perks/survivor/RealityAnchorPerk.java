package org.example.maniacrevolution.perk.perks.survivor;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.game.GameManager;
import org.example.maniacrevolution.ghost.GhostPossessionManager;
import org.example.maniacrevolution.perk.ChargedPerk;
import org.example.maniacrevolution.perk.PerkInstance;
import org.example.maniacrevolution.perk.PerkPhase;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.perk.PerkType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Якорь реальности — зарядный пассивный перк выжившего.
 * Критическое QTE даёт временный заряд, который автоматически расходуется
 * на первое подходящее защитное событие.
 */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public class RealityAnchorPerk extends ChargedPerk {
    public static final String ID = "reality_anchor";
    public static final int CHARGE_DURATION_SECONDS = 60;
    public static final int MAX_CHARGES = 2;
    public static final int MAX_CHARGES_PER_GAME = 2;
    public static final int POSSESSION_REDUCTION_TICKS = 6 * 20;
    public static final int NIGHTMARE_DELAY_TICKS = 20 * 20;
    public static final float CONTROL_DURATION_MULTIPLIER = 0.60F;

    private static final String NIGHTMARE_DELAY_USED = "reality_anchor_nightmare_delay_used";
    private static final Set<UUID> REDUCING_EFFECTS = new HashSet<>();

    public RealityAnchorPerk() {
        super(new Builder(ID)
                        .type(PerkType.CHARGED)
                        .team(PerkTeam.SURVIVOR)
                        .phases(PerkPhase.ANY),
                CHARGE_DURATION_SECONDS, MAX_CHARGES, MAX_CHARGES_PER_GAME);
    }

    @Override
    public Component getDescription() {
        return Component.translatable("perk.maniacrev.reality_anchor.desc",
                CHARGE_DURATION_SECONDS,
                MAX_CHARGES_PER_GAME,
                POSSESSION_REDUCTION_TICKS / 20,
                NIGHTMARE_DELAY_TICKS / 20,
                Math.round((1.0F - CONTROL_DURATION_MULTIPLIER) * 100.0F));
    }

    @Override
    public void onChargeGained(ServerPlayer player, int currentCharges) {
        player.displayClientMessage(Component.literal(
                "§aЯкорь реальности: заряд стабильности " + currentCharges + "/" + MAX_CHARGES), true);
    }

    @Override
    public void onChargeExpired(ServerPlayer player, int currentCharges) {
        player.displayClientMessage(Component.literal(
                "§7Якорь реальности: заряд стабильности рассеялся (" + currentCharges + "/" + MAX_CHARGES + ")"), true);
    }

    /** Вызывается сервером после подтверждённого критического QTE. */
    public static void onCriticalQTE(ServerPlayer player) {
        PerkInstance instance = getActiveInstance(player);
        if (instance != null) {
            instance.grantCharge(player);
        }
    }

    /** Автоматически расходует заряд при начале одержимости. */
    public static void onPossessionStarted(ServerPlayer target) {
        PerkInstance instance = getActiveInstance(target);
        if (instance == null || instance.getChargeCount() == 0) return;
        if (!GhostPossessionManager.shortenPossession(target, POSSESSION_REDUCTION_TICKS)) return;

        instance.consumeCharge(target);
        target.displayClientMessage(Component.literal(
                "§aЯкорь реальности сократил одержимость на 6 секунд."), true);
    }

    /**
     * Возвращает true, если кошмар нужно отложить. Ограничение «один раз за игру»
     * хранится в экземпляре перка и переживает сохранение мира.
     */
    public static boolean tryDelayNightmare(ServerPlayer player) {
        PerkInstance instance = getActiveInstance(player);
        if (instance == null || instance.hasMatchFlag(NIGHTMARE_DELAY_USED)) return false;
        if (!instance.consumeCharge(player)) return false;

        instance.setMatchFlag(NIGHTMARE_DELAY_USED);
        player.displayClientMessage(Component.literal(
                "§aЯкорь реальности удержал рассудок: кошмар отложен на 20 секунд."), true);
        return true;
    }

    @SubscribeEvent
    public static void onControlEffectApplicable(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide) return;
        if (REDUCING_EFFECTS.contains(player.getUUID())) return;

        MobEffectInstance original = event.getEffectInstance();
        if (!isReducibleControlEffect(original.getEffect()) || original.getDuration() <= 1) return;

        PerkInstance instance = getActiveInstance(player);
        if (instance == null || instance.getChargeCount() == 0) return;

        int reducedDuration = Math.max(1,
                (int) Math.ceil(original.getDuration() * CONTROL_DURATION_MULTIPLIER));
        if (reducedDuration >= original.getDuration()) return;

        event.setResult(Event.Result.DENY);
        if (!instance.consumeCharge(player)) return;

        MobEffectInstance reduced = new MobEffectInstance(
                original.getEffect(),
                reducedDuration,
                original.getAmplifier(),
                original.isAmbient(),
                original.isVisible(),
                original.showIcon()
        );

        REDUCING_EFFECTS.add(player.getUUID());
        try {
            player.addEffect(reduced);
        } finally {
            REDUCING_EFFECTS.remove(player.getUUID());
        }

        player.displayClientMessage(Component.literal(
                "§aЯкорь реальности сократил эффект контроля на 40%."), true);
    }

    private static boolean isReducibleControlEffect(MobEffect effect) {
        return effect == ModEffects.FEAR.get()
                || effect == ModEffects.SILENCE.get()
                || effect == ModEffects.STUN.get();
    }

    private static PerkInstance getActiveInstance(ServerPlayer player) {
        if (player == null || GameManager.getCurrentPhase() == null) return null;
        if (player.isCreative() || player.isSpectator()) return null;
        if (PerkTeam.fromPlayer(player) != PerkTeam.SURVIVOR) return null;

        PerkInstance instance = PlayerDataManager.get(player).getPerkInstance(ID);
        return instance != null && instance.getPerk() instanceof RealityAnchorPerk
                ? instance
                : null;
    }
}
