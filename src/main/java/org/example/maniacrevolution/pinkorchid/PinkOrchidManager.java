package org.example.maniacrevolution.pinkorchid;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.server.ServerStoppingEvent;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.entity.ModEntities;
import org.example.maniacrevolution.entity.PinkOrchidIllusionEntity;
import org.example.maniacrevolution.game.GameManager;
import org.example.maniacrevolution.item.PinkOrchidItem;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.PinkOrchidAppearancePacket;
import org.example.maniacrevolution.network.packets.PinkOrchidStatePacket;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.perk.perks.survivor.PinkOrchidPerk;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Серверное состояние записи, сохранённого маршрута, маркера и активной копии. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public final class PinkOrchidManager {
    private static final int EFFECT_AMPLIFIER = 0;
    private static final int EFFECT_DURATION_PADDING_TICKS = 2;
    private static final int CHUNK_TICKET_DISTANCE = 2;
    private static final int NO_ACTIVE_MATCH_PHASE = 0;
    private static final double APPEARANCE_SYNC_RADIUS_BLOCKS = 256.0D;
    private static final double SPAWN_PARTICLE_HEIGHT = 1.0D;
    private static final int MATERIALIZE_PARTICLES = 38;
    private static final int SHATTER_PARTICLES = 46;
    private static final int DISSOLVE_PARTICLES = 30;
    private static final double PARTICLE_HORIZONTAL_SPREAD = 0.45D;
    private static final double PARTICLE_VERTICAL_SPREAD = 0.85D;
    private static final double MATERIALIZE_PARTICLE_SPEED = 0.08D;
    private static final double SHATTER_PARTICLE_SPEED = 0.16D;
    private static final double DISSOLVE_PARTICLE_SPEED = 0.055D;
    private static final float MAIN_PARTICLE_SIZE = 0.85F;
    private static final float SECONDARY_PARTICLE_SIZE = 0.62F;
    private static final float SOUND_VOLUME = 0.9F;
    private static final float RECORD_START_PITCH = 1.35F;
    private static final float RECORD_FINISH_PITCH = 1.8F;
    private static final float SPAWN_SOUND_PITCH = 1.6F;
    private static final float SHATTER_SOUND_PITCH = 1.25F;
    private static final float DISSOLVE_SOUND_PITCH = 1.9F;
    private static final Vector3f PINK = new Vector3f(1.0F, 0.06F, 0.66F);
    private static final Vector3f CYAN = new Vector3f(0.0F, 0.92F, 1.0F);
    private static final Vector3f VIOLET = new Vector3f(0.62F, 0.08F, 1.0F);

    private static final TicketType<UUID> ROUTE_TICKET = TicketType.create(
            "maniacrev:pink_orchid", Comparator.comparing(UUID::toString));

    private static final Map<UUID, RecordingSession> RECORDINGS = new HashMap<>();
    private static final Map<UUID, RecordedRoute> ROUTES = new HashMap<>();
    private static final Map<UUID, UUID> ACTIVE_BY_OWNER = new HashMap<>();
    private static final Map<UUID, ForcedChunks> FORCED_CHUNKS = new HashMap<>();
    private static long lastBeginTick = Long.MIN_VALUE;
    private static boolean matchInitialized;

    private PinkOrchidManager() {
    }

    public static void beginForOwner(ServerPlayer owner) {
        long gameTick = owner.server.overworld().getGameTime();
        if (!matchInitialized || lastBeginTick != gameTick) {
            clearAllInternal(owner.server, true, true);
            matchInitialized = true;
            lastBeginTick = gameTick;
        }
        clearOwnerRuntime(owner, true);
        giveBoundItem(owner);
    }

    public static boolean toggleRecording(ServerPlayer player) {
        if (!isActiveOwner(player)) {
            player.displayClientMessage(
                    Component.translatable("message.maniacrev.pink_orchid.inactive_phase"), true);
            return false;
        }
        if (RECORDINGS.containsKey(player.getUUID())) {
            finishRecording(player, true);
        } else {
            startRecording(player);
        }
        return true;
    }

    public static boolean hasRecordedRoute(ServerPlayer player) {
        RecordedRoute route = ROUTES.get(player.getUUID());
        return route != null && !route.frames().isEmpty()
                && route.dimension().equals(player.level().dimension());
    }

    public static void activate(ServerPlayer owner) {
        RecordedRoute stored = ROUTES.get(owner.getUUID());
        if (stored == null || stored.frames().isEmpty()
                || !stored.dimension().equals(owner.level().dimension())) {
            return;
        }

        removeActiveIllusion(owner.server, owner.getUUID(), false);
        ServerLevel level = owner.serverLevel();
        PinkOrchidIllusionEntity illusion = ModEntities.PINK_ORCHID_ILLUSION.get().create(level);
        if (illusion == null) return;

        List<PinkOrchidRouteFrame> snapshot = stored.frames().stream()
                .map(PinkOrchidRouteFrame::copyFrame).toList();
        List<ItemStack> armor = List.of(
                owner.getItemBySlot(EquipmentSlot.FEET).copy(),
                owner.getItemBySlot(EquipmentSlot.LEGS).copy(),
                owner.getItemBySlot(EquipmentSlot.CHEST).copy(),
                owner.getItemBySlot(EquipmentSlot.HEAD).copy()
        );
        illusion.initialize(owner, snapshot, armor);
        forceRouteChunks(level, illusion.getUUID(), snapshot);

        Vec3 start = snapshot.get(0).position();
        AABB startBox = illusion.getDimensions(illusion.getPose()).makeBoundingBox(start);
        if (!level.getWorldBorder().isWithinBounds(
                    net.minecraft.core.BlockPos.containing(start))
                || level.getBlockCollisions(illusion, startBox).iterator().hasNext()
                || !level.addFreshEntity(illusion)) {
            releaseRouteChunks(illusion.getUUID());
            playShatter(level, start);
            owner.displayClientMessage(
                    Component.translatable("message.maniacrev.pink_orchid.route_blocked"), true);
            return;
        }

        ACTIVE_BY_OWNER.put(owner.getUUID(), illusion.getUUID());
        playMaterialize(level, start);
        playMaterialize(level, owner.position());
        ModNetworking.sendToNearby(new PinkOrchidAppearancePacket(
                owner.getUUID(), PinkOrchidPerk.MATERIALIZE_TICKS), owner,
                APPEARANCE_SYNC_RADIUS_BLOCKS);
    }

    public static void finishIllusion(PinkOrchidIllusionEntity illusion, boolean shattered) {
        if (illusion.level().isClientSide() || illusion.isRemoved()) return;
        UUID ownerId = illusion.getOwnerUUID();
        if (ownerId != null && illusion.getUUID().equals(ACTIVE_BY_OWNER.get(ownerId))) {
            ACTIVE_BY_OWNER.remove(ownerId);
        }
        releaseRouteChunks(illusion.getUUID());
        ServerLevel level = (ServerLevel) illusion.level();
        if (shattered) playShatter(level, illusion.position());
        else playDissolve(level, illusion.position());
        illusion.discardFromManager();
    }

    public static void onUnexpectedIllusionRemoval(PinkOrchidIllusionEntity illusion) {
        UUID ownerId = illusion.getOwnerUUID();
        if (ownerId != null && illusion.getUUID().equals(ACTIVE_BY_OWNER.get(ownerId))) {
            ACTIVE_BY_OWNER.remove(ownerId);
        }
        releaseRouteChunks(illusion.getUUID());
    }

    public static void clearOwnerRuntime(ServerPlayer owner, boolean removeItem) {
        UUID ownerId = owner.getUUID();
        RECORDINGS.remove(ownerId);
        ROUTES.remove(ownerId);
        owner.removeEffect(ModEffects.ORCHID_RECORDING.get());
        removeActiveIllusion(owner.server, ownerId, false);
        ModNetworking.sendToPlayer(PinkOrchidStatePacket.clear(), owner);
        if (removeItem) removeBoundItems(owner);
    }

    /** Командная очистка: не затрагивает личные предметы и кулдауны. */
    public static int clearAll(MinecraftServer server) {
        int count = RECORDINGS.size() + ROUTES.size() + ACTIVE_BY_OWNER.size();
        clearAllInternal(server, false, false);
        return count;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        int phase = GameManager.getPhaseValue();
        if (phase == NO_ACTIVE_MATCH_PHASE) {
            if (matchInitialized || !RECORDINGS.isEmpty() || !ROUTES.isEmpty()
                    || !ACTIVE_BY_OWNER.isEmpty()) {
                clearAllInternal(server, true, true);
                matchInitialized = false;
            }
            return;
        }

        enforceBoundItems(server);
        if (phase == org.example.maniacrevolution.perk.PerkPhase.REVERSAL.getScoreboardValue()) {
            clearPhaseThreeRuntime(server);
            return;
        }
        tickRecordings(server);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        clearAllInternal(event.getServer(), true, true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        clearOwnerRuntime(player, true);
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearOwnerRuntime(player, false);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        RecordedRoute route = ROUTES.get(player.getUUID());
        if (route != null && route.dimension().equals(player.level().dimension())) {
            Vec3 start = route.frames().get(0).position();
            ModNetworking.sendToPlayer(PinkOrchidStatePacket.marker(
                    start.x, start.y, start.z), player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && RECORDINGS.containsKey(player.getUUID())) {
            finishRecording(player, false);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onItemToss(ItemTossEvent event) {
        ItemStack stack = event.getEntity().getItem();
        if (!stack.is(ModItems.PINK_ORCHID.get())) return;
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
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        event.getDrops().removeIf(drop -> drop.getItem().is(ModItems.PINK_ORCHID.get()));
    }

    private static void startRecording(ServerPlayer player) {
        UUID ownerId = player.getUUID();
        ROUTES.remove(ownerId);
        ModNetworking.sendToPlayer(PinkOrchidStatePacket.clear(), player);

        RecordingSession session = new RecordingSession(player.level().dimension());
        session.frames().add(captureFrame(player));
        RECORDINGS.put(ownerId, session);
        player.addEffect(new MobEffectInstance(
                ModEffects.ORCHID_RECORDING.get(),
                PinkOrchidPerk.RECORDING_TICKS + EFFECT_DURATION_PADDING_TICKS,
                EFFECT_AMPLIFIER,
                false,
                false,
                true
        ));
        ModNetworking.sendToPlayer(PinkOrchidStatePacket.recordingStarted(
                PinkOrchidPerk.RECORDING_TICKS), player);
        player.serverLevel().playSound(null, player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS,
                SOUND_VOLUME, RECORD_START_PITCH);
    }

    private static void tickRecordings(MinecraftServer server) {
        for (UUID ownerId : new ArrayList<>(RECORDINGS.keySet())) {
            ServerPlayer player = server.getPlayerList().getPlayer(ownerId);
            RecordingSession session = RECORDINGS.get(ownerId);
            if (player == null || session == null) continue;
            if (!isActiveOwner(player)
                    || !session.dimension().equals(player.level().dimension())) {
                clearOwnerRuntime(player, false);
                continue;
            }

            if (session.frames().size() < PinkOrchidPerk.RECORDING_TICKS
                    && session.advanceAndShouldSample()) {
                session.frames().add(captureFrame(player));
            }
            if (session.frames().size() >= PinkOrchidPerk.RECORDING_TICKS) {
                finishRecording(player, false);
            }
        }
    }

    private static void finishRecording(ServerPlayer player, boolean manual) {
        RecordingSession session = RECORDINGS.remove(player.getUUID());
        player.removeEffect(ModEffects.ORCHID_RECORDING.get());
        if (session == null || session.frames().isEmpty()) return;

        List<PinkOrchidRouteFrame> frames = session.frames().stream()
                .map(PinkOrchidRouteFrame::copyFrame).toList();
        ROUTES.put(player.getUUID(), new RecordedRoute(session.dimension(), frames));
        Vec3 start = frames.get(0).position();
        ModNetworking.sendToPlayer(PinkOrchidStatePacket.recordingFinished(), player);
        ModNetworking.sendToPlayer(PinkOrchidStatePacket.marker(
                start.x, start.y, start.z), player);
        player.serverLevel().playSound(null, player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS,
                SOUND_VOLUME, manual ? RECORD_FINISH_PITCH : RECORD_FINISH_PITCH - 0.12F);
    }

    private static PinkOrchidRouteFrame captureFrame(ServerPlayer player) {
        InteractionHand swingHand = player.swingingArm == null
                ? InteractionHand.MAIN_HAND : player.swingingArm;
        InteractionHand usedHand = player.isUsingItem()
                ? player.getUsedItemHand() : InteractionHand.MAIN_HAND;
        return new PinkOrchidRouteFrame(
                player.position(),
                player.yBodyRot,
                player.getYHeadRot(),
                player.getXRot(),
                player.getPose(),
                player.isSprinting(),
                player.swinging,
                swingHand,
                player.isUsingItem(),
                usedHand,
                sanitizeHeldItem(player.getMainHandItem()),
                sanitizeHeldItem(player.getOffhandItem())
        );
    }

    private static ItemStack sanitizeHeldItem(ItemStack stack) {
        return stack.is(ModItems.PINK_ORCHID.get()) ? ItemStack.EMPTY : stack.copy();
    }

    private static boolean isActiveOwner(ServerPlayer player) {
        int phase = GameManager.getPhaseValue();
        return (phase == PinkOrchidPerk.FIRST_ACTIVE_PHASE.getScoreboardValue()
                || phase == PinkOrchidPerk.LAST_ACTIVE_PHASE.getScoreboardValue())
                && player.gameMode.getGameModeForPlayer() == GameType.ADVENTURE
                && PerkTeam.fromPlayer(player) == PerkTeam.SURVIVOR
                && PlayerDataManager.get(player).getPerkInstance(PinkOrchidPerk.ID) != null;
    }

    private static void forceRouteChunks(ServerLevel level, UUID illusionId,
                                         List<PinkOrchidRouteFrame> frames) {
        Set<ChunkPos> chunks = new HashSet<>();
        for (PinkOrchidRouteFrame frame : frames) {
            chunks.add(new ChunkPos((int) Math.floor(frame.position().x) >> 4,
                    (int) Math.floor(frame.position().z) >> 4));
        }
        for (ChunkPos chunk : chunks) {
            level.getChunkSource().addRegionTicket(
                    ROUTE_TICKET, chunk, CHUNK_TICKET_DISTANCE, illusionId, true);
            level.getChunk(chunk.x, chunk.z);
        }
        FORCED_CHUNKS.put(illusionId, new ForcedChunks(level, chunks));
    }

    private static void releaseRouteChunks(UUID illusionId) {
        ForcedChunks forced = FORCED_CHUNKS.remove(illusionId);
        if (forced == null) return;
        ServerLevel level = forced.level();
        for (ChunkPos chunk : forced.chunks()) {
            level.getChunkSource().removeRegionTicket(
                    ROUTE_TICKET, chunk, CHUNK_TICKET_DISTANCE, illusionId, true);
        }
    }

    private static void removeActiveIllusion(MinecraftServer server, UUID ownerId,
                                              boolean shattered) {
        UUID illusionId = ACTIVE_BY_OWNER.remove(ownerId);
        if (illusionId == null) return;
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getEntity(illusionId) instanceof PinkOrchidIllusionEntity illusion) {
                finishIllusion(illusion, shattered);
                return;
            }
        }
        releaseRouteChunks(illusionId);
    }

    private static void clearPhaseThreeRuntime(MinecraftServer server) {
        if (RECORDINGS.isEmpty() && ROUTES.isEmpty() && ACTIVE_BY_OWNER.isEmpty()) return;
        clearAllInternal(server, false, false);
    }

    private static void clearAllInternal(MinecraftServer server, boolean removeItems,
                                         boolean resetMatchFlag) {
        for (UUID ownerId : new ArrayList<>(ACTIVE_BY_OWNER.keySet())) {
            removeActiveIllusion(server, ownerId, false);
        }
        RECORDINGS.clear();
        ROUTES.clear();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.removeEffect(ModEffects.ORCHID_RECORDING.get());
            ModNetworking.sendToPlayer(PinkOrchidStatePacket.clear(), player);
            if (removeItems) removeBoundItems(player);
        }
        for (UUID illusionId : new ArrayList<>(FORCED_CHUNKS.keySet())) {
            releaseRouteChunks(illusionId);
        }
        if (resetMatchFlag) matchInitialized = false;
    }

    private static void enforceBoundItems(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                if (!stack.is(ModItems.PINK_ORCHID.get())) continue;
                UUID ownerId = PinkOrchidItem.getOwner(stack);
                if (ownerId == null || ownerId.equals(player.getUUID())) continue;
                player.getInventory().setItem(slot, ItemStack.EMPTY);
                returnToOwner(server, ownerId, stack.copy());
            }
            recoverFromExternalMenu(player);
            recoverCarried(player);
        }
    }

    private static void recoverFromExternalMenu(ServerPlayer viewer) {
        for (net.minecraft.world.inventory.Slot slot : viewer.containerMenu.slots) {
            if (slot.container instanceof Inventory || !slot.hasItem()
                    || !slot.getItem().is(ModItems.PINK_ORCHID.get())) continue;
            ItemStack recovered = slot.getItem().copy();
            slot.set(ItemStack.EMPTY);
            UUID ownerId = PinkOrchidItem.getOwner(recovered);
            if (ownerId != null) returnToOwner(viewer.server, ownerId, recovered);
        }
    }

    private static void recoverCarried(ServerPlayer viewer) {
        ItemStack carried = viewer.containerMenu.getCarried();
        if (!carried.is(ModItems.PINK_ORCHID.get())) return;
        viewer.containerMenu.setCarried(ItemStack.EMPTY);
        UUID ownerId = PinkOrchidItem.getOwner(carried);
        if (ownerId != null) returnToOwner(viewer.server, ownerId, carried.copy());
    }

    private static void returnToOwner(MinecraftServer server, UUID ownerId, ItemStack stack) {
        ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
        if (owner != null && !hasBoundItem(owner)) {
            if (!owner.getInventory().add(stack)) owner.containerMenu.setCarried(stack);
            owner.inventoryMenu.broadcastChanges();
        }
    }

    private static void giveBoundItem(ServerPlayer owner) {
        removeBoundItems(owner);
        ItemStack orchid = PinkOrchidItem.createFor(owner);
        if (!owner.getInventory().add(orchid)) owner.containerMenu.setCarried(orchid);
        owner.inventoryMenu.broadcastChanges();
    }

    private static boolean hasBoundItem(ServerPlayer owner) {
        for (int slot = 0; slot < owner.getInventory().getContainerSize(); slot++) {
            if (PinkOrchidItem.belongsTo(owner.getInventory().getItem(slot), owner.getUUID())) {
                return true;
            }
        }
        return false;
    }

    private static void removeBoundItems(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(ModItems.PINK_ORCHID.get())) {
                player.getInventory().setItem(slot, ItemStack.EMPTY);
            }
        }
        if (player.containerMenu.getCarried().is(ModItems.PINK_ORCHID.get())) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
        }
    }

    private static void playMaterialize(ServerLevel level, Vec3 position) {
        sendColoredParticles(level, position, MATERIALIZE_PARTICLES,
                MATERIALIZE_PARTICLE_SPEED);
        level.sendParticles(ParticleTypes.CHERRY_LEAVES,
                position.x, position.y + SPAWN_PARTICLE_HEIGHT, position.z,
                MATERIALIZE_PARTICLES / 2,
                PARTICLE_HORIZONTAL_SPREAD, PARTICLE_VERTICAL_SPREAD,
                PARTICLE_HORIZONTAL_SPREAD, MATERIALIZE_PARTICLE_SPEED);
        level.playSound(null, position.x, position.y, position.z,
                SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS,
                SOUND_VOLUME, SPAWN_SOUND_PITCH);
    }

    private static void playShatter(ServerLevel level, Vec3 position) {
        sendColoredParticles(level, position, SHATTER_PARTICLES, SHATTER_PARTICLE_SPEED);
        level.sendParticles(ParticleTypes.CHERRY_LEAVES,
                position.x, position.y + SPAWN_PARTICLE_HEIGHT, position.z,
                SHATTER_PARTICLES,
                PARTICLE_HORIZONTAL_SPREAD, PARTICLE_VERTICAL_SPREAD,
                PARTICLE_HORIZONTAL_SPREAD, SHATTER_PARTICLE_SPEED);
        level.playSound(null, position.x, position.y, position.z,
                SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS,
                SOUND_VOLUME, SHATTER_SOUND_PITCH);
    }

    private static void playDissolve(ServerLevel level, Vec3 position) {
        level.sendParticles(new DustParticleOptions(PINK, MAIN_PARTICLE_SIZE),
                position.x, position.y + SPAWN_PARTICLE_HEIGHT, position.z,
                DISSOLVE_PARTICLES, PARTICLE_HORIZONTAL_SPREAD,
                PARTICLE_VERTICAL_SPREAD, PARTICLE_HORIZONTAL_SPREAD,
                DISSOLVE_PARTICLE_SPEED);
        level.sendParticles(ParticleTypes.CHERRY_LEAVES,
                position.x, position.y + SPAWN_PARTICLE_HEIGHT, position.z,
                DISSOLVE_PARTICLES,
                PARTICLE_HORIZONTAL_SPREAD, PARTICLE_VERTICAL_SPREAD,
                PARTICLE_HORIZONTAL_SPREAD, DISSOLVE_PARTICLE_SPEED);
        level.playSound(null, position.x, position.y, position.z,
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS,
                SOUND_VOLUME, DISSOLVE_SOUND_PITCH);
    }

    private static void sendColoredParticles(ServerLevel level, Vec3 position,
                                             int count, double speed) {
        level.sendParticles(new DustParticleOptions(PINK, MAIN_PARTICLE_SIZE),
                position.x, position.y + SPAWN_PARTICLE_HEIGHT, position.z,
                count, PARTICLE_HORIZONTAL_SPREAD, PARTICLE_VERTICAL_SPREAD,
                PARTICLE_HORIZONTAL_SPREAD, speed);
        level.sendParticles(new DustParticleOptions(CYAN, SECONDARY_PARTICLE_SIZE),
                position.x, position.y + SPAWN_PARTICLE_HEIGHT, position.z,
                count / 3, PARTICLE_HORIZONTAL_SPREAD, PARTICLE_VERTICAL_SPREAD,
                PARTICLE_HORIZONTAL_SPREAD, speed);
        level.sendParticles(new DustParticleOptions(VIOLET, SECONDARY_PARTICLE_SIZE),
                position.x, position.y + SPAWN_PARTICLE_HEIGHT, position.z,
                count / 3, PARTICLE_HORIZONTAL_SPREAD, PARTICLE_VERTICAL_SPREAD,
                PARTICLE_HORIZONTAL_SPREAD, speed);
    }

    private static final class RecordingSession {
        private final ResourceKey<Level> dimension;
        private final List<PinkOrchidRouteFrame> frames = new ArrayList<>();
        private int ticksUntilSample = PinkOrchidPerk.ROUTE_SAMPLE_INTERVAL_TICKS;

        private RecordingSession(ResourceKey<Level> dimension) {
            this.dimension = dimension;
        }

        private ResourceKey<Level> dimension() {
            return dimension;
        }

        private List<PinkOrchidRouteFrame> frames() {
            return frames;
        }

        private boolean advanceAndShouldSample() {
            ticksUntilSample--;
            if (ticksUntilSample > 0) return false;
            ticksUntilSample = PinkOrchidPerk.ROUTE_SAMPLE_INTERVAL_TICKS;
            return true;
        }
    }

    private record RecordedRoute(ResourceKey<Level> dimension,
                                 List<PinkOrchidRouteFrame> frames) {
    }

    private record ForcedChunks(ServerLevel level, Set<ChunkPos> chunks) {
    }
}
