package org.example.maniacrevolution.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.example.maniacrevolution.forgetmenot.ForgetMeNotManager;

import javax.annotation.Nullable;
import java.util.UUID;

/** Неподвижный, не сталкивающийся с игроками установленный цветок. */
public final class ForgetMeNotEntity extends Entity {
    private static final String OWNER_TAG = "ForgetMeNotOwner";

    @Nullable
    private UUID ownerUUID;
    private Vec3 lockedPosition;

    public ForgetMeNotEntity(EntityType<? extends ForgetMeNotEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();
        if (lockedPosition == null) lockedPosition = position();
        setPos(lockedPosition.x, lockedPosition.y, lockedPosition.z);
        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide() || !(source.getDirectEntity() instanceof ServerPlayer attacker)) {
            return false;
        }
        if (ownerUUID != null && ownerUUID.equals(attacker.getUUID())) {
            return ForgetMeNotManager.reclaimByOwner(attacker, this);
        }
        return ForgetMeNotManager.destroyByManiac(attacker, this);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(double x, double y, double z) {
    }

    public void setOwnerUUID(UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
    }

    @Nullable
    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public void discardWithoutStateChange() {
        discard();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ownerUUID = tag.hasUUID(OWNER_TAG) ? tag.getUUID(OWNER_TAG) : null;
        lockedPosition = position();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerUUID != null) tag.putUUID(OWNER_TAG, ownerUUID);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
