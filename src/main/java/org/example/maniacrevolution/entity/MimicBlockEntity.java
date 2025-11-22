package org.example.maniacrevolution.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.UUID;

public class MimicBlockEntity extends Entity {

    // Синхронизируемые данные - BlockState для отображения
    private static final EntityDataAccessor<BlockState> DATA_BLOCK_STATE =
            SynchedEntityData.defineId(MimicBlockEntity.class, EntityDataSerializers.BLOCK_STATE);

    @Nullable
    private UUID ownerUUID;
    @Nullable
    private Entity cachedOwner;

    private int lifetime = 7 * 20; // 7 секунд

    // Конструктор для Forge (обязательный)
    public MimicBlockEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true; // Проходит сквозь блоки
    }

    // Конструктор для создания из кода
    public MimicBlockEntity(Level level, ServerPlayer owner, BlockState blockState) {
        this(ModEntities.MIMIC_BLOCK.get(), level);
        this.ownerUUID = owner.getUUID();
        this.cachedOwner = owner;
        this.setPos(owner.getX(), owner.getY(), owner.getZ());
        this.setBlockState(blockState);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_BLOCK_STATE, Blocks.STONE.defaultBlockState());
    }

    public void setBlockState(BlockState state) {
        this.entityData.set(DATA_BLOCK_STATE, state);
    }

    public BlockState getBlockState() {
        return this.entityData.get(DATA_BLOCK_STATE);
    }

    @Override
    public void tick() {
        super.tick();

        // Уменьшаем время жизни
        if (!level().isClientSide) {
            lifetime--;
            if (lifetime <= 0) {
                this.discard();
                return;
            }
        }

        // Следуем за владельцем
        Entity owner = getOwner();
        if (owner == null || owner.isRemoved()) {
            if (!level().isClientSide) {
                this.discard();
            }
            return;
        }

        // Телепортируемся к позиции владельца
        this.setPos(owner.getX(), owner.getY(), owner.getZ());
        this.setYRot(owner.getYRot());
        this.setXRot(0); // Блок не наклоняется
    }

    @Nullable
    public Entity getOwner() {
        if (this.cachedOwner != null && !this.cachedOwner.isRemoved()) {
            return this.cachedOwner;
        }

        if (this.ownerUUID != null && this.level() instanceof ServerLevel serverLevel) {
            this.cachedOwner = serverLevel.getEntity(this.ownerUUID);
            return this.cachedOwner;
        }

        return null;
    }

    // Энтити не можно выбрать/толкнуть
    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeCollidedWith() { return false; }
    @Override public boolean isNoGravity() { return true; }

    // Сохранение/загрузка NBT
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUUID = tag.getUUID("Owner");
        }
        this.lifetime = tag.getInt("Lifetime");

        if (tag.contains("Block")) {
            Block block = BuiltInRegistries.BLOCK.get(new ResourceLocation(tag.getString("Block")));
            this.setBlockState(block.defaultBlockState());
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUUID != null) {
            tag.putUUID("Owner", this.ownerUUID);
        }
        tag.putInt("Lifetime", this.lifetime);
        tag.putString("Block", BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()).toString());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
