package org.example.maniacrevolution.perk.perks.maniac;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.downed.DownedCapability;
import org.example.maniacrevolution.downed.DownedData;
import org.example.maniacrevolution.downed.DownedState;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.flower.FlowerTrailManager;
import org.example.maniacrevolution.flower.FlowerVariant;
import org.example.maniacrevolution.game.GameManager;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.BouquetPacket;
import org.example.maniacrevolution.perk.Perk;
import org.example.maniacrevolution.perk.PerkPhase;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.perk.PerkType;
import org.example.maniacrevolution.util.ManiacDamageAttribution;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Первый удар по уникальному выжившему добавляет его цветок в единый букет.
 * Следующий нокдаун автоматически расходует весь эффект букета.
 */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public class BouquetToTheOtherSidePerk extends Perk {
    public static final String ID = "bouquet_to_the_other_side";
    public static final int MAX_FLOWERS = 3;
    public static final int MODIFIER_PERCENT_PER_FLOWER = 20;

    private static final int DISPLAYED_LEVEL_OFFSET = 1;
    private static final int EFFECT_REFRESH_DURATION_TICKS = 10;
    private static final int EFFECT_REFRESH_THRESHOLD_TICKS = 5;
    private static final int HUD_SYNC_INTERVAL_TICKS = 20;
    private static final float PERCENT_DENOMINATOR = 100.0F;
    private static final float MINIMUM_REAL_DAMAGE = 0.0F;
    private static final double COLLECT_SOURCE_HEIGHT_BLOCKS = 1.05D;
    private static final double COLLECT_TARGET_HEIGHT_BLOCKS = 1.25D;
    private static final double KNOCKDOWN_BURST_HEIGHT_BLOCKS = 0.45D;

    private static final Map<UUID, LinkedHashMap<UUID, FlowerVariant>> COLLECTED_FLOWERS =
            new LinkedHashMap<>();

    public BouquetToTheOtherSidePerk() {
        super(new Builder(ID)
                .type(PerkType.PASSIVE)
                .team(PerkTeam.MANIAC)
                .phases(PerkPhase.HUNT, PerkPhase.MIDGAME, PerkPhase.REVERSAL));
    }

    @Override
    public Component getDescription() {
        return Component.translatable(
                "perk.maniacrev." + ID + ".desc",
                MAX_FLOWERS,
                MODIFIER_PERCENT_PER_FLOWER
        );
    }

    @Override
    public void onGameStart(ServerPlayer player) {
        resetBouquet(player);
        FlowerTrailManager.startMatch(player.server);
    }

    @Override
    public void onPhaseChange(ServerPlayer player, PerkPhase newPhase) {
        if (newPhase == PerkPhase.HUNT) {
            resetBouquet(player);
            FlowerTrailManager.startMatch(player.server);
        }
    }

    @Override
    public void applyPassiveEffect(ServerPlayer player) {
        refreshEffect(player);
        syncState(player);
    }

    @Override
    public void onTick(ServerPlayer player) {
        refreshEffect(player);
        if (player.tickCount % HUD_SYNC_INTERVAL_TICKS == 0) {
            syncState(player);
        }
    }

    @Override
    public void removePassiveEffect(ServerPlayer player) {
        resetBouquet(player);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)
                || event.getAmount() <= MINIMUM_REAL_DAMAGE) {
            return;
        }
        tryCollectFromDamage(victim, event.getSource());
    }

    /** Удар по уже лежащему игроку тоже даёт его цветок, хотя урон блокируется. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) {
            return;
        }
        DownedData downedData = DownedCapability.get(victim);
        if (downedData == null || downedData.getState() != DownedState.DOWNED) {
            return;
        }
        tryCollectFromDamage(victim, event.getSource());
    }

    private static void tryCollectFromDamage(ServerPlayer victim,
                                             net.minecraft.world.damagesource.DamageSource source) {
        if (!isActivePhase()
                || victim.gameMode.getGameModeForPlayer() != GameType.ADVENTURE
                || PerkTeam.fromPlayer(victim) != PerkTeam.SURVIVOR) {
            return;
        }

        ServerPlayer maniac = ManiacDamageAttribution.peekResponsibleManiac(victim, source);
        if (maniac == null
                || maniac == victim
                || maniac.isSpectator()
                || maniac.gameMode.getGameModeForPlayer() != GameType.ADVENTURE
                || PerkTeam.fromPlayer(maniac) != PerkTeam.MANIAC
                || PlayerDataManager.get(maniac).getPerkInstance(ID) == null) {
            return;
        }

        LinkedHashMap<UUID, FlowerVariant> flowers = COLLECTED_FLOWERS.computeIfAbsent(
                maniac.getUUID(),
                ignored -> new LinkedHashMap<>()
        );
        if (flowers.containsKey(victim.getUUID())) {
            return;
        }

        int currentCount = Math.max(getEffectFlowerCount(maniac), flowers.size());
        if (currentCount >= MAX_FLOWERS) {
            return;
        }

        FlowerVariant variant = FlowerTrailManager.getAssignedFlower(victim);
        flowers.put(victim.getUUID(), variant);
        int newCount = Math.min(MAX_FLOWERS, currentCount + 1);
        applyEffect(maniac, newCount);
        syncState(maniac);
        sendCollectVisual(maniac, victim, variant);
    }

    /** Вызывается ровно при создании нового состояния нокдауна. */
    public static void applyToNextKnockdown(ServerPlayer maniac,
                                            ServerPlayer downedSurvivor,
                                            DownedData downedData) {
        if (!isActivePhase()
                || PerkTeam.fromPlayer(maniac) != PerkTeam.MANIAC
                || PlayerDataManager.get(maniac).getPerkInstance(ID) == null) {
            return;
        }

        int flowerCount = getEffectFlowerCount(maniac);
        if (flowerCount <= 0) {
            return;
        }

        int modifierPercent = flowerCount * MODIFIER_PERCENT_PER_FLOWER;
        float remainingFraction = (PERCENT_DENOMINATOR - modifierPercent)
                / PERCENT_DENOMINATOR;
        int adjustedTimeoutTicks = Math.max(
                1,
                Math.round(DownedData.DOWNED_TIMEOUT_TICKS * remainingFraction)
        );
        float selfReviveManaMultiplier = 1.0F
                + modifierPercent / PERCENT_DENOMINATOR;
        downedData.setDownedTimeoutTicks(adjustedTimeoutTicks);
        downedData.setSelfReviveManaMultiplier(selfReviveManaMultiplier);

        LinkedHashMap<UUID, FlowerVariant> flowers = COLLECTED_FLOWERS.remove(
                maniac.getUUID()
        );
        List<FlowerVariant> consumed = flowers == null
                ? new ArrayList<>()
                : new ArrayList<>(flowers.values());
        while (consumed.size() < flowerCount) {
            consumed.add(FlowerTrailManager.getAssignedFlower(downedSurvivor));
        }

        maniac.removeEffect(ModEffects.BOUQUET.get());
        syncState(maniac);
        sendConsumeVisual(downedSurvivor, consumed);
    }

    private static boolean isActivePhase() {
        PerkPhase phase = GameManager.getCurrentPhase();
        return phase == PerkPhase.HUNT
                || phase == PerkPhase.MIDGAME
                || phase == PerkPhase.REVERSAL;
    }

    private static int getEffectFlowerCount(ServerPlayer maniac) {
        MobEffectInstance effect = maniac.getEffect(ModEffects.BOUQUET.get());
        if (effect == null) {
            return 0;
        }
        return Math.min(MAX_FLOWERS, effect.getAmplifier() + DISPLAYED_LEVEL_OFFSET);
    }

    private static void refreshEffect(ServerPlayer player) {
        Map<UUID, FlowerVariant> storedFlowers = COLLECTED_FLOWERS.get(player.getUUID());
        int storedCount = storedFlowers == null ? 0 : storedFlowers.size();
        int count = Math.max(storedCount, getEffectFlowerCount(player));
        if (count > 0) {
            applyEffect(player, count);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            COLLECTED_FLOWERS.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        COLLECTED_FLOWERS.clear();
    }

    private static void applyEffect(ServerPlayer player, int flowerCount) {
        int clampedCount = Math.max(1, Math.min(MAX_FLOWERS, flowerCount));
        int amplifier = clampedCount - DISPLAYED_LEVEL_OFFSET;
        MobEffectInstance current = player.getEffect(ModEffects.BOUQUET.get());
        if (current != null
                && current.getAmplifier() == amplifier
                && current.getDuration() > EFFECT_REFRESH_THRESHOLD_TICKS) {
            return;
        }
        player.addEffect(new MobEffectInstance(
                ModEffects.BOUQUET.get(),
                EFFECT_REFRESH_DURATION_TICKS,
                amplifier,
                false,
                false,
                true
        ));
    }

    private static void resetBouquet(ServerPlayer player) {
        COLLECTED_FLOWERS.remove(player.getUUID());
        player.removeEffect(ModEffects.BOUQUET.get());
        syncState(player);
    }

    private static void syncState(ServerPlayer player) {
        LinkedHashMap<UUID, FlowerVariant> flowers = COLLECTED_FLOWERS.get(player.getUUID());
        List<FlowerVariant> variants = flowers == null
                ? List.of()
                : List.copyOf(flowers.values());
        ModNetworking.sendToPlayer(BouquetPacket.state(variants), player);
    }

    private static void sendCollectVisual(ServerPlayer maniac, ServerPlayer victim,
                                          FlowerVariant variant) {
        BouquetPacket packet = BouquetPacket.collect(
                variant,
                victim.getX(),
                victim.getY() + COLLECT_SOURCE_HEIGHT_BLOCKS,
                victim.getZ(),
                maniac.getX(),
                maniac.getY() + COLLECT_TARGET_HEIGHT_BLOCKS,
                maniac.getZ()
        );
        for (ServerPlayer viewer : maniac.serverLevel().players()) {
            ModNetworking.sendToPlayer(packet, viewer);
        }
    }

    private static void sendConsumeVisual(ServerPlayer survivor,
                                          List<FlowerVariant> variants) {
        BouquetPacket packet = BouquetPacket.consume(
                variants,
                survivor.getX(),
                survivor.getY() + KNOCKDOWN_BURST_HEIGHT_BLOCKS,
                survivor.getZ()
        );
        for (ServerPlayer viewer : survivor.serverLevel().players()) {
            ModNetworking.sendToPlayer(packet, viewer);
        }
    }
}
