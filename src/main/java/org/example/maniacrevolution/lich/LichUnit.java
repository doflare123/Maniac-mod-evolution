package org.example.maniacrevolution.lich;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.example.maniacrevolution.util.SelectiveGlowingEffect;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.core.object.PlayState;

/** No vanilla target goals: orders and sight memory are owned by this server AI. */
public final class LichUnit extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(LichUnit.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> EMERGE = SynchedEntityData.defineId(LichUnit.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DETECTING = SynchedEntityData.defineId(LichUnit.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(LichUnit.class,EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> SELECTED = SynchedEntityData.defineId(LichUnit.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Long> BOW_START=SynchedEntityData.defineId(LichUnit.class,EntityDataSerializers.LONG);
    public static final int BOW_DRAW_TICKS=6;
    private long bowRelease=-1;
    private UUID bowTarget;
    public long bowStart() {return entityData.get(BOW_START);}
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public UUID owner;
    public UUID session;
    public LichRules.Order order = LichRules.Order.FOLLOW;
    public boolean aggressive;
    public Vec3 anchor, destination, lastSeen;
    public UUID orderedTarget, combatTarget;
    public long searchUntil, attackedUntil, nextAttack, nextHint;
    public boolean detecting;
    final LichNavigation routing;
    private int nextPatrolPoint;
    private final LichPursuit pursuit=new LichPursuit();
    final LichLunge lunge=new LichLunge();
    private Vec3 pursuitAnchor,searchPoint;
    private int searchIndex;
    private final LichAggroBurst aggroBurst=new LichAggroBurst();
    double aggroSpeed() {return emerging()==0?aggroBurst.multiplier(level().getGameTime()):1;}
    public LichUnit(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level); setPersistenceRequired();
        moveControl=new LichMoveControl(this);
        routing=new LichNavigation(this);
        getNavigation().setMaxVisitedNodesMultiplier(2);
        if (getNavigation() instanceof GroundPathNavigation ground) ground.setCanOpenDoors(true);
    }
    @Override protected PathNavigation createNavigation(Level level) {return new LichGroundNavigation(this,level);}
    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, .23)
                .add(Attributes.FOLLOW_RANGE, 16).add(Attributes.ATTACK_DAMAGE, 4).add(Attributes.KNOCKBACK_RESISTANCE, 0);
    }
    @Override protected void defineSynchedData() { super.defineSynchedData(); entityData.define(KIND, 0); entityData.define(EMERGE, 100);entityData.define(DETECTING,false);entityData.define(OWNER,Optional.empty());entityData.define(SELECTED,false);entityData.define(BOW_START,-1L); }
    public Optional<UUID> syncedOwner() {return entityData.get(OWNER);}
    public boolean selected() {return entityData.get(SELECTED);}
    public LichRules.Kind kind() { return LichRules.Kind.values()[entityData.get(KIND)]; }
    public int emerging() { return entityData.get(EMERGE); }
    public boolean detecting() { return entityData.get(DETECTING); }
    @Override public EntityDimensions getDimensions(Pose pose) {
        return kind()==LichRules.Kind.BUTCHER?EntityDimensions.scalable(.9F,2.2F):kind()==LichRules.Kind.GHOUL?EntityDimensions.scalable(.6F,1.5F):EntityDimensions.scalable(.6F,1.95F);
    }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key) { super.onSyncedDataUpdated(key); if(KIND.equals(key))refreshDimensions(); }
    public void configure(UUID owner, LichRules.Kind kind) {
        this.owner = owner; this.session=LichManager.session(); entityData.set(KIND, kind.ordinal());
        entityData.set(OWNER,Optional.ofNullable(owner));
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(kind.hp); setHealth(kind.hp);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(kind.speed);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(kind.damage);
        getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(kind == LichRules.Kind.BUTCHER ? .75 : 0);
        anchor = position();
        if (kind == LichRules.Kind.SKELETON) setItemSlot(EquipmentSlot.MAINHAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOW));
    }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new net.minecraft.world.entity.ai.goal.FloatGoal(this) {
            @Override public boolean canUse() {return emerging()==0&&super.canUse();}
        });
    }
    @Override public MobType getMobType() { return MobType.UNDEAD; }
    @Override public boolean canBeLeashed(net.minecraft.world.entity.player.Player p) { return false; }
    @Override protected void dropAllDeathLoot(DamageSource source) {}
    @Override public void knockback(double power, double x, double z) { if (emerging() == 0) super.knockback(power, x, z); }
    @Override public boolean isPushable() { return emerging() == 0 && super.isPushable(); }
    @Override public boolean hurt(DamageSource source, float amount) {
        if (LichManager.friendly(source.getEntity(), this)) return false;
        boolean hit = super.hurt(source, amount);
        if (hit) { attackedUntil = level().getGameTime() + 40; var army=LichManager.owner(owner); if(army!=null)SelectiveGlowingEffect.addGlowing(this,army.player,40); }
        return hit;
    }
    public void command(LichRules.Order order, Vec3 point, UUID target) {
        cancelBow();
        lunge.cancel();
        this.order = order; anchor = order == LichRules.Order.STAND ? position() : point;
        destination = anchor; orderedTarget = target; forgetPursuit();
        if (order == LichRules.Order.CHASE&&point!=null&&target!=null) {
            pursuitAnchor=point;combatTarget=target;
        }
        routing.stop();
        if(order==LichRules.Order.MOVE&&point!=null&&emerging()==0&&!level().isClientSide)routing.move(point);
    }
    @Override public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (isDeadOrDying()) {cancelBow();return;}
        var state = LichManager.owner(owner);
        if (state == null || state.player.level() != level()) { discard(); return; }
        aggroBurst.tick(level().getGameTime(),combatTarget!=null&&lastSeen!=null);
        entityData.set(SELECTED,state.selected.contains(getUUID()));
        if(state.selected.contains(getUUID()) && tickCount%5==0)SelectiveGlowingEffect.addGlowing(this,state.player,10);
        if (emerging() > 0) {
            cancelBow();
            entityData.set(EMERGE, emerging() - 1); routing.stop();
            return;
        }
        long now=level().getGameTime();
        var current=knownPlayer(combatTarget);
        lunge.recover(lunge.cooldown()>0&&current!=null&&current.isSprinting()
                &&distanceToSqr(current)<=9&&trackedVisible(current));
        if(lunge.tick(this,current,now))return;
        tickBow();
        routing.tick();
        if ((tickCount + getId()) % 5 != 0) return;
        Vec3 task = task(state.player, now);
        Vec3 combatAnchor = order == LichRules.Order.PATROL ? anchor : task;
        ServerPlayer target = current;
        if(combatTarget!=null&&!valid(target)) {forgetPursuit();target=null;}
        if(target!=null&&(!trackable(target,now)||(!aggressive&&order!=LichRules.Order.CHASE
                &&pursuitAnchor!=null&&target.position().distanceToSqr(pursuitAnchor)>Math.pow(kind()==LichRules.Kind.SKELETON?LichRules.SKELETON_TRACK_RANGE:20,2))))target=null;
        if(order==LichRules.Order.CHASE&&orderedTarget!=null) {
            var ordered=knownPlayer(orderedTarget);
            if(trackable(ordered,now))target=ordered;
        }
        if (target == null) {
            target = level().getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(16), p->visible(p)||nearHidden(p)).stream()
                    .filter(p -> aggressive || p.position().distanceToSqr(combatAnchor) <= 100)
                    .min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
            if(target!=null)pursuitAnchor=combatAnchor;
        }
        detecting = target != null && trackedVisible(target);
        entityData.set(DETECTING,detecting);
        if(!detecting)cancelBow();
        if (detecting) {
            aggroBurst.see(now);
            if(!target.getUUID().equals(pursuit.target()))pursuitAnchor=combatAnchor;
            pursuit.observe(target.getUUID(),target.position(),target.getDeltaMovement(),now);
            combatTarget = target.getUUID(); lastSeen = pursuit.position(); searchUntil=0;searchPoint=null;searchIndex=0;
            SelectiveGlowingEffect.addGlowing(this, state.player, 10);
            if (kind() == LichRules.Kind.GHOUL) SelectiveGlowingEffect.addGlowing(target, state.player, 40);
            getLookControl().setLookAt(target, 30, 30);
            double range = kind() == LichRules.Kind.SKELETON ? LichRules.SKELETON_ATTACK_RANGE : 2;
            if (distanceToSqr(target) <= range * range) {
                routing.stop();
                if(kind()==LichRules.Kind.SKELETON) {
                    if(bowRelease<0&&now>=nextAttack-BOW_DRAW_TICKS)beginBow(target,now);
                } else tryMelee(target,now);
            } else if(!lunge.begin(this,target,now)) {cancelBow();move(lastSeen);}
        } else if(target!=null) {
            if(!target.getUUID().equals(combatTarget)) {
                forgetPursuit();
                pursuitAnchor=combatAnchor;
            }
            combatTarget=target.getUUID();
            move(target.position());
        } else if (!searchLostTarget(now)) {
            forgetPursuit();
            if (!LichNavigation.arrived(position(),task,order == LichRules.Order.FOLLOW ? 9 : 1)) move(task);
            else {
                routing.stop();
                if(order==LichRules.Order.RETURN)LichManager.returned(this);
                if (order == LichRules.Order.MOVE || order == LichRules.Order.RETURN) command(LichRules.Order.STAND, position(), null);
            }
        }
        if (state.selected.contains(getUUID()) || attackedUntil > now) SelectiveGlowingEffect.addGlowing(this, state.player, 10);
        openPassages();
    }
    private boolean valid(ServerPlayer p) { return LichManager.survivor(p) && p.level() == level(); }
    private ServerPlayer knownPlayer(UUID id) {
        var entity=id==null?null:((net.minecraft.server.level.ServerLevel)level()).getEntity(id);
        return entity instanceof ServerPlayer player?player:null;
    }
    private boolean visible(ServerPlayer p) { return valid(p) && distanceToSqr(p) <= 256 && hasLineOfSight(p); }
    boolean trackedVisible(ServerPlayer p) { return valid(p) && distanceToSqr(p) <= (kind()==LichRules.Kind.SKELETON?LichRules.SKELETON_TRACK_RANGE*LichRules.SKELETON_TRACK_RANGE:256) && hasLineOfSight(p); }
    private boolean sameFloor(ServerPlayer p) {return Math.abs(getY()-p.getY())<=1.25;}
    private boolean nearHidden(ServerPlayer p) {return valid(p)&&sameFloor(p)&&distanceToSqr(p)<=4;}
    private boolean trackable(ServerPlayer p,long now) {
        return trackedVisible(p)||nearHidden(p)||valid(p)&&sameFloor(p)&&pursuit.remembers(p.getUUID(),p.position(),now);
    }
    boolean tryMelee(ServerPlayer p,long now) {
        if(now<nextAttack||distanceToSqr(p)>4||!trackedVisible(p))return false;
        attack(p);nextAttack=now+kind().interval;
        return true;
    }
    private void forgetPursuit() {
        pursuit.clear();combatTarget=null;lastSeen=null;searchUntil=0;pursuitAnchor=searchPoint=null;searchIndex=0;
    }
    private boolean searchLostTarget(long now) {
        if(lastSeen==null)return false;
        pursuit.lose(now);if(pursuit.expired(now))return false;
        if(pursuit.approaching()) {
            if(!LichNavigation.arrived(position(),lastSeen,1)&&!routing.failed()) {move(lastSeen);return true;}
            pursuit.startSearch(now);searchUntil=pursuit.searchUntil();routing.stop();
        }
        if(searchPoint!=null&&(LichNavigation.arrived(position(),searchPoint,.6)||routing.failed())) {
            searchPoint=null;routing.stop();
        }
        while(searchPoint==null&&searchIndex<4)searchPoint=routing.reachableGround(pursuit.candidate(searchIndex++));
        if(searchPoint!=null) {
            getLookControl().setLookAt(searchPoint.x,searchPoint.y+1,searchPoint.z,30,30);move(searchPoint);
        } else {
            routing.stop();double angle=now*.08;
            getLookControl().setLookAt(getX()+Math.cos(angle)*3,getEyeY(),getZ()+Math.sin(angle)*3,30,30);
        }
        return true;
    }
    Vec3 task(ServerPlayer lich, long now) {
        if (anchor == null) anchor = position();
        if (order == LichRules.Order.FOLLOW || order == LichRules.Order.RETURN) return lich.position();
        if (order == LichRules.Order.PATROL && tickCount>=nextPatrolPoint
                &&(destination == null || LichNavigation.arrived(position(),destination,2)||routing.failed())) {
            double a = random.nextDouble() * Math.PI * 2, r = random.nextDouble() * LichRules.PATROL_RADIUS;
            var point=routing.ground(anchor.add(Math.cos(a) * r, 0, Math.sin(a) * r));
            destination=point==null?position():point;nextPatrolPoint=tickCount+40;
        }
        if (order == LichRules.Order.SEARCH && now >= nextHint) {
            nextHint = now + 200;
            var nearest = lich.serverLevel().players().stream().filter(LichManager::survivor)
                    .min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
            // Coarse 32-block cells, refreshed once per 10 seconds, never sent as player positions.
            destination = nearest == null ? position().add(random.nextInt(17)-8, 0, random.nextInt(17)-8)
                    : new Vec3(Math.floor(nearest.getX()/32)*32 + 16, getY(), Math.floor(nearest.getZ()/32)*32 + 16);
            var ground=routing.ground(destination);if(ground!=null)destination=ground;
            anchor = destination;
        }
        return (order == LichRules.Order.PATROL || order == LichRules.Order.SEARCH) && destination != null ? destination : anchor;
    }
    private void move(Vec3 pos) {routing.move(pos);}
    private void beginBow(ServerPlayer target,long now) {
        bowTarget=target.getUUID();bowRelease=Math.max(now+BOW_DRAW_TICKS,nextAttack);
        entityData.set(BOW_START,now);startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
    }
    private void cancelBow() {
        if(bowRelease<0)return;
        bowRelease=-1;bowTarget=null;stopUsingItem();entityData.set(BOW_START,-1L);
    }
    private void tickBow() {
        if(bowRelease<0)return;
        var target=knownPlayer(bowTarget);
        if(!trackedVisible(target)||distanceToSqr(target)>LichRules.SKELETON_ATTACK_RANGE*LichRules.SKELETON_ATTACK_RANGE) {cancelBow();return;}
        getLookControl().setLookAt(target,30,30);
        long now=level().getGameTime();if(now<bowRelease)return;
        bowRelease=-1;bowTarget=null;stopUsingItem();attack(target);nextAttack=now+kind().interval;
    }
    private void attack(ServerPlayer p) {
        if(kind()!=LichRules.Kind.SKELETON)swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        if (kind() == LichRules.Kind.SKELETON) {
            var arrow = new Arrow(level(), this); arrow.setBaseDamage(0);
            arrow.pickup=net.minecraft.world.entity.projectile.AbstractArrow.Pickup.DISALLOWED;
            var army = LichManager.owner(owner); if (army != null) arrow.setOwner(army.player);
            arrow.getPersistentData().putUUID("lich_owner", owner);
            arrow.getPersistentData().putUUID("lich_session", session);
            arrow.getPersistentData().putBoolean("lich_arrow", true);
            Vec3 delta = LichArrowAim.direction(p.getEyePosition().add(0,-.3,0).subtract(arrow.position()));
            arrow.shoot(delta.x,delta.y,delta.z,LichArrowAim.SPEED,LichRules.SKELETON_SPREAD);
            level().addFreshEntity(arrow);
        } else org.example.maniacrevolution.util.ManiacDamageAttribution.hurtWithSource(p, owner,
                () -> p.hurt(damageSources().mobAttack(this), kind().damage));
    }
    private void openPassages() {
        for (var pos : net.minecraft.core.BlockPos.betweenClosed(blockPosition().offset(-1,0,-1),blockPosition().offset(1,1,1))) {
            var b = level().getBlockState(pos);
            if ((b.getBlock() instanceof DoorBlock || b.getBlock() instanceof TrapDoorBlock || b.getBlock() instanceof FenceGateBlock)
                    && b.hasProperty(BlockStateProperties.OPEN) && !b.getValue(BlockStateProperties.OPEN)
                    && b.getBlock() != Blocks.IRON_DOOR && b.getBlock() != Blocks.IRON_TRAPDOOR)
                level().setBlock(pos, b.setValue(BlockStateProperties.OPEN, true), 3);
        }
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag); if (owner != null) tag.putUUID("Owner", owner); if(session!=null)tag.putUUID("LichSession",session);
        tag.putInt("LichKind", kind().ordinal()); tag.putInt("LichEmerge", emerging());
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag); if (tag.hasUUID("Owner")) owner = tag.getUUID("Owner"); session=tag.hasUUID("LichSession")?tag.getUUID("LichSession"):null;
        entityData.set(OWNER,Optional.ofNullable(owner));
        entityData.set(KIND, Math.max(0,Math.min(3,tag.getInt("LichKind")))); entityData.set(EMERGE,tag.getInt("LichEmerge"));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "body", 0, state -> {
            if (kind() != LichRules.Kind.GHOUL && kind() != LichRules.Kind.BUTCHER) return PlayState.STOP;
            String stem = kind() == LichRules.Kind.GHOUL ? "lich_ghoul" : "lich_butcher";
            var frame=LichUnitVisuals.frame(this,tickCount+state.getPartialTick());
            String clip=frame.clip();
            if(frame.restart())state.getController().forceAnimationReset();
            state.getController().transitionLength(frame.loop()?2:0);
            state.setControllerSpeed(clip.equals("death")?(kind()==LichRules.Kind.BUTCHER?1.65F:1.25F):1);
            return state.setAndContinue(RawAnimation.begin().then("animation." + stem + "." + clip,
                    frame.loop() ? Animation.LoopType.LOOP : Animation.LoopType.PLAY_ONCE));
        }));
    }
}
