package org.example.maniacrevolution.strange;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Pure visual, with server-authoritative stance and animation timestamps. */
public final class StrangeEffectEntity extends Entity implements GeoEntity {
    public static final int DEFENSE = 0, COMBAT = 1, WHIP = 2, PORTAL = 3;
    public static final int PORTAL_OPEN = 4, PORTAL_CLOSE = 5;
    private static final EntityDataAccessor<Boolean> DRAW_LEFT = SynchedEntityData.defineId(StrangeEffectEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> CLOSE_DURATION = SynchedEntityData.defineId(StrangeEffectEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(StrangeEffectEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(StrangeEffectEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> START = SynchedEntityData.defineId(StrangeEffectEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> CLOSE = SynchedEntityData.defineId(StrangeEffectEntity.class, EntityDataSerializers.LONG);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private long animationStart = Long.MIN_VALUE;
    public StrangeEffectEntity(EntityType<? extends StrangeEffectEntity> type, Level level) {
        super(type, level); noPhysics = true; setNoGravity(true);
    }
    @Override protected void defineSynchedData() {
        entityData.define(OWNER, -1); entityData.define(KIND, DEFENSE); entityData.define(START, 0L);
        entityData.define(CLOSE, -1L);
        entityData.define(DRAW_LEFT, false); entityData.define(CLOSE_DURATION, StrangeRules.COLLAPSE_TICKS);
    }
    public int ownerId() { return entityData.get(OWNER); }
    public int kind() { return entityData.get(KIND); }
    public long age() { return Math.max(0, level().getGameTime() - entityData.get(START)); }
    public boolean casting() { return kind()==PORTAL_OPEN || kind()==PORTAL_CLOSE; }
    public boolean drawingLeft() { return entityData.get(DRAW_LEFT); }
    public void drawingLeft(boolean left) { entityData.set(DRAW_LEFT,left); }
    public void closing(int duration) { entityData.set(CLOSE_DURATION,duration); entityData.set(CLOSE, level().getGameTime()); }
    public boolean isClosing() { return entityData.get(CLOSE) >= 0; }
    public float portalScale(float partial) {
        if (!isClosing()) return StrangeRules.openingScale(age()+partial);
        return StrangeRules.closingScale(level().getGameTime()+partial-entityData.get(CLOSE),entityData.get(CLOSE_DURATION));
    }
    public void owner(Entity owner) { entityData.set(OWNER, owner.getId()); }
    public void kind(int kind) { entityData.set(KIND, kind); entityData.set(START, level().getGameTime()); }
    public String asset() { return kind() == PORTAL ? "strange_portal" : kind() == WHIP ? "strange_whip" : "strange_shields"; }
    @Override public void tick() {
        super.tick();
        if (level().isClientSide && kind() == PORTAL)
            org.example.maniacrevolution.strange.client.PortalVisual.particles(this);
        if (!level().isClientSide && !StrangeManager.managed(this)) discard();
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "magic", 0, state -> {
            // COMBAT has no visible render. A repeated cast must restart even if that hidden state was skipped.
            long start = entityData.get(START);
            if (animationStart != start) {
                animationStart = start;
                state.getController().forceAnimationReset();
            }
            return state.setAndContinue(kind() == WHIP ? RawAnimation.begin().thenPlayAndHold("animation.strange_whip.cast")
                    : RawAnimation.begin().thenLoop("animation." + asset() + ".idle"));
        }));
    }
}
