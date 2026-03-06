package org.example.maniacrevolution.hack;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * BlockEntity для компьютера.
 */
public class ComputerBlockEntity extends BlockEntity {

    // ── Статический трекер всех компьютеров (для команд сброса) ─────────────
    private static final java.util.Set<BlockPos> TRACKED_POSITIONS =
            java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    public static java.util.Set<BlockPos> getTrackedPositions() {
        return java.util.Collections.unmodifiableSet(TRACKED_POSITIONS);
    }

    private int computerId = 1;
    private float hackProgress = 0f;   // 0.0 .. 1.0
    private boolean isHacked = false;

    // Анимационное время (клиент)
    public float animTick = 0f;

    public ComputerBlockEntity(BlockPos pos, BlockState state) {
        super(ModHackRegistry.COMPUTER_BLOCK_ENTITY.get(), pos, state);
        TRACKED_POSITIONS.add(pos);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        TRACKED_POSITIONS.remove(worldPosition);
    }

    // ── Клиентский тик (анимация) ─────────────────────────────────────────────

    public static void clientTick(net.minecraft.world.level.Level level, BlockPos pos,
                                  BlockState state, ComputerBlockEntity be) {
        be.animTick++;
    }

    // ── Геттеры / сеттеры ────────────────────────────────────────────────────

    public int getComputerId() { return computerId; }
    public void setComputerId(int id) {
        this.computerId = id;
        setChanged();
        syncToClient();
    }

    public float getHackProgress() { return hackProgress; }
    public void setHackProgress(float p) {
        this.hackProgress = Math.min(1f, Math.max(0f, p));
        syncToClient();
    }

    public boolean isHacked() { return isHacked; }
    public void setHacked(boolean h) {
        this.isHacked = h;
        syncToClient();
    }

    /** Сброс прогресса (из команды /maniacrev computer reset) */
    public void resetProgress() {
        hackProgress = 0f;
        isHacked = false;
        setChanged();
        syncToClient();
    }

    // ── Синхронизация ─────────────────────────────────────────────────────────

    private void syncToClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) handleUpdateTag(tag);
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("ComputerId", computerId);
        tag.putFloat("HackProgress", hackProgress);
        tag.putBoolean("IsHacked", isHacked);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        computerId = tag.getInt("ComputerId");
        // hackProgress и isHacked НЕ читаем из NBT —
        // они будут синхронизированы из HackManager в onLoad().
        // Это предотвращает показ старого прогресса после перезагрузки мира.
        hackProgress = 0f;
        isHacked = false;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // Вызывается после загрузки чанка на сервере.
        // Синхронизируем отображаемый прогресс с HackManager.
        if (level != null && !level.isClientSide()) {
            org.example.maniacrevolution.hack.HackManager mgr =
                    org.example.maniacrevolution.hack.HackManager.get();
            float pts = mgr.getProgress(computerId);
            float max = org.example.maniacrevolution.hack.HackConfig.HACK_POINTS_REQUIRED;
            hackProgress = (max > 0) ? pts / max : 0f;
            isHacked = mgr.isHacked(computerId);
            syncToClient();
        }
    }
}