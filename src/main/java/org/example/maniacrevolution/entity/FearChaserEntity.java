package org.example.maniacrevolution.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.example.maniacrevolution.nightmare.NightmareConfig;
import org.example.maniacrevolution.nightmare.NightmareManager;

import java.util.UUID;

public class FearChaserEntity extends Entity {
    private UUID targetPlayer;
    private long activeAtGameTime;

    public FearChaserEntity(EntityType<? extends FearChaserEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
    }

    public void setTargetPlayer(UUID targetPlayer) {
        this.targetPlayer = targetPlayer;
    }

    public void setActiveAtGameTime(long activeAtGameTime) {
        this.activeAtGameTime = activeAtGameTime;
    }

    @Override
    public void tick() {
        super.tick();
        this.noPhysics = true;

        if (level().isClientSide()) {
            for (int i = 0; i < 8; i++) {
                level().addParticle(ParticleTypes.SOUL,
                        getX() + (random.nextDouble() - 0.5D) * 0.8D,
                        getY() + random.nextDouble() * 1.8D,
                        getZ() + (random.nextDouble() - 0.5D) * 0.8D,
                        0.0D, 0.02D, 0.0D);
            }
            return;
        }
        if (!(level() instanceof ServerLevel serverLevel) || targetPlayer == null) {
            discard();
            return;
        }

        Player found = serverLevel.getPlayerByUUID(targetPlayer);
        if (!(found instanceof ServerPlayer target)) {
            discard();
            return;
        }
        if (target == null || !target.isAlive() || target.level() != level()) {
            discard();
            return;
        }

        if (level().getGameTime() < activeAtGameTime) {
            return;
        }

        Vec3 toTarget = target.position().subtract(position());
        if (toTarget.lengthSqr() <= NightmareConfig.FEAR_CHASER_CATCH_RADIUS * NightmareConfig.FEAR_CHASER_CATCH_RADIUS) {
            NightmareManager.getInstance().onFearChaserCaught(target);
            discard();
            return;
        }

        Vec3 step = toTarget.normalize().scale(NightmareConfig.FEAR_CHASER_SPEED);
        setDeltaMovement(step);
        setPos(getX() + step.x, getY() + step.y, getZ() + step.z);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("TargetPlayer")) targetPlayer = tag.getUUID("TargetPlayer");
        activeAtGameTime = tag.getLong("ActiveAtGameTime");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (targetPlayer != null) tag.putUUID("TargetPlayer", targetPlayer);
        tag.putLong("ActiveAtGameTime", activeAtGameTime);
    }
}
