package org.example.maniacrevolution.lich;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Corpses and placed phylacteries are entities, never blocks. */
public final class LichObject extends Entity implements GeoEntity {
    private static final EntityDataAccessor<Boolean> PHYLACTERY = SynchedEntityData.defineId(LichObject.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<java.util.Optional<UUID>> OWNER=SynchedEntityData.defineId(LichObject.class,EntityDataSerializers.OPTIONAL_UUID);
    public java.util.Optional<UUID> syncedOwner() {return entityData.get(OWNER);}
    private static final EntityDataAccessor<String> ACTION = SynchedEntityData.defineId(LichObject.class, EntityDataSerializers.STRING);
    private long actionUntil;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public UUID owner;
    public UUID session;
    public String map = "";
    public LichObject(EntityType<?> type, Level level) { super(type, level); }
    public boolean phylactery() { return entityData.get(PHYLACTERY); }
    public String visualAction() {return entityData.get(ACTION);}
    @Override public EntityDimensions getDimensions(Pose pose) { return phylactery()?EntityDimensions.scalable(.6F,.8F):EntityDimensions.scalable(1.4F,.35F); }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key) { super.onSyncedDataUpdated(key); if(PHYLACTERY.equals(key))refreshDimensions(); }
    public void configure(UUID owner, String map, boolean phylactery) { this.owner = owner; this.session=LichManager.session(); this.map = map; entityData.set(PHYLACTERY, phylactery);entityData.set(OWNER,java.util.Optional.ofNullable(owner)); }
    public void action(String action,int duration) {entityData.set(ACTION,action);actionUntil=level().getGameTime()+duration;}
    @Override protected void defineSynchedData() { entityData.define(PHYLACTERY, false); entityData.define(ACTION,"");entityData.define(OWNER,java.util.Optional.empty()); }
    @Override public boolean isPickable() { return true; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && phylactery() && source.getEntity() instanceof ServerPlayer p && LichManager.survivor(p)) {
            LichManager.hitPhylactery(owner); return true;
        }
        return false;
    }
    @Override public InteractionResult interact(Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer p && hand == InteractionHand.MAIN_HAND) {
            if (phylactery()) LichManager.pickup(p, this);
            else if (p.getMainHandItem().is(org.example.maniacrevolution.ModItems.LICH_BOOK.get())) LichManager.openCorpse(p, this);
        }
        return InteractionResult.SUCCESS;
    }
    @Override public void tick() {
        super.tick();
        if (!level().isClientSide && !LichManager.validObject(this)) discard();
        if(!level().isClientSide&&!entityData.get(ACTION).isEmpty()&&level().getGameTime()>=actionUntil)entityData.set(ACTION,"");
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        if (owner != null) tag.putUUID("Owner", owner); tag.putString("Map", map); tag.putBoolean("Phylactery", phylactery());
        if(session!=null)tag.putUUID("LichSession",session);
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null; map = tag.getString("Map"); entityData.set(PHYLACTERY, tag.getBoolean("Phylactery"));entityData.set(OWNER,java.util.Optional.ofNullable(owner));
        session=tag.hasUUID("LichSession")?tag.getUUID("LichSession"):null;
    }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<>(this, "body", 0, s -> {
            String action=entityData.get(ACTION),stem=phylactery()?"lich_phylactery":"lich_corpse";
            return s.setAndContinue(action.isEmpty()?RawAnimation.begin().thenLoop("animation."+stem+".idle"):RawAnimation.begin().thenPlayAndHold("animation."+stem+"."+action));
        }));
    }
}
