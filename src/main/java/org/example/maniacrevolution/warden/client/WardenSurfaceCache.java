package org.example.maniacrevolution.warden.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.data.ModelData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/** Bounded, rolling cache of actual baked model faces. Never scans the world during rendering. */
final class WardenSurfaceCache {
    static final int RADIUS = 12;
    private final int radius, side, volume, blocksPerTick;
    private static final Map<Integer, int[]> SCAN_ORDERS = new HashMap<>();
    private final int[] scanOrder;
    private final int maxFaces, maxPoints;
    WardenSurfaceCache() { this(RADIUS, 2048); }
    WardenSurfaceCache(int radius, int blocksPerTick) {
        this(radius, blocksPerTick, 12000, 120000);
    }
    WardenSurfaceCache(int radius, int blocksPerTick, int maxFaces, int maxPoints) {
        if (radius < 1 || radius > 20 || blocksPerTick < 1 || blocksPerTick > 8192) throw new IllegalArgumentException("cache budget");
        if (maxFaces < 1 || maxFaces > 12000 || maxPoints < 1 || maxPoints > 120000) throw new IllegalArgumentException("geometry budget");
        this.maxFaces = maxFaces; this.maxPoints = maxPoints;
        this.radius = radius; this.side = radius * 2 + 1; this.volume = side * side * side; this.blocksPerTick = blocksPerTick;
        scanOrder = SCAN_ORDERS.computeIfAbsent(radius, r -> java.util.stream.IntStream.range(0, volume).boxed()
                .sorted(java.util.Comparator.comparingInt(i -> {
                    int x = i % side - radius, y = i / (side * side) - radius, z = i / side % side - radius;
                    return x * x + y * y + z * z;
                })).mapToInt(Integer::intValue).toArray());
    }
    private final Map<BlockPos, Entry> entries = new HashMap<>();
    private final Set<BlockPos> specialPositions = new HashSet<>();
    private BlockPos center;
    private int cursor;
    private int visited;
    private int faceCount;
    private int pointCount;
    private boolean limited;
    private long revision;
    private long flattenedRevision = -1;
    private List<Face> flattened = List.of();

    record Face(Vec3 a, Vec3 b, Vec3 c, Vec3 d, Vec3 normal, boolean yellow, List<Vec3> points,
                net.minecraft.world.phys.AABB bounds, boolean water) {
        Face(Vec3 a, Vec3 b, Vec3 c, Vec3 d, Vec3 normal, boolean yellow) {
            this(a, b, c, d, normal, yellow, 0.25);
        }
        Face(Vec3 a, Vec3 b, Vec3 c, Vec3 d, Vec3 normal, boolean yellow, double spacing) {
            this(a, b, c, d, normal, yellow, spacing, false);
        }
        Face(Vec3 a, Vec3 b, Vec3 c, Vec3 d, Vec3 normal, boolean yellow, double spacing, boolean water) {
            this(a, b, c, d, normal, yellow, water ? WardenWaterPattern.samples(a, b, c, d, normal) : sample(a, b, c, d, normal, spacing),
                    new net.minecraft.world.phys.AABB(
                            Math.min(Math.min(a.x, b.x), Math.min(c.x, d.x)) - 0.04,
                            Math.min(Math.min(a.y, b.y), Math.min(c.y, d.y)) - 0.04,
                            Math.min(Math.min(a.z, b.z), Math.min(c.z, d.z)) - 0.04,
                            Math.max(Math.max(a.x, b.x), Math.max(c.x, d.x)) + 0.04,
                            Math.max(Math.max(a.y, b.y), Math.max(c.y, d.y)) + 0.04,
                            Math.max(Math.max(a.z, b.z), Math.max(c.z, d.z)) + 0.04), water);
        }
        private static List<Vec3> sample(Vec3 a, Vec3 b, Vec3 c, Vec3 d, Vec3 normal, double spacing) {
            List<Vec3> points = new ArrayList<>();
            int nu = Math.min(16, Math.max(1, (int) Math.ceil(Math.max(a.distanceTo(b), c.distanceTo(d)) / spacing)));
            int nv = Math.min(16, Math.max(1, (int) Math.ceil(Math.max(a.distanceTo(d), b.distanceTo(c)) / spacing)));
            for (int i = 0; i < nu; i++) for (int j = 0; j < nv; j++) {
                double u = (i + 0.5) / nu, v = (j + 0.5) / nv;
                double wa = (1 - u) * (1 - v), wb = u * (1 - v), wc = u * v, wd = (1 - u) * v;
                points.add(new Vec3(a.x * wa + b.x * wb + c.x * wc + d.x * wd + normal.x,
                        a.y * wa + b.y * wb + c.y * wc + d.y * wd + normal.y,
                        a.z * wa + b.z * wb + c.z * wc + d.z * wd + normal.z));
            }
            return List.copyOf(points);
        }
    }

    private record Entry(BlockState state, int neighbors, List<Face> faces) {}

    void start(BlockPos origin) {
        clear();
        center = origin.immutable();
    }

    void clear() {
        entries.clear();
        specialPositions.clear();
        center = null;
        cursor = visited = faceCount = 0;
        pointCount = 0;
        limited = false;
        revision++;
        flattened = List.of();
        flattenedRevision = -1;
    }

    // Budget saturation keeps a partial point cache; native world depth still provides full occlusion.
    boolean ready() { return visited >= volume; }
    boolean limited() { return limited; }
    int faceCount() { return faceCount; }
    int pointCount() { return pointCount; }
    BlockPos center() { return center; }
    Iterable<BlockPos> specialPositions() { return specialPositions; }
    long revision() { return revision; }

    List<Face> flatFaces() {
        if (flattenedRevision != revision) {
            var result = new ArrayList<Face>(faceCount);
            for (var entry : entries.values()) result.addAll(entry.faces);
            flattened = List.copyOf(result);
            flattenedRevision = revision;
        }
        return flattened;
    }

    Iterable<List<Face>> faces() {
        return () -> entries.values().stream().map(Entry::faces).iterator();
    }

    BlockPos scanOffset(int index) {
        int slot = scanOrder[index];
        return new BlockPos(slot % side - radius, slot / (side * side) - radius, slot / side % side - radius);
    }

    void tick(Minecraft mc) {
        if (center == null || mc.level == null) return;
        for (int i = 0; i < blocksPerTick; i++) {
            int index = cursor;
            cursor = (cursor + 1) % volume;
            visited = Math.min(volume, visited + 1);
            // Dense rooms cannot spend the whole point budget on distant lower floors first.
            BlockPos pos = center.offset(scanOffset(index));
            if (!mc.level.hasChunkAt(pos)) {
                remove(pos);
                specialPositions.remove(pos);
                continue;
            }
            BlockState state = mc.level.getBlockState(pos);
            if (state.hasBlockEntity()) specialPositions.add(pos); else specialPositions.remove(pos);
            boolean water = state.getFluidState().is(net.minecraft.tags.FluidTags.WATER);
            if (state.getRenderShape() != RenderShape.MODEL && !water) {
                remove(pos);
                continue;
            }
            int neighbors = 1;
            for (Direction direction : Direction.values()) {
                neighbors = 31 * neighbors + mc.level.getBlockState(pos.relative(direction)).hashCode();
            }
            // Fluid corner heights depend on diagonal neighbors and water above them as well.
            if (water) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++)
                for (int y = 0; y <= 1; y++) neighbors = 31 * neighbors + mc.level.getBlockState(pos.offset(x, y, z)).hashCode();
            Entry old = entries.get(pos);
            if (old != null && old.state == state && old.neighbors == neighbors) continue;
            remove(pos);
            if (faceCount >= maxFaces) { limited = true; continue; }
            List<Face> faces = new ArrayList<>();
            int newPointCount = 0;
            var model = state.getRenderShape() == RenderShape.MODEL ? mc.getBlockRenderer().getBlockModel(state) : null;
            var random = RandomSource.create();
            boolean yellow = WardenVisionClassification.isInteractive(state, mc.level, pos);
            Vec3 offset = Vec3.atLowerCornerOf(pos).add(state.getOffset(mc.level, pos));
            // Include unculled faces and only the visible neighbor-facing faces, as vanilla does.
            for (int side = -1; model != null && side < 6; side++) {
                Direction direction = side < 0 ? null : Direction.values()[side];
                if (direction != null && !Block.shouldRenderFace(state, mc.level, pos, direction, pos.relative(direction))) continue;
                random.setSeed(state.getSeed(pos));
                for (var quad : model.getQuads(state, direction, random, ModelData.EMPTY, null)) {
                    if (faceCount + faces.size() >= maxFaces) { limited = true; break; }
                    int[] data = quad.getVertices();
                    int stride = data.length / 4;
                    Vec3[] vertices = new Vec3[4];
                    for (int vertex = 0; vertex < 4; vertex++) {
                        int base = vertex * stride;
                        vertices[vertex] = new Vec3(Float.intBitsToFloat(data[base]),
                                Float.intBitsToFloat(data[base + 1]), Float.intBitsToFloat(data[base + 2])).add(offset);
                    }
                    Vec3 normal = Vec3.atLowerCornerOf(quad.getDirection().getNormal()).scale(0.003);
                    Face face = new Face(vertices[0], vertices[1], vertices[2], vertices[3], normal, yellow);
                    if (pointCount + newPointCount + face.points.size() > maxPoints) { limited = true; break; }
                    faces.add(face);
                    newPointCount += face.points.size();
                }
            }
            if (water && faceCount + faces.size() < maxFaces && pointCount + newPointCount < maxPoints) {
                var liquid = WardenMeshCapture.water(pos, maxFaces - faceCount - faces.size(), maxPoints - pointCount - newPointCount);
                mc.getBlockRenderer().renderLiquid(pos, mc.level, liquid.quadBuffer(), state, state.getFluidState());
                faces.addAll(liquid.faces()); newPointCount += liquid.pointCount();
                if (liquid.limited()) limited = true;
            }
            // Remember fully occluded models too: the warm cache must not rebuild them every tick.
            entries.put(pos, new Entry(state, neighbors, List.copyOf(faces)));
            if (!faces.isEmpty()) revision++;
            faceCount += faces.size();
            pointCount += newPointCount;
        }
    }

    private void remove(BlockPos pos) {
        Entry old = entries.remove(pos);
        if (old != null && !old.faces.isEmpty()) revision++;
        if (old != null) faceCount -= old.faces.size();
        if (old != null) for (Face face : old.faces) pointCount -= face.points.size();
    }
}
