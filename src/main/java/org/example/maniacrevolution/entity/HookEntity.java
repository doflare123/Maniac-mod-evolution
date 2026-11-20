package org.example.maniacrevolution.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.NetworkHooks;
import org.example.maniacrevolution.effect.ModEffects;

import javax.annotation.Nullable;
import java.util.UUID;

public class HookEntity extends Projectile {

    private static final EntityDataAccessor<Integer> DATA_HOOKED_ENTITY =
            SynchedEntityData.defineId(HookEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_RETURNING =
            SynchedEntityData.defineId(HookEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_OWNER_ID =
            SynchedEntityData.defineId(HookEntity.class, EntityDataSerializers.INT);

    private static final double HOOK_SPEED = 4.5;
    private static final EntityDataAccessor<Boolean> DATA_OFF_HAND =
            SynchedEntityData.defineId(HookEntity.class, EntityDataSerializers.BOOLEAN);
    public static final double MAX_DISTANCE = 24.0;
    private static final double PULL_SPEED = 1.0;

    private Vec3 interpolationTarget;
    private int interpolationSteps;
    private double distanceTraveled = 0;
    private int returnTimer = 0;
    @Nullable
    private UUID ownerUUID;
    @Nullable
    private LivingEntity hookedEntity;

    public HookEntity(EntityType<? extends HookEntity> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    public HookEntity(Level level, Player owner) {
        this(level, owner, InteractionHand.MAIN_HAND);
    }

    public HookEntity(Level level, Player owner, InteractionHand hand) {
        super(ModEntities.HOOK.get(), level);
        this.setOwner(owner);
        this.entityData.set(DATA_OWNER_ID, owner.getId());
        this.ownerUUID = owner.getUUID();
        this.entityData.set(DATA_OFF_HAND, hand == InteractionHand.OFF_HAND);

        // Устанавливаем начальную скорость в направлении взгляда
        Vec3 lookVec = HookTrajectory.aimDirection(owner, 1, hand);
        this.setDeltaMovement(lookVec.scale(HOOK_SPEED));

        // Позиция спавна перед игроком
        Vec3 spawnPos = HookTrajectory.launchPosition(owner, 1, hand);
        this.setPos(spawnPos);
        updateFlightRotation();
        this.yRotO = getYRot();
        this.xRotO = getXRot();
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_HOOKED_ENTITY, -1);
        this.entityData.define(DATA_RETURNING, false);
        this.entityData.define(DATA_OFF_HAND, false);
        this.entityData.define(DATA_OWNER_ID, -1);
    }

    @Override
    @Nullable
    public Entity getOwner() {
        Entity owner = super.getOwner();
        return owner != null ? owner : level().getEntity(entityData.get(DATA_OWNER_ID));
    }

    public boolean isReturning() {
        return this.entityData.get(DATA_RETURNING);
    }

    public boolean isOffHand() {
        return this.entityData.get(DATA_OFF_HAND);
    }

    @Override
    public void remove(RemovalReason reason) {
        HookDeployment.finished(this);
        super.remove(reason);
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps, boolean teleport) {
        interpolationTarget = new Vec3(x, y, z);
        interpolationSteps = 2;
        setYRot(yaw);
        setXRot(pitch);
    }

    private void updateFlightRotation() {
        Vec3 direction = getDeltaMovement().scale(isReturning() ? -1 : 1);
        if (direction.lengthSqr() < 1.0E-6) return;
        setYRot((float) (Mth.atan2(direction.x, direction.z) * Mth.RAD_TO_DEG));
        setXRot((float) (Mth.atan2(direction.y, direction.horizontalDistance()) * Mth.RAD_TO_DEG));
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            if (interpolationSteps > 0 && interpolationTarget != null) {
                setPos(position().lerp(interpolationTarget, 1.0 / interpolationSteps));
                interpolationSteps--;
            }
            return;
        }

        // Проверяем владельца
        Player owner = getOwnerPlayer();
        if (owner == null || !owner.isAlive() || owner.level() != level()) {
            this.discard();
            return;
        }
        this.entityData.set(DATA_OWNER_ID, owner.getId());

        // Проверяем захваченную цель
        if (hookedEntity != null) {
            if (!hookedEntity.isAlive() || hookedEntity.isRemoved()) {
                hookedEntity = null;
                this.entityData.set(DATA_HOOKED_ENTITY, -1);
                this.startReturning();
            }
        }

        boolean isReturning = this.entityData.get(DATA_RETURNING);

        if (!isReturning) {
            // Фаза полёта вперёд
            handleForwardFlight(owner);
        } else {
            // Фаза возврата
            handleReturn(owner);
        }

        // Проверяем дистанцию до владельца
        updateFlightRotation();
        double distanceToOwner = this.distanceToSqr(owner);
        if (isReturning && distanceToOwner < 1.0) {
            finishReturn();
        }
    }

    private void handleForwardFlight(Player owner) {
        Vec3 motion = this.getDeltaMovement();
        double remainingDistance = MAX_DISTANCE - distanceTraveled;
        if (remainingDistance <= 0) {
            this.startReturning();
            return;
        }
        if (motion.length() > remainingDistance) motion = motion.normalize().scale(remainingDistance);
        Vec3 currentPos = this.position();
        Vec3 nextPos = currentPos.add(motion);

        // Проверяем пройденное расстояние
        distanceTraveled += motion.length();

        HitResult hit = HookTrajectory.trace(this, owner, currentPos, nextPos);
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target) {
            // Захватили цель!
            this.setPos(entityHit.getLocation());
            this.hookTarget(target);
            this.level().playSound(null, this.blockPosition(),
                    SoundEvents.CROSSBOW_HIT, SoundSource.PLAYERS, 0.5f, 1.0f);
            return;
        }

        // Движение
        if (hit.getType() == HitResult.Type.BLOCK) {
            this.setPos(hit.getLocation());
            this.level().playSound(null, this.blockPosition(),
                    SoundEvents.ARROW_HIT, SoundSource.PLAYERS, 0.5f, 0.7f);
            this.startReturning();
            return;
        }
        this.setPos(nextPos);
        if (distanceTraveled >= MAX_DISTANCE) this.startReturning();
    }

    private void handleReturn(Player owner) {
        Vec3 ownerPos = owner.position().add(0, owner.getEyeHeight() / 2, 0);
        Vec3 hookPos = this.position();
        Vec3 direction = ownerPos.subtract(hookPos).normalize();

        // Двигаем хук обратно
        this.setDeltaMovement(direction.scale(Math.min(PULL_SPEED, ownerPos.distanceTo(hookPos))));
        Vec3 nextPos = hookPos.add(this.getDeltaMovement());
        this.setPos(nextPos);

        // Если есть захваченная цель - притягиваем её
        if (hookedEntity != null && hookedEntity.isAlive()) {
            Vec3 targetPos = hookedEntity.position();
            Vec3 pullDirection = ownerPos.subtract(targetPos).normalize();

            // Притягиваем цель к владельцу
            hookedEntity.setDeltaMovement(pullDirection.scale(PULL_SPEED));
            hookedEntity.hurtMarked = true;

            // Проверяем достижение владельца
            if (hookedEntity.distanceToSqr(owner) < 2.0) {
                finishReturn();
            }
        }

        returnTimer++;
        if (returnTimer > 200) { // 10 секунд максимум на возврат
            this.discard();
        }
    }

    private void finishReturn() {
        if (isRemoved()) return;
        if (hookedEntity instanceof Player player && player.isAlive()) {
            // Displayed level = amplifier + 1: level 20 is exactly -20% movement speed.
            player.addEffect(new MobEffectInstance(ModEffects.SLOWDOWN.get(), 20, 19));
        }
        discard();
    }

    private void hookTarget(LivingEntity target) {
        this.hookedEntity = target;
        this.entityData.set(DATA_HOOKED_ENTITY, target.getId());
        this.startReturning();

        // Эффект при захвате
        target.hurt(this.damageSources().thrown(this, this.getOwner()), 0.5f);
    }

    private void startReturning() {
        this.entityData.set(DATA_RETURNING, true);
        this.setDeltaMovement(Vec3.ZERO);
    }

    @Nullable
    private Player getOwnerPlayer() {
        Entity owner = this.getOwner();
        if (owner instanceof Player player) {
            return player;
        }

        if (ownerUUID != null && this.level().getServer() != null) {
            return this.level().getServer().getPlayerList().getPlayer(ownerUUID);
        }

        return null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.ownerUUID != null) {
            tag.putUUID("Owner", this.ownerUUID);
        }
        tag.putDouble("DistanceTraveled", this.distanceTraveled);
        tag.putBoolean("Returning", this.entityData.get(DATA_RETURNING));
        tag.putBoolean("OffHand", isOffHand());
        if (this.hookedEntity != null) {
            tag.putUUID("HookedEntity", this.hookedEntity.getUUID());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) {
            this.ownerUUID = tag.getUUID("Owner");
        }
        this.distanceTraveled = tag.getDouble("DistanceTraveled");
        this.entityData.set(DATA_RETURNING, tag.getBoolean("Returning"));
        this.entityData.set(DATA_OFF_HAND, tag.getBoolean("OffHand"));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected void onHit(HitResult result) {
        // Не нужно, обрабатываем коллизии в tick()
    }
}
