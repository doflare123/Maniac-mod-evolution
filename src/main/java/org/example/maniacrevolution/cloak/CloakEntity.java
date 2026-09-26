package org.example.maniacrevolution.cloak;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class CloakEntity extends Entity implements GeoEntity {
    private static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(CloakEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET = SynchedEntityData.defineId(CloakEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STAGE = SynchedEntityData.defineId(CloakEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> START = SynchedEntityData.defineId(CloakEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Float> HEIGHT = SynchedEntityData.defineId(CloakEntity.class, EntityDataSerializers.FLOAT);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public CloakEntity(EntityType<? extends CloakEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override protected void defineSynchedData() {
        entityData.define(OWNER, -1);
        entityData.define(TARGET, -1);
        entityData.define(STAGE, 0);
        entityData.define(START, 0L);
        entityData.define(HEIGHT, 0f);
    }

    public int ownerId() { return entityData.get(OWNER); }
    public int targetId() { return entityData.get(TARGET); }
    public void owner(Entity entity) { entityData.set(OWNER, entity.getId()); }
    public void target(Entity entity) { entityData.set(TARGET, entity == null ? -1 : entity.getId()); }
    public CloakStage stage() { return CloakStage.values()[entityData.get(STAGE)]; }
    public long ageInStage() { return Math.max(0, level().getGameTime() - entityData.get(START)); }
    public float hoverHeight() { return entityData.get(HEIGHT); }
    public void hoverHeight(double y) { entityData.set(HEIGHT, (float)y); }
    public void stage(CloakStage stage) {
        entityData.set(STAGE, stage.ordinal());
        entityData.set(START, level().getGameTime());
    }
    @Override public void tick() {
        super.tick();
        if (!level().isClientSide && !CloakManager.isManaged(this)) discard();
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "cloak", 0, state -> {
            CloakStage stage = stage();
            RawAnimation animation = RawAnimation.begin();
            return state.setAndContinue(stage.loop ? animation.thenLoop("animation.cloak." + stage.clip)
                    : animation.thenPlayAndHold("animation.cloak." + stage.clip));
        }));
    }
}
