package org.example.maniacrevolution.techies;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.downed.*;
import org.example.maniacrevolution.game.GameManager;
import org.example.maniacrevolution.hack.HackManager;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.util.ManaUtil;
import org.example.maniacrevolution.util.PlayerModeUtil;

/** Match-only server records: no hidden entities, forced chunks or destructive explosions. */
@Mod.EventBusSubscriber(modid=Maniacrev.MODID)
public final class TechiesManager {
    public static final class Mine {
        final int id; final UUID owner; final ResourceLocation dimension; final BlockPos support;
        final Vec3 center; final MineType type;
        boolean placed; long started=-1; int version;
        Mine(int id, ServerPlayer p, BlockPos support, MineType type) {
            this.id=id; owner=p.getUUID(); dimension=p.level().dimension().location(); this.support=support.immutable();
            center=center(support); this.type=type;
        }
    }
    private record Cell(ResourceLocation dimension,int x,int y,int z) {}
    private static final class Placement {
        final Mine mine; final Vec3 origin; final int slot; long start=-1;
        Placement(Mine mine, ServerPlayer p) { this.mine=mine; origin=p.position(); slot=p.getInventory().selected; }
    }
    private static final Map<Integer,Mine> MINES=new HashMap<>();
    private static final Map<Cell,Set<Integer>> INDEX=new HashMap<>();
    private static final Map<UUID,Set<Integer>> GREENS=new HashMap<>();
    private static final Set<Integer> ACTIVE=new HashSet<>();
    private static final Map<UUID,Placement> PLACING=new HashMap<>();
    private static final Map<UUID,MineType> PREVIEW=new HashMap<>();
    private static final Map<UUID,Integer> PREVIEW_SLOT=new HashMap<>();
    private static final Map<UUID,long[]> COOLDOWNS=new HashMap<>();
    private static final Map<UUID,Map<Integer,Integer>> KNOWN=new HashMap<>();
    private static final Map<UUID,TechiesStatePacket> HUD=new HashMap<>();
    private static final Set<UUID> EQUIPPED=new HashSet<>();
    private record Visibility(ResourceLocation dimension,String team,boolean mode,Cell cell,int target,long revision,long interval) {}
    private record Aim(Vec3 eye,Vec3 look,ResourceLocation dimension,long revision,int target) {}
    private static final Map<UUID,Visibility> VISIBILITY=new HashMap<>();
    private static final Map<UUID,Aim> AIMS=new HashMap<>();
    private static final Map<UUID,TechiesPosePacket> POSES=new HashMap<>();
    private static final Map<UUID,Long> PRESSES=new HashMap<>();
    private record OfflineMana(float mana,float maximum,boolean changed) {}
    private static final Map<UUID,OfflineMana> OFFLINE_MANA=new HashMap<>();
    private static final Map<UUID,Integer> OWNER_COUNTS=new HashMap<>();
    private static long revision;
    private static int nextId=1;
    private static long now;
    private TechiesManager() {}
    private static Cell cell(ResourceLocation d, Vec3 v) {
        return new Cell(d,Math.floorDiv((int)Math.floor(v.x),16),Math.floorDiv((int)Math.floor(v.y),16),Math.floorDiv((int)Math.floor(v.z),16));
    }
    public static Vec3 center(BlockPos p) { return new Vec3(p.getX()+.5,p.getY()+1.002,p.getZ()+.5); }
    private static List<Mine> nearby(ResourceLocation d, Vec3 v, int cells) {
        List<Mine> result=new ArrayList<>(); Cell c=cell(d,v);
        for(int x=c.x-cells;x<=c.x+cells;x++) for(int y=c.y-cells;y<=c.y+cells;y++) for(int z=c.z-cells;z<=c.z+cells;z++)
            for(int id:INDEX.getOrDefault(new Cell(d,x,y,z),Set.of())) { Mine m=MINES.get(id); if(m!=null) result.add(m); }
        return result;
    }
    private static void add(Mine m) {
        MINES.put(m.id,m); INDEX.computeIfAbsent(cell(m.dimension,m.center),k->new HashSet<>()).add(m.id);
        OWNER_COUNTS.merge(m.owner,1,Integer::sum); revision++;
    }
    private static void remove(Mine m) {
        revision++;
        if(MINES.remove(m.id)!=null) OWNER_COUNTS.computeIfPresent(m.owner,(owner,count)->count<=1?null:count-1);
        ACTIVE.remove(m.id);
        Cell key=cell(m.dimension,m.center); Set<Integer> set=INDEX.get(key);
        if(set!=null) { set.remove(m.id); if(set.isEmpty()) INDEX.remove(key); }
        Set<Integer> green=GREENS.get(m.owner); if(green!=null) { green.remove(m.id); if(green.isEmpty()) GREENS.remove(m.owner); }
    }
    public static boolean active(ServerPlayer p) {
        var o=p.getScoreboard().getObjective("SurvivorClass");
        int id=o==null?-1:p.getScoreboard().getOrCreatePlayerScore(p.getScoreboardName(),o).getScore();
        return TechiesRules.active(p.isAlive(),PlayerModeUtil.isSurvivalOrAdventure(p),GameManager.getPhaseValue(),id,
                p.getTeam()==null?"":p.getTeam().getName());
    }
    private static boolean downed(ServerPlayer p) { var d=DownedCapability.get(p); return d!=null && d.getState()==DownedState.DOWNED; }
    private static boolean maniac(ServerPlayer p) { return p.getTeam()!=null && "maniac".equals(p.getTeam().getName()); }
    private static boolean usable(ServerPlayer p) { return p.isAlive() && PlayerModeUtil.isSurvivalOrAdventure(p) && !downed(p); }
    public static boolean placing(ServerPlayer p) { return PLACING.containsKey(p.getUUID()); }
    private static boolean busy(ServerPlayer p) {
        if(HackManager.isInitialized() && HackManager.get().isParticipatingInActiveHack(p)) return true;
        if(HackManager.isInitialized()) for(var s:HackManager.get().getActiveSessions().values())
            if(s.getAllParticipants().contains(p)) return true;
        for(var other:p.server.getPlayerList().getPlayers()) {
            var d=DownedCapability.get(other); if(d!=null && p.getUUID().equals(d.getReviverUUID())) return true;
        }
        return false;
    }
    public static boolean validSupport(ServerLevel level, BlockPos support) {
        return level.hasChunkAt(support) && level.getWorldBorder().isWithinBounds(support)
                && level.getBlockState(support).isFaceSturdy(level,support,Direction.UP)
                && level.getBlockState(support.above()).getCollisionShape(level,support.above()).isEmpty();
    }
    private static boolean space(ResourceLocation d, BlockPos pos, int except) {
        Vec3 c=center(pos);
        for(Mine m:nearby(d,c,1)) if(m.id!=except && !TechiesRules.spaced(m.center.distanceToSqr(c))) return false;
        return true;
    }
    private static BlockHitResult ray(ServerPlayer p) {
        Vec3 eye=p.getEyePosition(); return p.serverLevel().clip(new ClipContext(eye,eye.add(p.getLookAngle().scale(4)),
                ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
    }
    private static void message(ServerPlayer p,String text) { p.displayClientMessage(net.minecraft.network.chat.Component.literal(text),true); }
    public static void action(ServerPlayer p,int action,int value) {
        now=p.server.overworld().getGameTime();
        if(action==TechiesActionPacket.HIT) { hit(p,value); return; }
        if(action==TechiesActionPacket.CANCEL) { if(!placing(p)) cancel(p); return; }
        if(org.example.maniacrevolution.ghost.GhostPossessionManager.isPossessed(p)) return;
        if(!active(p)) return;
        if(action==TechiesActionPacket.REMOTE) {
            if(placing(p) || !holdingRemote(p)) return;
            if(org.example.maniacrevolution.event.PenaltySlotManager.isInPenaltySlot(p)) { message(p,"Пульт недоступен в штрафном слоте"); return; }
            Mine m=target(p);
            if(m==null) { message(p,"Нет доступной зелёной мины"); return; }
            if(m.id!=value) { message(p,"Цель обновилась — нажмите пульт снова"); return; }
            m.started=now; m.version++; revision++; ACTIVE.add(m.id); PRESSES.put(p.getUUID(),now); syncPose(p); return;
        }
        if(downed(p)) { message(p,"В downed доступен пульт; установка запрещена"); return; }
        if(busy(p)) { message(p,"Сначала завершите взлом/QTE или поднятие союзника"); return; }
        if(placing(p) || p.containerMenu!=p.inventoryMenu) return;
        if(action==TechiesActionPacket.SELECT) {
            if(value<0 || value>=MineType.values().length) return;
            PREVIEW.put(p.getUUID(),MineType.values()[value]); PREVIEW_SLOT.put(p.getUUID(),p.getInventory().selected);
        } else if(action==TechiesActionPacket.PLACE) {
            if(org.example.maniacrevolution.event.PenaltySlotManager.isInPenaltySlot(p)) { message(p,"Установка недоступна в штрафном слоте"); return; }
            MineType type=PREVIEW.get(p.getUUID()); if(type==null || PREVIEW_SLOT.getOrDefault(p.getUUID(),-1)!=p.getInventory().selected) return;
            var hit=ray(p); if(hit.getType()!=HitResult.Type.BLOCK || hit.getDirection()!=Direction.UP) return;
            BlockPos support=hit.getBlockPos();
            if(!validSupport(p.serverLevel(),support) || p.getEyePosition().distanceToSqr(center(support))>16
                    || !space(p.level().dimension().location(),support,-1)) { message(p,"Недопустимая позиция мины"); return; }
            long[] cd=COOLDOWNS.computeIfAbsent(p.getUUID(),k->new long[3]);
            if(cd[type.ordinal()]>now || !ManaUtil.consumeMana(p,type.cost)) return;
            cd[type.ordinal()]=now+type.cooldown;
            Mine mine=new Mine(nextId++,p,support,type); add(mine);
            Placement placement=new Placement(mine,p); placement.start=now; PLACING.put(p.getUUID(),placement);
        }
        syncHud(p);
    }
    public static boolean holdingRemote(ServerPlayer p) { return p.getMainHandItem().is(ModItems.TECHIES_REMOTE.get()) || p.getOffhandItem().is(ModItems.TECHIES_REMOTE.get()); }
    private static Mine target(ServerPlayer p) {
        Aim prior=AIMS.get(p.getUUID());
        if(prior!=null && prior.revision==revision && prior.dimension.equals(p.level().dimension().location())
                && prior.eye.equals(p.getEyePosition()) && prior.look.equals(p.getLookAngle())) return MINES.get(prior.target);
        Mine best=null; double score=-Double.MAX_VALUE;
        for(int id:GREENS.getOrDefault(p.getUUID(),Set.of())) {
            Mine m=MINES.get(id); if(m==null || m.started>=0 || !m.dimension.equals(p.level().dimension().location())) continue;
            Vec3 delta=m.center.subtract(p.getEyePosition()); double dot=delta.lengthSqr()<1e-12?1:p.getLookAngle().dot(delta.normalize());
            if(dot>score || (dot==score && (best==null || m.id<best.id))) { score=dot; best=m; }
        }
        AIMS.put(p.getUUID(),new Aim(p.getEyePosition(),p.getLookAngle(),p.level().dimension().location(),revision,best==null?0:best.id));
        return best;
    }
    private static boolean line(ServerPlayer p, Vec3 from, Vec3 to) {
        return p.serverLevel().clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getType()==HitResult.Type.MISS;
    }
    private static boolean affected(Mine m,ServerPlayer p) {
        if(!usable(p) || !m.dimension.equals(p.level().dimension().location())) return false;
        Vec3 nearest=new Vec3(Math.max(p.getBoundingBox().minX,Math.min(m.center.x,p.getBoundingBox().maxX)),
                Math.max(p.getBoundingBox().minY,Math.min(m.center.y,p.getBoundingBox().maxY)),
                Math.max(p.getBoundingBox().minZ,Math.min(m.center.z,p.getBoundingBox().maxZ)));
        if(nearest.distanceToSqr(m.center)>4) return false;
        if(line(p,m.center,nearest) || visiblePoint(m,p,nearest,p.getEyePosition())) return true;
        // Partial cover may hide the closest point while leaving another part inside the sphere exposed.
        // Every sampled segment is clipped to the sphere AND remains inside the convex player AABB.
        AABB box=p.getBoundingBox();
        for(int x=0;x<3;x++) for(int y=0;y<3;y++) for(int z=0;z<3;z++) {
            Vec3 point=new Vec3(x==0?box.minX:x==1?(box.minX+box.maxX)/2:box.maxX,
                    y==0?box.minY:y==1?(box.minY+box.maxY)/2:box.maxY,
                    z==0?box.minZ:z==1?(box.minZ+box.maxZ)/2:box.maxZ);
            if(visiblePoint(m,p,nearest,point)) return true;
        }
        return false;
    }
    private static boolean visiblePoint(Mine m,ServerPlayer p,Vec3 nearest,Vec3 point) {
        Vec3 direction=point.subtract(nearest);
        if(direction.lengthSqr()<1e-12) return false;
        if(point.distanceToSqr(m.center)>4) {
            Vec3 origin=nearest.subtract(m.center);
            double a=direction.lengthSqr(),b=2*origin.dot(direction),c=origin.lengthSqr()-4;
            double t=(-b+Math.sqrt(Math.max(0,b*b-4*a*c)))/(2*a);
            point=nearest.add(direction.scale(Math.max(0,Math.min(1,t))));
        }
        return line(p,m.center,point);
    }
    private static boolean triggered(Mine m,List<ServerPlayer> players) {
        for(ServerPlayer p:players) if(maniac(p) && affected(m,p)) return true;
        return false;
    }
    private static void hit(ServerPlayer p,int id) {
        Mine m=MINES.get(id);
        if(m==null || !m.placed || m.type==MineType.GREEN || !usable(p) || !maniac(p)
                || !m.dimension.equals(p.level().dimension().location()) || !TechiesRules.reversible(now-m.started) || m.started<0
                || org.example.maniacrevolution.ghost.GhostPossessionManager.isPossessed(p)
                || org.example.maniacrevolution.scp173.Scp173Manager.held(p)) return;
        double reach=p.getAttributeValue(ForgeMod.ENTITY_REACH.get());
        // Class attacks retain their agreed reach. A legal weak vanilla swing still destroys a mine.
        if(org.example.maniacrevolution.scp173.Scp173Manager.active(p)) reach=Math.min(reach,1.5);
        if(org.example.maniacrevolution.warden.WardenCombatManager.active(p)) reach=Math.min(reach,3);
        Vec3 eye=p.getEyePosition(); var contact=new AABB(m.center.add(-.45,0,-.45),m.center.add(.45,.8,.45))
                .clip(eye,eye.add(p.getLookAngle().scale(reach)));
        if(contact.isEmpty() || !line(p,eye,contact.get())
                || org.example.maniacrevolution.event.PenaltySlotManager.isInPenaltySlot(p) || p.containerMenu!=p.inventoryMenu) return;
        // Reject occluded hits and preserve each special class melee cooldown instead of bypassing its manager.
        for(var other:p.serverLevel().getEntities(p,new AABB(eye,contact.get()).inflate(.1),e->e.isPickable())) {
            var obstruction=other.getBoundingBox().clip(eye,contact.get());
            if(obstruction.isPresent() && eye.distanceToSqr(obstruction.get())<eye.distanceToSqr(contact.get())) return;
        }
        if(org.example.maniacrevolution.scp173.Scp173Manager.active(p)
                && !org.example.maniacrevolution.scp173.Scp173GameplayManager.tryMineAttack(p)) return;
        if(org.example.maniacrevolution.warden.WardenCombatManager.active(p)
                && !org.example.maniacrevolution.warden.WardenCombatManager.tryMineAttack(p)) return;
        p.resetAttackStrengthTicker(); remove(m);
    }
    static void explode(Mine m,net.minecraft.server.MinecraftServer server,List<ServerPlayer> players) {
        float stolen=0;
        for(ServerPlayer p:players) {
            if(!affected(m,p)) continue;
            if(m.type==MineType.BLUE) {
                if(!p.getUUID().equals(m.owner)) {
                    float amount=TechiesRules.drain(ManaUtil.getMana(p));
                    if(amount>0 && ManaUtil.consumeMana(p,amount)) stolen+=amount;
                }
                if(!p.isInvulnerable()) {
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,m.type.slowTicks,m.type.slowLevel));
                    push(m,p);
                }
            } else {
                // Let actual Minecraft protection decide; a team/phase alone is not invulnerability.
                var damage=new net.minecraft.world.damagesource.DamageSource(p.level().registryAccess()
                        .registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(net.minecraft.resources.ResourceKey.create(
                                net.minecraft.core.registries.Registries.DAMAGE_TYPE,Maniacrev.loc("techies_mine"))));
                if(!p.isInvulnerableTo(damage)) {
                    boolean damaged=p.hurt(damage,m.type.damage);
                    if(usable(p)) {
                        if(damaged) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,m.type.slowTicks,m.type.slowLevel));
                        // The mine impulse is independent of ordinary damage immunity frames.
                        push(m,p);
                    }
                }
            }
        }
        ServerPlayer owner=players.stream().filter(p->p.getUUID().equals(m.owner)).findFirst().orElseGet(()->server.getPlayerList().getPlayer(m.owner));
        if(stolen>0 && owner!=null) ManaUtil.addMana(owner,stolen);
        else if(stolen>0) {
            var snapshot=OFFLINE_MANA.get(m.owner);
            if(snapshot!=null) OFFLINE_MANA.put(m.owner,new OfflineMana(Math.min(snapshot.maximum,snapshot.mana+stolen),snapshot.maximum,true));
        }
        ServerLevel level=server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,m.dimension));
        if(level!=null) {
            level.sendParticles(m.type==MineType.BLUE?ParticleTypes.ELECTRIC_SPARK:ParticleTypes.EXPLOSION,m.center.x,m.center.y,m.center.z,8,.4,.3,.4,.03);
            level.playSound(null,m.center.x,m.center.y,m.center.z,TechiesSounds.explosion(m.type),SoundSource.PLAYERS,.85f,1);
        }
        remove(m);
    }
    private static void push(Mine m,ServerPlayer p) {
        Vec3 away=p.position().subtract(m.center);
        if(away.horizontalDistanceSqr()<1e-8) away=p.getLookAngle().scale(-1);
        p.knockback(m.type.push,-away.x,-away.z);
        p.hurtMarked=true;
        // Player motion is client-simulated: deliver the impulse to its owner immediately.
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(p));
    }
    private static void cancel(ServerPlayer p) {
        Placement placement=PLACING.remove(p.getUUID()); if(placement!=null) remove(placement.mine);
        PREVIEW.remove(p.getUUID()); PREVIEW_SLOT.remove(p.getUUID()); syncHud(p); syncPose(p);
    }
    public static int clear(net.minecraft.server.MinecraftServer server) {
        int count=(int)MINES.values().stream().filter(m->m.placed).count();
        MINES.clear(); INDEX.clear(); GREENS.clear(); ACTIVE.clear(); PLACING.clear(); PREVIEW.clear(); PREVIEW_SLOT.clear(); OWNER_COUNTS.clear();
        // Undelivered, already-earned mana is player-resource bookkeeping, not a live mine/session.
        OFFLINE_MANA.entrySet().removeIf(entry->!entry.getValue().changed);
        KNOWN.clear(); HUD.clear(); VISIBILITY.clear(); AIMS.clear(); POSES.clear(); PRESSES.clear(); revision++;
        if(server!=null) for(var p:server.getPlayerList().getPlayers()) { send(p,0,0,0,BlockPos.ZERO,-1,0,0,0,0); syncHud(p); syncPose(p); }
        return count;
    }
    public static void resetMatch(net.minecraft.server.MinecraftServer server) {
        COOLDOWNS.clear(); EQUIPPED.clear(); OFFLINE_MANA.clear(); clear(server);
    }
    private static void send(ServerPlayer p,int op,int id,int type,BlockPos pos,long start,int selected,long r,long g,long b) {
        ModNetworking.sendToPlayer(new TechiesStatePacket(op,p.level().dimension().location(),now,id,type,pos,start,selected,r,g,b),p);
    }
    private static void syncHud(ServerPlayer p) {
        Placement s=PLACING.get(p.getUUID()); MineType type=PREVIEW.get(p.getUUID());
        Mine t=active(p) && holdingRemote(p)?target(p):null;
        long[] cd=COOLDOWNS.getOrDefault(p.getUUID(),new long[3]);
        // Compare payload without ticking timestamp: send only changes; client advances against its accepted server clock.
        var packet=new TechiesStatePacket(3,p.level().dimension().location(),0,t==null?0:t.id,
                type==null?-1:type.ordinal(),s==null?BlockPos.ZERO:s.mine.support,s==null?-1:s.start,
                s==null?PREVIEW_SLOT.getOrDefault(p.getUUID(),-1):s.slot,cd[0],cd[1],cd[2]);
        if(!packet.equals(HUD.put(p.getUUID(),packet))) send(p,3,packet.id(),packet.type(),packet.pos(),packet.started(),packet.selected(),cd[0],cd[1],cd[2]);
    }
    private static void syncPose(ServerPlayer p) {
        var type=PREVIEW.get(p.getUUID()); var s=PLACING.get(p.getUUID());
        var packet=new TechiesPosePacket(p.getUUID(),0,type==null?-1:type.ordinal(),s==null?-1:s.start,PRESSES.getOrDefault(p.getUUID(),-100L));
        if(!packet.equals(POSES.put(p.getUUID(),packet))) ModNetworking.sendToNearby(
                new TechiesPosePacket(p.getUUID(),now,packet.type(),packet.start(),packet.press()),p,64);
    }
    @SubscribeEvent public static void tracking(PlayerEvent.StartTracking e) {
        if(e.getEntity() instanceof ServerPlayer viewer && e.getTarget() instanceof ServerPlayer owner) {
            var pose=POSES.get(owner.getUUID()); if(pose!=null) ModNetworking.sendToPlayer(
                    new TechiesPosePacket(owner.getUUID(),now,pose.type(),pose.start(),pose.press()),viewer);
        }
    }
    private static void visibility(ServerPlayer p) {
        Mine chosen=active(p)&&holdingRemote(p)?target(p):null;
        Visibility signature=new Visibility(p.level().dimension().location(),p.getTeam()==null?"":p.getTeam().getName(),
                PlayerModeUtil.isSurvivalOrAdventure(p),cell(p.level().dimension().location(),p.position()),chosen==null?0:chosen.id,revision,now/10);
        if(signature.equals(VISIBILITY.put(p.getUUID(),signature))) return;
        Map<Integer,Integer> known=KNOWN.computeIfAbsent(p.getUUID(),k->new HashMap<>());
        Set<Integer> wanted=new HashSet<>();
        if(PlayerModeUtil.isSurvivalOrAdventure(p) && p.getTeam()!=null) {
            List<Mine> candidates=nearby(p.level().dimension().location(),p.position(),4);
            Mine target=chosen; if(target!=null) candidates.add(target);
            for(Mine m:candidates) if((m.placed && m.started>=0 || "survivors".equals(p.getTeam().getName()))
                    && (m.center.distanceToSqr(p.position())<=4096 || m==target)) {
                wanted.add(m.id);
                if(!Objects.equals(known.get(m.id),m.version)) { send(p,m.placed?1:4,m.id,m.type.ordinal(),m.support,m.started,0,0,0,0); known.put(m.id,m.version); }
            }
        }
        for(int id:new HashSet<>(known.keySet())) if(!wanted.contains(id)) { send(p,2,id,0,BlockPos.ZERO,-1,0,0,0,0); known.remove(id); }
    }
    private static void loadout(ServerPlayer p) {
        if(!active(p)) { EQUIPPED.remove(p.getUUID()); return; }
        if(EQUIPPED.contains(p.getUUID())) return;
        boolean hasRemote=false;
        for(int slot=0;slot<p.getInventory().getContainerSize();slot++)
            if(p.getInventory().getItem(slot).is(ModItems.TECHIES_REMOTE.get())) { hasRemote=true; break; }
        if(!hasRemote) {
            if(p.getInventory().getFreeSlot()<0) return;
            p.getInventory().add(new ItemStack(ModItems.TECHIES_REMOTE.get()));
        }
        ItemStack head=p.getItemBySlot(EquipmentSlot.HEAD);
        if(!head.is(ModItems.TECHIES_HELMET.get())) {
            if(!head.isEmpty() && p.getInventory().getFreeSlot()<0) return;
            if(!head.isEmpty()) p.getInventory().add(head.copy());
            p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ModItems.TECHIES_HELMET.get()));
        }
        EQUIPPED.add(p.getUUID()); p.inventoryMenu.broadcastChanges();
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e) {
        if(e.phase!=TickEvent.Phase.END) return;
        var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer(); if(server==null) return;
        update(server,server.getPlayerList().getPlayers(),server.overworld().getGameTime());
    }
    /** Single tick implementation; explicit inputs also allow bootstrapped Forge regression fixtures. */
    static void update(net.minecraft.server.MinecraftServer server,List<ServerPlayer> players,long tick) {
        now=tick;
        int phase=GameManager.getPhaseValue();
        if(phase<1 || phase>3) {
            if(!MINES.isEmpty() || !PREVIEW.isEmpty() || !COOLDOWNS.isEmpty() || !OFFLINE_MANA.isEmpty()) resetMatch(server);
            EQUIPPED.clear(); return;
        }
        for(ServerPlayer p:players) {
            loadout(p);
            Placement s=PLACING.get(p.getUUID());
            if(!active(p) || downed(p)) { if(PREVIEW.containsKey(p.getUUID())) cancel(p); }
            else if(s!=null) {
                if(!s.mine.dimension.equals(p.level().dimension().location()) || TechiesRules.moved(s.origin.distanceToSqr(p.position())) || busy(p)) {
                    message(p,"Установка прервана: движение или другое действие (мана и КД не возвращаются)"); cancel(p);
                }
                else if(now-s.start>=s.mine.type.install) {
                    if(validSupport(p.serverLevel(),s.mine.support) && space(s.mine.dimension,s.mine.support,s.mine.id)) {
                        s.mine.placed=true; s.mine.version++; revision++;
                        if(s.mine.type==MineType.GREEN) GREENS.computeIfAbsent(p.getUUID(),k->new HashSet<>()).add(s.mine.id);
                        else if(triggered(s.mine,players)) { s.mine.started=now; s.mine.version++; ACTIVE.add(s.mine.id); }
                        PLACING.remove(p.getUUID()); PREVIEW.remove(p.getUUID()); PREVIEW_SLOT.remove(p.getUUID());
                    } else { message(p,"Опора или позиция изменилась: установка прервана"); cancel(p); }
                } else if((now-s.start)%10==0) p.serverLevel().playSound(null,p.blockPosition(),TechiesSounds.INSTALL.get(),SoundSource.PLAYERS,.45f,1);
            } else if(PREVIEW.containsKey(p.getUUID()) && (PREVIEW_SLOT.getOrDefault(p.getUUID(),-1)!=p.getInventory().selected || p.containerMenu!=p.inventoryMenu)) cancel(p);
            if(usable(p) && maniac(p)) for(Mine m:nearby(p.level().dimension().location(),p.position(),1))
                if(m.placed && m.type!=MineType.GREEN && m.started<0 && affected(m,p)) { m.started=now; m.version++; revision++; ACTIVE.add(m.id); }
        }
        for(int id:new HashSet<>(ACTIVE)) {
            Mine m=MINES.get(id); if(m==null) continue;
            long elapsed=now-m.started;
            if(m.type!=MineType.GREEN && TechiesRules.reversible(elapsed) && !triggered(m,players)) { m.started=-1; m.version++; revision++; ACTIVE.remove(id); }
            else if(elapsed>=m.type.activation) explode(m,server,players);
        }
        for(ServerPlayer p:players) { syncHud(p); syncPose(p); visibility(p); }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void damaged(LivingDamageEvent e) {
        if(e.getAmount()>0 && e.getEntity() instanceof ServerPlayer p && placing(p)) cancel(p);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        if(e.getEntity() instanceof ServerPlayer p) {
            if(OWNER_COUNTS.containsKey(p.getUUID())) p.getCapability(org.example.maniacrevolution.mana.ManaProvider.MANA).ifPresent(m -> OFFLINE_MANA.put(p.getUUID(),new OfflineMana(m.getMana(),m.getMaxMana(),false)));
            cancel(p); KNOWN.remove(p.getUUID()); HUD.remove(p.getUUID()); EQUIPPED.remove(p.getUUID()); VISIBILITY.remove(p.getUUID()); AIMS.remove(p.getUUID());
        }
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        if(e.getEntity() instanceof ServerPlayer p) {
            var snapshot=OFFLINE_MANA.remove(p.getUUID());
            if(snapshot!=null && snapshot.changed) {
                p.getCapability(org.example.maniacrevolution.mana.ManaProvider.MANA).ifPresent(m -> m.setMana(snapshot.mana));
                ManaUtil.forceSync(p);
            }
            KNOWN.remove(p.getUUID()); HUD.remove(p.getUUID()); VISIBILITY.remove(p.getUUID()); EQUIPPED.remove(p.getUUID());
        }
    }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if(e.getEntity() instanceof ServerPlayer p) { cancel(p); KNOWN.remove(p.getUUID()); send(p,0,0,0,BlockPos.ZERO,-1,0,0,0,0); }
    }
    @SubscribeEvent public static void died(net.minecraftforge.event.entity.living.LivingDeathEvent e) {
        if(e.getEntity() instanceof ServerPlayer p) { cancel(p); EQUIPPED.remove(p.getUUID()); }
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void interact(PlayerInteractEvent.EntityInteract e) {
        if(e.getEntity() instanceof ServerPlayer p && placing(p)) e.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void attack(net.minecraftforge.event.entity.player.AttackEntityEvent e) {
        if(e.getEntity() instanceof ServerPlayer p && PREVIEW.containsKey(p.getUUID())) e.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void useItem(PlayerInteractEvent.RightClickItem e) {
        if(e.getEntity() instanceof ServerPlayer p && PREVIEW.containsKey(p.getUUID())) {
            if(!placing(p)) cancel(p);
            e.setCanceled(true);
        }
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void useBlock(PlayerInteractEvent.RightClickBlock e) {
        if(e.getEntity() instanceof ServerPlayer p && PREVIEW.containsKey(p.getUUID())) {
            if(!placing(p)) cancel(p);
            e.setCanceled(true);
        }
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void mineBlock(PlayerInteractEvent.LeftClickBlock e) {
        if(e.getEntity() instanceof ServerPlayer p && PREVIEW.containsKey(p.getUUID())) e.setCanceled(true);
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent e) { clear(null); COOLDOWNS.clear(); EQUIPPED.clear(); OFFLINE_MANA.clear(); nextId=1; }
}
