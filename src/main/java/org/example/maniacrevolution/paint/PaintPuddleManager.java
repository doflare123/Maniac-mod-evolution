package org.example.maniacrevolution.paint;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
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
import org.example.maniacrevolution.item.PaintCanItem;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.PaintPuddlePacket;
import org.example.maniacrevolution.network.packets.PaintQteStatePacket;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.perk.perks.maniac.ThePaintThickensPerk;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public final class PaintPuddleManager {
    private static final double MAX_TRIGGER_HEIGHT_BLOCKS = 1.25D;
    private static final int SERVER_QTE_GRACE_TICKS = 4;
    private static final float FULL_ROTATION_DEGREES = 360.0F;
    private static final int DROPLET_COUNT = 10;
    private static final float DROPLET_SIZE = 0.85F;

    private static final List<PaintPuddle> PUDDLES = new ArrayList<>();
    private static final Map<UUID, PaintQteSession> QTE_SESSIONS = new HashMap<>();
    private static final Map<UUID, ResourceKey<Level>> SYNCED_DIMENSIONS = new HashMap<>();
    private static final RandomSource RANDOM = RandomSource.create();

    private static long nextPuddleId = 1L;
    private static int nextSessionId = 1;
    private static long lastBeginTick = Long.MIN_VALUE;
    private static boolean matchInitialized;

    private PaintPuddleManager() {
    }

    public static void beginForOwner(ServerPlayer owner) {
        long gameTick = owner.server.overworld().getGameTime();
        if (!matchInitialized || lastBeginTick != gameTick) {
            clearMatch(owner.server);
            matchInitialized = true;
            lastBeginTick = gameTick;
        }

        removePaintCans(owner);
        ItemStack can = PaintCanItem.createFor(owner);
        if (!owner.getInventory().add(can)) {
            owner.containerMenu.setCarried(can);
        }
        owner.inventoryMenu.broadcastChanges();
    }

    public static boolean canOwnerPlace(ServerPlayer owner) {
        return PlayerDataManager.get(owner).getPerkInstance(ThePaintThickensPerk.ID) != null
                && countOwnerPuddles(owner.getUUID()) < ThePaintThickensPerk.PAINT_CAN_USES;
    }

    public static boolean placePuddle(ServerPlayer owner, Vec3 hitLocation) {
        if (!canOwnerPlace(owner)) {
            return false;
        }

        PaintColor color = PaintColor.values()[RANDOM.nextInt(PaintColor.values().length)];
        PaintPuddle puddle = new PaintPuddle(
                nextPuddleId++,
                owner.getUUID(),
                owner.level().dimension(),
                hitLocation.x,
                hitLocation.y + 0.004D,
                hitLocation.z,
                color,
                RANDOM.nextFloat() * FULL_ROTATION_DEGREES
        );
        PUDDLES.add(puddle);
        broadcastInDimension(owner.server, puddle.dimension, puddle.toAddPacket());

        ServerLevel level = owner.serverLevel();
        Vector3f particleColor = new Vector3f(color.red(), color.green(), color.blue());
        level.sendParticles(new DustParticleOptions(particleColor, DROPLET_SIZE),
                puddle.x, puddle.y + 0.08D, puddle.z,
                DROPLET_COUNT, 0.34D, 0.08D, 0.34D, 0.025D);
        level.playSound(null, puddle.x, puddle.y, puddle.z,
                SoundEvents.SLIME_BLOCK_PLACE, SoundSource.PLAYERS, 0.75F, 1.25F);
        return true;
    }

    public static void handleQtePress(ServerPlayer player, int sessionId,
                                      int attemptIndex, int score) {
        PaintQteSession session = QTE_SESSIONS.get(player.getUUID());
        if (session == null || session.id != sessionId || session.nextAttempt != attemptIndex) {
            return;
        }

        long now = player.server.overworld().getGameTime();
        long latestAllowedTick = session.startedTick
                + session.totalDurationTicks
                + SERVER_QTE_GRACE_TICKS;
        if (now > latestAllowedTick) {
            return;
        }

        session.totalScore += Mth.clamp(
                score,
                ThePaintThickensPerk.QTE_SCORE_MISS,
                ThePaintThickensPerk.QTE_SCORE_HIT
        );
        session.nextAttempt++;
        if (session.nextAttempt >= ThePaintThickensPerk.QTE_ATTEMPTS) {
            finishQte(player, session);
        }
    }

    public static void removeOwner(ServerPlayer owner) {
        removePaintCans(owner);
        QTE_SESSIONS.remove(owner.getUUID());
        ModNetworking.sendToPlayer(PaintQteStatePacket.clear(), owner);

        List<Long> removed = PUDDLES.stream()
                .filter(puddle -> puddle.owner.equals(owner.getUUID()))
                .map(puddle -> puddle.id)
                .toList();
        PUDDLES.removeIf(puddle -> puddle.owner.equals(owner.getUUID()));
        for (long id : removed) {
            ModNetworking.sendToAllPlayers(PaintPuddlePacket.remove(id));
        }
    }

    /** Clears map paint only. Cans, effects and active QTE sessions are preserved. */
    public static int clearAllPuddles(MinecraftServer server) {
        int removedCount = PUDDLES.size();
        PUDDLES.clear();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ModNetworking.sendToPlayer(PaintPuddlePacket.clear(), player);
        }
        return removedCount;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer() == null) {
            return;
        }

        MinecraftServer server = event.getServer();
        if (GameManager.getPhaseValue() == 0) {
            if (matchInitialized || !PUDDLES.isEmpty() || !QTE_SESSIONS.isEmpty()) {
                clearMatch(server);
            }
            return;
        }

        enforceBoundItems(server);
        synchronizePlayers(server);
        tickPuddles(server);
        tickQtes(server);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPaintCanToss(ItemTossEvent event) {
        if (event.getEntity().getItem().is(ModItems.PAINT_CAN.get())) {
            event.setCanceled(true);
            ItemStack stack = event.getEntity().getItem();
            if (!event.getPlayer().getInventory().add(stack)) {
                event.getPlayer().containerMenu.setCarried(stack);
            }
        }
    }

    @SubscribeEvent
    public static void onContainerClosed(PlayerContainerEvent.Close event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            recoverPaintFromOpenMenu(player);
            recoverForeignCarriedPaint(player);
        }
    }

    private static void tickPuddles(MinecraftServer server) {
        for (PaintPuddle puddle : PUDDLES) {
            ServerLevel level = server.getLevel(puddle.dimension);
            if (level == null) continue;

            Set<UUID> currentlyInside = new HashSet<>();
            for (ServerPlayer player : level.players()) {
                if (!isInside(player, puddle)) continue;
                currentlyInside.add(player.getUUID());
                if (puddle.playersInside.contains(player.getUUID())) continue;

                puddle.playersInside.add(player.getUUID());
                if (canTrigger(player)) {
                    startQte(player, puddle);
                }
            }
            puddle.playersInside.retainAll(currentlyInside);
        }
    }

    private static boolean isInside(ServerPlayer player, PaintPuddle puddle) {
        double dx = player.getX() - puddle.x;
        double dz = player.getZ() - puddle.z;
        return dx * dx + dz * dz
                <= ThePaintThickensPerk.PUDDLE_TRIGGER_RADIUS_BLOCKS
                * ThePaintThickensPerk.PUDDLE_TRIGGER_RADIUS_BLOCKS
                && Math.abs(player.getY() - puddle.y) <= MAX_TRIGGER_HEIGHT_BLOCKS;
    }

    private static boolean canTrigger(ServerPlayer player) {
        if (!player.isAlive() || player.isRemoved() || player.isSpectator()
                || PerkTeam.fromPlayer(player) != PerkTeam.SURVIVOR
                || player.hasEffect(ModEffects.PAINT_PROTECTION.get())) {
            return false;
        }
        GameType gameType = player.gameMode.getGameModeForPlayer();
        if (gameType != GameType.ADVENTURE && gameType != GameType.SURVIVAL) {
            return false;
        }
        DownedData downed = DownedCapability.get(player);
        return downed == null || downed.getState() != DownedState.DOWNED;
    }

    private static void startQte(ServerPlayer player, PaintPuddle puddle) {
        PaintQteSession session = new PaintQteSession(
                nextSessionId++, puddle.id, puddle.color,
                player.server.overworld().getGameTime(),
                ThePaintThickensPerk.QTE_TOTAL_DURATION_TICKS
        );
        QTE_SESSIONS.put(player.getUUID(), session);
        ModNetworking.sendToPlayer(PaintQteStatePacket.start(
                session.id, puddle.color,
                ThePaintThickensPerk.QTE_ATTEMPTS,
                ThePaintThickensPerk.QTE_TOTAL_DURATION_MILLISECONDS,
                ThePaintThickensPerk.MISS_ZONE_PERCENT,
                ThePaintThickensPerk.NEAR_ZONE_PERCENT,
                ThePaintThickensPerk.HIT_ZONE_PERCENT,
                ThePaintThickensPerk.SCREEN_PAINT_DURATION_TICKS
        ), player);
    }

    private static void tickQtes(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        for (PaintQteSession session : new ArrayList<>(QTE_SESSIONS.values())) {
            ServerPlayer player = findSessionPlayer(server, session);
            if (player == null) continue;

            if (now > session.startedTick
                    + session.totalDurationTicks
                    + SERVER_QTE_GRACE_TICKS) {
                finishQte(player, session);
            }
        }
    }

    private static ServerPlayer findSessionPlayer(MinecraftServer server, PaintQteSession target) {
        for (Map.Entry<UUID, PaintQteSession> entry : QTE_SESSIONS.entrySet()) {
            if (entry.getValue() == target) {
                return server.getPlayerList().getPlayer(entry.getKey());
            }
        }
        return null;
    }

    private static void finishQte(ServerPlayer player, PaintQteSession session) {
        if (QTE_SESSIONS.get(player.getUUID()) != session) return;
        QTE_SESSIONS.remove(player.getUUID());

        player.addEffect(new MobEffectInstance(
                ModEffects.PAINT_PROTECTION.get(),
                ThePaintThickensPerk.PROTECTION_DURATION_TICKS,
                0, false, false, true
        ));
        if (session.totalScore <= ThePaintThickensPerk.MISS_MAX_SCORE) {
            player.addEffect(new MobEffectInstance(
                    ModEffects.STUN.get(),
                    ThePaintThickensPerk.STUN_DURATION_TICKS,
                    0, false, false, true
            ));
        } else if (session.totalScore <= ThePaintThickensPerk.NEAR_MAX_SCORE) {
            player.addEffect(new MobEffectInstance(
                    ModEffects.SLOWDOWN.get(),
                    ThePaintThickensPerk.SLOWDOWN_DURATION_TICKS,
                    ThePaintThickensPerk.SLOWDOWN_PERCENT - 1,
                    false, false, true
            ));
        }
        ModNetworking.sendToPlayer(PaintQteStatePacket.clear(), player);
    }

    private static void synchronizePlayers(MinecraftServer server) {
        Set<UUID> online = new HashSet<>();
        long gameTick = server.overworld().getGameTime();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            online.add(player.getUUID());
            ResourceKey<Level> synced = SYNCED_DIMENSIONS.get(player.getUUID());
            if (!player.level().dimension().equals(synced)) {
                ModNetworking.sendToPlayer(PaintPuddlePacket.clear(), player);
                for (PaintPuddle puddle : PUDDLES) {
                    if (puddle.dimension.equals(player.level().dimension())) {
                        ModNetworking.sendToPlayer(puddle.toAddPacket(), player);
                    }
                }
                SYNCED_DIMENSIONS.put(player.getUUID(), player.level().dimension());
            }
        }
        SYNCED_DIMENSIONS.keySet().removeIf(id -> !online.contains(id));
    }

    private static void enforceBoundItems(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                if (!stack.is(ModItems.PAINT_CAN.get())) continue;
                UUID ownerId = PaintCanItem.getOwner(stack);
                if (ownerId != null && !ownerId.equals(player.getUUID())) {
                    ItemStack recovered = stack.copy();
                    player.getInventory().setItem(slot, ItemStack.EMPTY);
                    returnToOwner(server, ownerId, recovered);
                }
            }
            recoverPaintFromOpenMenu(player);
            recoverForeignCarriedPaint(player);
        }
    }

    private static void recoverForeignCarriedPaint(ServerPlayer viewer) {
        ItemStack carried = viewer.containerMenu.getCarried();
        if (!carried.is(ModItems.PAINT_CAN.get())) return;
        UUID ownerId = PaintCanItem.getOwner(carried);
        if (ownerId != null && !ownerId.equals(viewer.getUUID())) {
            viewer.containerMenu.setCarried(ItemStack.EMPTY);
            returnToOwner(viewer.server, ownerId, carried.copy());
        }
    }

    private static void recoverPaintFromOpenMenu(ServerPlayer viewer) {
        for (net.minecraft.world.inventory.Slot slot : viewer.containerMenu.slots) {
            if (slot.container instanceof Inventory || !slot.hasItem()
                    || !slot.getItem().is(ModItems.PAINT_CAN.get())) {
                continue;
            }
            ItemStack recovered = slot.getItem().copy();
            slot.set(ItemStack.EMPTY);
            UUID ownerId = PaintCanItem.getOwner(recovered);
            if (ownerId != null) {
                returnToOwner(viewer.server, ownerId, recovered);
            }
        }
    }

    private static void returnToOwner(MinecraftServer server, UUID ownerId, ItemStack stack) {
        ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
        if (owner != null && !owner.getInventory().add(stack)) {
            owner.containerMenu.setCarried(stack);
        }
    }

    private static void removePaintCans(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(ModItems.PAINT_CAN.get())) {
                player.getInventory().setItem(slot, ItemStack.EMPTY);
            }
        }
        if (player.containerMenu.getCarried().is(ModItems.PAINT_CAN.get())) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
        }
    }

    private static int countOwnerPuddles(UUID ownerId) {
        return (int) PUDDLES.stream().filter(puddle -> puddle.owner.equals(ownerId)).count();
    }

    private static void broadcastInDimension(MinecraftServer server,
                                             ResourceKey<Level> dimension,
                                             PaintPuddlePacket packet) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level().dimension().equals(dimension)) {
                ModNetworking.sendToPlayer(packet, player);
            }
        }
    }

    private static void clearMatch(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            removePaintCans(player);
            ModNetworking.sendToPlayer(PaintPuddlePacket.clear(), player);
            ModNetworking.sendToPlayer(PaintQteStatePacket.clear(), player);
        }
        PUDDLES.clear();
        QTE_SESSIONS.clear();
        SYNCED_DIMENSIONS.clear();
        nextPuddleId = 1L;
        nextSessionId = 1;
        matchInitialized = false;
    }

    private static final class PaintPuddle {
        private final long id;
        private final UUID owner;
        private final ResourceKey<Level> dimension;
        private final double x;
        private final double y;
        private final double z;
        private final PaintColor color;
        private final float rotation;
        private final Set<UUID> playersInside = new HashSet<>();

        private PaintPuddle(long id, UUID owner, ResourceKey<Level> dimension,
                            double x, double y, double z,
                            PaintColor color, float rotation) {
            this.id = id;
            this.owner = owner;
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.color = color;
            this.rotation = rotation;
        }

        private PaintPuddlePacket toAddPacket() {
            return PaintPuddlePacket.add(id, x, y, z, color, rotation);
        }
    }

    private static final class PaintQteSession {
        private final int id;
        private final long sourcePuddleId;
        private final PaintColor color;
        private final long startedTick;
        private final int totalDurationTicks;
        private int nextAttempt;
        private int totalScore;

        private PaintQteSession(int id, long sourcePuddleId, PaintColor color,
                                long startedTick, int totalDurationTicks) {
            this.id = id;
            this.sourcePuddleId = sourcePuddleId;
            this.color = color;
            this.startedTick = startedTick;
            this.totalDurationTicks = totalDurationTicks;
        }
    }
}
