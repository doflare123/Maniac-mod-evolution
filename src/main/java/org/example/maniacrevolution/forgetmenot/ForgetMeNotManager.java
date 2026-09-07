package org.example.maniacrevolution.forgetmenot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
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
import org.example.maniacrevolution.entity.ForgetMeNotEntity;
import org.example.maniacrevolution.entity.ModEntities;
import org.example.maniacrevolution.game.GameManager;
import org.example.maniacrevolution.item.ForgetMeNotItem;
import org.example.maniacrevolution.mana.ManaProvider;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.SyncManaPacket;
import org.example.maniacrevolution.perk.PerkInstance;
import org.example.maniacrevolution.perk.PerkPhase;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.perk.perks.survivor.ForgetMeNotPerk;
import org.example.maniacrevolution.util.ManiacDamageAttribution;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Серверная механика предмета, установленного цветка и подготовки телепортации. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public final class ForgetMeNotManager {
    private static final double MOVEMENT_CANCEL_DISTANCE_BLOCKS = 0.2D;
    private static final double MOVEMENT_CANCEL_DISTANCE_SQUARED =
            MOVEMENT_CANCEL_DISTANCE_BLOCKS * MOVEMENT_CANCEL_DISTANCE_BLOCKS;
    private static final float MINIMUM_REAL_DAMAGE = 0.0F;
    private static final int EFFECT_AMPLIFIER = 0;
    private static final int EFFECT_DURATION_PADDING_TICKS = 2;

    private static final int CHANNEL_PARTICLES_PER_TICK = 2;
    private static final double CHANNEL_PLAYER_HEIGHT = 1.0D;
    private static final double CHANNEL_FLOWER_HEIGHT = 0.35D;
    private static final double CHANNEL_RADIUS_BLOCKS = 0.55D;
    private static final double CHANNEL_ANGULAR_SPEED = 0.42D;
    private static final float BLUE_PARTICLE_SIZE = 0.72F;
    private static final float YELLOW_PARTICLE_SIZE = 0.48F;
    private static final Vector3f BLUE_PARTICLE_COLOR = new Vector3f(0.05F, 0.72F, 1.0F);
    private static final Vector3f YELLOW_PARTICLE_COLOR = new Vector3f(1.0F, 0.83F, 0.08F);

    private static final int BURST_BLUE_PARTICLES = 34;
    private static final int BURST_YELLOW_PARTICLES = 14;
    private static final int BREAK_PARTICLES = 24;
    private static final double BURST_HORIZONTAL_SPREAD = 0.45D;
    private static final double BURST_VERTICAL_SPREAD = 0.8D;
    private static final double BURST_SPEED = 0.08D;
    private static final double BREAK_HORIZONTAL_SPREAD = 0.35D;
    private static final double BREAK_VERTICAL_SPREAD = 0.25D;
    private static final double BREAK_SPEED = 0.06D;
    private static final float SOUND_VOLUME = 0.9F;
    private static final float PLACEMENT_SOUND_PITCH = 1.35F;
    private static final float CHANNEL_SOUND_START_PITCH = 1.15F;
    private static final float CHANNEL_SOUND_END_PITCH = 1.65F;
    private static final float TELEPORT_SOUND_PITCH = 1.2F;
    private static final float RECLAIM_SOUND_PITCH = 1.45F;
    private static final float DESTROY_SOUND_PITCH = 0.72F;

    private static final Map<UUID, ChannelSession> CHANNELS = new HashMap<>();
    private static long lastBeginTick = Long.MIN_VALUE;
    private static boolean matchInitialized;

    private ForgetMeNotManager() {
    }

    public static void beginForOwner(ServerPlayer owner) {
        long gameTick = owner.server.overworld().getGameTime();
        ForgetMeNotSavedData data = ForgetMeNotSavedData.get(owner.server);
        if (!matchInitialized || lastBeginTick != gameTick) {
            removeAllPlacedEntities(owner.server, data, false);
            data.clear();
            CHANNELS.clear();
            matchInitialized = true;
            lastBeginTick = gameTick;
        }

        removeBoundItems(owner);
        data.setInInventory(owner.getUUID());
        giveBoundItem(owner);
    }

    /** Gives one initialized personal flower without resetting other owners. */
    public static boolean giveForTesting(ServerPlayer owner) {
        if (owner.getInventory().getFreeSlot() < 0
                && !owner.containerMenu.getCarried().isEmpty()) {
            return false;
        }

        if (CHANNELS.containsKey(owner.getUUID())) interrupt(owner);
        ForgetMeNotSavedData data = ForgetMeNotSavedData.get(owner.server);
        ForgetMeNotEntity placed = resolvePlacedFlower(owner.server, owner.getUUID(), false);
        if (placed != null) placed.discardWithoutStateChange();
        removeBoundItems(owner);
        if (owner.containerMenu.getCarried().is(ModItems.FORGET_ME_NOT.get())) {
            owner.containerMenu.setCarried(ItemStack.EMPTY);
        }
        data.setInInventory(owner.getUUID());
        giveBoundItem(owner);
        return true;
    }

    public static boolean canPlace(ServerPlayer owner, BlockPos floorPos) {
        if (!isActiveOwner(owner)) return false;
        ForgetMeNotSavedData.FlowerRecord record =
                ForgetMeNotSavedData.get(owner.server).getRecord(owner.getUUID());
        return record != null
                && record.status() == ForgetMeNotSavedData.FlowerStatus.INVENTORY
                && isSafePlacement(owner, owner.serverLevel(), floorPos);
    }

    public static boolean place(ServerPlayer owner, BlockPos floorPos) {
        if (!canPlace(owner, floorPos)) return false;

        ServerLevel level = owner.serverLevel();
        BlockPos flowerPos = floorPos.above();
        ForgetMeNotEntity flower = ModEntities.FORGET_ME_NOT.get().create(level);
        if (flower == null) return false;

        flower.setOwnerUUID(owner.getUUID());
        flower.setPos(flowerPos.getX() + 0.5D, flowerPos.getY(), flowerPos.getZ() + 0.5D);
        if (!level.addFreshEntity(flower)) return false;

        ForgetMeNotSavedData.get(owner.server).setPlaced(
                owner.getUUID(), level.dimension(), flowerPos, flower.getUUID());
        playPlacementEffects(level, flower.position());
        owner.displayClientMessage(
                Component.translatable("message.maniacrev.forget_me_not.placed"), true);
        return true;
    }

    public static ActivationFailure getActivationFailure(ServerPlayer player) {
        ForgetMeNotSavedData.FlowerRecord record =
                ForgetMeNotSavedData.get(player.server).getRecord(player.getUUID());
        if (record == null || record.status() != ForgetMeNotSavedData.FlowerStatus.PLACED) {
            return ActivationFailure.NEEDS_PLACED_FLOWER;
        }
        if (!record.dimension().equals(player.level().dimension())) {
            return ActivationFailure.OTHER_DIMENSION;
        }

        ForgetMeNotEntity flower = resolvePlacedFlower(player.server, player.getUUID(), true);
        if (flower == null) return ActivationFailure.NEEDS_PLACED_FLOWER;
        if (!player.isAlive() || player.isRemoved() || player.isSpectator() || isDowned(player)) {
            return ActivationFailure.PLAYER_UNAVAILABLE;
        }
        if (player.hasEffect(ModEffects.SHAKEN_MEMORY.get())) {
            return ActivationFailure.RECENT_MANIAC_HIT;
        }
        if (!isSafeTeleportTarget(player, flower)) {
            return ActivationFailure.UNSAFE_DESTINATION;
        }
        return ActivationFailure.NONE;
    }

    public static boolean canStartTeleport(ServerPlayer player) {
        return getActivationFailure(player) == ActivationFailure.NONE;
    }

    public static void startTeleport(ServerPlayer player) {
        ForgetMeNotEntity flower = resolvePlacedFlower(player.server, player.getUUID(), true);
        if (flower == null) return;

        CHANNELS.put(player.getUUID(), new ChannelSession(
                player.position(),
                player.level().dimension(),
                flower.getUUID(),
                player.server.overworld().getGameTime(),
                GameManager.getCurrentPhase()
        ));
        player.addEffect(new MobEffectInstance(
                ModEffects.FORGET_ME_NOT_CALL.get(),
                ForgetMeNotPerk.CHANNEL_DURATION_TICKS + EFFECT_DURATION_PADDING_TICKS,
                EFFECT_AMPLIFIER,
                false,
                false,
                true
        ));
        player.serverLevel().playSound(null, player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS,
                SOUND_VOLUME, CHANNEL_SOUND_START_PITCH);
    }

    public static boolean reclaimByOwner(ServerPlayer owner, ForgetMeNotEntity flower) {
        if (!owner.getUUID().equals(flower.getOwnerUUID())) return false;
        if (owner.getInventory().getFreeSlot() < 0) {
            owner.displayClientMessage(
                    Component.translatable("message.maniacrev.forget_me_not.inventory_full"), true);
            return false;
        }

        ForgetMeNotSavedData data = ForgetMeNotSavedData.get(owner.server);
        ForgetMeNotSavedData.FlowerRecord record = data.getRecord(owner.getUUID());
        if (record == null || record.status() != ForgetMeNotSavedData.FlowerStatus.PLACED
                || !flower.getUUID().equals(record.entityId())) {
            return false;
        }

        Vec3 position = flower.position();
        flower.discardWithoutStateChange();
        data.setInInventory(owner.getUUID());
        giveBoundItem(owner);
        playBreakEffects(owner.serverLevel(), position, false);
        owner.displayClientMessage(
                Component.translatable("message.maniacrev.forget_me_not.reclaimed"), true);
        return true;
    }

    public static boolean destroyByManiac(ServerPlayer maniac, ForgetMeNotEntity flower) {
        if (PerkTeam.fromPlayer(maniac) != PerkTeam.MANIAC) return false;
        UUID ownerId = flower.getOwnerUUID();
        if (ownerId == null) return false;

        Vec3 position = flower.position();
        flower.discardWithoutStateChange();
        ForgetMeNotSavedData.get(maniac.server).setDestroyed(ownerId);
        playBreakEffects(maniac.serverLevel(), position, true);
        return true;
    }

    /** Удаляет все установленные незабудки без возврата предметов. */
    public static int clearAllPlaced(MinecraftServer server) {
        ForgetMeNotSavedData data = ForgetMeNotSavedData.get(server);
        return removeAllPlacedEntities(server, data, true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer survivor)
                || event.getAmount() <= MINIMUM_REAL_DAMAGE) {
            return;
        }

        // Любой прошедший урон сбивает уже начатую подготовку.
        if (CHANNELS.containsKey(survivor.getUUID())) {
            interrupt(survivor);
        }

        if (survivor.gameMode.getGameModeForPlayer() != GameType.ADVENTURE
                || PerkTeam.fromPlayer(survivor) != PerkTeam.SURVIVOR
                || PlayerDataManager.get(survivor).getPerkInstance(ForgetMeNotPerk.ID) == null) {
            return;
        }

        PerkPhase phase = GameManager.getCurrentPhase();
        if (phase != PerkPhase.HUNT && phase != PerkPhase.MIDGAME
                && phase != PerkPhase.REVERSAL) {
            return;
        }

        ServerPlayer responsibleManiac = ManiacDamageAttribution.peekResponsibleManiac(
                survivor, event.getSource());
        if (responsibleManiac == null
                || PerkTeam.fromPlayer(responsibleManiac) != PerkTeam.MANIAC) {
            return;
        }

        survivor.addEffect(new MobEffectInstance(
                ModEffects.SHAKEN_MEMORY.get(),
                ForgetMeNotPerk.RECENT_DAMAGE_LOCK_TICKS,
                EFFECT_AMPLIFIER,
                false,
                false,
                true
        ));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        tickChannels(event.getServer());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        CHANNELS.remove(player.getUUID());
        player.removeEffect(ModEffects.FORGET_ME_NOT_CALL.get());

        ForgetMeNotEntity flower = resolvePlacedFlower(player.server, player.getUUID(), true);
        if (flower == null) return;
        Vec3 position = flower.position();
        ServerLevel level = (ServerLevel) flower.level();
        flower.discardWithoutStateChange();
        ForgetMeNotSavedData.get(player.server).setDestroyed(player.getUUID());
        playBreakEffects(level, position, true);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        CHANNELS.clear();
        matchInitialized = false;
        lastBeginTick = Long.MIN_VALUE;
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ForgetMeNotSavedData.FlowerRecord record =
                ForgetMeNotSavedData.get(player.server).getRecord(player.getUUID());
        if (record == null || record.status() != ForgetMeNotSavedData.FlowerStatus.INVENTORY) return;
        event.getDrops().removeIf(item -> ForgetMeNotItem.belongsTo(
                item.getItem(), player.getUUID()));
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ForgetMeNotSavedData.FlowerRecord record =
                ForgetMeNotSavedData.get(player.server).getRecord(player.getUUID());
        if (record == null || record.status() != ForgetMeNotSavedData.FlowerStatus.INVENTORY
                || PlayerDataManager.get(player).getPerkInstance(ForgetMeNotPerk.ID) == null
                || hasBoundItem(player)) {
            return;
        }
        giveBoundItem(player);
    }

    private static void tickChannels(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        for (Map.Entry<UUID, ChannelSession> entry : new ArrayList<>(CHANNELS.entrySet())) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            ChannelSession session = entry.getValue();
            if (player == null) {
                CHANNELS.remove(entry.getKey());
                continue;
            }

            ForgetMeNotEntity flower = resolvePlacedFlower(server, player.getUUID(), true);
            if (shouldInterrupt(player, flower, session)) {
                interrupt(player);
                continue;
            }

            PerkInstance instance = PlayerDataManager.get(player).getPerkInstance(ForgetMeNotPerk.ID);
            if (instance != null) {
                // Кулдаун должен начаться после результата, а не съедаться во время подготовки.
                instance.setCooldownRemaining(ForgetMeNotPerk.SUCCESS_COOLDOWN_TICKS);
            }

            int elapsed = (int) (now - session.startedTick());
            if (elapsed >= ForgetMeNotPerk.CHANNEL_DURATION_TICKS) {
                if (!isSafeTeleportTarget(player, flower)) {
                    interrupt(player);
                } else {
                    completeTeleport(player, flower);
                }
                continue;
            }

            playChannelParticles(player.serverLevel(), player.position(), flower.position(), elapsed);
            if (elapsed == ForgetMeNotPerk.CHANNEL_DURATION_TICKS / 2) {
                player.serverLevel().playSound(null, player.blockPosition(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS,
                        SOUND_VOLUME, CHANNEL_SOUND_END_PITCH);
            }
        }
    }

    private static boolean shouldInterrupt(ServerPlayer player, ForgetMeNotEntity flower,
                                           ChannelSession session) {
        if (flower == null || !player.isAlive() || player.isRemoved() || player.isSpectator()
                || isDowned(player) || player.hasEffect(ModEffects.SHAKEN_MEMORY.get())
                || !player.level().dimension().equals(session.startDimension())
                || GameManager.getCurrentPhase() != session.startPhase()
                || !flower.getUUID().equals(session.flowerEntityId())) {
            return true;
        }
        return player.position().distanceToSqr(session.startPosition())
                > MOVEMENT_CANCEL_DISTANCE_SQUARED;
    }

    private static void interrupt(ServerPlayer player) {
        CHANNELS.remove(player.getUUID());
        player.removeEffect(ModEffects.FORGET_ME_NOT_CALL.get());
        PerkInstance instance = PlayerDataManager.get(player).getPerkInstance(ForgetMeNotPerk.ID);
        if (instance != null) {
            instance.setCooldownRemaining(ForgetMeNotPerk.INTERRUPTED_COOLDOWN_TICKS);
        }
        player.getCapability(ManaProvider.MANA).ifPresent(mana -> {
            mana.addMana(ForgetMeNotPerk.INTERRUPTED_MANA_REFUND);
            ModNetworking.sendToPlayer(new SyncManaPacket(
                    mana.getMana(), mana.getMaxMana(), mana.getTotalRegenRate()), player);
        });
        PlayerDataManager.syncToClient(player);
        player.displayClientMessage(
                Component.translatable("message.maniacrev.forget_me_not.interrupted",
                        (int) ForgetMeNotPerk.INTERRUPTED_MANA_REFUND,
                        ForgetMeNotPerk.INTERRUPTED_COOLDOWN_SECONDS),
                true);
    }

    private static void completeTeleport(ServerPlayer player, ForgetMeNotEntity flower) {
        CHANNELS.remove(player.getUUID());
        player.removeEffect(ModEffects.FORGET_ME_NOT_CALL.get());

        ServerLevel level = (ServerLevel) flower.level();
        Vec3 departure = player.position();
        Vec3 destination = flower.position();
        playTeleportBurst(level, departure);
        level.playSound(null, departure.x, departure.y, departure.z,
                SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS,
                SOUND_VOLUME, TELEPORT_SOUND_PITCH);

        player.teleportTo(level, destination.x, destination.y, destination.z,
                Set.of(), player.getYRot(), player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);

        PerkInstance instance = PlayerDataManager.get(player).getPerkInstance(ForgetMeNotPerk.ID);
        if (instance != null) {
            instance.setCooldownRemaining(ForgetMeNotPerk.SUCCESS_COOLDOWN_TICKS);
        }

        playTeleportBurst(level, destination);
        level.playSound(null, destination.x, destination.y, destination.z,
                SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS,
                SOUND_VOLUME, TELEPORT_SOUND_PITCH);
        PlayerDataManager.syncToClient(player);
    }

    private static boolean isSafePlacement(ServerPlayer player, ServerLevel level,
                                           BlockPos floorPos) {
        BlockPos targetPos = floorPos.above();
        return level.getWorldBorder().isWithinBounds(targetPos)
                && level.getBlockState(targetPos).isAir();
    }

    private static boolean isSafeTeleportTarget(ServerPlayer player, ForgetMeNotEntity flower) {
        if (!(flower.level() instanceof ServerLevel level)
                || !flower.level().dimension().equals(player.level().dimension())) {
            return false;
        }
        Vec3 target = flower.position();
        BlockPos floorPos = BlockPos.containing(target.x, target.y - 1.0D, target.z);
        return level.getWorldBorder().isWithinBounds(BlockPos.containing(target))
                && level.getBlockState(floorPos).isFaceSturdy(level, floorPos, Direction.UP)
                && isSafeBox(player, level, target.x, target.y, target.z);
    }

    private static boolean isSafeBox(ServerPlayer player, ServerLevel level,
                                     double x, double y, double z) {
        AABB targetBox = player.getDimensions(Pose.STANDING).makeBoundingBox(x, y, z);
        return level.noCollision(player, targetBox);
    }

    private static ForgetMeNotEntity resolvePlacedFlower(MinecraftServer server, UUID ownerId,
                                                           boolean markMissingDestroyed) {
        ForgetMeNotSavedData data = ForgetMeNotSavedData.get(server);
        ForgetMeNotSavedData.FlowerRecord record = data.getRecord(ownerId);
        if (record == null || record.status() != ForgetMeNotSavedData.FlowerStatus.PLACED
                || record.entityId() == null) {
            return null;
        }

        ServerLevel level = server.getLevel(record.dimension());
        if (level == null) return null;
        level.getChunk(record.pos());
        Entity entity = level.getEntity(record.entityId());
        if (entity instanceof ForgetMeNotEntity flower
                && ownerId.equals(flower.getOwnerUUID()) && !flower.isRemoved()) {
            return flower;
        }
        if (markMissingDestroyed) data.setDestroyed(ownerId);
        return null;
    }

    private static int removeAllPlacedEntities(MinecraftServer server,
                                               ForgetMeNotSavedData data,
                                               boolean markDestroyed) {
        int removed = 0;
        for (Map.Entry<UUID, ForgetMeNotSavedData.FlowerRecord> entry : data.getRecords()) {
            if (entry.getValue().status() != ForgetMeNotSavedData.FlowerStatus.PLACED) continue;
            ForgetMeNotEntity flower = resolvePlacedFlower(server, entry.getKey(), false);
            if (flower != null) {
                flower.discardWithoutStateChange();
                removed++;
            }
            if (markDestroyed) data.setDestroyed(entry.getKey());
        }
        if (markDestroyed) {
            for (UUID ownerId : new ArrayList<>(CHANNELS.keySet())) {
                ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
                if (owner != null) interrupt(owner);
                else CHANNELS.remove(ownerId);
            }
        }
        return removed;
    }

    private static void giveBoundItem(ServerPlayer owner) {
        ItemStack flower = ForgetMeNotItem.createFor(owner);
        if (!owner.getInventory().add(flower)) {
            owner.containerMenu.setCarried(flower);
        }
        owner.inventoryMenu.broadcastChanges();
    }

    private static void removeBoundItems(ServerPlayer owner) {
        for (int slot = 0; slot < owner.getInventory().getContainerSize(); slot++) {
            ItemStack stack = owner.getInventory().getItem(slot);
            if (stack.is(ModItems.FORGET_ME_NOT.get())) {
                owner.getInventory().setItem(slot, ItemStack.EMPTY);
            }
        }
    }

    private static boolean hasBoundItem(ServerPlayer owner) {
        for (int slot = 0; slot < owner.getInventory().getContainerSize(); slot++) {
            if (ForgetMeNotItem.belongsTo(owner.getInventory().getItem(slot), owner.getUUID())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isActiveOwner(ServerPlayer player) {
        PerkPhase phase = GameManager.getCurrentPhase();
        return player.gameMode.getGameModeForPlayer() == GameType.ADVENTURE
                && PerkTeam.fromPlayer(player) == PerkTeam.SURVIVOR
                && PlayerDataManager.get(player).getPerkInstance(ForgetMeNotPerk.ID) != null
                && (phase == PerkPhase.HUNT || phase == PerkPhase.MIDGAME
                || phase == PerkPhase.REVERSAL);
    }

    private static boolean isDowned(ServerPlayer player) {
        DownedData downed = DownedCapability.get(player);
        return downed != null && downed.getState() == DownedState.DOWNED;
    }

    private static void playPlacementEffects(ServerLevel level, Vec3 position) {
        level.sendParticles(new DustParticleOptions(BLUE_PARTICLE_COLOR, BLUE_PARTICLE_SIZE),
                position.x, position.y + CHANNEL_FLOWER_HEIGHT, position.z,
                BREAK_PARTICLES, BREAK_HORIZONTAL_SPREAD, BREAK_VERTICAL_SPREAD,
                BREAK_HORIZONTAL_SPREAD, BREAK_SPEED);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                position.x, position.y + CHANNEL_FLOWER_HEIGHT, position.z,
                CHANNEL_PARTICLES_PER_TICK, BREAK_HORIZONTAL_SPREAD,
                BREAK_VERTICAL_SPREAD, BREAK_HORIZONTAL_SPREAD, BREAK_SPEED);
        level.playSound(null, position.x, position.y, position.z,
                SoundEvents.GRASS_PLACE, SoundSource.PLAYERS,
                SOUND_VOLUME, PLACEMENT_SOUND_PITCH);
    }

    private static void playChannelParticles(ServerLevel level, Vec3 playerPos,
                                             Vec3 flowerPos, int elapsedTicks) {
        for (int index = 0; index < CHANNEL_PARTICLES_PER_TICK; index++) {
            double angle = (elapsedTicks + index * Math.PI) * CHANNEL_ANGULAR_SPEED;
            double offsetX = Math.cos(angle) * CHANNEL_RADIUS_BLOCKS;
            double offsetZ = Math.sin(angle) * CHANNEL_RADIUS_BLOCKS;
            double height = CHANNEL_PLAYER_HEIGHT
                    + (elapsedTicks % ForgetMeNotPerk.TICKS_PER_SECOND)
                    / (double) ForgetMeNotPerk.TICKS_PER_SECOND;
            level.sendParticles(new DustParticleOptions(BLUE_PARTICLE_COLOR, BLUE_PARTICLE_SIZE),
                    playerPos.x + offsetX, playerPos.y + height, playerPos.z + offsetZ,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(new DustParticleOptions(YELLOW_PARTICLE_COLOR, YELLOW_PARTICLE_SIZE),
                    flowerPos.x - offsetX * 0.55D,
                    flowerPos.y + CHANNEL_FLOWER_HEIGHT,
                    flowerPos.z - offsetZ * 0.55D,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static void playTeleportBurst(ServerLevel level, Vec3 position) {
        level.sendParticles(new DustParticleOptions(BLUE_PARTICLE_COLOR, BLUE_PARTICLE_SIZE),
                position.x, position.y + CHANNEL_PLAYER_HEIGHT, position.z,
                BURST_BLUE_PARTICLES, BURST_HORIZONTAL_SPREAD, BURST_VERTICAL_SPREAD,
                BURST_HORIZONTAL_SPREAD, BURST_SPEED);
        level.sendParticles(new DustParticleOptions(YELLOW_PARTICLE_COLOR, YELLOW_PARTICLE_SIZE),
                position.x, position.y + CHANNEL_PLAYER_HEIGHT, position.z,
                BURST_YELLOW_PARTICLES, BURST_HORIZONTAL_SPREAD, BURST_VERTICAL_SPREAD,
                BURST_HORIZONTAL_SPREAD, BURST_SPEED);
    }

    private static void playBreakEffects(ServerLevel level, Vec3 position, boolean destroyed) {
        Vector3f color = destroyed ? new Vector3f(0.08F, 0.16F, 0.34F) : BLUE_PARTICLE_COLOR;
        level.sendParticles(new DustParticleOptions(color, BLUE_PARTICLE_SIZE),
                position.x, position.y + CHANNEL_FLOWER_HEIGHT, position.z,
                BREAK_PARTICLES, BREAK_HORIZONTAL_SPREAD, BREAK_VERTICAL_SPREAD,
                BREAK_HORIZONTAL_SPREAD, BREAK_SPEED);
        level.playSound(null, position.x, position.y, position.z,
                SoundEvents.GRASS_BREAK, SoundSource.PLAYERS,
                SOUND_VOLUME, destroyed ? DESTROY_SOUND_PITCH : RECLAIM_SOUND_PITCH);
    }

    public enum ActivationFailure {
        NONE(""),
        NEEDS_PLACED_FLOWER("message.maniacrev.forget_me_not.needs_flower"),
        OTHER_DIMENSION("message.maniacrev.forget_me_not.other_dimension"),
        RECENT_MANIAC_HIT("message.maniacrev.forget_me_not.recent_hit"),
        UNSAFE_DESTINATION("message.maniacrev.forget_me_not.unsafe_destination"),
        PLAYER_UNAVAILABLE("message.maniacrev.forget_me_not.player_unavailable");

        private final String translationKey;

        ActivationFailure(String translationKey) {
            this.translationKey = translationKey;
        }

        public Component message() {
            return translationKey.isEmpty()
                    ? Component.empty()
                    : Component.translatable(translationKey);
        }
    }

    private record ChannelSession(Vec3 startPosition,
                                  net.minecraft.resources.ResourceKey<Level> startDimension,
                                  UUID flowerEntityId,
                                  long startedTick,
                                  PerkPhase startPhase) {
    }
}
