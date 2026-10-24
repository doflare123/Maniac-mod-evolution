package org.example.maniacrevolution.forester;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.entity.ModEntities;

public final class PepperCanisterEntity extends ThrowableItemProjectile {
    private static final EntityDataAccessor<Boolean> BURNING = SynchedEntityData.defineId(PepperCanisterEntity.class, EntityDataSerializers.BOOLEAN);
    private int burningTicks;
    private Vec3 landing;
    public PepperCanisterEntity(EntityType<? extends PepperCanisterEntity> type, Level level) { super(type, level); }
    public PepperCanisterEntity(Level level, Player owner) {
        super(ModEntities.PEPPER_CANISTER.get(), owner, level);
        setDeltaMovement(launchVelocity(owner));
    }
    public static Vec3 launchVelocity(Player player) {
        Vec3 inherited = player.getDeltaMovement();
        return player.getLookAngle().scale(ForesterRules.THROW_SPEED)
                .add(inherited.x, player.onGround() ? 0 : inherited.y, inherited.z);
    }
    @Override protected Item getDefaultItem() { return ModItems.PEPPER_CANISTER.get(); }
    @Override protected void defineSynchedData() { super.defineSynchedData(); entityData.define(BURNING, false); }
    public boolean burning() { return entityData.get(BURNING); }
    @Override protected float getGravity() { return (float) ForesterRules.GRAVITY; }
    @Override protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (level().isClientSide) return;
        landing = hit.getLocation();
        if (hit instanceof BlockHitResult block) landing = landing.add(Vec3.atLowerCornerOf(block.getDirection().getNormal()).scale(.03));
        entityData.set(BURNING, true); setNoGravity(true); setDeltaMovement(Vec3.ZERO);
        level().playSound(null, blockPosition(), net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH, getSoundSource(), .6F, 1.4F);
    }
    @Override public void tick() {
        if (!burning()) {
            super.tick();
            if (burning() && landing != null) { setPos(landing); setDeltaMovement(Vec3.ZERO); }
            if (!level().isClientSide && tickCount > 200) onHit(new BlockHitResult(position(), net.minecraft.core.Direction.UP, blockPosition(), false));
            return;
        }
        baseTick(); setDeltaMovement(Vec3.ZERO); clearFire();
        if (level() instanceof ServerLevel server) {
            if (++burningTicks >= ForesterRules.PEPPER_BURN) { discard(); return; }
            if (burningTicks % 4 == 0) {
                server.sendParticles(ParticleTypes.SMOKE, getX(), getY() + .15, getZ(), 4, .2, .2, .2, .012);
                // A sparse cloud makes the affected radius readable without hiding the scene.
                server.sendParticles(ParticleTypes.SMOKE, getX(), getY() + .8, getZ(), 6, 2.1, .5, 2.1, .003);
                for (Player player : server.getEntitiesOfClass(Player.class, getBoundingBox().inflate(ForesterRules.PEPPER_RADIUS), ForesterManager::vulnerable)) {
                    if (distanceToSqr(player) <= ForesterRules.PEPPER_RADIUS * ForesterRules.PEPPER_RADIUS)
                        player.addEffect(new MobEffectInstance(ModEffects.PEPPER.get(), ForesterRules.PEPPER_EFFECT, 0, false, false, true));
                }
            }
        }
    }
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) { return false; }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag); tag.putBoolean("Burning", burning()); tag.putInt("BurningTicks", burningTicks);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag); entityData.set(BURNING, tag.getBoolean("Burning"));
        burningTicks = Math.max(0, tag.getInt("BurningTicks")); setNoGravity(burning());
    }
}
