package org.example.maniacrevolution.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.example.maniacrevolution.item.NetherSwapItem;

public class NetherSwapProjectile extends Projectile {

    public static final float SPEED = 6.0f; // Блоков за тик — очень быстро

    private int ownerEntityId = -1; // Для синхронизации на клиент

    public NetherSwapProjectile(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    public NetherSwapProjectile(Level level, LivingEntity shooter, Vec3 direction) {
        super(ModEntities.NETHER_SWAP_PROJECTILE.get(), level);
        this.setOwner(shooter);
        this.ownerEntityId = shooter.getId();
        this.setPos(
                shooter.getX(),
                shooter.getEyeY() - 0.1,
                shooter.getZ()
        );
        // Нормализуем направление и умножаем на скорость
        Vec3 velocity = direction.normalize().scale(SPEED);
        this.setDeltaMovement(velocity);
        // Разворачиваем в сторону движения
        this.setYRot((float)(Math.toDegrees(Math.atan2(-velocity.x, velocity.z))));
        this.setXRot((float)(Math.toDegrees(Math.atan2(-velocity.y,
                Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z)))));
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    public void tick() {
        super.tick();
        // Удаляемся через 5 секунд если ни в кого не попали
        if (this.tickCount > 100) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (this.level().isClientSide()) return;

        Entity hit = result.getEntity();
        Entity owner = this.getOwner();

        if (!(owner instanceof ServerPlayer shooter)) {
            this.discard();
            return;
        }

        // Не телепортируемся сами с собой
        if (hit == shooter) return;
        // Только игроки
        if (!(hit instanceof ServerPlayer target)) {
            this.discard();
            return;
        }

        // Сохраняем позиции
        Vec3 shooterPos = shooter.position();
        float shooterYaw = shooter.getYRot();
        float shooterPitch = shooter.getXRot();

        Vec3 targetPos = target.position();
        float targetYaw = target.getYRot();
        float targetPitch = target.getXRot();

        // Телепортируем
        shooter.teleportTo((ServerLevel) this.level(),
                targetPos.x, targetPos.y, targetPos.z,
                java.util.Set.of(),
                targetYaw, targetPitch);
        target.teleportTo((ServerLevel) this.level(),
                shooterPos.x, shooterPos.y, shooterPos.z,
                java.util.Set.of(),
                shooterYaw, shooterPitch);

        // Спецэффекты на обеих позициях
        spawnSwapEffects((ServerLevel) this.level(), shooterPos);
        spawnSwapEffects((ServerLevel) this.level(), targetPos);

        this.discard();
    }

    @Override
    protected void onHit(HitResult result) {
        if (result.getType() == HitResult.Type.BLOCK) {
            this.discard();
        }
        super.onHit(result);
    }

    private void spawnSwapEffects(ServerLevel level, Vec3 pos) {
        // Фиолетово-синие частицы портала
        level.sendParticles(
                net.minecraft.core.particles.ParticleTypes.PORTAL,
                pos.x, pos.y + 1, pos.z,
                40, 0.3, 0.8, 0.3, 0.3
        );
        // Вспышка
        level.sendParticles(
                net.minecraft.core.particles.ParticleTypes.END_ROD,
                pos.x, pos.y + 1, pos.z,
                20, 0.2, 0.6, 0.2, 0.05
        );
        // Искры
        level.sendParticles(
                net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                pos.x, pos.y + 1, pos.z,
                15, 0.2, 0.5, 0.2, 0.1
        );
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("ownerEntityId", ownerEntityId);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        ownerEntityId = tag.getInt("ownerEntityId");
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public int getOwnerEntityId() {
        return ownerEntityId;
    }
}