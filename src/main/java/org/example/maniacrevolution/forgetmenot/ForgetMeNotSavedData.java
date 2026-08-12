package org.example.maniacrevolution.forgetmenot;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Постоянное серверное состояние личных незабудок в текущем матче. */
public final class ForgetMeNotSavedData extends SavedData {
    private static final String DATA_NAME = "maniacrev_forget_me_not";
    private static final String TAG_FLOWERS = "Flowers";

    private final Map<UUID, FlowerRecord> records = new LinkedHashMap<>();

    public static ForgetMeNotSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                ForgetMeNotSavedData::load,
                ForgetMeNotSavedData::new,
                DATA_NAME
        );
    }

    public static ForgetMeNotSavedData load(CompoundTag tag) {
        ForgetMeNotSavedData data = new ForgetMeNotSavedData();
        ListTag list = tag.getList(TAG_FLOWERS, Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag entryTag = list.getCompound(index);
            if (!entryTag.hasUUID("Owner")) continue;

            UUID owner = entryTag.getUUID("Owner");
            FlowerStatus status;
            try {
                status = FlowerStatus.valueOf(entryTag.getString("Status"));
            } catch (IllegalArgumentException ignored) {
                status = FlowerStatus.DESTROYED;
            }

            ResourceLocation dimensionId = ResourceLocation.tryParse(entryTag.getString("Dimension"));
            ResourceKey<Level> dimension = dimensionId == null
                    ? Level.OVERWORLD
                    : ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimensionId);
            BlockPos pos = new BlockPos(
                    entryTag.getInt("X"),
                    entryTag.getInt("Y"),
                    entryTag.getInt("Z")
            );
            UUID entityId = entryTag.hasUUID("Entity") ? entryTag.getUUID("Entity") : null;
            data.records.put(owner, new FlowerRecord(status, dimension, pos, entityId));
        }
        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, FlowerRecord> entry : records.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putUUID("Owner", entry.getKey());
            FlowerRecord record = entry.getValue();
            entryTag.putString("Status", record.status().name());
            entryTag.putString("Dimension", record.dimension().location().toString());
            entryTag.putInt("X", record.pos().getX());
            entryTag.putInt("Y", record.pos().getY());
            entryTag.putInt("Z", record.pos().getZ());
            if (record.entityId() != null) entryTag.putUUID("Entity", record.entityId());
            list.add(entryTag);
        }
        tag.put(TAG_FLOWERS, list);
        return tag;
    }

    public FlowerRecord getRecord(UUID owner) {
        return records.get(owner);
    }

    public List<Map.Entry<UUID, FlowerRecord>> getRecords() {
        return new ArrayList<>(records.entrySet());
    }

    public void setInInventory(UUID owner) {
        records.put(owner, new FlowerRecord(
                FlowerStatus.INVENTORY, Level.OVERWORLD, BlockPos.ZERO, null));
        setDirty();
    }

    public void setPlaced(UUID owner, ResourceKey<Level> dimension, BlockPos pos, UUID entityId) {
        records.put(owner, new FlowerRecord(
                FlowerStatus.PLACED, dimension, pos.immutable(), entityId));
        setDirty();
    }

    public void setDestroyed(UUID owner) {
        FlowerRecord previous = records.get(owner);
        ResourceKey<Level> dimension = previous == null ? Level.OVERWORLD : previous.dimension();
        BlockPos pos = previous == null ? BlockPos.ZERO : previous.pos();
        records.put(owner, new FlowerRecord(FlowerStatus.DESTROYED, dimension, pos, null));
        setDirty();
    }

    public void clear() {
        records.clear();
        setDirty();
    }

    public enum FlowerStatus {
        INVENTORY,
        PLACED,
        DESTROYED
    }

    public record FlowerRecord(FlowerStatus status, ResourceKey<Level> dimension,
                               BlockPos pos, UUID entityId) {
    }
}
