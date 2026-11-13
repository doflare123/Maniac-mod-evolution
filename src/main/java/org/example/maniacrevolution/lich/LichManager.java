package org.example.maniacrevolution.lich;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.*;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.downed.*;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.entity.ModEntities;
import org.example.maniacrevolution.game.GameManager;
import org.example.maniacrevolution.hack.ComputerBlock;
import org.example.maniacrevolution.map.MapRegistry;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.util.*;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public final class LichManager {
    public static final class Army {
        public final ServerPlayer player;
        final ServerLevel level;
        public final Map<UUID, LichUnit> units = new LinkedHashMap<>();
        public final Set<UUID> selected = new HashSet<>();
        final Map<Integer, Set<UUID>> groups = new HashMap<>();
        int hp = 10, page;
        long melee, nextRequest;
        LichObject phylactery;
        boolean phylacteryIssued;
        Cast cast;
        long castSerial;
        Army(ServerPlayer player) { this.player = player; level = player.serverLevel(); }
    }
    private record Cast(String action, UUID corpse, LichRules.Kind kind, Vec3 start, long finish) {}
    private static final Map<UUID, Army> ARMIES = new HashMap<>();
    private static final Map<UUID, LichObject> CORPSES = new LinkedHashMap<>();
    private static final Map<UUID, String> EDIT_MAP = new HashMap<>();
    private record Soul(int hp) {}
    private static final Map<UUID, Soul> SOULS = new HashMap<>();
    private static final Map<UUID, UUID> RESERVATIONS = new HashMap<>();
    private static final Set<UUID> CLEARED_UNITS = new HashSet<>(), CLEARED_CORPSES = new HashSet<>();
    private static long delayedUntil;
    private static UUID session = UUID.randomUUID();
    private static boolean ended, running;
    private static boolean guaranteedPaused;
    private LichManager() {}
    public static Army owner(UUID id) { return id == null ? null : ARMIES.get(id); }
    static UUID session() { return session; }
    private static boolean phaseActive() { return GameManager.getPhaseValue()>=1&&GameManager.getPhaseValue()<=3; }
    public static boolean active(ServerPlayer p) {
        return p != null && p.isAlive() && PlayerModeUtil.isSurvivalOrAdventure(p) && phaseActive()
                && p.getTeam() != null && "maniac".equals(p.getTeam().getName())
                && PlayerDataManager.get(p).getManiacClassId() == org.example.maniacrevolution.character.maniac.LichClass.SCOREBOARD_ID;
    }
    public static boolean survivor(ServerPlayer p) {
        if (p == null || !p.isAlive() || !PlayerModeUtil.isSurvivalOrAdventure(p)
                || p.getTeam() == null || !"survivors".equals(p.getTeam().getName())) return false;
        var downed = DownedCapability.get(p);
        return downed == null || downed.getState() != DownedState.DOWNED;
    }
    public static boolean friendly(Entity attacker, Entity target) {
        if (!(attacker instanceof LichUnit) && !(attacker != null && attacker.getPersistentData().getBoolean("lich_arrow"))) return false;
        return target instanceof LichUnit || target instanceof LichObject || target instanceof ServerPlayer p
                && (p.getTeam() == null || !"survivors".equals(p.getTeam().getName()) || !survivor(p));
    }
    private static void give(ServerPlayer p, ItemStack stack) {
        if (!p.getInventory().add(stack)) p.drop(stack, false);
    }
    private static void strip(ServerPlayer p, net.minecraft.world.item.Item item) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (p.getInventory().getItem(i).is(item)) p.getInventory().setItem(i, ItemStack.EMPTY);
    }
    private static boolean carried(Army s) {
        return s.phylactery == null && s.hp > 0;
    }
    private static void stripPhylactery(ServerPlayer p) {
        var item=ModItems.LICH_PHYLACTERY.get();
        strip(p,item);
        if(p.containerMenu.getCarried().is(item))p.containerMenu.setCarried(ItemStack.EMPTY);
    }
    private static void syncCarriedPhylactery(Army s) {
        if(!carried(s)) {
            stripPhylactery(s.player);
            return;
        }
        var item=ModItems.LICH_PHYLACTERY.get();
        var inventory=s.player.getInventory();
        var cursor=s.player.containerMenu.getCarried();
        boolean found=cursor.is(item);
        if(found)cursor.setCount(1);
        for(int slot=0;slot<inventory.getContainerSize();slot++) {
            var stack=inventory.getItem(slot);
            if(!stack.is(item))continue;
            if(found)inventory.setItem(slot,ItemStack.EMPTY);
            else {stack.setCount(1);found=true;}
        }
        // Item tags change during casts. The issued stack can also be on a cursor or in a container.
        if(found)s.phylacteryIssued=true;
        else if(!s.phylacteryIssued)s.phylacteryIssued=inventory.add(new ItemStack(item));
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        update(e.getServer(), e.getServer().getPlayerList().getPlayers());
    }
    static void update(MinecraftServer server, List<ServerPlayer> players) {
        boolean match = phaseActive();
        if (!match) {
            if (running) cleanup(server);
            running = false; ended = false;
        } else if (!ended) {
            if(!running){guaranteedPaused=false;delayedUntil=0;}
            running = true;
            for (var entry : List.copyOf(ARMIES.entrySet())) {
                var s = entry.getValue();
                if (!active(s.player) || !players.contains(s.player) || s.player.level() != s.level) {
                    clear(s); ARMIES.remove(entry.getKey());
                }
            }
            for (var p : players) if (active(p) && !ARMIES.containsKey(p.getUUID())) {
                var s = new Army(p); var soul = SOULS.get(p.getUUID());
                if (soul != null) s.hp = soul.hp;
                ARMIES.put(p.getUUID(), s);
                strip(p, ModItems.LICH_BOOK.get()); stripPhylactery(p);
                give(p, new ItemStack(ModItems.LICH_BOOK.get()));
            }
            for (var s : ARMIES.values()) {
                syncCarriedPhylactery(s);
                s.units.values().removeIf(u -> !u.isAlive() && !unloaded(u));
                s.selected.retainAll(s.units.keySet()); s.groups.values().forEach(g -> g.retainAll(s.units.keySet()));
                tickCast(s);
                if (server.getTickCount() % 10 == 0) snapshot(s, false, null);
            }
            CORPSES.values().removeIf(c -> c.isRemoved() && !unloaded(c));
            replenish(server);
        }
        if (server.getTickCount() % 10 == 0) for (var p : players)
            if (p.hasPermissions(2) && p.getMainHandItem().is(ModItems.LICH_CORPSE_EDITOR.get())) highlightPoints(p);
        if (server.getTickCount() % 5 == 0) forms(server);
    }
    private static boolean unloaded(Entity entity) { return entity.getRemovalReason() == Entity.RemovalReason.UNLOADED_TO_CHUNK; }
    private static void clear(Army s) {
        cancelCast(s);
        SOULS.put(s.player.getUUID(), new Soul(s.hp));
        for (var u : s.units.values()) { CLEARED_UNITS.add(u.getUUID()); SelectiveGlowingEffect.removeAllGlowing(u); if (!u.isRemoved()) u.discard(); }
        if (s.phylactery != null && !s.phylactery.isRemoved()) s.phylactery.discard();
        strip(s.player, ModItems.LICH_BOOK.get()); stripPhylactery(s.player);
        var tag = new CompoundTag(); tag.putString("mode", "clear"); send(s.player, tag);
    }
    private static void cleanup(MinecraftServer server) {
        ARMIES.values().forEach(s->s.player.removeEffect(ModEffects.LICH_PHYLACTERY_GUARD.get()));
        for(var id:SOULS.keySet()) {
            var player=server.getPlayerList().getPlayer(id);
            if(player!=null)player.removeEffect(ModEffects.LICH_PHYLACTERY_GUARD.get());
        }
        ARMIES.values().forEach(LichManager::clear); ARMIES.clear(); clearCorpses(); SOULS.clear(); delayedUntil = 0; guaranteedPaused=false;
        for (var level : server.getAllLevels()) for (var entity : level.getAllEntities())
            if (entity.getPersistentData().getBoolean("lich_arrow")) entity.discard();
        session=UUID.randomUUID();CLEARED_UNITS.clear();CLEARED_CORPSES.clear();RESERVATIONS.clear();
    }
    public static int clearUnits() {
        int n = 0; for (var s : ARMIES.values()) { n += s.units.size(); for (var u : s.units.values()) { CLEARED_UNITS.add(u.getUUID()); if (!u.isRemoved()) u.discard(); } s.units.clear(); s.selected.clear(); s.groups.clear(); } return n;
    }
    public static int clearCorpses() {
        int n = CORPSES.size(); for (var c : CORPSES.values()) { CLEARED_CORPSES.add(c.getUUID()); if (!c.isRemoved()) c.discard(); } CORPSES.clear(); return n;
    }
    public static void pauseGuaranteedCorpses(boolean paused) { guaranteedPaused=paused; }
    @SubscribeEvent public static void join(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide()) return;
        if(e.getEntity().getPersistentData().getBoolean("lich_arrow")&&(!e.getEntity().getPersistentData().hasUUID("lich_session")||!session.equals(e.getEntity().getPersistentData().getUUID("lich_session")))){e.setCanceled(true);return;}
        if (e.getEntity() instanceof LichUnit u) {
            var s = owner(u.owner);
            if (s == null || !session.equals(u.session) || CLEARED_UNITS.contains(u.getUUID()) || s.level != e.getLevel()) { e.setCanceled(true); return; }
            var previous = s.units.put(u.getUUID(), u);
            if (previous != null && previous != u) {
                u.order = previous.order; u.anchor = previous.anchor; u.destination = previous.destination;
                u.aggressive = previous.aggressive; u.orderedTarget = previous.orderedTarget;
            }
        }
        if (e.getEntity() instanceof LichObject c) {
            if(!session.equals(c.session)){e.setCanceled(true);return;}
            if (c.phylactery()) {
                var s = owner(c.owner);
                if (s == null || s.phylactery == null || !s.phylactery.getUUID().equals(c.getUUID())) e.setCanceled(true);
                else s.phylactery = c;
            } else if (CORPSES.containsKey(c.getUUID()) && !CLEARED_CORPSES.contains(c.getUUID())) CORPSES.put(c.getUUID(), c);
            else e.setCanceled(true);
        }
    }
    public static boolean validObject(LichObject c) {
        if (!c.phylactery()) return CORPSES.get(c.getUUID()) == c;
        var s = owner(c.owner); return s != null && s.hp > 0 && s.phylactery == c;
    }
    private static String currentMap(MinecraftServer server) {
        var o = server.getScoreboard().getObjective("map");
        var map = o == null ? null : MapRegistry.getMapByNumericId(server.getScoreboard().getOrCreatePlayerScore("Game", o).getScore());
        return map == null ? "" : map.getId();
    }
    private static boolean corpse(ServerLevel level, Vec3 pos, String map) {
        var c = ModEntities.LICH_OBJECT.get().create(level); if (c == null) return false;
        c.configure(null, map, false); c.setPos(pos); CORPSES.put(c.getUUID(), c);
        if (level.addFreshEntity(c)) return true;
        CORPSES.remove(c.getUUID()); return false;
    }
    public record CorpseSpawnResult(int spawned, int skipped) {}
    public static CorpseSpawnResult spawnCorpsesAtAllPoints(MinecraftServer server) {
        int spawned = 0, skipped = 0;
        CORPSES.values().removeIf(c -> c.isRemoved() && !unloaded(c));
        for (var point : List.copyOf(CorpsePoints.get(server).points)) {
            var dimension = net.minecraft.resources.ResourceLocation.tryParse(point.dimension());
            var level = dimension == null ? null : server.getLevel(net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION, dimension));
            var pos = point.pos();
            if (level == null || level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)) {
                skipped++; continue;
            }
            // Explicit test command loads authored points once; it creates no persistent chunk tickets.
            level.getChunkAt(pos);
            var location = Vec3.atBottomCenterOf(pos);
            if (!safe(level, pos, .8, .5) || CORPSES.values().stream().anyMatch(c ->
                    c.level() == level && c.distanceToSqr(location) < 1)
                    || !corpse(level, location, point.map())) skipped++;
            else spawned++;
        }
        return new CorpseSpawnResult(spawned, skipped);
    }
    private static void replenish(MinecraftServer server) {
        if (ARMIES.isEmpty() || guaranteedPaused) return;
        int army = ARMIES.values().stream().mapToInt(s -> s.units.size()).sum();
        int missing = LichRules.missing(army, CORPSES.size(), server.getTickCount(), delayedUntil);
        if (missing == 0) return;
        var points = new ArrayList<>(CorpsePoints.get(server).points.stream().filter(p -> p.map().equals(currentMap(server))).toList());
        Collections.shuffle(points);
        for (var p : points) {
            var level = server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, new net.minecraft.resources.ResourceLocation(p.dimension())));
            if (level == null || !level.hasChunkAt(p.pos()) || !safe(level, p.pos(), .8, .5)
                    || CORPSES.values().stream().anyMatch(c -> c.level() == level && c.distanceToSqr(Vec3.atBottomCenterOf(p.pos())) < 1)) continue;
            corpse(level, Vec3.atBottomCenterOf(p.pos()), p.map());
            if (--missing == 0) break;
        }
    }
    private static boolean safe(ServerLevel level, BlockPos pos, double width, double height) {
        return LichRules.safeSurface(level,pos,width,height);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void death(LivingDeathEvent e) {
        if (!e.isCanceled() && e.getEntity() instanceof ServerPlayer p && p.getTeam() != null && "survivors".equals(p.getTeam().getName())
                && phaseActive() && !ARMIES.isEmpty()) corpse(p.serverLevel(), p.position(), currentMap(p.server));
    }
    public static void openCorpse(ServerPlayer p, LichObject c) {
        if (!active(p) || c.phylactery() || c.level() != p.level() || p.distanceToSqr(c) > 9 || !p.hasLineOfSight(c) || !validObject(c)) return;
        var s = owner(p.getUUID()); if (s == null) return;
        ui(p,"book");
        snapshot(s, true, c.getUUID());
    }
    public static void request(ServerPlayer p, String action, CompoundTag d) {
        if (action.equals("editor") || action.equals("map") || action.equals("delete_point") || action.equals("corpse_list")) {
            if (!p.hasPermissions(2)) return;
            if (action.equals("map")) {
                String map = d.getString("map"); if (MapRegistry.getMapById(map) == null) return;
                EDIT_MAP.put(p.getUUID(), map); if(p.getMainHandItem().is(ModItems.LICH_CORPSE_EDITOR.get()))p.getMainHandItem().getOrCreateTag().putString("LichMap", map); editor(p, false);
            } else if (action.equals("delete_point")) {
                if (!d.hasUUID("id")) return;
                var points = CorpsePoints.get(p.server); points.points.removeIf(x -> x.id().equals(d.getUUID("id"))); points.setDirty(); editor(p, false);
            } else if (action.equals("corpse_list")) corpseList(p, d.getString("map"), d.getInt("page"));
            else editor(p, true);
            return;
        }
        if (!active(p)) return;
        var s = owner(p.getUUID()); if (s == null || ended) return;
        // Combat has its own cycle and must acknowledge a click even next to a menu request.
        if(action.equals("shoot")||action.equals("melee")) {LichCombat.input(p);return;}
        long now = p.server.getTickCount(); if (now < s.nextRequest) return; s.nextRequest = now + 1;
        Set<UUID> previousSelection=switch(action) {
            case "select","all","none","group" -> Set.copyOf(s.selected);
            default -> null;
        };
        switch (action) {
            case "open" -> { ui(p,"book"); snapshot(s, true, null); }
            case "page" -> { s.page = Math.max(0, d.getInt("page")); snapshot(s, false, null); }
            case "select" -> {
                if (!d.hasUUID("id") || !s.units.containsKey(d.getUUID("id"))) return;
                UUID id = d.getUUID("id");
                if (!s.selected.remove(id)) {
                    if (!d.getBoolean("add")) s.selected.clear();
                    s.selected.add(id);
                }
                ui(p,"book");
            }
            case "all" -> { s.selected.clear(); s.selected.addAll(s.units.keySet()); }
            case "none" -> s.selected.clear();
            case "group" -> {
                int key = d.getInt("key"); if (key < 0 || key > 9) return;
                if (d.getBoolean("save")) {
                    s.groups.put(key, new HashSet<>(s.selected));
                    cue(p,"group",null,null,List.of(),key);
                }
                else { s.selected.clear(); s.selected.addAll(s.groups.getOrDefault(key, Set.of())); }
            }
            case "reaction" -> {
                for (var id : s.selected) { var u = s.units.get(id); if (u != null) u.aggressive = d.getBoolean("attack"); }
                if(!s.selected.isEmpty())cue(p,d.getBoolean("attack")?"aggressive":"hold",null,null,List.of(),0);
            }
            case "order" -> {
                int ordinal = d.getInt("order"); if (ordinal < 0 || ordinal >= LichRules.Order.values().length) return;
                var order = LichRules.Order.values()[ordinal];
                if (order == LichRules.Order.CHASE || order == LichRules.Order.GUARD || order == LichRules.Order.MOVE) {
                    p.displayClientMessage(Component.literal("§dПКМ книгой по " + (order == LichRules.Order.CHASE ? "видимому выжившему" : order == LichRules.Order.GUARD ? "компьютеру" : "точке мира")), true); return;
                }
                Vec3 point = p.position(); apply(s, order, point, null);
            }
            case "raise", "absorb" -> {
                if (s.cast != null || !d.hasUUID("corpse")) return;
                var c = CORPSES.get(d.getUUID("corpse"));
                if (c == null || c.isRemoved() || c.level() != p.level() || p.distanceToSqr(c) > 9 || !p.hasLineOfSight(c) || RESERVATIONS.containsKey(c.getUUID())) return;
                int kind = d.getInt("kind"); if (kind < 0 || kind >= LichRules.Kind.values().length) return;
                s.cast = new Cast(action, c.getUUID(), LichRules.Kind.values()[kind], p.position(), p.level().getGameTime() + 10);
                RESERVATIONS.put(c.getUUID(),p.getUUID()); c.action(action,10);
            }
            case "repair" -> {
                if (s.cast != null || s.hp <= 0 || s.hp >= 10 || !nearPhylactery(s)) return;
                UUID target=d.hasUUID("phylactery")?d.getUUID("phylactery"):null;
                if(target!=null) {
                    if(s.phylactery==null||!s.phylactery.getUUID().equals(target)
                            ||p.hasEffect(ModEffects.STUN.get())||org.example.maniacrevolution.event.PenaltySlotManager.isInPenaltySlot(p))return;
                    Vec3 eye=p.getEyePosition();
                    var hit=s.phylactery.getBoundingBox().inflate(.1).clip(eye,eye.add(p.getLookAngle().scale(4)));
                    if(hit.isEmpty()||!clearSight(p,hit.get()))return;
                }
                s.cast = new Cast(action,target,null,p.position(),p.level().getGameTime()+10);
                if(s.phylactery!=null)s.phylactery.action("repair",10);
            }
            default -> { return; }
        }
        if(previousSelection!=null)selectionCue(s,previousSelection);
        snapshot(s, false, null);
        if(s.cast!=null&&(action.equals("raise")||action.equals("absorb")||action.equals("repair"))) {
            s.castSerial++;castFeedback(s,s.cast,"casting",null);
        }
        if (s.cast != null) { itemCast(s,s.cast.action); forms(p.server); }
    }
    private static boolean nearPhylactery(Army s) { return carried(s) || s.phylactery != null && !s.phylactery.isRemoved() && s.player.distanceToSqr(s.phylactery) <= 9 && s.player.hasLineOfSight(s.phylactery); }
    private static void tickCast(Army s) {
        var c = s.cast; if (c == null) return;
        var p = s.player;
        if (LichRules.castInterrupted(p.position().distanceToSqr(c.start), false, p.hasEffect(ModEffects.STUN.get()))) { cancelCast(s); return; }
        p.serverLevel().sendParticles(ParticleTypes.SOUL, p.getX(), p.getY()+1, p.getZ(), 2, .2,.2,.2, 0);
        if (p.level().getGameTime() < c.finish) return;
        clearCast(s);
        var result=completeCast(s,c);
        castFeedback(s,c,result==null?"failed":result instanceof LichUnit?"emerge":"complete",result);
        snapshot(s,false,null);
    }
    private static Entity completeCast(Army s,Cast c) {
        var p=s.player;
        if (c.action.equals("repair")) {
            if(c.corpse!=null&&(s.phylactery==null||!s.phylactery.getUUID().equals(c.corpse)))return null;
            if (s.hp > 0 && s.hp < 10 && nearPhylactery(s) && ManaUtil.consumeMana(p, 5)) { s.hp++; LichSounds.play(p, "repair_complete", .7F); return p; }
            return null;
        }
        var corpse = CORPSES.get(c.corpse);
        if (corpse == null || corpse.isRemoved() || corpse.level() != p.level() || p.distanceToSqr(corpse) > 9 || !p.hasLineOfSight(corpse)) return null;
        if (c.action.equals("absorb")) {
            CORPSES.remove(c.corpse); CLEARED_CORPSES.add(c.corpse); corpse.discard(); ManaUtil.addMana(p, 20);
            if (s.units.size() < 3) delayedUntil = p.server.getTickCount() + 1200;
            LichSounds.play(p, "absorb_complete", .7F);
            return p;
        } else {
            var unit = ModEntities.LICH_UNIT.get().create(p.serverLevel()); if (unit == null) return null;
            unit.setPos(corpse.position()); unit.configure(p.getUUID(), c.kind);
            if (!p.serverLevel().noCollision(unit) || !ManaUtil.consumeMana(p, c.kind.cost)) return null;
            if (!p.serverLevel().addFreshEntity(unit)) { s.units.remove(unit.getUUID()); ManaUtil.addMana(p, c.kind.cost); return null; }
            s.units.put(unit.getUUID(), unit); CORPSES.remove(c.corpse); CLEARED_CORPSES.add(c.corpse); corpse.discard();
            return unit;
        }
    }
    static int corpseCount() { return CORPSES.size(); }
    static List<LichObject> corpses() { return List.copyOf(CORPSES.values()); }
    public static void quick(ServerPlayer p) {
        if (!active(p)) return; var s = owner(p.getUUID()); if (s == null) return;
        if(p.isShiftKeyDown()&&p.getMainHandItem().is(ModItems.LICH_BOOK.get())) {
            var previous=Set.copyOf(s.selected);
            s.selected.clear();selectionCue(s,previous);snapshot(s,false,null);return;
        }
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        // Server entities, not the client's tracking list. No artificial range limit for entities.
        Entity best = null; double bestDistance = Double.POSITIVE_INFINITY;
        var candidates = new ArrayList<Entity>(); candidates.addAll(s.units.values()); candidates.addAll(p.serverLevel().players()); candidates.addAll(CORPSES.values());
        for (var entity : candidates) {
            if (entity == p || entity.isRemoved() || entity.level() != p.level()) continue;
            if (entity instanceof ServerPlayer other && !survivor(other)) continue;
            if (entity instanceof LichObject && p.distanceToSqr(entity) > 9) continue;
            double length = eye.distanceTo(entity.getBoundingBox().getCenter()) + 3;
            var hit = entity.getBoundingBox().inflate(.15).clip(eye, eye.add(look.scale(length)));
            if (hit.isEmpty() || !clearSight(p, hit.get())) continue;
            double distance = eye.distanceToSqr(hit.get()); if (distance < bestDistance) { bestDistance = distance; best = entity; }
        }
        if (best instanceof LichUnit u) {
            if (!s.selected.remove(u.getUUID())) s.selected.add(u.getUUID());
            cue(p,"selection",null,null,List.of(u),0);
            ui(p,"book");snapshot(s,false,null);return;
        }
        if (best instanceof ServerPlayer target) { apply(s, LichRules.Order.CHASE, target.position(), target.getUUID()); return; }
        if (best instanceof LichObject c) { openCorpse(p, c); return; }
        var block = p.pick(128, 1, false);
        if (block instanceof BlockHitResult hit && block.getType() == HitResult.Type.BLOCK && p.level().getBlockState(hit.getBlockPos()).getBlock() instanceof ComputerBlock) {
            apply(s, LichRules.Order.GUARD, Vec3.atBottomCenterOf(hit.getBlockPos()), null); return;
        }
        if (p.getXRot() < -65) apply(s, LichRules.Order.RETURN, p.position(), null);
        else if (p.getXRot() > 65) apply(s, LichRules.Order.FOLLOW, p.position(), null);
        else if (block.getType() == HitResult.Type.BLOCK) apply(s, LichRules.Order.MOVE, block.getLocation(), null);
    }
    private static void apply(Army s, LichRules.Order order, Vec3 pos, UUID target) {
        var recipients=new ArrayList<LichUnit>();
        for (var id : s.selected) {
            var unit=s.units.get(id);
            if(unit!=null) {unit.command(order,pos,target);recipients.add(unit);}
        }
        if(!recipients.isEmpty()) {
            Vec3 anchor=switch(order) {case MOVE,GUARD,PATROL -> pos;default -> null;};
            var entity=target==null?null:s.level.getEntity(target);
            var pulses=switch(order) {case STAND,FOLLOW,RETURN,SEARCH -> recipients;default -> List.<LichUnit>of();};
            cue(s.player,order.name(),anchor,entity,pulses,0);
            ui(s.player,"command");
        }
        snapshot(s, false, null);
    }
    private static void selectionCue(Army s,Set<UUID> previous) {
        if(previous.equals(s.selected))return;
        var changed=s.units.values().stream()
                .filter(unit->previous.contains(unit.getUUID())!=s.selected.contains(unit.getUUID())).toList();
        cue(s.player,"selection",null,null,changed,0);
    }
    static void returned(LichUnit unit) {
        var s=owner(unit.owner);
        if(s!=null&&s.units.get(unit.getUUID())==unit)cue(s.player,"arrived",null,null,List.of(unit),0);
    }
    private static void cue(ServerPlayer player,String kind,Vec3 point,Entity target,List<LichUnit> units,int detail) {
        int[] ids=units.stream().filter(unit->!unit.isRemoved()).mapToInt(Entity::getId).toArray();
        for(int offset=0;offset<Math.max(1,ids.length);offset+=128) {
            var tag=new CompoundTag();
            tag.putString("mode","cue");tag.putString("kind",kind);
            tag.putString("dimension",player.level().dimension().location().toString());
            if(point!=null) {tag.putDouble("x",point.x);tag.putDouble("y",point.y);tag.putDouble("z",point.z);}
            if(target!=null)tag.putInt("target",target.getId());
            tag.putIntArray("units",Arrays.copyOfRange(ids,offset,Math.min(offset+128,ids.length)));
            tag.putInt("detail",detail);
            send(player,tag);
        }
    }
    private static void ui(ServerPlayer p, String sound) {
        var tag=new CompoundTag();tag.putString("mode","ui");tag.putString("sound",sound);send(p,tag);
        var stack=p.getMainHandItem();var state=owner(p.getUUID());
        if(sound.equals("book")&&stack.getItem() instanceof LichItem book&&book.role==LichItem.Role.BOOK&&(state==null||state.cast==null))
            book.triggerAnim(p,software.bernie.geckolib.animatable.GeoItem.getOrAssignId(stack,p.serverLevel()),"idle","page_turn");
    }
    private static void cancelCast(Army s) {
        if(s.cast==null)return;
        var cast=s.cast;clearCast(s);castFeedback(s,cast,"cancelled",null);snapshot(s,false,null);
    }
    private static void clearCast(Army s) {
        if (s.cast == null) return;
        if(s.cast.corpse!=null){RESERVATIONS.remove(s.cast.corpse,s.player.getUUID());var c=CORPSES.get(s.cast.corpse);if(c!=null)c.action("",0);}
        if(s.phylactery!=null)s.phylactery.action("",0);
        s.cast=null; itemCast(s,""); forms(s.player.server);
    }
    private static void castFeedback(Army s,Cast cast,String state,Entity entity) {
        var tag=new CompoundTag();tag.putString("mode","cast");tag.putString("state",state);
        tag.putString("action",cast.action);tag.putLong("serial",s.castSerial);
        long now=s.player.level().getGameTime();
        tag.putLong("start",state.equals("casting")?cast.finish-10:now);
        tag.putLong("finish",state.equals("casting")?cast.finish:now+(state.equals("emerge")?100:16));
        tag.putInt("kind",cast.kind==null?0:cast.kind.ordinal());
        tag.putInt("entity",entity instanceof LichUnit?entity.getId():-1);
        if(state.equals("casting")) {
            Entity focus=cast.action.equals("repair")?(s.phylactery==null?s.player:s.phylactery):CORPSES.get(cast.corpse);
            if(focus!=null)tag.putInt("focus",focus.getId());
        }
        send(s.player,tag);
    }
    private static void itemCast(Army s,String action) {
        for(int i=0;i<s.player.getInventory().getContainerSize();i++){
            var stack=s.player.getInventory().getItem(i);
            if(stack.is(ModItems.LICH_BOOK.get())||stack.is(ModItems.LICH_PHYLACTERY.get())){
                stack.getOrCreateTag().putString("LichCast",action);var item=(LichItem)stack.getItem();
                String clip=action.isEmpty()?"reset":item.role==LichItem.Role.BOOK?"cast_"+action:action.equals("repair")?"repair":"reset";
                item.triggerAnim(s.player,software.bernie.geckolib.animatable.GeoItem.getOrAssignId(stack,s.player.serverLevel()),"idle",clip);
            }
        }
    }
    static boolean clearSight(ServerPlayer p, Vec3 target) {
        // Vanilla LivingEntity.hasLineOfSight silently caps range at 128; book selection must not.
        Vec3 start = p.getEyePosition(); int steps = Math.max(1,(int)Math.ceil(start.distanceTo(target)/8));
        for(int i=0;i<=steps;i++) if(!p.level().hasChunkAt(BlockPos.containing(start.lerp(target,(double)i/steps)))) return false;
        return p.level().clip(new ClipContext(start,target,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getType()==HitResult.Type.MISS;
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void adventurePhylactery(PlayerInteractEvent.RightClickBlock event) {
        var player=event.getEntity();
        // ItemStack.useOn rejects adventure items before LichItem.useOn can run.
        // This places an owned entity, so no block placement permission is needed.
        if(event.isCanceled()||event.getUseItem()==Event.Result.DENY||player.getAbilities().mayBuild||player.isSpectator()
                ||event.getHand()!=net.minecraft.world.InteractionHand.MAIN_HAND
                ||!event.getItemStack().is(ModItems.LICH_PHYLACTERY.get()))return;
        if(player instanceof ServerPlayer serverPlayer) {
            if(!active(serverPlayer))return;
            place(serverPlayer,event.getPos(),event.getFace());
        }
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.sidedSuccess(player.level().isClientSide));
    }
    public static void place(ServerPlayer p, BlockPos clicked, Direction face) {
        var s = owner(p.getUUID()); if (!active(p) || s == null || !carried(s) || face != Direction.UP || p.distanceToSqr(Vec3.atCenterOf(clicked)) > 36) return;
        var pos = clicked.above(); if (!safe(p.serverLevel(), pos, .6, .8)) return;
        var object = ModEntities.LICH_OBJECT.get().create(p.serverLevel()); if (object == null) return;
        object.configure(p.getUUID(), currentMap(p.server), true); object.setPos(Vec3.atBottomCenterOf(pos).add(0,.002,0)); s.phylactery = object;
        if (!p.serverLevel().addFreshEntity(object)) { s.phylactery = null; return; }
        stripPhylactery(p);
        cue(p,"place",object.position(),null,List.of(),0);
    }
    public static void pickup(ServerPlayer p, LichObject object) {
        var s = owner(p.getUUID()); if (!active(p) || s == null || s.phylactery != object || p.distanceToSqr(object) > 9 || !p.hasLineOfSight(object)) return;
        stripPhylactery(p);
        if (!p.getInventory().add(new ItemStack(ModItems.LICH_PHYLACTERY.get()))) return;
        s.phylacteryIssued=true; s.phylactery = null; object.discard();
        cue(p,"pickup",object.position(),null,List.of(),0);
    }
    public static void hitPhylactery(UUID id) {
        var s = owner(id); if (s == null || s.hp <= 0 || ended || !active(s.player)) return;
        if(s.player.hasEffect(ModEffects.LICH_PHYLACTERY_GUARD.get()))return;
        s.player.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModEffects.LICH_PHYLACTERY_GUARD.get(),
                LichRules.hitCooldown(GameManager.getPhaseValue()),0,false,false,true));
        s.hp--; cancelCast(s);
        var location = s.phylactery == null ? s.player : s.phylactery;
        s.level.sendParticles(org.example.maniacrevolution.effect.LichPhylacteryGuardEffect.HIT_PARTICLE,
                location.getX(),location.getY()+1,location.getZ(),35,.45,.55,.45,.03);
        LichSounds.play(location, s.hp == 0 ? "phylactery_destroy" : s.phylactery == null ? "phylactery_carried_hit" : "phylactery_hit", 1);
        if(s.phylactery!=null)s.phylactery.action("hurt",8);
        if (s.hp == 0) {
            ended = true;
            s.player.server.getPlayerList().broadcastSystemMessage(Component.literal("§bФилактерия уничтожена! Победа выживших."), false);
            // User-confirmed datapack contract: killing the Lich triggers the existing victory rules.
            s.player.kill();
            cleanup(s.player.server);
        }
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true) public static void attack(LivingAttackEvent e) {
        if (friendly(e.getSource().getDirectEntity(), e.getEntity()) || friendly(e.getSource().getEntity(), e.getEntity())) { e.setCanceled(true); return; }
        if (e.getEntity() instanceof ServerPlayer p) {
            var s = owner(p.getUUID()); if (s == null || !active(p)) return;
            cancelCast(s);
            if (e.getAmount() > 0 && carried(s) && e.getSource().getEntity() instanceof ServerPlayer attacker && survivor(attacker)) hitPhylactery(p.getUUID());
        }
    }
    /** AttackEntityEvent is raised even when vanilla hurt rejects an invulnerable player. */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true) public static void carriedMelee(AttackEntityEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer attacker) || !survivor(attacker) || !(e.getTarget() instanceof ServerPlayer p)
                || !active(p) || attacker.distanceToSqr(p) > 16 || !attacker.hasLineOfSight(p)) return;
        var s = owner(p.getUUID()); if (s == null) return; cancelCast(s);
        if (carried(s)) hitPhylactery(p.getUUID());
    }
    @SubscribeEvent public static void projectile(net.minecraftforge.event.entity.ProjectileImpactEvent e) {
        if (!(e.getRayTraceResult() instanceof EntityHitResult hit)) return;
        var projectile = e.getProjectile();
        if (projectile.getPersistentData().getBoolean("lich_arrow")) {
            e.setImpactResult(net.minecraftforge.event.entity.ProjectileImpactEvent.ImpactResult.STOP_AT_CURRENT_NO_DAMAGE);
            if (hit.getEntity() instanceof ServerPlayer target && survivor(target) && projectile instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow)
                target.hurt(projectile.damageSources().arrow(arrow, arrow.getOwner()),4);
            projectile.discard(); return;
        }
        if (hit.getEntity() instanceof ServerPlayer p && active(p) && projectile.getOwner() instanceof ServerPlayer attacker && survivor(attacker)) {
            var s = owner(p.getUUID()); if (s != null) { cancelCast(s); if (carried(s)) hitPhylactery(p.getUUID()); }
        }
    }
    @SubscribeEvent public static void toss(ItemTossEvent e) {
        if (!(e.getPlayer() instanceof ServerPlayer p) || !active(p) || !e.getEntity().getItem().is(ModItems.LICH_PHYLACTERY.get())) return;
        e.setCanceled(true);
        var s=owner(p.getUUID());
        if(s!=null&&carried(s)&&!p.getInventory().add(e.getEntity().getItem()))s.phylacteryIssued=false;
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void hurt(LivingHurtEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && active(p)) {
            var s = owner(p.getUUID()); if (s != null && s.hp > 0) e.setAmount(e.getAmount() * .5F);
        }
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void melee(AttackEntityEvent e) {
        if (e.isCanceled() || !(e.getEntity() instanceof ServerPlayer p) || !active(p)) return;
        if(!LichCombat.authorized(p))e.setCanceled(true);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        var s = ARMIES.remove(p.getUUID()); if (s != null) clear(s); EDIT_MAP.remove(p.getUUID());
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent e) {
        ARMIES.clear(); CORPSES.clear(); EDIT_MAP.clear(); SOULS.clear(); RESERVATIONS.clear(); CLEARED_UNITS.clear(); CLEARED_CORPSES.clear(); running = ended = false; delayedUntil = 0;
        guaranteedPaused=false;session=UUID.randomUUID();
    }
    private static void send(ServerPlayer p, CompoundTag tag) { ModNetworking.sendToPlayer(new LichSnapshot(tag), p); }
    static int[] selectedOrders(Army s) {
        int[] orders=new int[LichRules.Order.values().length];
        for(var id:s.selected) {var unit=s.units.get(id);if(unit!=null)orders[unit.order.ordinal()]++;}
        return orders;
    }
    private static void snapshot(Army s, boolean open, UUID corpse) {
        var tag = new CompoundTag(); tag.putString("mode", "army"); tag.putBoolean("open", open); tag.putInt("hp", s.hp);
        tag.putBoolean("placed", s.phylactery != null); tag.putInt("cast", s.cast == null ? 0 : (int)Math.max(0, s.cast.finish-s.player.level().getGameTime()));
        tag.putInt("selected", s.selected.size());
        tag.putIntArray("orders",selectedOrders(s));
        if (corpse != null) tag.putUUID("corpse", corpse);
        tag.putInt("total", s.units.size()); int pages = Math.max(1, (s.units.size()+11)/12); s.page = Math.min(s.page, pages-1);
        tag.putInt("page", s.page); tag.putInt("pages", pages);
        var rows = new ListTag(); int index = 0;
        for (var u : s.units.values()) {
            if (index++ < s.page*12 || index > (s.page+1)*12) continue;
            var row = new CompoundTag(); row.putUUID("id", u.getUUID()); row.putString("name", u.kind().title);
            row.putInt("entityId",u.getId());
            row.putFloat("hp", u.getHealth()); row.putInt("distance", (int)u.position().distanceTo(s.player.position())); row.putString("order", u.order.title);
            row.putBoolean("selected", s.selected.contains(u.getUUID())); row.putBoolean("attack", u.aggressive); row.putBoolean("detect", u.detecting);
            row.putBoolean("hurt", u.attackedUntil > u.level().getGameTime()); row.putBoolean("unloaded", unloaded(u)); row.putInt("kind", u.kind().ordinal()); rows.add(row);
        }
        tag.put("rows", rows); send(s.player, tag);
    }
    private static void forms(MinecraftServer server) {
        var tag = new CompoundTag(); tag.putString("mode", "forms"); var rows = new ListTag();
        for (var s : ARMIES.values()) if (active(s.player)) {
            var row = new CompoundTag(); row.putUUID("id",s.player.getUUID()); row.putString("cast",s.cast == null ? "" : s.cast.action);
            row.putLong("finish",s.cast == null ? 0 : s.cast.finish); rows.add(row);
        }
        tag.put("rows",rows); for(var p:server.getPlayerList().getPlayers())send(p,tag);
    }
    public static void editor(ServerPlayer p, boolean open) {
        if (!p.hasPermissions(2)) return;
        var tag = new CompoundTag(); tag.putString("mode", "editor"); tag.putBoolean("open", open); tag.putString("map", selectedMap(p));
        var rows = new ListTag(); for (var map : MapRegistry.getAllMaps()) {
            var row = new CompoundTag(); row.putString("id", map.getId()); row.putString("name", map.getName());
            row.putInt("points", (int)CorpsePoints.get(p.server).points.stream().filter(x -> x.map().equals(map.getId())).count()); rows.add(row);
        }
        tag.put("rows", rows); send(p, tag);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void editorEntityInteraction(PlayerInteractEvent event) {
        if (!(event instanceof PlayerInteractEvent.EntityInteract || event instanceof PlayerInteractEvent.EntityInteractSpecific)
                || !(event.getEntity() instanceof ServerPlayer p) || event.getHand()!=net.minecraft.world.InteractionHand.MAIN_HAND
                || !p.hasPermissions(2) || !p.isShiftKeyDown() || !p.getMainHandItem().is(ModItems.LICH_CORPSE_EDITOR.get())) return;
        editor(p,true);event.setCanceled(true);event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
    }
    private static String selectedMap(ServerPlayer p) {
        String map = p.getMainHandItem().is(ModItems.LICH_CORPSE_EDITOR.get())&&p.getMainHandItem().hasTag()?p.getMainHandItem().getTag().getString("LichMap"):"";
        return MapRegistry.getMapById(map) == null ? EDIT_MAP.getOrDefault(p.getUUID(), currentMap(p.server)) : map;
    }
    public static void editPoint(ServerPlayer p, BlockPos pos) {
        if (!p.hasPermissions(2) || !p.getMainHandItem().is(ModItems.LICH_CORPSE_EDITOR.get())) return;
        String map = selectedMap(p); if (MapRegistry.getMapById(map) == null) { editor(p, true); return; }
        if (!safe(p.serverLevel(), pos, .8,.5)) { p.sendSystemMessage(Component.literal("§cНужна свободная точка на твёрдой поверхности.")); return; }
        boolean added=CorpsePoints.get(p.server).toggle(map, p.level().dimension().location().toString(), pos);
        cue(p,added?"addPoint":"removePoint",Vec3.atBottomCenterOf(pos),null,List.of(),0);
        p.sendSystemMessage(Component.literal("§dТочка трупа: " + map + " " + pos.toShortString() + " (повторный ПКМ удаляет)"));
    }
    private static void highlightPoints(ServerPlayer p) {
        var map=selectedMap(p);
        var points=CorpsePoints.get(p.server).points.stream().filter(point->point.map().equals(map)&&point.dimension().equals(p.level().dimension().location().toString())).toList();
        for(int start=0;start<Math.max(1,points.size());start+=128){
            var tag=new CompoundTag();tag.putString("mode","points");tag.putBoolean("first",start==0);tag.putString("dimension",p.level().dimension().location().toString());
            tag.putBoolean("validMap",MapRegistry.getMapById(map)!=null);
            tag.putLongArray("positions",points.subList(start,Math.min(points.size(),start+128)).stream().mapToLong(x->x.pos().asLong()).toArray());send(p,tag);
        }
    }
    public static void corpseList(ServerPlayer p, String map, int page) {
        if (!p.hasPermissions(2)) return;
        var tag = new CompoundTag(); tag.putString("mode", "corpses"); tag.putBoolean("open", true); tag.putString("map", map);
        var authored = CorpsePoints.get(p.server).points.stream().filter(x -> x.map().equals(map)).toList();
        var live = CORPSES.values().stream().filter(c -> c.map.equals(map)).toList();
        int total = authored.size()+live.size(), pages = Math.max(1,(total+11)/12); page = Math.max(0, Math.min(page,pages-1));
        tag.putInt("page", page); tag.putInt("pages", pages); var rows = new ListTag();
        for (int i=page*12; i<Math.min(total,(page+1)*12); i++) {
            var row = new CompoundTag();
            if (i < authored.size()) { var point = authored.get(i); row.putUUID("id", point.id()); row.putString("text", "Точка " + point.pos().toShortString()+" "+point.dimension()); row.putBoolean("point", true); }
            else { var c = live.get(i-authored.size()); row.putUUID("id", c.getUUID()); row.putString("text", "Труп " + c.blockPosition().toShortString()+" "+c.level().dimension().location()); }
            rows.add(row);
        }
        tag.put("rows", rows); send(p, tag);
    }
}
