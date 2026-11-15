package org.example.maniacrevolution.atmosphere.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Supplier;

/** Session-local section index. Entries retain metadata, never unloaded chunks or sections. */
final class AtmosphereLightIndex {
    static final int MAX_SECTIONS = 2048;
    static final int MAX_ROW = 4;

    record Emitter(BlockPos block, Vec3 position, int emission, int color, BlockState state) {}

    record Group(List<Emitter> members, Vec3 position, Vec3 extent, int emission, int color) {
        BlockPos key() { return members.get(0).block; }
        float radius() { return 4 + emission * .35F; }
        double reach() { return radius() + extent.length(); }
        boolean contains(BlockPos block) { return members.stream().anyMatch(member -> member.block.equals(block)); }
    }

    private static final class Entry {
        final WeakReference<Object> section;
        final List<Group> groups;
        boolean dirty;

        Entry(Object section, List<Group> groups) {
            this.section = new WeakReference<>(section);
            this.groups = groups;
        }
    }

    private final Map<Long, Entry> sections = new LinkedHashMap<>(256, .75F, true);
    private long scans;

    List<Group> get(long key, Object section, Supplier<List<Emitter>> scan) {
        Entry cached = sections.get(key);
        if (cached != null && !cached.dirty && cached.section.get() == section) return cached.groups;
        List<Group> groups = group(scan.get());
        scans++;
        sections.put(key, new Entry(section, groups));
        if (sections.size() > MAX_SECTIONS) sections.remove(sections.keySet().iterator().next());
        return groups;
    }

    void invalidate(BlockPos block) {
        Entry entry = sections.get(SectionPos.asLong(block));
        if (entry != null) entry.dirty = true;
    }

    void invalidate(ChunkPos chunk) {
        for (var entry : sections.entrySet()) {
            if (SectionPos.x(entry.getKey()) == chunk.x && SectionPos.z(entry.getKey()) == chunk.z)
                entry.getValue().dirty = true;
        }
    }

    void clear() { sections.clear(); scans = 0; }
    int size() { return sections.size(); }
    long scans() { return scans; }

    /** World-aligned four-block cells keep rows stable when the camera or scan boundary moves. */
    static List<Group> group(List<Emitter> emitters) {
        Map<BlockPos, Emitter> remaining = new HashMap<>();
        for (Emitter emitter : emitters) remaining.put(emitter.block, emitter);
        List<Emitter> ordered = new ArrayList<>(emitters);
        ordered.sort(Comparator.comparingInt((Emitter e) -> e.block.getX())
                .thenComparingInt(e -> e.block.getY()).thenComparingInt(e -> e.block.getZ()));
        List<Group> groups = new ArrayList<>();
        for (Emitter first : ordered) {
            if (!remaining.containsKey(first.block)) continue;
            List<Emitter> longest = List.of(first);
            for (Direction direction : new Direction[]{Direction.EAST, Direction.UP, Direction.SOUTH}) {
                List<Emitter> row = new ArrayList<>();
                row.add(first);
                for (int step = 1; step < MAX_ROW; step++) {
                    BlockPos block = first.block.relative(direction, step);
                    if ((block.getX() >> 2) != (first.block.getX() >> 2)
                            || (block.getY() >> 2) != (first.block.getY() >> 2)
                            || (block.getZ() >> 2) != (first.block.getZ() >> 2)) break;
                    Emitter next = remaining.get(block);
                    if (next == null || next.state != first.state || next.emission != first.emission
                            || next.color != first.color) break;
                    row.add(next);
                }
                if (row.size() > longest.size()) longest = row;
            }
            for (Emitter member : longest) remaining.remove(member.block);
            Vec3 last = longest.get(longest.size() - 1).position;
            Vec3 extent = last.subtract(first.position).scale(.5);
            groups.add(new Group(List.copyOf(longest), first.position.add(extent), extent, first.emission, first.color));
        }
        return List.copyOf(groups);
    }
}
