package org.example.maniacrevolution.helsing;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.example.maniacrevolution.entity.ModEntities;
import org.example.maniacrevolution.effect.ModEffects;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;

public final class HelsingBoltEntity extends AbstractArrow {
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(HelsingBoltEntity.class, EntityDataSerializers.INT);
    public HelsingBoltEntity(EntityType<? extends AbstractArrow> type, Level level) { super(type, level); pickup = Pickup.DISALLOWED; }
    public HelsingBoltEntity(Level level, LivingEntity owner, HelsingBoltItem.Kind kind) {
        super(ModEntities.HELSING_BOLT.get(), owner, level);
        entityData.set(KIND, kind.ordinal()); pickup = Pickup.DISALLOWED;
    }
    @Override protected void defineSynchedData() { super.defineSynchedData(); entityData.define(KIND, 0); }
    @Override protected ItemStack getPickupItem() { return ItemStack.EMPTY; }
    public HelsingBoltItem.Kind kind() { return HelsingBoltItem.Kind.values()[Math.floorMod(entityData.get(KIND), 4)]; }
    @Override protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && HelsingManager.isManiac(entity);
    }
    @Override protected void onHitEntity(EntityHitResult hit) {
        if (level().isClientSide || !(hit.getEntity() instanceof LivingEntity target)) return;
        HelsingBoltItem.Kind kind = kind();
        float damage = (float) (kind == HelsingBoltItem.Kind.SILVER ? HelsingConfig.SILVER_DAMAGE.get() : HelsingConfig.BOLT_DAMAGE.get()).doubleValue();
        if (target.hurt(damageSources().arrow(this, getOwner()), damage)) {
            switch (kind) {
                case IRON -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, HelsingConfig.IRON_DURATION.get(), 1, false, false, true));
                case RUNE -> target.addEffect(new MobEffectInstance(ModEffects.HELSING_RUNE.get(), HelsingConfig.RUNE_DURATION.get(), 0, false, false, true));
                case ENCHANTED -> target.addEffect(new MobEffectInstance(ModEffects.HELSING_WEAKNESS.get(), HelsingConfig.BOLT_WEAKNESS_DURATION.get(), 0, false, false, true));
                default -> {}
            }
        }
        discard();
    }
    @Override public void tick() { super.tick(); if (tickCount > 200) discard(); }
    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putInt("BoltKind", entityData.get(KIND)); }
    @Override public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); entityData.set(KIND, Math.floorMod(tag.getInt("BoltKind"), 4)); }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
