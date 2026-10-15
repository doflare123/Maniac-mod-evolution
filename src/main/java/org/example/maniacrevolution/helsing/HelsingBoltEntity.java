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
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.game.GameManager;
import org.example.maniacrevolution.perk.PerkPhase;

public final class HelsingBoltEntity extends AbstractArrow {
    private static final ResourceKey<DamageType> SILVER_CHIP = ResourceKey.create(Registries.DAMAGE_TYPE, Maniacrev.loc("helsing_silver_chip"));
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
        float damage = HelsingConfig.BOLT_DAMAGE.get().floatValue();
        DamageSource source = damageSources().arrow(this, getOwner());
        if (kind == HelsingBoltItem.Kind.SILVER) {
            boolean reversal = GameManager.getCurrentPhase() == PerkPhase.REVERSAL;
            int maniacClass = target instanceof ServerPlayer player ? PlayerDataManager.get(player).getManiacClassId() : -1;
            damage = HelsingSilverDamage.amount(HelsingConfig.BOLT_DAMAGE.get(), reversal, maniacClass);
            if (!reversal) {
                source = new DamageSource(level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SILVER_CHIP), this, getOwner());
            }
        }
        if (target.hurt(source, damage)) {
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
