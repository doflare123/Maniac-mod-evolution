package org.example.maniacrevolution.atmosphere.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/** Cache lifetime, incremental invalidation and deterministic row geometry without a running client. */
public final class AtmosphereLightIndexTest {
    public static void main(String[] args) {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        cacheChecks();
        groupingChecks();
        System.out.println("Atmosphere light index checks passed");
    }

    private static void cacheChecks() {
        var index = new AtmosphereLightIndex();
        Object section = new Object();
        long a = SectionPos.asLong(0, 12, 0), b = SectionPos.asLong(1, 12, 0);
        var first = index.get(a, section, () -> List.of(emitter(0, 200, 0, Blocks.GLOWSTONE.defaultBlockState(), 15, 0xFFFFFF)));
        for (int tick = 0; tick < 100; tick++)
            check(index.get(a, section, () -> { throw new AssertionError("Stationary/moving revisit rescanned unchanged section"); }) == first,
                    "Unchanged group objects must survive revisits");
        index.get(b, section, List::of);
        check(index.scans() == 2, "Empty sections are cached too");
        index.invalidate(new BlockPos(0, 200, 0));
        index.get(a, section, List::of);
        index.get(b, section, () -> { throw new AssertionError("Unrelated section invalidated"); });
        check(index.scans() == 3, "A block edit rebuilds only its section");
        index.get(a, new Object(), List::of);
        check(index.scans() == 4, "Replacement after chunk unload must revalidate metadata");
        index.invalidate(new ChunkPos(1, 0));
        index.get(b, section, List::of);
        check(index.scans() == 5, "Full chunk packet invalidates an existing section object");
        index.clear();
        for (int x = 0; x < AtmosphereLightIndex.MAX_SECTIONS; x++)
            index.get(SectionPos.asLong(x, 12, 0), section, List::of);
        index.get(a, section, () -> { throw new AssertionError("Recent entry vanished"); });
        index.get(SectionPos.asLong(AtmosphereLightIndex.MAX_SECTIONS, 12, 0), section, List::of);
        check(index.size() == AtmosphereLightIndex.MAX_SECTIONS, "Cache memory must remain bounded");
        index.get(a, section, () -> { throw new AssertionError("LRU evicted the recently used section"); });
        long scans = index.scans();
        index.get(b, section, List::of);
        check(index.scans() == scans + 1, "LRU must evict the least recently used section");
        index.clear();
        check(index.size() == 0 && index.scans() == 0, "World change releases all metadata");
    }

    private static void groupingChecks() {
        var glow = Blocks.GLOWSTONE.defaultBlockState();
        List<AtmosphereLightIndex.Emitter> row = new ArrayList<>();
        for (int x = -4; x < 4; x++) row.add(emitter(x, 200, 0, glow, 15, 0xFFFFFF));
        var groups = AtmosphereLightIndex.group(row);
        check(groups.size() == 2 && groups.stream().allMatch(g -> g.members().size() == 4), "Eight adjacent lamps use two slots");
        check(groups.get(0).extent().x == 1.5 && groups.get(0).extent().y == 0, "Row retains its axis and length");
        Collections.shuffle(row, new Random(7));
        check(groups.equals(AtmosphereLightIndex.group(row)), "Camera-independent groups must not depend on scan order");
        Set<BlockPos> members = new HashSet<>();
        groups.forEach(g -> g.members().forEach(e -> check(members.add(e.block()), "Lamp counted twice")));
        check(members.size() == row.size(), "Grouping must preserve every lamp");
        var separated = AtmosphereLightIndex.group(List.of(
                emitter(0, 200, 0, glow, 15, 0xFFFFFF), emitter(2, 200, 0, glow, 15, 0xFFFFFF)));
        check(separated.size() == 2, "Do not merge across a gap/wall");
        var colors = AtmosphereLightIndex.group(List.of(
                emitter(0, 200, 0, glow, 15, 0xFFFFFF), emitter(1, 200, 0, glow, 15, 0xFF0000),
                emitter(2, 200, 0, glow, 14, 0xFF0000), emitter(3, 200, 0, Blocks.SEA_LANTERN.defaultBlockState(), 14, 0xFF0000)));
        check(colors.size() == 4, "Different colors, emissions or block states stay independent");
        var vertical = AtmosphereLightIndex.group(List.of(
                emitter(0, 200, 0, glow, 15, 0xFFFFFF), emitter(0, 201, 0, glow, 15, 0xFFFFFF)));
        check(vertical.size() == 1 && vertical.get(0).extent().y == .5, "Vertical rows are supported");
        var boundary = AtmosphereLightIndex.group(List.of(
                emitter(15, 200, 0, glow, 15, 0xFFFFFF), emitter(16, 200, 0, glow, 15, 0xFFFFFF)));
        check(boundary.size() == 2, "Stable row boundaries align with sections");
    }

    private static AtmosphereLightIndex.Emitter emitter(int x, int y, int z, BlockState state, int emission, int color) {
        var block = new BlockPos(x, y, z);
        return new AtmosphereLightIndex.Emitter(block, AtmosphereLightPalette.position(block, state), emission, color, state);
    }

    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
