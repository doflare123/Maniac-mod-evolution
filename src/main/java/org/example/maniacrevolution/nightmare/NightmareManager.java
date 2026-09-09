package org.example.maniacrevolution.nightmare;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.data.PlayerData;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.maze.MazeGenerator;
import org.example.maniacrevolution.maze.MazeManager;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.NightmareScreamerPacket;
import org.example.maniacrevolution.network.packets.SyncNightmarePacket;
import org.example.maniacrevolution.perk.perks.survivor.RealityAnchorPerk;
import org.example.maniacrevolution.util.ManiacDamageAttribution;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class NightmareManager {
    private static final int COCOON_SEARCH_RADIUS = 8;
    private static final int COCOON_SEARCH_DOWN = 2;
    private static final int COCOON_SEARCH_UP = 3;
    private static final String RECONNECT_STATE_TAG = "maniacrevNightmareReconnect";
    private static final NightmareSyncState DISABLED_STATE = new NightmareSyncState(
            false, NightmareConfig.MAX_SANITY, NightmareTrialType.NONE, 0, 0);

    private static final NightmareManager INSTANCE = new NightmareManager();

    public static NightmareManager getInstance() {
        return INSTANCE;
    }

    private final Map<UUID, NightmarePlayerState> states = new ConcurrentHashMap<>();
    private final Map<UUID, NightmareSyncState> lastSentStates = new HashMap<>();
    private final Map<UUID, Integer> lastSentTicks = new HashMap<>();
    private UUID sessionId = UUID.randomUUID();
    private int nextTrialIndex;

    private NightmareManager() {}

    public void tick(MinecraftServer server) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        boolean keeperPresent = players.stream().anyMatch(this::isKeeper);

        if (!keeperPresent) {
            syncDisabled(players);
            return;
        }

        long tick = server.overworld().getGameTime();
        List<ServerPlayer> keepers = players.stream().filter(this::isKeeper).toList();

        for (ServerPlayer player : players) {
            NightmarePlayerState state = state(player);
            if (isKeeper(player) || player.isSpectator() || player.isCreative()) {
                sendState(player, new NightmareSyncState(false, state.sanity,
                        NightmareTrialType.NONE, 0, 0));
                continue;
            }

            if (state.isInTrial()) {
                tickTrial(player, state, tick);
            } else {
                tickSanity(player, state, keepers, tick);
            }

            sync(player, state, keeperPresent);
        }
    }

    public boolean onCocoonHit(ServerPlayer rescuer, BlockPos pos) {
        if (!rescuer.getMainHandItem().is(ModItems.AWAKENING_NEEDLE.get())) {
            return false;
        }

        for (ServerPlayer player : rescuer.server.getPlayerList().getPlayers()) {
            NightmarePlayerState state = states.get(player.getUUID());
            if (state != null && state.isInTrial() && pos.equals(state.cocoonPos)) {
                rescueFromTrial(player, state);
                return true;
            }
        }
        return false;
    }

    public boolean castConcentratedNightmare(ServerPlayer keeper) {
        if (!isKeeper(keeper)) {
            keeper.displayClientMessage(Component.literal("Эта способность доступна только Хранителю кошмаров"), true);
            return false;
        }

        ServerPlayer target = findGazedSurvivor(keeper, NightmareConfig.CONCENTRATED_NIGHTMARE_RANGE);
        if (target == null) {
            keeper.displayClientMessage(Component.literal("Нет цели для кошмара"), true);
            return false;
        }

        NightmarePlayerState state = state(target);
        state.responsibleKeeperId = keeper.getUUID();
        state.sanity = Math.max(0.0F, state.sanity -
                NightmareConfig.MAX_SANITY * NightmareConfig.CONCENTRATED_NIGHTMARE_SANITY_PERCENT);
        ModNetworking.sendToPlayer(new NightmareScreamerPacket(NightmareConfig.CONCENTRATED_NIGHTMARE_SCREAMER_TICKS), target);
        sync(target, state, true);
        target.displayClientMessage(Component.literal("Кошмар ударил по рассудку"), true);
        return true;
    }

    public void onFearChaserCaught(ServerPlayer target) {
        NightmarePlayerState state = states.get(target.getUUID());
        if (state != null && state.trialType == NightmareTrialType.FEAR_RACE) {
            finishFearRaceDeath(target, state);
        }
    }

    public void onPlayerDeath(ServerPlayer player) {
        NightmarePlayerState state = states.get(player.getUUID());
        if (state != null && state.trialType == NightmareTrialType.ARENA) {
            player.setGameMode(GameType.SPECTATOR);
            cleanupTrialArea(state);
            if (state.cocoonPos != null && state.returnLevel != null) {
                state.returnLevel.destroyBlock(state.cocoonPos, false);
            }
            restoreInventory(player, state);
            state.clearTrial();
        }
    }

    public void clear(ServerPlayer player) {
        NightmarePlayerState state = state(player);
        if (state.mazeId != null) {
            MazeManager.getInstance().destroyMaze(state.mazeId);
        }
        if (state.trialArea != null) {
            state.trialArea.destroy();
        }
        if (state.cocoonPos != null && state.returnLevel != null) {
            state.returnLevel.destroyBlock(state.cocoonPos, false);
        }
        state.sanity = NightmareConfig.MAX_SANITY;
        state.lastGazeTick = Long.MIN_VALUE;
        state.abductionCooldownUntil = 0L;
        state.sanityImmunityUntil = 0L;
        state.mazeTrialsStarted = 0;
        restoreInventory(player, state);
        state.clearTrial();
        sync(player, state, hasKeeper(player.server));
    }

    public int clearAll(MinecraftServer server) {
        // A global reset also invalidates detached snapshots of offline players.
        sessionId = UUID.randomUUID();
        int count = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            clear(player);
            count++;
        }
        states.keySet().removeIf(uuid -> server.getPlayerList().getPlayer(uuid) == null);
        return count;
    }

    /** Called before vanilla saves the disconnecting player's position and inventory. */
    public void onPlayerLogout(ServerPlayer player) {
        NightmarePlayerState state = states.remove(player.getUUID());
        if (state != null) {
            abortTrial(player, state);
            // Preserve scalar gameplay state without retaining offline player/level
            // objects. A reconnect must not reset sanity or the forced-maze counter.
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Session", sessionId);
            tag.putFloat("Sanity", state.sanity);
            tag.putLong("LastGazeTick", state.lastGazeTick);
            tag.putLong("AbductionCooldown", state.abductionCooldownUntil);
            tag.putLong("SanityImmunity", state.sanityImmunityUntil);
            tag.putInt("MazeTrialsStarted", state.mazeTrialsStarted);
            player.getPersistentData().put(RECONNECT_STATE_TAG, tag);
        }
        invalidateSync(player);
    }

    public void invalidateSync(ServerPlayer player) {
        lastSentStates.remove(player.getUUID());
        lastSentTicks.remove(player.getUUID());
    }

    public void shutdown(MinecraftServer server) {
        for (ServerPlayer player : new ArrayList<>(server.getPlayerList().getPlayers())) {
            onPlayerLogout(player);
        }
        for (NightmarePlayerState state : states.values()) {
            removeTrialBlocks(state);
        }
        states.clear();
        lastSentStates.clear();
        lastSentTicks.clear();
        nextTrialIndex = 0;
        sessionId = UUID.randomUUID();
        MazeManager.getInstance().clearAll();
    }

    private void abortTrial(ServerPlayer player, NightmarePlayerState state) {
        // Return before removing the floor under the player. An interrupted trial
        // grants neither success rewards nor failure damage.
        if (state.isInTrial() && state.returnLevel != null && state.returnPos != null) {
            BlockPos pos = state.returnPos;
            player.teleportTo(state.returnLevel, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                    player.getYRot(), player.getXRot());
        }
        if (state.savedMainInventory != null) {
            restoreInventory(player, state);
        }
        removeTrialBlocks(state);
        state.clearTrial();
    }

    private void removeTrialBlocks(NightmarePlayerState state) {
        if (state.mazeId != null) {
            MazeManager.getInstance().destroyMaze(state.mazeId);
        }
        cleanupTrialArea(state);
        if (state.cocoonPos != null && state.returnLevel != null) {
            state.returnLevel.destroyBlock(state.cocoonPos, false);
        }
    }

    public boolean isKeeper(ServerPlayer player) {
        PlayerData data = PlayerDataManager.get(player);
        return data.getManiacClassId() == NightmareConfig.KEEPER_CLASS_ID;
    }

    public UUID getTrialResponsibleKeeperId(ServerPlayer player) {
        NightmarePlayerState state = states.get(player.getUUID());
        return state != null && state.isInTrial() ? state.responsibleKeeperId : null;
    }

    private void tickSanity(ServerPlayer player, NightmarePlayerState state,
                            List<ServerPlayer> keepers, long tick) {
        if (tick < state.sanityImmunityUntil) {
            state.sanity = Math.min(NightmareConfig.MAX_SANITY,
                    state.sanity + NightmareConfig.SANITY_REGEN_PER_TICK);
            return;
        }

        ServerPlayer responsibleWatcher = null;
        double nearestWatcherDistanceSquared = Double.MAX_VALUE;
        for (ServerPlayer keeper : keepers) {
            if (isLookingAt(keeper, player, NightmareConfig.GAZE_RANGE, NightmareConfig.GAZE_DOT_THRESHOLD)) {
                double distanceSquared = keeper.distanceToSqr(player);
                if (distanceSquared < nearestWatcherDistanceSquared) {
                    nearestWatcherDistanceSquared = distanceSquared;
                    responsibleWatcher = keeper;
                }
            }
        }

        if (responsibleWatcher != null) {
            state.lastGazeTick = tick;
            if (state.sanity > NightmareConfig.SANITY_BREAKPOINT) {
                state.responsibleKeeperId = responsibleWatcher.getUUID();
            }
            state.sanity = Math.max(0.0F, state.sanity - NightmareConfig.SANITY_DRAIN_PER_TICK);
        } else if (tick - state.lastGazeTick >= NightmareConfig.SANITY_REGEN_DELAY_TICKS) {
            state.sanity = Math.min(NightmareConfig.MAX_SANITY,
                    state.sanity + NightmareConfig.SANITY_REGEN_PER_TICK);
        }

        if (state.sanity <= NightmareConfig.SANITY_BREAKPOINT && tick >= state.abductionCooldownUntil) {
            if (RealityAnchorPerk.tryDelayNightmare(player)) {
                state.abductionCooldownUntil = tick + RealityAnchorPerk.NIGHTMARE_DELAY_TICKS;
            } else {
                startTrial(player, state, tick);
            }
        }
    }

    private void startTrial(ServerPlayer player, NightmarePlayerState state, long tick) {
        if (state.mazeTrialsStarted < NightmareConfig.FORCED_MAZE_COUNT) {
            startMazeTrial(player, state, tick);
        } else if (player.getRandom().nextBoolean()) {
            startArenaTrial(player, state, tick);
        } else {
            startRaceTrial(player, state, tick);
        }
    }

    private void startMazeTrial(ServerPlayer player, NightmarePlayerState state, long tick) {
        ServerLevel level = (ServerLevel) player.level();
        captureTrialInventory(player, state);
        state.returnLevel = level;
        state.returnPos = player.blockPosition();
        state.trialType = NightmareTrialType.MAZE;
        state.mazeTrialsStarted++;
        state.trialEndsAt = tick + NightmareConfig.MAZE_DURATION_TICKS;
        state.sanity = NightmareConfig.MAX_SANITY;

        BlockPos origin = nextTrialOrigin();
        long seed = level.getGameTime() ^ player.getUUID().getMostSignificantBits();
        state.mazeId = MazeManager.getInstance().spawnMaze(level, origin, seed);

        BlockPos usedOrigin = MazeManager.getInstance().getOrigin(state.mazeId);
        MazeGenerator generator = new MazeGenerator(seed);
        BlockPos entry = generator.getEntryPoint(usedOrigin);
        state.exitPos = generator.getExitPoint(usedOrigin);

        placeCocoon(level, state);

        player.teleportTo(level, entry.getX() + 0.5D, entry.getY(), entry.getZ() + 0.5D,
                player.getYRot(), player.getXRot());
        giveTrialLighter(player);
        player.displayClientMessage(Component.literal("Найди выход из лабиринта"), true);
    }

    private void startArenaTrial(ServerPlayer player, NightmarePlayerState state, long tick) {
        ServerLevel level = (ServerLevel) player.level();
        captureTrialInventory(player, state);
        state.returnLevel = level;
        state.returnPos = player.blockPosition();
        state.trialType = NightmareTrialType.ARENA;
        state.trialEndsAt = tick + NightmareConfig.ARENA_DURATION_TICKS;
        state.sanity = NightmareConfig.MAX_SANITY;

        BlockPos origin = nextTrialOrigin();
        state.trialArea = NightmareTrialBuilder.buildArena(level, origin, level.getGameTime() ^ player.getUUID().getLeastSignificantBits());
        placeCocoon(level, state);

        BlockPos spawn = NightmareTrialBuilder.arenaSpawn(origin);
        player.teleportTo(level, spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D,
                player.getYRot(), player.getXRot());
        giveTrialLighter(player);
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                NightmareConfig.ARENA_DURATION_TICKS + 40, 0, false, true));
        player.displayClientMessage(Component.literal("Выживи на арене"), true);
    }

    private void startRaceTrial(ServerPlayer player, NightmarePlayerState state, long tick) {
        ServerLevel level = (ServerLevel) player.level();
        captureTrialInventory(player, state);
        state.returnLevel = level;
        state.returnPos = player.blockPosition();
        state.trialType = NightmareTrialType.FEAR_RACE;
        state.raceStartsAt = tick + NightmareConfig.FEAR_RACE_COUNTDOWN_TICKS;
        state.trialEndsAt = state.raceStartsAt + NightmareConfig.FEAR_RACE_DURATION_TICKS;
        state.sanity = NightmareConfig.MAX_SANITY;

        BlockPos origin = nextTrialOrigin();
        state.trialArea = NightmareTrialBuilder.buildRace(level, origin, player.getUUID(),
                level.getGameTime() ^ player.getUUID().getMostSignificantBits(), state.raceStartsAt);
        state.raceFinishPos = NightmareTrialBuilder.raceFinish(origin);
        placeCocoon(level, state);

        BlockPos spawn = NightmareTrialBuilder.raceSpawn(origin);
        player.teleportTo(level, spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D,
                0.0F, 0.0F);
        giveTrialLighter(player);
        player.displayClientMessage(Component.literal("Беги до конца дороги"), true);
    }

    private void placeCocoon(ServerLevel level, NightmarePlayerState state) {
        BlockPos preferredPos = state.returnPos != null ? state.returnPos : level.getSharedSpawnPos();
        BlockPos cocoonPos = findNearestCocoonAir(level, preferredPos);
        if (cocoonPos == null) {
            state.cocoonPos = null;
            org.example.maniacrevolution.Maniacrev.LOGGER.warn(
                    "Could not place nightmare cocoon near {} without replacing blocks", preferredPos);
            return;
        }

        state.cocoonPos = cocoonPos;
        level.setBlockAndUpdate(cocoonPos, org.example.maniacrevolution.block.ModBlocks.NIGHTMARE_COCOON.get().defaultBlockState());
    }

    private BlockPos findNearestCocoonAir(ServerLevel level, BlockPos origin) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (int dy = -COCOON_SEARCH_DOWN; dy <= COCOON_SEARCH_UP; dy++) {
            int y = origin.getY() + dy;
            if (y < level.getMinBuildHeight() || y + 1 >= level.getMaxBuildHeight()) {
                continue;
            }

            for (int dx = -COCOON_SEARCH_RADIUS; dx <= COCOON_SEARCH_RADIUS; dx++) {
                for (int dz = -COCOON_SEARCH_RADIUS; dz <= COCOON_SEARCH_RADIUS; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (!canPlaceCocoonAt(level, pos)) {
                        continue;
                    }

                    double distance = origin.distSqr(pos);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = pos.immutable();
                    }
                }
            }
        }

        return best;
    }

    private boolean canPlaceCocoonAt(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir();
    }

    private void tickTrial(ServerPlayer player, NightmarePlayerState state, long tick) {
        if (state.trialType == NightmareTrialType.MAZE) {
            if (state.exitPos != null && player.blockPosition().distSqr(state.exitPos) <=
                    NightmareConfig.MAZE_EXIT_RADIUS * NightmareConfig.MAZE_EXIT_RADIUS) {
                finishTrial(player, state, false, 0.0F);
                return;
            }
            if (tick >= state.trialEndsAt) {
                finishTrial(player, state, true, NightmareConfig.MAZE_FAIL_DAMAGE);
            }
        } else if (state.trialType == NightmareTrialType.ARENA) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, true));
            if (!player.isAlive()) {
                player.setGameMode(GameType.SPECTATOR);
                cleanupTrialArea(state);
                state.clearTrial();
                return;
            }
            if (tick >= state.trialEndsAt) {
                finishTrial(player, state, false, 0.0F);
            }
        } else if (state.trialType == NightmareTrialType.FEAR_RACE) {
            if (tick < state.raceStartsAt) {
                long seconds = Math.max(1L, (state.raceStartsAt - tick + 19L) / 20L);
                player.displayClientMessage(Component.literal(String.valueOf(seconds)), true);
                return;
            }
            if (isRaceFinishDoorOpened(player, state)) {
                finishTrial(player, state, false, 0.0F);
                return;
            }
            if (tick >= state.trialEndsAt) {
                finishTrial(player, state, true, NightmareConfig.FEAR_RACE_FAIL_DAMAGE);
            }
        }
    }

    private void finishTrial(ServerPlayer player, NightmarePlayerState state, boolean failed, float damage) {
        ServerLevel returnLevel = state.returnLevel != null ? state.returnLevel : (ServerLevel) player.level();
        BlockPos returnPos = state.returnPos != null ? state.returnPos : returnLevel.getSharedSpawnPos();

        if (state.mazeId != null) {
            MazeManager.getInstance().destroyMaze(state.mazeId);
        }
        cleanupTrialArea(state);
        if (state.cocoonPos != null) returnLevel.destroyBlock(state.cocoonPos, false);

        player.teleportTo(returnLevel, returnPos.getX() + 0.5D, returnPos.getY(), returnPos.getZ() + 0.5D,
                player.getYRot(), player.getXRot());
        restoreInventory(player, state);
        if (damage > 0.0F) {
            player.hurt(player.damageSources().magic(), damage);
        }
        state.abductionCooldownUntil = returnLevel.getGameTime() + NightmareConfig.ABDUCTION_COOLDOWN_TICKS;
        state.sanityImmunityUntil = returnLevel.getGameTime() + NightmareConfig.SANITY_IMMUNITY_AFTER_TRIAL_TICKS;
        state.clearTrial();
        player.displayClientMessage(Component.literal(failed ? "Испытание провалено" : "Испытание пройдено"), true);
    }

    private void cleanupTrialArea(NightmarePlayerState state) {
        if (state.trialArea != null) {
            state.trialArea.destroy();
        }
    }

    private void finishFearRaceDeath(ServerPlayer player, NightmarePlayerState state) {
        ServerLevel returnLevel = state.returnLevel != null ? state.returnLevel : (ServerLevel) player.level();
        UUID responsibleKeeperId = state.responsibleKeeperId;

        cleanupTrialArea(state);
        if (state.cocoonPos != null) returnLevel.destroyBlock(state.cocoonPos, false);
        restoreInventory(player, state);
        state.clearTrial();
        ManiacDamageAttribution.hurtWithSource(
                player,
                responsibleKeeperId,
                () -> player.hurt(player.damageSources().magic(), Float.MAX_VALUE)
        );
        player.setGameMode(GameType.SPECTATOR);
    }

    private void captureTrialInventory(ServerPlayer player, NightmarePlayerState state) {
        if (state.savedMainInventory != null) return;
        state.savedMainInventory = player.getInventory().items.stream().map(ItemStack::copy).toList();
        state.savedArmorInventory = player.getInventory().armor.stream().map(ItemStack::copy).toList();
        state.savedOffhandInventory = player.getInventory().offhand.stream().map(ItemStack::copy).toList();
        player.getInventory().clearContent();
    }

    private void restoreInventory(ServerPlayer player, NightmarePlayerState state) {
        org.example.maniacrevolution.item.NightmareLighterItem.clearState(player);
        player.removeEffect(MobEffects.WEAKNESS);
        if (state.savedMainInventory == null) return;

        player.getInventory().clearContent();
        for (int i = 0; i < state.savedMainInventory.size() && i < player.getInventory().items.size(); i++) {
            player.getInventory().items.set(i, state.savedMainInventory.get(i).copy());
        }
        for (int i = 0; i < state.savedArmorInventory.size() && i < player.getInventory().armor.size(); i++) {
            player.getInventory().armor.set(i, state.savedArmorInventory.get(i).copy());
        }
        for (int i = 0; i < state.savedOffhandInventory.size() && i < player.getInventory().offhand.size(); i++) {
            player.getInventory().offhand.set(i, state.savedOffhandInventory.get(i).copy());
        }
        state.savedMainInventory = null;
        state.savedArmorInventory = null;
        state.savedOffhandInventory = null;
        player.inventoryMenu.broadcastChanges();
    }

    private void giveTrialLighter(ServerPlayer player) {
        player.getInventory().setItem(0, new ItemStack(ModItems.NIGHTMARE_LIGHTER.get()));
        player.getInventory().selected = 0;
        org.example.maniacrevolution.item.NightmareLighterItem.resetFuel(player);
        player.inventoryMenu.broadcastChanges();
    }

    private void rescueFromTrial(ServerPlayer player, NightmarePlayerState state) {
        finishTrial(player, state, false, NightmareConfig.COCOON_RESCUE_DAMAGE);
    }

    private boolean isRaceFinishDoorOpened(ServerPlayer player, NightmarePlayerState state) {
        if (state.raceFinishPos == null || player.level() != state.returnLevel) return false;
        if (player.blockPosition().distSqr(state.raceFinishPos) > 9.0D) return false;

        BlockState doorState = player.level().getBlockState(state.raceFinishPos);
        return doorState.getBlock() instanceof DoorBlock && doorState.getValue(DoorBlock.OPEN);
    }

    private ServerPlayer findGazedSurvivor(ServerPlayer keeper, double range) {
        ServerPlayer best = null;
        double bestDistance = Double.MAX_VALUE;
        for (ServerPlayer candidate : keeper.server.getPlayerList().getPlayers()) {
            if (candidate == keeper || isKeeper(candidate) || candidate.isSpectator()) continue;
            if (isLookingAt(keeper, candidate, range, NightmareConfig.GAZE_DOT_THRESHOLD - 0.03D)) {
                double distance = keeper.distanceToSqr(candidate);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = candidate;
                }
            }
        }
        return best;
    }

    private boolean isLookingAt(ServerPlayer watcher, ServerPlayer target, double range, double dotThreshold) {
        if (watcher.level() != target.level()) return false;
        if (watcher.distanceToSqr(target) > range * range) return false;

        Vec3 eyes = watcher.getEyePosition();
        Vec3 targetEyes = target.getEyePosition();
        Vec3 direction = targetEyes.subtract(eyes).normalize();
        double dot = watcher.getLookAngle().normalize().dot(direction);
        return dot >= dotThreshold && watcher.hasLineOfSight(target);
    }

    private BlockPos nextTrialOrigin() {
        int index = nextTrialIndex++;
        int x = NightmareConfig.TRIAL_BASE_ORIGIN.getX() + (index % 8) * NightmareConfig.TRIAL_AREA_STEP;
        int z = NightmareConfig.TRIAL_BASE_ORIGIN.getZ() + (index / 8) * NightmareConfig.TRIAL_AREA_STEP;
        return new BlockPos(x, NightmareConfig.TRIAL_BASE_ORIGIN.getY(), z);
    }

    private NightmarePlayerState state(ServerPlayer player) {
        return states.computeIfAbsent(player.getUUID(), id -> {
            NightmarePlayerState state = new NightmarePlayerState();
            CompoundTag tag = player.getPersistentData().getCompound(RECONNECT_STATE_TAG);
            if (tag.hasUUID("Session") && sessionId.equals(tag.getUUID("Session"))) {
                state.sanity = tag.getFloat("Sanity");
                state.lastGazeTick = tag.getLong("LastGazeTick");
                state.abductionCooldownUntil = tag.getLong("AbductionCooldown");
                state.sanityImmunityUntil = tag.getLong("SanityImmunity");
                state.mazeTrialsStarted = tag.getInt("MazeTrialsStarted");
            }
            player.getPersistentData().remove(RECONNECT_STATE_TAG);
            return state;
        });
    }

    private boolean hasKeeper(MinecraftServer server) {
        return server.getPlayerList().getPlayers().stream().anyMatch(this::isKeeper);
    }

    private void sync(ServerPlayer player, NightmarePlayerState state, boolean keeperPresent) {
        int secondsLeft = state.isInTrial()
                ? Math.max(0, (int) ((state.trialEndsAt - player.server.overworld().getGameTime()) / 20))
                : 0;
        long immunityTicksLeft = Math.max(0L, state.sanityImmunityUntil - player.level().getGameTime());
        int immunitySecondsLeft = (int) ((immunityTicksLeft + 19L) / 20L);
        sendState(player, new NightmareSyncState(keeperPresent, state.sanity,
                state.trialType, secondsLeft, immunitySecondsLeft));
    }

    private void syncDisabled(List<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            sendState(player, DISABLED_STATE);
        }
    }

    private void sendState(ServerPlayer player, NightmareSyncState next) {
        UUID id = player.getUUID();
        int tick = player.server.getTickCount();
        NightmareSyncState previous = lastSentStates.get(id);
        if (!next.shouldSend(previous, tick - lastSentTicks.getOrDefault(id, tick))) return;
        ModNetworking.sendToPlayer(new SyncNightmarePacket(next.visible(), next.sanity(),
                NightmareConfig.MAX_SANITY, next.trialType(), next.trialSecondsLeft(),
                next.immunitySecondsLeft()), player);
        lastSentStates.put(id, next);
        lastSentTicks.put(id, tick);
    }
}
