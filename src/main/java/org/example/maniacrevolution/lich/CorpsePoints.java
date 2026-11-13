package org.example.maniacrevolution.lich;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** World-owned editor data. Match cleanup never removes authored points. */
public final class CorpsePoints extends SavedData {
    public record Point(UUID id, String map, String dimension, BlockPos pos) {}
    public final List<Point> points = new ArrayList<>();
    public static CorpsePoints get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(CorpsePoints::load, CorpsePoints::new, "maniacrev_lich_points");
    }
    private static CorpsePoints load(CompoundTag tag) {
        var data = new CorpsePoints();
        for (var value : tag.getList("points", Tag.TAG_COMPOUND)) {
            var p = (CompoundTag) value;
            data.points.add(new Point(p.getUUID("id"), p.getString("map"), p.getString("dim"), BlockPos.of(p.getLong("pos"))));
        }
        return data;
    }
    public boolean toggle(String map, String dim, BlockPos pos) {
        boolean added=!points.removeIf(p -> p.map.equals(map) && p.dimension.equals(dim) && p.pos.equals(pos));
        if(added)points.add(new Point(UUID.randomUUID(), map, dim, pos.immutable()));
        setDirty();
        return added;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        var list = new ListTag();
        for (var p : points) {
            var row = new CompoundTag(); row.putUUID("id", p.id); row.putString("map", p.map);
            row.putString("dim", p.dimension); row.putLong("pos", p.pos.asLong()); list.add(row);
        }
        tag.put("points", list); return tag;
    }
}
