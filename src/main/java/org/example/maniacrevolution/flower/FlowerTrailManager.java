package org.example.maniacrevolution.flower;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.downed.DownedCapability;
import org.example.maniacrevolution.downed.DownedData;
import org.example.maniacrevolution.downed.DownedState;
import org.example.maniacrevolution.game.GameManager;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.FlowerTrailPacket;
import org.example.maniacrevolution.perk.PerkPhase;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.perk.perks.maniac.FlowersLeadInvestigationPerk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/** Серверное состояние цветочных следов, общее для всех владельцев перка. */
public final class FlowerTrailManager {
    private static final double SURFACE_RENDER_OFFSET = 0.015D;
    private static final double SURFACE_RAY_START_OFFSET = 0.05D;
    private static final double DROP_START_HEIGHT_BLOCKS = 0.90D;

    private static final int MIN_FALL_DURATION_TICKS = 10;
    private static final int MAX_FALL_DURATION_TICKS = 16;
    private static final double BASE_FALL_DISTANCE_BLOCKS = 0.90D;
    private static final double EXTRA_FALL_TICKS_PER_BLOCK = 2.0D;

    private static final float MIN_FLOWER_SCALE = 0.90F;
    private static final float MAX_FLOWER_SCALE = 1.10F;
    private static final float FULL_ROTATION_DEGREES = 360.0F;
    private static final double FULL_CIRCLE_RADIANS = Math.PI * 2.0D;
    private static final long ASSIGNMENT_SEED_MULTIPLIER = 0x9E3779B97F4A7C15L;

    private static final Map<UUID, FlowerVariant> PLAYER_FLOWERS = new HashMap<>();
    private static final Map<UUID, Long> NEXT_SPAWN_TICKS = new HashMap<>();
    private static final Map<UUID, ResourceKey<Level>> SYNCED_VIEWER_DIMENSIONS = new HashMap<>();
    private static final List<FlowerTrace> ACTIVE_TRACES = new ArrayList<>();
    private static final List<FlowerVariant> ASSIGNMENT_ORDER = new ArrayList<>();
    private static final List<FlowerVariant> COLORFUL_TEST_VARIANTS =
            java.util.Arrays.stream(FlowerVariant.values())
                    .filter(FlowerVariant::hasColorfulPriority)
                    .toList();

    private static RandomSource visualRandom = RandomSource.create();
    private static int nextAssignmentIndex;
    private static long nextTraceId = 1L;
    private static long nextTestTraceId = Long.MAX_VALUE;
    private static long lastProcessedGameTick = Long.MIN_VALUE;
    private static long lastStartSignalGameTick = Long.MIN_VALUE;
    private static boolean initialized;

    private FlowerTrailManager() {
    }

    public static void startMatch(MinecraftServer server) {
        long gameTick = getGameTick(server);
        if (initialized && lastStartSignalGameTick == gameTick) {
            return;
        }

        lastStartSignalGameTick = gameTick;
        resetForMatch(server, gameTick);
    }

    public static void showExistingTrailsTo(ServerPlayer player) {
        ensureInitialized(player.server);
        if (isEligibleViewer(player)) {
            syncViewer(player, getGameTick(player.server));
        } else {
            hideTrailsFrom(player);
        }
    }

    public static void hideTrailsFrom(ServerPlayer player) {
        SYNCED_VIEWER_DIMENSIONS.remove(player.getUUID());
        ModNetworking.sendToPlayer(FlowerTrailPacket.clear(), player);
    }

    public static void tick(MinecraftServer server) {
        ensureInitialized(server);

        PerkPhase phase = GameManager.getCurrentPhase();
        if (phase != PerkPhase.HUNT && phase != PerkPhase.MIDGAME) {
            return;
        }

        long gameTick = getGameTick(server);
        if (lastProcessedGameTick == gameTick) {
            return;
        }
        lastProcessedGameTick = gameTick;

        ACTIVE_TRACES.removeIf(trace -> trace.getAgeTicks(gameTick)
                >= FlowersLeadInvestigationPerk.FLOWER_LIFETIME_TICKS);

        List<ServerPlayer> viewers = collectEligibleViewers(server);
        synchronizeViewers(server, viewers, gameTick);
        if (viewers.isEmpty()) {
            return;
        }

        for (ServerPlayer survivor : server.getPlayerList().getPlayers()) {
            if (!isActiveSurvivor(survivor)) {
                NEXT_SPAWN_TICKS.put(
                        survivor.getUUID(),
                        gameTick + FlowersLeadInvestigationPerk.SPAWN_INTERVAL_TICKS
                );
                continue;
            }

            assignFlower(survivor.getUUID());
            long nextSpawnTick = NEXT_SPAWN_TICKS.computeIfAbsent(
                    survivor.getUUID(),
                    ignored -> gameTick + FlowersLeadInvestigationPerk.SPAWN_INTERVAL_TICKS
            );
            if (gameTick < nextSpawnTick) {
                continue;
            }

            spawnTrace(survivor, viewers, gameTick);
            NEXT_SPAWN_TICKS.put(
                    survivor.getUUID(),
                    gameTick + FlowersLeadInvestigationPerk.SPAWN_INTERVAL_TICKS
            );
        }
    }

    /** Spawns one real flower-trail animation for testing, visible to the whole dimension. */
    public static int spawnTestTrace(ServerPlayer source) {
        long gameTick = getGameTick(source.server);
        Vec3 target = findSurfaceBelow(source);
        double startX = source.getX();
        double startY = source.getY() + DROP_START_HEIGHT_BLOCKS;
        double startZ = source.getZ();
        int fallDurationTicks = calculateFallDuration(startY - target.y);

        double windAngle = visualRandom.nextDouble() * FULL_CIRCLE_RADIANS;
        FlowerVariant variant = COLORFUL_TEST_VARIANTS.get(
                visualRandom.nextInt(COLORFUL_TEST_VARIANTS.size())
        );
        FlowerTrace trace = new FlowerTrace(
                nextTestTraceId--,
                source.level().dimension(),
                gameTick,
                startX,
                startY,
                startZ,
                target.x,
                target.y,
                target.z,
                variant,
                fallDurationTicks,
                (float) Math.cos(windAngle),
                (float) Math.sin(windAngle),
                visualRandom.nextFloat() * FULL_ROTATION_DEGREES,
                Mth.lerp(visualRandom.nextFloat(), MIN_FLOWER_SCALE, MAX_FLOWER_SCALE)
        );

        FlowerTrailPacket packet = trace.createPacket(0);
        int viewers = 0;
        for (ServerPlayer player : source.serverLevel().players()) {
            ModNetworking.sendToPlayer(packet, player);
            viewers++;
        }
        return viewers;
    }

    /** Возвращает единый назначенный игроку на текущий матч цветок. */
    public static FlowerVariant getAssignedFlower(ServerPlayer player) {
        ensureInitialized(player.server);
        return assignFlower(player.getUUID());
    }

    private static void ensureInitialized(MinecraftServer server) {
        if (!initialized) {
            resetForMatch(server, getGameTick(server));
        }
    }

    private static void resetForMatch(MinecraftServer server, long gameTick) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (hasSelectedPerk(player)) {
                ModNetworking.sendToPlayer(FlowerTrailPacket.clear(), player);
            }
        }

        PLAYER_FLOWERS.clear();
        NEXT_SPAWN_TICKS.clear();
        SYNCED_VIEWER_DIMENSIONS.clear();
        ACTIVE_TRACES.clear();
        ASSIGNMENT_ORDER.clear();
        nextAssignmentIndex = 0;
        nextTraceId = 1L;
        lastProcessedGameTick = Long.MIN_VALUE;
        initialized = true;

        long randomSeed = server.overworld().getSeed()
                ^ gameTick * ASSIGNMENT_SEED_MULTIPLIER;
        visualRandom = RandomSource.create(randomSeed);
        buildAssignmentOrder(randomSeed);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (isActiveSurvivor(player)) {
                assignFlower(player.getUUID());
                NEXT_SPAWN_TICKS.put(
                        player.getUUID(),
                        gameTick + FlowersLeadInvestigationPerk.SPAWN_INTERVAL_TICKS
                );
            }
        }
    }

    private static void buildAssignmentOrder(long seed) {
        List<FlowerVariant> colorful = new ArrayList<>();
        List<FlowerVariant> remaining = new ArrayList<>();
        for (FlowerVariant variant : FlowerVariant.values()) {
            (variant.hasColorfulPriority() ? colorful : remaining).add(variant);
        }

        Random random = new Random(seed);
        java.util.Collections.shuffle(colorful, random);
        java.util.Collections.shuffle(remaining, random);
        ASSIGNMENT_ORDER.addAll(colorful);
        ASSIGNMENT_ORDER.addAll(remaining);
    }

    private static FlowerVariant assignFlower(UUID playerId) {
        return PLAYER_FLOWERS.computeIfAbsent(playerId, ignored -> {
            if (ASSIGNMENT_ORDER.isEmpty()) {
                ASSIGNMENT_ORDER.addAll(List.of(FlowerVariant.values()));
            }
            FlowerVariant variant = ASSIGNMENT_ORDER.get(
                    nextAssignmentIndex % ASSIGNMENT_ORDER.size()
            );
            nextAssignmentIndex++;
            return variant;
        });
    }

    private static void spawnTrace(ServerPlayer survivor, List<ServerPlayer> viewers,
                                   long gameTick) {
        Vec3 target = findSurfaceBelow(survivor);
        double startX = survivor.getX();
        double startY = survivor.getY() + DROP_START_HEIGHT_BLOCKS;
        double startZ = survivor.getZ();
        int fallDurationTicks = calculateFallDuration(startY - target.y);

        double windAngle = visualRandom.nextDouble() * FULL_CIRCLE_RADIANS;
        float windX = (float) Math.cos(windAngle);
        float windZ = (float) Math.sin(windAngle);
        float rotation = visualRandom.nextFloat() * FULL_ROTATION_DEGREES;
        float scale = Mth.lerp(
                visualRandom.nextFloat(),
                MIN_FLOWER_SCALE,
                MAX_FLOWER_SCALE
        );

        FlowerTrace trace = new FlowerTrace(
                nextTraceId++,
                survivor.level().dimension(),
                gameTick,
                startX,
                startY,
                startZ,
                target.x,
                target.y,
                target.z,
                assignFlower(survivor.getUUID()),
                fallDurationTicks,
                windX,
                windZ,
                rotation,
                scale
        );
        ACTIVE_TRACES.add(trace);

        FlowerTrailPacket packet = trace.createPacket(0);
        for (ServerPlayer viewer : viewers) {
            if (viewer.level().dimension().equals(trace.dimension())) {
                ModNetworking.sendToPlayer(packet, viewer);
            }
        }
    }

    private static Vec3 findSurfaceBelow(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        double x = player.getX();
        double z = player.getZ();
        Vec3 rayStart = new Vec3(
                x,
                player.getY() + SURFACE_RAY_START_OFFSET,
                z
        );
        Vec3 rayEnd = new Vec3(
                x,
                player.getY() - FlowersLeadInvestigationPerk.SURFACE_SEARCH_DEPTH_BLOCKS,
                z
        );
        BlockHitResult hit = level.clip(new ClipContext(
                rayStart,
                rayEnd,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));
        if (hit.getType() == HitResult.Type.BLOCK) {
            Vec3 location = hit.getLocation();
            return new Vec3(location.x, location.y + SURFACE_RENDER_OFFSET, location.z);
        }

        return new Vec3(x, player.getY() + SURFACE_RENDER_OFFSET, z);
    }

    private static int calculateFallDuration(double fallDistanceBlocks) {
        double extraDistance = Math.max(
                0.0D,
                fallDistanceBlocks - BASE_FALL_DISTANCE_BLOCKS
        );
        int calculated = MIN_FALL_DURATION_TICKS
                + (int) Math.round(extraDistance * EXTRA_FALL_TICKS_PER_BLOCK);
        return Mth.clamp(calculated, MIN_FALL_DURATION_TICKS, MAX_FALL_DURATION_TICKS);
    }

    private static void synchronizeViewers(MinecraftServer server,
                                           List<ServerPlayer> viewers,
                                           long gameTick) {
        Set<UUID> currentViewerIds = new HashSet<>();
        for (ServerPlayer viewer : viewers) {
            currentViewerIds.add(viewer.getUUID());
            ResourceKey<Level> syncedDimension = SYNCED_VIEWER_DIMENSIONS.get(viewer.getUUID());
            if (!viewer.level().dimension().equals(syncedDimension)) {
                syncViewer(viewer, gameTick);
            }
        }

        List<UUID> removedViewerIds = SYNCED_VIEWER_DIMENSIONS.keySet().stream()
                .filter(id -> !currentViewerIds.contains(id))
                .toList();
        for (UUID viewerId : removedViewerIds) {
            ServerPlayer player = server.getPlayerList().getPlayer(viewerId);
            if (player != null) {
                ModNetworking.sendToPlayer(FlowerTrailPacket.clear(), player);
            }
            SYNCED_VIEWER_DIMENSIONS.remove(viewerId);
        }
    }

    private static void syncViewer(ServerPlayer viewer, long gameTick) {
        ModNetworking.sendToPlayer(FlowerTrailPacket.clear(), viewer);
        ResourceKey<Level> viewerDimension = viewer.level().dimension();
        for (FlowerTrace trace : ACTIVE_TRACES) {
            int ageTicks = trace.getAgeTicks(gameTick);
            if (trace.dimension().equals(viewerDimension)
                    && ageTicks < FlowersLeadInvestigationPerk.FLOWER_LIFETIME_TICKS) {
                ModNetworking.sendToPlayer(trace.createPacket(ageTicks), viewer);
            }
        }
        SYNCED_VIEWER_DIMENSIONS.put(viewer.getUUID(), viewerDimension);
    }

    private static List<ServerPlayer> collectEligibleViewers(MinecraftServer server) {
        List<ServerPlayer> viewers = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (isEligibleViewer(player)) {
                viewers.add(player);
            }
        }
        return viewers;
    }

    private static boolean isEligibleViewer(ServerPlayer player) {
        return player.isAlive()
                && !player.isRemoved()
                && !player.isSpectator()
                && PerkTeam.fromPlayer(player) == PerkTeam.MANIAC
                && hasSelectedPerk(player);
    }

    private static boolean hasSelectedPerk(ServerPlayer player) {
        return PlayerDataManager.get(player).getPerkInstance(
                FlowersLeadInvestigationPerk.ID
        ) != null;
    }

    private static boolean isActiveSurvivor(ServerPlayer player) {
        if (!player.isAlive()
                || player.isRemoved()
                || player.gameMode.getGameModeForPlayer() != GameType.ADVENTURE
                || PerkTeam.fromPlayer(player) != PerkTeam.SURVIVOR) {
            return false;
        }

        DownedData downedData = DownedCapability.get(player);
        return downedData == null || downedData.getState() != DownedState.DOWNED;
    }

    private static long getGameTick(MinecraftServer server) {
        return server.overworld().getGameTime();
    }

    private record FlowerTrace(long id, ResourceKey<Level> dimension, long createdGameTick,
                               double startX, double startY, double startZ,
                               double targetX, double targetY, double targetZ,
                               FlowerVariant variant, int fallDurationTicks,
                               float windX, float windZ,
                               float baseRotationDegrees, float scale) {
        private int getAgeTicks(long gameTick) {
            return (int) Math.max(0L, gameTick - createdGameTick);
        }

        private FlowerTrailPacket createPacket(int ageTicks) {
            return FlowerTrailPacket.add(
                    id,
                    startX,
                    startY,
                    startZ,
                    targetX,
                    targetY,
                    targetZ,
                    variant,
                    FlowersLeadInvestigationPerk.FLOWER_LIFETIME_TICKS,
                    ageTicks,
                    fallDurationTicks,
                    windX,
                    windZ,
                    baseRotationDegrees,
                    scale
            );
        }
    }
}
