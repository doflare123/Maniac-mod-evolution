package org.example.maniacrevolution.hack;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * BlockEntity для компьютера.
 *
 * Хранит:
 *   - computerId (задаётся в creative)
 *   - hackProgress (0.0 .. 1.0) — для рендеринга
 *   - isHacked (boolean)
 *
 * Синхронизируется с клиентом через getUpdatePacket() / onDataPacket().
 *
 * Регистрация в ModBlockEntities:
 *   BLOCK_ENTITIES.register("computer", () -> BlockEntityType.Builder
 *       .of(ComputerBlockEntity::new, ModBlocks.COMPUTER.get())
 *       .build(null));
 */
public class ComputerBlockEntity extends BlockEntity {

    // ── Статический трекер всех компьютеров (для команд сброса) ─────────────
    private static final java.util.Set<BlockPos> TRACKED_POSITIONS =
            java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    public static java.util.Set<BlockPos> getTrackedPositions() {
        return java.util.Collections.unmodifiableSet(TRACKED_POSITIONS);
    }

    public static void resetTrackedBlockEntities(net.minecraft.server.MinecraftServer server) {
        if (server == null) return;
        java.util.Set<BlockPos> positions;
        synchronized (TRACKED_POSITIONS) {
            positions = new java.util.HashSet<>(TRACKED_POSITIONS);
        }
        for (BlockPos pos : positions) {
            for (ServerLevel level : server.getAllLevels()) {
                if (level.getBlockEntity(pos) instanceof ComputerBlockEntity computer) {
                    computer.resetProgress();
                }
            }
        }
    }

    public static void syncTrackedBlockEntities(net.minecraft.server.MinecraftServer server) {
        if (server == null) return;
        java.util.Set<BlockPos> positions;
        synchronized (TRACKED_POSITIONS) {
            positions = new java.util.HashSet<>(TRACKED_POSITIONS);
        }
        for (BlockPos pos : positions) {
            for (ServerLevel level : server.getAllLevels()) {
                if (level.getBlockEntity(pos) instanceof ComputerBlockEntity computer) {
                    computer.applyHackManagerState();
                    computer.syncToClient();
                }
            }
        }
    }

    private int computerId = 1;
    private float hackProgress = 0f;   // 0.0 .. 1.0
    private boolean isHacked = false;

    private boolean blocked = false;

    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean b) {
        this.blocked = b;
        setChanged();
        syncToClient();
    }

    // Анимационное время (клиент)
    public float animTick = 0f;

    public ComputerBlockEntity(BlockPos pos, BlockState state) {
        super(ModHackRegistry.COMPUTER_BLOCK_ENTITY.get(), pos, state);
        registerPosition();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        TRACKED_POSITIONS.remove(worldPosition);
    }

    @Override
    public void setLevel(net.minecraft.world.level.Level level) {
        super.setLevel(level);
        registerPosition();
        applyHackManagerState();
    }

    private void registerPosition() {
        TRACKED_POSITIONS.add(worldPosition);
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
        blocked = false;
        setChanged();
        syncToClient();
    }

    // ── Синхронизация ─────────────────────────────────────────────────────────

    private void syncToClient() {
        if (level != null && !level.isClientSide()) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.getChunkSource().blockChanged(worldPosition);
            }
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
        tag.putBoolean("Blocked", blocked);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        computerId   = tag.getInt("ComputerId");
        hackProgress = tag.getFloat("HackProgress");
        isHacked     = tag.getBoolean("IsHacked");
        blocked = tag.getBoolean("Blocked");
        registerPosition();
        applyHackManagerState();
    }

    private void applyHackManagerState() {
        if (level == null || level.isClientSide() || !HackManager.isInitialized()) return;

        HackManager manager = HackManager.get();
        float pointsRequired = Math.max(0.0001f, HackConfig.HACK_POINTS_REQUIRED);
        hackProgress = Math.min(1f, Math.max(0f, manager.getProgress(computerId) / pointsRequired));
        isHacked = manager.isHacked(computerId);
        blocked = manager.isBlocked(computerId);
    }
}
