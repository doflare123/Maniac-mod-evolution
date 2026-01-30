package org.example.maniacrevolution.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.UUID;

public class HookEntity extends Projectile {

    private static final EntityDataAccessor<Integer> DATA_HOOKED_ENTITY =
            SynchedEntityData.defineId(HookEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_RETURNING =
            SynchedEntityData.defineId(HookEntity.class, EntityDataSerializers.BOOLEAN);

    private static final double HOOK_SPEED = 3.5;
    private static final double MAX_DISTANCE = 12.0;
    private static final double PULL_SPEED = 0.8;

    private Vec3 startPos;
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
        super(ModEntities.HOOK.get(), level);
        this.setOwner(owner);
        this.ownerUUID = owner.getUUID();
        this.startPos = owner.position();

        // Устанавливаем начальную скорость в направлении взгляда
        Vec3 lookVec = owner.getLookAngle();
        this.setDeltaMovement(lookVec.scale(HOOK_SPEED));

        // Позиция спавна перед игроком
        Vec3 spawnPos = owner.getEyePosition().add(lookVec.scale(0.5));
        this.setPos(spawnPos);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_HOOKED_ENTITY, -1);
        this.entityData.define(DATA_RETURNING, false);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            return;
        }

        // Проверяем владельца
        Player owner = getOwnerPlayer();
        if (owner == null || !owner.isAlive()) {
            this.discard();
            return;
        }

        // Проверяем захваченную цель
        if (hookedEntity != null) {
            if (!hookedEntity.isAlive() || hookedEntity.isRemoved()) {
                this.startReturning();
                return;
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
        double distanceToOwner = this.distanceToSqr(owner);
        if (isReturning && distanceToOwner < 1.0) {
            this.discard();
        }
    }

    private void handleForwardFlight(Player owner) {
        Vec3 motion = this.getDeltaMovement();
        Vec3 currentPos = this.position();
        Vec3 nextPos = currentPos.add(motion);

        // Проверяем пройденное расстояние
        distanceTraveled += motion.length();
        if (distanceTraveled > MAX_DISTANCE) {
            this.startReturning();
            return;
        }

        // Проверяем коллизию с блоками
        ClipContext clipContext = new ClipContext(
                currentPos,
                nextPos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this
        );
        BlockHitResult blockHit = this.level().clip(clipContext);

        if (blockHit.getType() != HitResult.Type.MISS) {
            // Столкновение с блоком
            this.level().playSound(null, this.blockPosition(),
                    SoundEvents.ARROW_HIT, SoundSource.PLAYERS, 0.5f, 1.0f);
            this.startReturning();
            return;
        }

        // Проверяем коллизию с сущностями
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                this.level(),
                this,
                currentPos,
                nextPos,
                this.getBoundingBox().expandTowards(motion).inflate(1.0),
                entity -> !entity.isSpectator() && entity.isAlive() && entity != owner
        );

        if (entityHit != null && entityHit.getEntity() instanceof LivingEntity target) {
            // Захватили цель!
            this.hookTarget(target);
            this.level().playSound(null, this.blockPosition(),
                    SoundEvents.CROSSBOW_HIT, SoundSource.PLAYERS, 0.5f, 1.0f);
            return;
        }

        // Движение
        this.setPos(nextPos);
    }

    private void handleReturn(Player owner) {
        Vec3 ownerPos = owner.position().add(0, owner.getEyeHeight() / 2, 0);
        Vec3 hookPos = this.position();
        Vec3 direction = ownerPos.subtract(hookPos).normalize();

        // Двигаем хук обратно
        this.setDeltaMovement(direction.scale(PULL_SPEED));
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
                this.discard();
            }
        }

        returnTimer++;
        if (returnTimer > 200) { // 10 секунд максимум на возврат
            this.discard();
        }
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