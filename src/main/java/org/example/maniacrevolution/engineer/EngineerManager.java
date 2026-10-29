package org.example.maniacrevolution.engineer;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.example.maniacrevolution.*;
import org.example.maniacrevolution.block.ModBlocks;
import org.example.maniacrevolution.downed.*;
import org.example.maniacrevolution.game.GameManager;
import org.example.maniacrevolution.hack.*;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.util.PlayerModeUtil;
import net.minecraft.world.InteractionHand;

/** All purchases, work clocks, targeting and sound production are server-authoritative. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public final class EngineerManager {
    public static final int BUILD = 0, ARM_PLATE = 2, BREAK = 3, COLLECT = 4;
    private record Key(ResourceLocation dimension, BlockPos pos) {}
    private record Device(Key key, UUID owner, boolean barricade, long expires, EngineerDeviceBlockEntity entity) {}
    private static final class Account {
        int scrap = EngineerRules.START_SCRAP, earned, selected = -1;
        double fraction;
        float armor;
        long buildReady;
        final Set<Item> given = new HashSet<>();
    }
    private static final class Work {
        final int type, selection, slot;
        final ResourceLocation dimension;
        final BlockPos pos;
        final Vec3 origin;
        final float yaw, pitch;
        final Direction facing;
        final long started;
        long held;
        InteractionHand consumable;
        Work(ServerPlayer p, int type, int selection, BlockPos pos, long now) {
            this.type = type; this.selection = selection; this.pos = pos.immutable(); started = held = now;
            dimension = p.level().dimension().location(); origin = p.position(); yaw = p.getYRot(); pitch = p.getXRot();
            facing = p.getDirection().getOpposite(); slot = p.getInventory().selected;
        }
        int duration() { return type == BREAK ? EngineerRules.BREAK_TICKS : type == COLLECT ? EngineerRules.COLLECT_TICKS : type == ARM_PLATE ? EngineerRules.PLATE_TICKS : EngineerRules.INSTALL_TICKS; }
    }
    private static final Map<UUID, Account> ACCOUNTS = new HashMap<>();
    private static final Map<UUID, Work> WORK = new HashMap<>();
    private static final Map<Key, Device> DEVICES = new HashMap<>();
    private static final Map<UUID, EngineerStatePacket> HUD = new HashMap<>();
    private static final EngineerLedger LEDGER = new EngineerLedger();
    private static UUID match = UUID.randomUUID();
    private static int lastPhase;
    private EngineerManager() {}

    public static boolean classActive(ServerPlayer p) {
        var objective = p.getScoreboard().getObjective("SurvivorClass");
        return participant(p) && !p.isCreative() && EngineerRules.phase(GameManager.getPhaseValue()) && team(p, "survivors") && objective != null
                && p.getScoreboard().getOrCreatePlayerScore(p.getScoreboardName(), objective).getScore() == EngineerRules.CLASS_ID;
    }
    public static boolean active(ServerPlayer p) {
        return participant(p) && (classActive(p) || ACCOUNTS.containsKey(p.getUUID()) || p.getInventory().items.stream().anyMatch(s -> !s.isEmpty() && s.getItem() instanceof EngineerItem)
                || !p.getOffhandItem().isEmpty() && p.getOffhandItem().getItem() instanceof EngineerItem);
    }
    private static boolean team(Player p, String team) { return p.getTeam() != null && team.equals(p.getTeam().getName()); }
    private static boolean participant(ServerPlayer p) { return p.isAlive() && !p.isSpectator(); }
    private static boolean usable(ServerPlayer p) {
        var d = DownedCapability.get(p);
        return participant(p) && (p.isCreative() || (d == null || d.getState() != DownedState.DOWNED)
                && !org.example.maniacrevolution.ghost.GhostPossessionManager.isPossessed(p)
                && !org.example.maniacrevolution.event.PenaltySlotManager.isInPenaltySlot(p));
    }
    public static boolean busy(ServerPlayer p) { return WORK.containsKey(p.getUUID()); }
    private static boolean otherWork(ServerPlayer p) {
        if (org.example.maniacrevolution.techies.TechiesManager.placing(p)) return true;
        if (HackManager.isInitialized()) for (var s : HackManager.get().getActiveSessions().values()) if (s.getAllParticipants().contains(p)) return true;
        for (var other : p.server.getPlayerList().getPlayers()) {
            var downed = DownedCapability.get(other);
            if (downed != null && p.getUUID().equals(downed.getReviverUUID())) return true;
        }
        return false;
    }
    private static Account account(ServerPlayer p) { return ACCOUNTS.computeIfAbsent(p.getUUID(), k -> new Account()); }
    private static boolean hasTablet(ServerPlayer p) { return p.getInventory().contains(new ItemStack(ModItems.ENGINEER_TABLET.get())); }
    private static boolean holding(ServerPlayer p, Item item) { return p.getMainHandItem().is(item) || p.getOffhandItem().is(item); }
    private static long now(ServerPlayer p) { return p.server.overworld().getGameTime(); }
    private static void message(ServerPlayer p, String key) { p.displayClientMessage(Component.translatable("message.maniacrev.engineer." + key), true); }
    public static void forget(ResourceLocation dimension, BlockPos pos) { DEVICES.remove(new Key(dimension, pos)); }
    private static int count(UUID owner, boolean barricade) { return (int) DEVICES.values().stream().filter(d -> d.owner.equals(owner) && d.barricade == barricade).count(); }

    public static BlockHitResult ray(Player p) {
        Vec3 from = p.getEyePosition();
        return p.level().clip(new ClipContext(from, from.add(p.getLookAngle().scale(EngineerRules.REACH)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, p));
    }
    public static BlockPos target(Player p) {
        var hit = ray(p); if (hit.getType() != HitResult.Type.BLOCK) return null;
        var state = p.level().getBlockState(hit.getBlockPos());
        return state.getBlock() instanceof EngineerDeviceBlock ? EngineerDeviceBlock.base(hit.getBlockPos(), state) : null;
    }
    public static BlockPos placement(Player p) {
        var hit = ray(p);
        return hit.getType() == HitResult.Type.BLOCK && hit.getDirection() == Direction.UP ? hit.getBlockPos().above() : null;
    }
    public static boolean validPlacement(Level level, BlockPos pos, int type, Direction facing) {
        if (pos == null || type < 0 || type > 1 || !level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)
                || pos.getY() < level.getMinBuildHeight() || pos.getY() + (type == 0 ? 1 : 0) >= level.getMaxBuildHeight()
                || !level.getBlockState(pos).isAir() || level.getBlockEntity(pos) != null
                || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) return false;
        if (type == 0 && (!level.getBlockState(pos.above()).isAir() || level.getBlockEntity(pos.above()) != null)) return false;
        // No spacing requirement. Only the actual occupied volume must be empty of entities.
        AABB occupied = type == 0 ? facing.getAxis() == Direction.Axis.Z
                ? new AABB(pos.getX(), pos.getY(), pos.getZ() + .375, pos.getX() + 1, pos.getY() + 1.5, pos.getZ() + .625)
                : new AABB(pos.getX() + .375, pos.getY(), pos.getZ(), pos.getX() + .625, pos.getY() + 1.5, pos.getZ() + 1)
                : new AABB(pos).deflate(.15);
        return level.getEntities((net.minecraft.world.entity.Entity) null, occupied, e -> e.isAlive() && !(e instanceof net.minecraft.world.entity.item.ItemEntity)).isEmpty();
    }
    private static boolean canAfford(ServerPlayer p, Account a, int type) { return canAfford(p, a, type, false); }
    private static boolean canAfford(ServerPlayer p, Account a, int type, boolean physical) {
        if (!p.isCreative() && !physical && a.scrap < EngineerRules.cost(type)) { message(p, "no_scrap"); return false; }
        if (!p.isCreative() && type < 2 && now(p) < a.buildReady) { message(p, "cooldown"); return false; }
        if (type == 0 && count(p.getUUID(), true) >= EngineerRules.MAX_BARRICADES
                || type == 1 && count(p.getUUID(), false) >= EngineerRules.MAX_PANELS) { message(p, "limit"); return false; }
        if (type == 2 && a.armor >= EngineerRules.MAX_ARMOR) { message(p, "armor_full"); return false; }
        return true;
    }
    public static boolean canEatScrap(ServerPlayer p) {
        return usable(p) && !busy(p) && !otherWork(p) && canAfford(p, account(p), 2, true);
    }
    public static boolean eatScrap(ServerPlayer p) {
        if (!canEatScrap(p)) return false;
        Account a = account(p); a.armor = EngineerRules.addArmor(a.armor);
        sound(p.serverLevel(), p.position(), SoundEvents.ARMOR_EQUIP_IRON, .7f, 1.4f); sync(p); return true;
    }
    public static void depositScrap(ServerPlayer p, ItemStack stack) {
        if (!usable(p) || stack.isEmpty() || !stack.is(ModItems.ENGINEER_SCRAP.get())) return;
        Account a = account(p); int amount = Math.min(EngineerRules.PLATE_COST, EngineerRules.MAX_SCRAP - a.scrap);
        if (amount <= 0) { message(p, "scrap_full"); return; }
        a.scrap += amount; if (!p.isCreative()) stack.shrink(1); sync(p);
    }
    private static boolean dismantleRole(ServerPlayer p, boolean breaking) {
        return !EngineerRules.phase(GameManager.getPhaseValue()) || team(p, breaking ? "maniac" : "survivors") || !breaking && active(p);
    }
    public static void action(ServerPlayer p, EngineerActionPacket packet) {
        long now = now(p); var action = packet.action(); Work w = WORK.get(p.getUUID());
        if (action == EngineerActionPacket.Action.CANCEL) { cancel(p); return; }
        if (action == EngineerActionPacket.Action.KEEP_WORKING) {
            if (w != null && (w.type == BREAK || w.type == COLLECT) && packet.pos().equals(w.pos)
                    && packet.value() == w.type && w.pos.equals(target(p)) && usable(p)) w.held = now;
            return;
        }
        if (!usable(p) || p.containerMenu != p.inventoryMenu) return;
        if (action == EngineerActionPacket.Action.BREAK || action == EngineerActionPacket.Action.COLLECT) {
            if (w != null || otherWork(p)) return;
            boolean breaking = action == EngineerActionPacket.Action.BREAK;
            if (!breaking && !p.isShiftKeyDown()) return;
            if (!dismantleRole(p, breaking) || !packet.pos().equals(target(p))) return;
            if (!(p.level().getBlockEntity(packet.pos()) instanceof EngineerDeviceBlockEntity)) return;
            // A watched SCP cannot attack or dismantle scenery while its movement/attack gate is closed.
            if (breaking && org.example.maniacrevolution.scp173.Scp173Manager.held(p)) return;
            WORK.put(p.getUUID(), new Work(p, breaking ? BREAK : COLLECT, -1, packet.pos(), now)); sync(p); return;
        }
        if (!active(p)) return;
        Account a = account(p);
        if (action == EngineerActionPacket.Action.OPEN_WORKSHOP || action == EngineerActionPacket.Action.OPEN_REMOTE) { sync(p); return; }
        if (action == EngineerActionPacket.Action.USE_HELD) {
            if (w != null || otherWork(p) || packet.value() < 0 || packet.value() >= InteractionHand.values().length) return;
            InteractionHand hand = InteractionHand.values()[packet.value()];
            ItemStack stack = p.getItemInHand(hand);
            if (!(stack.getItem() instanceof EngineerItem item)) return;
            if (item.kind == EngineerItem.Kind.SCRAP) { if (!p.isUsingItem()) item.use(p.level(), p, hand); return; }
            int type = item.kind == EngineerItem.Kind.BARRICADE ? 0 : item.kind == EngineerItem.Kind.PANEL ? 1 : -1;
            if (type < 0 || !canAfford(p, a, type, true)) return;
            BlockPos pos = placement(p);
            if (!packet.pos().equals(pos) || !validPlacement(p.level(), pos, type, p.getDirection())) { message(p, "invalid_position"); return; }
            Work work = new Work(p, BUILD, type, pos, now); work.consumable = hand;
            a.selected = type; WORK.put(p.getUUID(), work);
            sound(p.serverLevel(), Vec3.atCenterOf(pos), SoundEvents.IRON_TRAPDOOR_OPEN, .75f, .8f);
        } else if (action == EngineerActionPacket.Action.SELECT) {
            int type = packet.value();
            if (w != null || type < 0 || type > 2 || !hasTablet(p) || otherWork(p) || !canAfford(p, a, type)) return;
            if (type == 2) WORK.put(p.getUUID(), new Work(p, ARM_PLATE, type, p.blockPosition(), now));
            else a.selected = type;
        } else if (action == EngineerActionPacket.Action.PLACE) {
            int type = a.selected; BlockPos pos = placement(p);
            if (w != null || type < 0 || !hasTablet(p) || otherWork(p) || !canAfford(p, a, type)) return;
            if (!packet.pos().equals(pos) || !validPlacement(p.level(), pos, type, p.getDirection())) { message(p, "invalid_position"); return; }
            WORK.put(p.getUUID(), new Work(p, BUILD, type, pos, now));
            sound(p.serverLevel(), Vec3.atCenterOf(pos), SoundEvents.IRON_TRAPDOOR_OPEN, .75f, .8f);
        } else if (action == EngineerActionPacket.Action.PLAY) {
            if (w != null || !holding(p, ModItems.ENGINEER_REMOTE.get()) || packet.value() < 0 || packet.value() > 1) return;
            var be = p.serverLevel().hasChunkAt(packet.pos()) && p.level().getBlockEntity(packet.pos()) instanceof EngineerDeviceBlockEntity d ? d : null;
            if (be == null || be.barricade() || !p.getUUID().equals(be.owner) || !match.equals(be.match)) return;
            long worldNow = p.level().getGameTime();
            if (be.uses <= 0 || worldNow < be.readyAt || be.playing()) { message(p, "panel_unavailable"); return; }
            be.uses--; be.scenario = packet.value(); be.playingUntil = worldNow + EngineerRules.PLAY_TICKS;
            be.readyAt = be.playingUntil + EngineerRules.PANEL_COOLDOWN; be.changed();
        }
        sync(p);
    }

    private static void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.BLOCKS, volume, pitch);
    }
    public static void deviceTick(EngineerDeviceBlockEntity be) {
        if (!(be.getLevel() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        if (be.owner == null || !match.equals(be.match) || now >= be.expires) { remove(be, false); return; }
        DEVICES.put(new Key(level.dimension().location(), be.getBlockPos()), new Device(new Key(level.dimension().location(), be.getBlockPos()), be.owner, be.barricade(), be.expires, be));
        if (be.barricade()) return;
        if (be.playingUntil > 0 && now >= be.playingUntil) {
            be.playingUntil = 0;
            if (be.uses == 0) { remove(be, true); return; }
            be.changed();
        }
        if (!be.playing()) return;
        long elapsed = EngineerRules.PLAY_TICKS - (be.playingUntil - now);
        Vec3 at = Vec3.atCenterOf(be.getBlockPos());
        if (be.scenario == 0 && elapsed % 7 == 0) {
            // A small moving sound origin suggests footsteps without creating a real player/source UUID.
            double offset = Math.sin(elapsed * .12) * .8;
            sound(level, at.add(offset, -.35, 0), SoundEvents.STONE_STEP, .9f, .9f + level.random.nextFloat() * .2f);
        } else if (be.scenario == 1 && elapsed % 12 == 0) {
            sound(level, at, elapsed % 48 == 0 ? SoundEvents.NOTE_BLOCK_BIT.get() : SoundEvents.LEVER_CLICK, .7f, elapsed % 48 == 0 ? .65f : 1.5f);
        }
        if (elapsed % 14 == 0) org.example.maniacrevolution.warden.WardenNoiseManager.deviceNoise(level, at);
        if (elapsed % 10 == 0) level.sendParticles(ParticleTypes.NOTE, at.x, at.y + .5, at.z, 1, 0, 0, 0, 0);
    }
    private static void remove(EngineerDeviceBlockEntity be, boolean pop) {
        if (!(be.getLevel() instanceof ServerLevel level)) return;
        Vec3 at = Vec3.atCenterOf(be.getBlockPos());
        if (pop) {
            sound(level, at, SoundEvents.GENERIC_EXPLODE, .65f, 1.6f);
            level.sendParticles(ParticleTypes.POOF, at.x, at.y, at.z, 12, .2, .3, .2, .02);
        }
        level.removeBlock(be.getBlockPos(), false);
    }
    private static void cancel(ServerPlayer p) {
        Work w = WORK.remove(p.getUUID());
        if (w != null && (w.type == BREAK || w.type == COLLECT)) p.serverLevel().destroyBlockProgress(p.getId(), w.pos, -1);
        var a = ACCOUNTS.get(p.getUUID()); if (a != null) a.selected = -1;
        sync(p);
    }
    private static void finish(ServerPlayer p, Work w) {
        if (w.type == BREAK || w.type == COLLECT) {
            if (p.level().getBlockEntity(w.pos) instanceof EngineerDeviceBlockEntity be) {
                sound(p.serverLevel(), Vec3.atCenterOf(w.pos), SoundEvents.IRON_DOOR_CLOSE, 1, .8f);
                remove(be, false);
            }
            cancel(p); return;
        }
        Account a = account(p);
        if (!canAfford(p, a, w.selection, w.consumable != null)) { cancel(p); return; }
        if (w.type == ARM_PLATE) {
            a.armor = EngineerRules.addArmor(a.armor);
            sound(p.serverLevel(), p.position(), SoundEvents.ARMOR_EQUIP_IRON, .7f, 1.4f);
        } else {
            if (!validPlacement(p.level(), w.pos, w.selection, w.facing)) { message(p, "invalid_position"); cancel(p); return; }
            BlockState state = (w.selection == 0 ? ModBlocks.ENGINEER_BARRICADE : ModBlocks.ENGINEER_PANEL).get().defaultBlockState()
                    .setValue(EngineerDeviceBlock.FACING, w.facing);
            if (!p.level().setBlock(w.pos, state, 3)) { cancel(p); return; }
            if (w.selection == 0 && !p.level().setBlock(w.pos.above(), state.setValue(EngineerDeviceBlock.HALF, DoubleBlockHalf.UPPER), 3)) {
                p.level().removeBlock(w.pos, false); cancel(p); return;
            }
            if (!(p.level().getBlockEntity(w.pos) instanceof EngineerDeviceBlockEntity be)) { p.level().removeBlock(w.pos, false); cancel(p); return; }
            be.owner = p.getUUID(); be.match = match;
            be.expires = w.selection == 0 ? p.level().getGameTime() + EngineerRules.BARRICADE_LIFETIME : Long.MAX_VALUE;
            be.changed();
            Key key = new Key(p.level().dimension().location(), w.pos);
            DEVICES.put(key, new Device(key, p.getUUID(), w.selection == 0, be.expires, be));
            a.buildReady = now(p) + EngineerRules.BUILD_COOLDOWN;
            sound(p.serverLevel(), Vec3.atCenterOf(w.pos), SoundEvents.ANVIL_PLACE, .7f, 1.4f);
        }
        if (!p.isCreative()) {
            if (w.consumable != null) p.getItemInHand(w.consumable).shrink(1);
            else a.scrap -= EngineerRules.cost(w.selection);
        }
        cancel(p);
    }
    private static void loadout(ServerPlayer p, Account a) {
        for (Item item : List.of(ModItems.ENGINEER_TABLET.get(), ModItems.ENGINEER_REMOTE.get())) {
            if (a.given.contains(item)) continue;
            if (p.getInventory().contains(new ItemStack(item))) a.given.add(item);
            else if (p.getInventory().getFreeSlot() >= 0) { p.getInventory().add(new ItemStack(item)); a.given.add(item); p.inventoryMenu.broadcastChanges(); }
        }
    }
    public static void observeProgress(int computer, float points) { LEDGER.observe(computer, points); }
    public static void onProgress(int computer, float before, float after, Map<ServerPlayer, Float> contributions) {
        float fresh = LEDGER.freshFraction(computer, before, after);
        if (fresh <= 0) return;
        for (var contribution : contributions.entrySet()) {
            ServerPlayer p = contribution.getKey();
            if (!classActive(p) || !usable(p) || contribution.getValue() <= 0) continue;
            Account a = account(p);
            a.fraction += contribution.getValue() * fresh / Math.max(.0001f, HackConfig.HACK_POINTS_REQUIRED) * 100;
            int earned = Math.min((int) (a.fraction + 1.0e-5), EngineerRules.MATCH_INCOME - a.earned);
            a.fraction -= Math.floor(a.fraction + 1.0e-5);
            if (earned > 0) { a.earned += earned; a.scrap = Math.min(EngineerRules.MAX_SCRAP, a.scrap + earned); sync(p); }
        }
    }
    private static void sync(ServerPlayer p) {
        boolean active = active(p); Account a = ACCOUNTS.get(p.getUUID()); Work w = WORK.get(p.getUUID());
        List<EngineerStatePacket.Panel> panels = new ArrayList<>();
        if (active) DEVICES.values().stream().filter(d -> !d.barricade && d.owner.equals(p.getUUID()))
                .sorted(Comparator.comparing((Device d) -> d.key.dimension.toString()).thenComparingLong(d -> d.key.pos.asLong()))
                .limit(EngineerRules.MAX_PANELS).forEach(d -> {
                    var be = d.entity; boolean loaded = !be.isRemoved() && be.getLevel() != null && be.getLevel().hasChunkAt(d.key.pos);
                    long timeOffset = be.getLevel() == null ? 0 : now(p) - be.getLevel().getGameTime();
                    panels.add(new EngineerStatePacket.Panel(d.key.dimension, d.key.pos, be.uses, be.playingUntil == 0 ? 0 : be.playingUntil + timeOffset, be.readyAt + timeOffset, loaded));
                });
        var snapshot = new EngineerStatePacket(0, active, a == null ? 0 : a.scrap, a == null ? 0 : a.earned,
                count(p.getUUID(), true), active && a != null ? a.armor : 0, a == null ? -1 : a.selected, w == null ? -1 : w.type,
                w == null ? BlockPos.ZERO : w.pos, w == null ? -1 : w.started, a == null ? 0 : a.buildReady, List.copyOf(panels));
        if (!snapshot.equals(HUD.put(p.getUUID(), snapshot))) ModNetworking.sendToPlayer(new EngineerStatePacket(now(p), active, snapshot.scrap(), snapshot.earned(), snapshot.barricades(), snapshot.armor(), snapshot.selected(), snapshot.work(), snapshot.workPos(), snapshot.started(), snapshot.buildReady(), snapshot.panels()), p);
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) { HUD.remove(p.getUUID()); }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) { cancel(p); HUD.remove(p.getUUID()); }
    }
    @SubscribeEvent public static void death(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) { var a = ACCOUNTS.get(p.getUUID()); if (a != null) a.armor = 0; cancel(p); }
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void damage(LivingDamageEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && e.getAmount() > 0) {
            if (WORK.containsKey(p.getUUID())) { message(p, "interrupted"); cancel(p); }
            Account a = ACCOUNTS.get(p.getUUID());
            boolean attack = EngineerRules.phase(GameManager.getPhaseValue())
                    ? e.getSource().getEntity() instanceof ServerPlayer attacker && team(attacker, "maniac")
                    : e.getSource().getEntity() instanceof net.minecraft.world.entity.LivingEntity;
            if (usable(p) && a != null && a.armor > 0 && attack) {
                var hit = EngineerRules.absorb(a.armor, e.getAmount());
                a.armor = hit.armor(); e.setAmount(hit.damage());
                sound(p.serverLevel(), p.position(), a.armor == 0 ? SoundEvents.SHIELD_BREAK : SoundEvents.SHIELD_BLOCK, .7f, 1.4f); sync(p);
            }
        }
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void click(PlayerInteractEvent.LeftClickBlock e) {
        if (e.getLevel().getBlockState(e.getPos()).getBlock() instanceof EngineerDeviceBlock && !e.getEntity().isCreative()) e.setCanceled(true);
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        var server = ServerLifecycleHooks.getCurrentServer(); if (server == null) return;
        update(server, server.getPlayerList().getPlayers());
    }
    static void update(MinecraftServer server, List<ServerPlayer> players) {
        int phase = GameManager.getPhaseValue();
        if (EngineerRules.phase(lastPhase) && !EngineerRules.phase(phase)) resetMatch(server);
        lastPhase = phase;
        DEVICES.entrySet().removeIf(entry -> {
            Device d = entry.getValue(); var level = server.getLevel(ResourceKey.create(Registries.DIMENSION, d.key.dimension));
            if (level == null || level.getGameTime() >= d.expires) return true;
            return level.hasChunkAt(d.key.pos) && level.getBlockEntity(d.key.pos) != d.entity;
        });
        for (ServerPlayer p : players) {
            if (active(p)) account(p);
            if (classActive(p)) loadout(p, account(p));
            Work w = WORK.get(p.getUUID());
            if (w != null) {
                boolean invalid = !usable(p) || !w.dimension.equals(p.level().dimension().location())
                        || p.containerMenu != p.inventoryMenu || w.slot != p.getInventory().selected
                        || EngineerRules.interrupted(w.origin.distanceToSqr(p.position()), p.getYRot() - w.yaw, p.getXRot() - w.pitch);
                if (w.type == BREAK || w.type == COLLECT) invalid |= now(p) - w.held > 8 || !w.pos.equals(target(p))
                        || !dismantleRole(p, w.type == BREAK)
                        || w.type == COLLECT && !p.isShiftKeyDown()
                        || w.type == BREAK && org.example.maniacrevolution.scp173.Scp173Manager.held(p);
                else {
                    boolean equipment = w.consumable == null ? hasTablet(p) : !p.getItemInHand(w.consumable).isEmpty()
                            && p.getItemInHand(w.consumable).getItem() instanceof EngineerItem item
                            && item.kind == (w.selection == 0 ? EngineerItem.Kind.BARRICADE : EngineerItem.Kind.PANEL);
                    invalid |= !active(p) || !equipment || otherWork(p);
                }
                if (invalid) { message(p, "interrupted"); cancel(p); }
                else if (now(p) - w.started >= w.duration()) finish(p, w);
                else if (w.type == BREAK || w.type == COLLECT) p.serverLevel().destroyBlockProgress(p.getId(), w.pos, (int) ((now(p) - w.started) * 10 / w.duration()));
            }
            var a = ACCOUNTS.get(p.getUUID());
            if (!active(p) && a != null) a.selected = -1;
            if (active(p) || WORK.containsKey(p.getUUID()) || HUD.containsKey(p.getUUID())) sync(p);
        }
    }
    public static void resetMatch(MinecraftServer server) {
        for (var d : List.copyOf(DEVICES.values())) {
            var level = server.getLevel(ResourceKey.create(Registries.DIMENSION, d.key.dimension));
            if (level != null && level.hasChunkAt(d.key.pos) && level.getBlockEntity(d.key.pos) instanceof EngineerDeviceBlockEntity be) remove(be, false);
        }
        for (var p : server.getPlayerList().getPlayers()) {
            Work w = WORK.get(p.getUUID()); if (w != null) p.serverLevel().destroyBlockProgress(p.getId(), w.pos, -1);
        }
        DEVICES.clear(); WORK.clear(); ACCOUNTS.clear(); LEDGER.clear(); HUD.clear(); match = UUID.randomUUID();
        for (var p : server.getPlayerList().getPlayers()) {
            ModNetworking.sendToPlayer(new EngineerStatePacket(now(p), false, 0, 0, 0, 0, -1, -1, BlockPos.ZERO, -1, 0, List.of()), p);
        }
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent e) {
        DEVICES.clear(); WORK.clear(); ACCOUNTS.clear(); LEDGER.clear(); HUD.clear(); lastPhase = 0; match = UUID.randomUUID();
    }
}
