package org.example.maniacrevolution.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;
import java.util.Optional;

public class NetherSwapProjectile extends Projectile {

    public static final float SPEED = 4.0f; // Блоков за тик

    private int ownerEntityId = -1;

    public NetherSwapProjectile(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
    }

    public NetherSwapProjectile(Level level, LivingEntity shooter, Vec3 direction) {
        super(ModEntities.NETHER_SWAP_PROJECTILE.get(), level);
        this.setOwner(shooter);
        this.ownerEntityId = shooter.getId();

        // Стартуем чуть впереди от глаз стрелка
        Vec3 start = shooter.getEyePosition().add(direction.normalize().scale(1.0));
        this.setPos(start.x, start.y, start.z);

        Vec3 velocity = direction.normalize().scale(SPEED);
        this.setDeltaMovement(velocity);

        this.setYRot((float) Math.toDegrees(Math.atan2(-velocity.x, velocity.z)));
        this.setXRot((float) Math.toDegrees(Math.atan2(-velocity.y,
                Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z))));
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    public void tick() {
        // Не вызываем super.tick() — вручную контролируем движение и коллизии
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();

        if (this.tickCount > 200) {
            this.discard();
            return;
        }

        Vec3 motion = this.getDeltaMovement();
        Vec3 from = this.position();
        Vec3 to = from.add(motion);

        // Проверка коллизий только на сервере
        if (!this.level().isClientSide()) {
            Entity owner = this.getOwner();

            // Ищем игроков на пути снаряда
            AABB scanBox = new AABB(from, to).inflate(1.0);
            List<Entity> candidates = this.level().getEntities(this, scanBox,
                    e -> e != owner && e instanceof Player && !e.isSpectator());

            Entity hitEntity = null;
            double bestDist = Double.MAX_VALUE;

            for (Entity candidate : candidates) {
                AABB box = candidate.getBoundingBox().inflate(0.2);
                Optional<Vec3> intersection = box.clip(from, to);
                if (intersection.isPresent()) {
                    double dist = from.distanceTo(intersection.get());
                    if (dist < bestDist) {
                        bestDist = dist;
                        hitEntity = candidate;
                    }
                }
            }

            if (hitEntity != null) {
                performSwap(hitEntity);
                return;
            }

            // Проверка блоков
            var blockHit = this.level().clip(new net.minecraft.world.level.ClipContext(
                    from, to,
                    net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE,
                    this));

            if (blockHit.getType() == HitResult.Type.BLOCK) {
                this.discard();
                return;
            }
        }

        // Двигаем снаряд
        this.setPos(to.x, to.y, to.z);
    }

    private void performSwap(Entity hit) {
        Entity owner = this.getOwner();
        if (!(owner instanceof ServerPlayer shooter)) {
            this.discard();
            return;
        }
        if (!(hit instanceof ServerPlayer target)) {
            this.discard();
            return;
        }

        // Сохраняем позиции ДО телепортации
        Vec3 shooterPos    = shooter.position();
        float shooterYaw   = shooter.getYRot();
        float shooterPitch = shooter.getXRot();

        Vec3 targetPos    = target.position();
        float targetYaw   = target.getYRot();
        float targetPitch = target.getXRot();

        // Меняем местами
        shooter.teleportTo((ServerLevel) this.level(),
                targetPos.x, targetPos.y, targetPos.z,
                java.util.Set.of(), targetYaw, targetPitch);
        target.teleportTo((ServerLevel) this.level(),
                shooterPos.x, shooterPos.y, shooterPos.z,
                java.util.Set.of(), shooterYaw, shooterPitch);

        shooter.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 5 * 20, 0, false, false, true));

        spawnSwapEffects((ServerLevel) this.level(), shooterPos);
        spawnSwapEffects((ServerLevel) this.level(), targetPos);

        this.discard();
    }

    private void spawnSwapEffects(ServerLevel level, Vec3 pos) {
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,
                pos.x, pos.y + 1, pos.z, 40, 0.3, 0.8, 0.3, 0.3);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                pos.x, pos.y + 1, pos.z, 20, 0.2, 0.6, 0.2, 0.05);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                pos.x, pos.y + 1, pos.z, 15, 0.2, 0.5, 0.2, 0.1);
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
