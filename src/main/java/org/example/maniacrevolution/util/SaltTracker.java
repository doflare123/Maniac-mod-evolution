package org.example.maniacrevolution.util;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class SaltTracker extends SavedData {
    private static final String DATA_NAME = "maniac_revolution_salt_tracker";
    private final Set<BlockPos> saltPositions = new HashSet<>();

    public SaltTracker() {
    }

    public static SaltTracker load(CompoundTag tag) {
        SaltTracker tracker = new SaltTracker();
        ListTag list = tag.getList("SaltPositions", Tag.TAG_COMPOUND);

        for (int i = 0; i < list.size(); i++) {
            CompoundTag posTag = list.getCompound(i);
            BlockPos pos = new BlockPos(
                    posTag.getInt("x"),
                    posTag.getInt("y"),
                    posTag.getInt("z")
            );
            tracker.saltPositions.add(pos);
        }

        return tracker;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        ListTag list = new ListTag();

        for (BlockPos pos : saltPositions) {
            CompoundTag posTag = new CompoundTag();
            posTag.putInt("x", pos.getX());
            posTag.putInt("y", pos.getY());
            posTag.putInt("z", pos.getZ());
            list.add(posTag);
        }

        tag.put("SaltPositions", list);
        return tag;
    }

    public static SaltTracker get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                SaltTracker::load,
                SaltTracker::new,
                DATA_NAME
        );
    }

    public void addSaltPosition(BlockPos pos) {
        saltPositions.add(pos.immutable());
        setDirty();
    }

    public void removeSaltPosition(BlockPos pos) {
        saltPositions.remove(pos);
        setDirty();
    }

    public Set<BlockPos> getSaltPositions() {
        return new HashSet<>(saltPositions);
    }

    public void clearAllSalt(Level level) {
        for (BlockPos pos : new HashSet<>(saltPositions)) {
            if (level.getBlockState(pos).getBlock() instanceof org.example.maniacrevolution.block.SaltBlock) {
                level.removeBlock(pos, false);
            }
            saltPositions.remove(pos);
        }
        setDirty();
    }
}