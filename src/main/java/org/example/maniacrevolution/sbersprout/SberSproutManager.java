package org.example.maniacrevolution.sbersprout;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.downed.DownedCapability;
import org.example.maniacrevolution.downed.DownedData;
import org.example.maniacrevolution.downed.DownedState;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.game.GameManager;
import org.example.maniacrevolution.hack.ComputerBlockEntity;
import org.example.maniacrevolution.hack.HackConfig;
import org.example.maniacrevolution.hack.HackManager;
import org.example.maniacrevolution.hack.HackSession;
import org.example.maniacrevolution.item.SberSproutItem;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.SberSproutStatePacket;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.perk.perks.survivor.SberSproutPerk;
import org.example.maniacrevolution.util.ManaUtil;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Серверное состояние непрерывных сеансов и заполненных СберРостков. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public final class SberSproutManager {
    private static final int NO_ACTIVE_MATCH_PHASE = 0;
    private static final int EFFECT_AMPLIFIER = 0;
    private static final int EFFECT_DURATION_TICKS = 40;
    private static final int EFFECT_REFRESH_THRESHOLD_TICKS = 20;
    private static final int EXTRACTION_PARTICLES = 34;
    private static final int PLANTING_PARTICLES = 42;
    private static final double PARTICLE_SPREAD = 0.45D;
    private static final double PARTICLE_SPEED = 0.09D;
    private static final float SOUND_VOLUME = 0.85F;
    private static final float EXTRACTION_PITCH = 1.65F;
    private static final float PLANTING_PITCH = 1.2F;
    private static final Vector3f GREEN = new Vector3f(0.05F, 1.0F, 0.32F);
    private static final Vector3f CYAN = new Vector3f(0.0F, 0.95F, 1.0F);
    private static final Vector3f GOLD = new Vector3f(1.0F, 0.72F, 0.08F);

    private static final Map<UUID, SproutSession> SESSIONS = new HashMap<>();
    private static final Map<UUID, UUID> SUPPRESSED_HACK_SESSIONS = new HashMap<>();
    private static long lastBeginTick = Long.MIN_VALUE;
    private static boolean matchInitialized;

    private SberSproutManager() {
    }

    public enum ExtractionFailure {
        NONE(null),
        INACTIVE_PHASE("message.maniacrev.sber_sprout.inactive_phase"),
        NO_SESSION("message.maniacrev.sber_sprout.no_session"),
        TOO_LITTLE("message.maniacrev.sber_sprout.too_little"),
        ALREADY_FILLED("message.maniacrev.sber_sprout.already_filled"),
        INVENTORY_FULL("message.maniacrev.sber_sprout.inventory_full");

        private final String key;

        ExtractionFailure(String key) {
            this.key = key;
        }

        public Component message() {
            if (key == null) return Component.empty();
            return switch (this) {
                case INACTIVE_PHASE -> Component.translatable(key,
                        SberSproutPerk.FIRST_ACTIVE_PHASE.getScoreboardValue(),
                        SberSproutPerk.LAST_ACTIVE_PHASE.getScoreboardValue());
                case TOO_LITTLE -> Component.translatable(key,
                        SberSproutPerk.MINIMUM_EXTRACTION_PERCENT);
                default -> Component.translatable(key);
            };
        }
    }

    public static void beginForOwner(ServerPlayer owner) {
        long gameTick = owner.server.overworld().getGameTime();
        if (!matchInitialized || lastBeginTick != gameTick) {
            clearAllInternal(owner.server, true, true);
            matchInitialized = true;
            lastBeginTick = gameTick;
        }
        clearOwner(owner, true);
    }

    public static void onParticipantJoined(ServerPlayer player, HackSession hackSession) {
        if (!isActiveOwner(player) || hackSession == null) return;
        UUID playerId = player.getUUID();
        UUID suppressed = SUPPRESSED_HACK_SESSIONS.get(playerId);
        if (hackSession.getSessionId().equals(suppressed)) return;
        if (suppressed != null) SUPPRESSED_HACK_SESSIONS.remove(playerId);

        SproutSession current = SESSIONS.get(playerId);
        if (current != null && current.hackSessionId().equals(hackSession.getSessionId())) {
            refreshEffect(player);
            return;
        }
        if (current != null) deactivateOwnerSession(player, false);

        SproutSession created = new SproutSession(hackSession.getSessionId(),
                hackSession.getComputerId(), player.level().dimension(),
                hackSession.getPos().immutable(), 0.0F);
        SESSIONS.put(playerId, created);
        refreshEffect(player);
        sync(player, created, SberSproutStatePacket.UPDATE);
    }

    public static void onContribution(ServerPlayer player, HackSession hackSession,
                                      float actualPoints) {
        if (player == null || hackSession == null || actualPoints <= 0.0F
                || !isActiveOwner(player)) return;
        UUID suppressed = SUPPRESSED_HACK_SESSIONS.get(player.getUUID());
        if (hackSession.getSessionId().equals(suppressed)) return;

        SproutSession session = SESSIONS.get(player.getUUID());
        if (session != null && !session.hackSessionId().equals(hackSession.getSessionId())) {
            return;
        }
        if (session == null) {
            onParticipantJoined(player, hackSession);
            session = SESSIONS.get(player.getUUID());
        }
        if (session == null) return;

        float updated = roundPercent(session.contributionPercent()
                + pointsToPercent(actualPoints));
        SproutSession changed = session.withContribution(updated);
        SESSIONS.put(player.getUUID(), changed);
        refreshEffect(player);
        sync(player, changed, SberSproutStatePacket.UPDATE);
    }

    public static void onParticipantLeft(ServerPlayer player, UUID hackSessionId) {
        if (player == null) return;
        SproutSession session = SESSIONS.get(player.getUUID());
        if (session != null && session.hackSessionId().equals(hackSessionId)) {
            deactivateOwnerSession(player, false);
        }
        SUPPRESSED_HACK_SESSIONS.remove(player.getUUID(), hackSessionId);
    }

    public static void onHackSessionEnded(UUID hackSessionId) {
        if (hackSessionId == null) return;
        for (UUID ownerId : new ArrayList<>(SESSIONS.keySet())) {
            SproutSession session = SESSIONS.get(ownerId);
            if (session == null || !session.hackSessionId().equals(hackSessionId)) continue;
            ServerPlayer player = findPlayer(ownerId);
            if (player != null) deactivateOwnerSession(player, false);
            else SESSIONS.remove(ownerId);
        }
        SUPPRESSED_HACK_SESSIONS.entrySet().removeIf(
                entry -> entry.getValue().equals(hackSessionId));
    }

    public static ExtractionFailure getExtractionFailure(ServerPlayer player) {
        if (!isActiveOwner(player)) return ExtractionFailure.INACTIVE_PHASE;
        SproutSession session = SESSIONS.get(player.getUUID());
        HackSession hackSession = session == null ? null
                : HackManager.get().getSession(session.hackSessionId());
        if (session == null || hackSession == null || hackSession.isFinished()
                || !hackSession.hasParticipant(player)) return ExtractionFailure.NO_SESSION;
        if (hasOwnFilledItem(player)) return ExtractionFailure.ALREADY_FILLED;
        float extractable = Math.min(session.contributionPercent(),
                pointsToPercent(hackSession.getCurrentPoints()));
        if (extractable < SberSproutPerk.MINIMUM_EXTRACTION_PERCENT) {
            return ExtractionFailure.TOO_LITTLE;
        }
        if (player.getInventory().getFreeSlot() < 0
                && !player.containerMenu.getCarried().isEmpty()) {
            return ExtractionFailure.INVENTORY_FULL;
        }
        return ExtractionFailure.NONE;
    }

    /** Мана за извлечение уже списана общей системой активных перков. */
    public static void extract(ServerPlayer player) {
        if (getExtractionFailure(player) != ExtractionFailure.NONE) return;
        SproutSession session = SESSIONS.get(player.getUUID());
        HackSession hackSession = HackManager.get().getSession(session.hackSessionId());
        if (hackSession == null) return;

        float requestedPercent = roundPercent(Math.min(session.contributionPercent(),
                pointsToPercent(hackSession.getCurrentPoints())));
        float removedPoints = HackManager.get().removeSproutProgress(player,
                session.hackSessionId(), percentToPoints(requestedPercent));
        float removedPercent = roundPercent(pointsToPercent(removedPoints));
        if (removedPercent < SberSproutPerk.MINIMUM_EXTRACTION_PERCENT) return;

        float savedPercent = roundPercent(removedPercent
                * SberSproutPerk.PRESERVED_PROGRESS_PERCENT / 100.0F);
        giveFilledItem(player, savedPercent);
        SproutSession emptied = session.withContribution(0.0F);
        SESSIONS.put(player.getUUID(), emptied);
        refreshEffect(player);
        ModNetworking.sendToPlayer(SberSproutStatePacket.extracted(
                session.blockPos(), savedPercent), player);
        playExtraction(player, session.blockPos());
        player.displayClientMessage(Component.translatable(
                "message.maniacrev.sber_sprout.extracted", formatPercent(savedPercent)), true);
    }

    public static boolean tryPlant(ServerPlayer player, BlockPos pos,
                                   ComputerBlockEntity computer, ItemStack stack) {
        if (!SberSproutItem.belongsTo(stack, player.getUUID())) {
            player.displayClientMessage(Component.translatable(
                    "message.maniacrev.sber_sprout.not_owner"), true);
            return false;
        }
        if (!isActiveOwner(player)) {
            player.displayClientMessage(Component.translatable(
                    "message.maniacrev.sber_sprout.inactive_phase",
                    SberSproutPerk.FIRST_ACTIVE_PHASE.getScoreboardValue(),
                    SberSproutPerk.LAST_ACTIVE_PHASE.getScoreboardValue()), true);
            return false;
        }
        HackManager manager = HackManager.get();
        if (computer == null || computer.isHacked()
                || manager.isHacked(computer.getComputerId())) {
            player.displayClientMessage(Component.translatable(
                    "message.maniacrev.sber_sprout.computer_finished"), true);
            return false;
        }
        if (computer.isBlocked() || manager.isBlocked(computer.getComputerId())) {
            player.displayClientMessage(Component.translatable(
                    "message.maniacrev.sber_sprout.computer_blocked"), true);
            return false;
        }
        if (!ManaUtil.hasMana(player, SberSproutPerk.PLANTING_MANA_COST)) {
            player.displayClientMessage(Component.translatable(
                    "message.maniacrev.sber_sprout.not_enough_mana",
                    SberSproutPerk.PLANTING_MANA_COST), true);
            return false;
        }

        float storedPercent = roundPercent(SberSproutItem.getStoredPercent(stack));
        if (storedPercent <= 0.0F) return false;
        HackSession hackSession = manager.getOrStartSessionForSprout(player, pos,
                computer.getComputerId());
        if (hackSession == null || hackSession.isFinished()) return false;
        if (!ManaUtil.consumeMana(player, SberSproutPerk.PLANTING_MANA_COST)) return false;

        float addedPoints = manager.addSproutProgress(player, hackSession.getSessionId(),
                percentToPoints(storedPercent));
        float plantedPercent = roundPercent(pointsToPercent(addedPoints));
        SproutSession session = SESSIONS.get(player.getUUID());
        if (session == null || !session.hackSessionId().equals(hackSession.getSessionId())) {
            onParticipantJoined(player, hackSession);
            session = SESSIONS.get(player.getUUID());
        }
        if (session != null && plantedPercent > 0.0F) {
            SproutSession changed = session.withContribution(roundPercent(
                    session.contributionPercent() + plantedPercent));
            SESSIONS.put(player.getUUID(), changed);
            refreshEffect(player);
            sync(player, changed, SberSproutStatePacket.UPDATE);
        }
        stack.shrink(1);
        ModNetworking.sendToPlayer(SberSproutStatePacket.planted(pos, plantedPercent), player);
        playPlanting(player, pos);
        player.displayClientMessage(Component.translatable(
                "message.maniacrev.sber_sprout.planted", formatPercent(plantedPercent)), true);
        return true;
    }

    public static void deactivateOwnerSession(ServerPlayer player, boolean suppressCurrentHack) {
        SproutSession removed = SESSIONS.remove(player.getUUID());
        if (removed != null && suppressCurrentHack) {
            SUPPRESSED_HACK_SESSIONS.put(player.getUUID(), removed.hackSessionId());
        }
        player.removeEffect(ModEffects.SBER_SPROUT_SESSION.get());
        ModNetworking.sendToPlayer(SberSproutStatePacket.clear(), player);
    }

    public static int clearAll(MinecraftServer server) {
        int count = SESSIONS.size();
        for (Map.Entry<UUID, SproutSession> entry : SESSIONS.entrySet()) {
            SUPPRESSED_HACK_SESSIONS.put(entry.getKey(),
                    entry.getValue().hackSessionId());
        }
        SESSIONS.clear();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            count += countFilledItems(player);
            player.removeEffect(ModEffects.SBER_SPROUT_SESSION.get());
            ModNetworking.sendToPlayer(SberSproutStatePacket.clear(), player);
            removeFilledItems(player);
        }
        return count;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        int phase = GameManager.getPhaseValue();
        if (phase == NO_ACTIVE_MATCH_PHASE) {
            if (matchInitialized || !SESSIONS.isEmpty()) {
                clearAllInternal(server, true, true);
                matchInitialized = false;
            }
            return;
        }
        enforceBoundItems(server);
        if (phase == SberSproutPerk.LAST_ACTIVE_PHASE.getScoreboardValue() + 1) {
            for (UUID ownerId : new ArrayList<>(SESSIONS.keySet())) {
                ServerPlayer player = server.getPlayerList().getPlayer(ownerId);
                if (player != null) deactivateOwnerSession(player, false);
            }
            return;
        }
        validateSessions(server);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getAmount() > 0.0F && event.getEntity() instanceof ServerPlayer player) {
            deactivateOwnerSession(player, true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) clearOwner(player, true);
    }

    @SubscribeEvent
    public static void onTeleport(EntityTeleportEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            deactivateOwnerSession(player, true);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            deactivateOwnerSession(player, true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            deactivateOwnerSession(player, false);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        clearAllInternal(event.getServer(), true, true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onItemToss(ItemTossEvent event) {
        ItemStack stack = event.getEntity().getItem();
        if (!stack.is(ModItems.SBER_SPROUT.get())) return;
        event.setCanceled(true);
        if (!event.getPlayer().getInventory().add(stack.copy())) {
            event.getPlayer().containerMenu.setCarried(stack.copy());
        }
    }

    @SubscribeEvent
    public static void onContainerClosed(PlayerContainerEvent.Close event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            recoverFromExternalMenu(player);
            recoverCarried(player);
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            event.getDrops().removeIf(drop -> drop.getItem().is(ModItems.SBER_SPROUT.get()));
        }
    }

    private static void validateSessions(MinecraftServer server) {
        for (UUID ownerId : new ArrayList<>(SESSIONS.keySet())) {
            ServerPlayer player = server.getPlayerList().getPlayer(ownerId);
            SproutSession session = SESSIONS.get(ownerId);
            HackSession hackSession = session == null ? null
                    : HackManager.get().getSession(session.hackSessionId());
            if (player == null) continue;
            if (session == null || !isActiveOwner(player)
                    || !session.dimension().equals(player.level().dimension())
                    || hackSession == null || hackSession.isFinished()
                    || !hackSession.hasParticipant(player) || isDowned(player)) {
                deactivateOwnerSession(player, isDowned(player));
                continue;
            }
            MobEffectInstance effect = player.getEffect(ModEffects.SBER_SPROUT_SESSION.get());
            if (effect == null || effect.getDuration() <= EFFECT_REFRESH_THRESHOLD_TICKS) {
                refreshEffect(player);
            }
        }
    }

    private static void refreshEffect(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(ModEffects.SBER_SPROUT_SESSION.get(),
                EFFECT_DURATION_TICKS, EFFECT_AMPLIFIER, false, false, true));
    }

    private static boolean isActiveOwner(ServerPlayer player) {
        int phase = GameManager.getPhaseValue();
        return (phase == SberSproutPerk.FIRST_ACTIVE_PHASE.getScoreboardValue()
                || phase == SberSproutPerk.LAST_ACTIVE_PHASE.getScoreboardValue())
                && player.gameMode.getGameModeForPlayer() == GameType.ADVENTURE
                && PerkTeam.fromPlayer(player) == PerkTeam.SURVIVOR
                && PlayerDataManager.get(player).getPerkInstance(SberSproutPerk.ID) != null;
    }

    private static boolean isDowned(ServerPlayer player) {
        DownedData data = DownedCapability.get(player);
        return data != null && data.getState() == DownedState.DOWNED;
    }

    private static void sync(ServerPlayer player, SproutSession session, int state) {
        float saved = roundPercent(session.contributionPercent()
                * SberSproutPerk.PRESERVED_PROGRESS_PERCENT / 100.0F);
        ModNetworking.sendToPlayer(new SberSproutStatePacket(state,
                session.blockPos().asLong(), session.contributionPercent(), saved), player);
    }

    private static float pointsToPercent(float points) {
        return points / Math.max(0.0001F, HackConfig.HACK_POINTS_REQUIRED) * 100.0F;
    }

    private static float percentToPoints(float percent) {
        return percent / 100.0F * Math.max(0.0001F, HackConfig.HACK_POINTS_REQUIRED);
    }

    public static float roundPercent(float value) {
        return Math.round(Math.max(0.0F, value) * SberSproutPerk.ROUNDING_FACTOR)
                / (float) SberSproutPerk.ROUNDING_FACTOR;
    }

    private static String formatPercent(float value) {
        return String.format(java.util.Locale.ROOT,
                "%." + SberSproutPerk.DISPLAY_DECIMAL_PLACES + "f",
                roundPercent(value));
    }

    private static void giveFilledItem(ServerPlayer owner, float savedPercent) {
        ItemStack filled = SberSproutItem.createFor(owner, savedPercent);
        if (!owner.getInventory().add(filled)) owner.containerMenu.setCarried(filled);
        owner.inventoryMenu.broadcastChanges();
    }

    private static boolean hasOwnFilledItem(ServerPlayer owner) {
        for (int slot = 0; slot < owner.getInventory().getContainerSize(); slot++) {
            if (SberSproutItem.belongsTo(owner.getInventory().getItem(slot), owner.getUUID())) {
                return true;
            }
        }
        return SberSproutItem.belongsTo(owner.containerMenu.getCarried(), owner.getUUID());
    }

    private static int countFilledItems(ServerPlayer player) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(ModItems.SBER_SPROUT.get())) count++;
        }
        if (player.containerMenu.getCarried().is(ModItems.SBER_SPROUT.get())) count++;
        return count;
    }

    private static void clearOwner(ServerPlayer player, boolean removeItems) {
        deactivateOwnerSession(player, false);
        SUPPRESSED_HACK_SESSIONS.remove(player.getUUID());
        if (removeItems) removeFilledItems(player);
    }

    private static void clearAllInternal(MinecraftServer server, boolean removeItems,
                                         boolean resetMatchFlag) {
        SESSIONS.clear();
        SUPPRESSED_HACK_SESSIONS.clear();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.removeEffect(ModEffects.SBER_SPROUT_SESSION.get());
            ModNetworking.sendToPlayer(SberSproutStatePacket.clear(), player);
            if (removeItems) removeFilledItems(player);
        }
        if (resetMatchFlag) matchInitialized = false;
    }

    private static void removeFilledItems(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(ModItems.SBER_SPROUT.get())) {
                player.getInventory().setItem(slot, ItemStack.EMPTY);
            }
        }
        if (player.containerMenu.getCarried().is(ModItems.SBER_SPROUT.get())) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
        }
    }

    private static void enforceBoundItems(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            int ownCount = 0;
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                if (!stack.is(ModItems.SBER_SPROUT.get())) continue;
                UUID ownerId = SberSproutItem.getOwner(stack);
                if (ownerId == null) {
                    player.getInventory().setItem(slot, ItemStack.EMPTY);
                } else if (!ownerId.equals(player.getUUID())) {
                    player.getInventory().setItem(slot, ItemStack.EMPTY);
                    returnToOwner(server, ownerId, stack.copy());
                } else if (ownCount++ > 0) {
                    player.getInventory().setItem(slot, ItemStack.EMPTY);
                }
            }
            recoverFromExternalMenu(player);
            recoverCarried(player);
        }
    }

    private static void recoverFromExternalMenu(ServerPlayer viewer) {
        for (net.minecraft.world.inventory.Slot slot : viewer.containerMenu.slots) {
            if (slot.container instanceof Inventory || !slot.hasItem()
                    || !slot.getItem().is(ModItems.SBER_SPROUT.get())) continue;
            ItemStack recovered = slot.getItem().copy();
            slot.set(ItemStack.EMPTY);
            UUID ownerId = SberSproutItem.getOwner(recovered);
            if (ownerId != null) returnToOwner(viewer.server, ownerId, recovered);
        }
    }

    private static void recoverCarried(ServerPlayer viewer) {
        ItemStack carried = viewer.containerMenu.getCarried();
        if (!carried.is(ModItems.SBER_SPROUT.get())) return;
        viewer.containerMenu.setCarried(ItemStack.EMPTY);
        UUID ownerId = SberSproutItem.getOwner(carried);
        if (ownerId != null) returnToOwner(viewer.server, ownerId, carried.copy());
    }

    private static void returnToOwner(MinecraftServer server, UUID ownerId, ItemStack stack) {
        ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
        if (owner != null && !hasOwnFilledItem(owner)) {
            if (!owner.getInventory().add(stack)) owner.containerMenu.setCarried(stack);
            owner.inventoryMenu.broadcastChanges();
        }
    }

    private static ServerPlayer findPlayer(UUID ownerId) {
        MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : server.getPlayerList().getPlayer(ownerId);
    }

    private static void playExtraction(ServerPlayer owner, BlockPos pos) {
        ServerLevel level = owner.serverLevel();
        Vec3 center = Vec3.atCenterOf(pos).add(0.0D, 0.65D, 0.0D);
        sendColoredParticles(owner, center, EXTRACTION_PARTICLES);
        level.sendParticles(owner, ParticleTypes.COMPOSTER, true,
                center.x, center.y, center.z,
                EXTRACTION_PARTICLES / 3, PARTICLE_SPREAD, PARTICLE_SPREAD,
                PARTICLE_SPREAD, PARTICLE_SPEED);
        owner.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, SOUND_VOLUME, EXTRACTION_PITCH);
    }

    private static void playPlanting(ServerPlayer owner, BlockPos pos) {
        ServerLevel level = owner.serverLevel();
        Vec3 center = Vec3.atCenterOf(pos).add(0.0D, 0.65D, 0.0D);
        sendColoredParticles(owner, center, PLANTING_PARTICLES);
        level.sendParticles(owner, ParticleTypes.HAPPY_VILLAGER, true,
                center.x, center.y, center.z,
                PLANTING_PARTICLES / 3, PARTICLE_SPREAD, PARTICLE_SPREAD,
                PARTICLE_SPREAD, PARTICLE_SPEED);
        owner.playNotifySound(SoundEvents.AZALEA_LEAVES_PLACE,
                SoundSource.PLAYERS, SOUND_VOLUME, PLANTING_PITCH);
    }

    private static void sendColoredParticles(ServerPlayer owner, Vec3 center, int count) {
        ServerLevel level = owner.serverLevel();
        level.sendParticles(owner, new DustParticleOptions(GREEN, 0.9F), true,
                center.x, center.y, center.z, count,
                PARTICLE_SPREAD, PARTICLE_SPREAD, PARTICLE_SPREAD, PARTICLE_SPEED);
        level.sendParticles(owner, new DustParticleOptions(CYAN, 0.7F), true,
                center.x, center.y, center.z, count / 2,
                PARTICLE_SPREAD, PARTICLE_SPREAD, PARTICLE_SPREAD, PARTICLE_SPEED);
        level.sendParticles(owner, new DustParticleOptions(GOLD, 0.65F), true,
                center.x, center.y, center.z, count / 3,
                PARTICLE_SPREAD, PARTICLE_SPREAD, PARTICLE_SPREAD, PARTICLE_SPEED);
    }

    private record SproutSession(UUID hackSessionId, int computerId,
                                 ResourceKey<Level> dimension, BlockPos blockPos,
                                 float contributionPercent) {
        private SproutSession withContribution(float value) {
            return new SproutSession(hackSessionId, computerId, dimension, blockPos, value);
        }
    }
}
