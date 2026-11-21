package org.example.maniacrevolution.atmosphere.client;

import foundry.veil.api.client.render.post.PostPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.atmosphere.AtmosphereRules;
import org.example.maniacrevolution.atmosphere.LightTransition;
import org.example.maniacrevolution.settings.GraphicsMode;
import org.joml.Vector4f;

import java.util.*;

/** Indexed world lights are independent of camera visibility and of the shader's current slots. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class AtmosphereLights {
    private static final class Source {
        AtmosphereLightIndex.Group group;
        final LightTransition strength = new LightTransition(1);
        List<Vec3> probes;
        Vec3 checkedEye;
        long checkedRevision = -1, probeRevision = -1;
        boolean visible;
        List<Source> touchingEmitters = List.of();

        Source(AtmosphereLightIndex.Group group) { this.group = group; }
    }

    static final int MAX_SOURCES = 64;
    // Full influence radius, row half-length and a movement/prefetch margin beyond the fog.
    private static final int SCAN_RADIUS = (int) Math.ceil(AtmosphereRules.FOG_END + 9.25 + 1.5 + 6);
    private static final AtmosphereLightIndex index = new AtmosphereLightIndex();
    private static final Map<BlockPos, Source> sources = new LinkedHashMap<>();
    private static final Set<BlockPos> previousSelection = new HashSet<>();
    private static final List<Source> selected = new ArrayList<>();
    private static final Vector4f[] positions = new Vector4f[MAX_SOURCES];
    private static final Vector4f[] colors = new Vector4f[MAX_SOURCES];
    private static final Vector4f[] extents = new Vector4f[MAX_SOURCES];
    private static LightTransition handStrength = new LightTransition();
    private static long visibilityRevision;
    private static int uploadedCount, uploadedHandCount;

    static {
        for (int i = 0; i < MAX_SOURCES; i++) {
            positions[i] = new Vector4f(); colors[i] = new Vector4f(); extents[i] = new Vector4f();
        }
    }

    private AtmosphereLights() {}

    /** Stop rendering without throwing away the world index on blindness, menus or quality changes. */
    static void suspend() {
        sources.clear(); selected.clear(); previousSelection.clear();
        handStrength = new LightTransition(); uploadedCount = uploadedHandCount = 0;
    }

    static void clear() { suspend(); index.clear(); visibilityRevision = 0; }

    /** Called after a client block change; normal packet updates and local predictions both pass here. */
    public static void blockChanged(BlockPos block, BlockState before, BlockState after) {
        if (before == after) return;
        // The lighter's temporary vanilla LIGHT has neither geometry nor an atmospheric emitter.
        if (inertAir(before) && inertAir(after)) return;
        index.invalidate(block);
        visibilityRevision++;
    }

    private static boolean inertAir(BlockState state) {
        return state.is(Blocks.LIGHT) || state.isAir() && state.getLightEmission() == 0;
    }

    @SubscribeEvent
    public static void chunkLoaded(ChunkEvent.Load event) {
        if (event.getLevel() != Minecraft.getInstance().level) return;
        // Also handles a full chunk packet replacing the contents of an existing section object.
        index.invalidate(event.getChunk().getPos());
        visibilityRevision++;
    }

    @SubscribeEvent
    public static void chunkUnloaded(ChunkEvent.Unload event) {
        if (event.getLevel() == Minecraft.getInstance().level) visibilityRevision++;
    }

    static void tick(GraphicsMode mode, boolean burningHandLight) {
        if (!mode.enabled()) { suspend(); return; }
        handStrength.tick(burningHandLight ? 1 : 0);
        var mc = Minecraft.getInstance();
        Vec3 eye = mc.player.getEyePosition();
        Set<BlockPos> found = new HashSet<>();
        for (var group : nearbyGroups(mc.player.blockPosition())) {
            double reach = AtmosphereRules.FOG_END + group.reach() + 2;
            if (group.position().distanceToSqr(eye) >= reach * reach) continue;
            Source source = sources.computeIfAbsent(group.key(), key -> new Source(group));
            if (source.group != group) {
                source.group = group;
                source.checkedEye = null;
                source.probes = null;
            }
            source.strength.snap(1);
            found.add(group.key());
        }
        for (var entry : sources.entrySet()) if (!found.contains(entry.getKey())) entry.getValue().strength.tick(0);
        sources.entrySet().removeIf(entry -> entry.getValue().strength.dark()
                || !mc.level.hasChunkAt(entry.getKey())
                || entry.getValue().group.position().distanceToSqr(eye) > SCAN_RADIUS * SCAN_RADIUS);
        select(eye);
    }

    private static List<AtmosphereLightIndex.Group> nearbyGroups(BlockPos center) {
        var level = Minecraft.getInstance().level;
        int minY = Math.max(level.getMinBuildHeight(), center.getY() - SCAN_RADIUS) >> 4;
        int maxY = Math.min(level.getMaxBuildHeight() - 1, center.getY() + SCAN_RADIUS) >> 4;
        List<AtmosphereLightIndex.Group> result = new ArrayList<>();
        for (int cx = (center.getX() - SCAN_RADIUS) >> 4; cx <= (center.getX() + SCAN_RADIUS) >> 4; cx++) {
            for (int cz = (center.getZ() - SCAN_RADIUS) >> 4; cz <= (center.getZ() + SCAN_RADIUS) >> 4; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                var chunk = level.getChunk(cx, cz);
                for (int sy = minY; sy <= maxY; sy++) {
                    var section = chunk.getSection(level.getSectionIndex(sy << 4));
                    int x = cx << 4, y = sy << 4, z = cz << 4;
                    result.addAll(index.get(SectionPos.asLong(cx, sy, cz), section,
                            () -> scanSection(section, x, y, z)));
                }
            }
        }
        return result;
    }

    private static List<AtmosphereLightIndex.Emitter> scanSection(LevelChunkSection section, int x0, int y0, int z0) {
        if (section.hasOnlyAir() || !section.maybeHas(state -> !state.is(Blocks.LIGHT)
                && state.getLightEmission() > 0)) return List.of();
        var level = Minecraft.getInstance().level;
        var cursor = new BlockPos.MutableBlockPos();
        List<AtmosphereLightIndex.Emitter> result = new ArrayList<>();
        for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) {
            BlockState state = section.getBlockState(x, y, z);
            if (state.is(Blocks.LIGHT)) continue;
            cursor.set(x0 + x, y0 + y, z0 + z);
            int emission = state.getLightEmission(level, cursor);
            if (emission > 0) result.add(new AtmosphereLightIndex.Emitter(cursor.immutable(),
                    AtmosphereLightPalette.position(cursor, state), emission, AtmosphereLightPalette.color(state), state));
        }
        return result;
    }

    private static boolean clearRay(Vec3 eye, Vec3 point, BlockPos emitter) {
        var mc = Minecraft.getInstance();
        var hit = mc.level.clip(new ClipContext(eye, point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
        return hit.getType() == HitResult.Type.MISS || emitter != null && hit.getBlockPos().equals(emitter);
    }

    /** Conservative visibility of lit space, rather than requiring the lamp itself to face the camera. */
    private static boolean influenceVisible(Source source, Vec3 eye) {
        if (source.checkedEye != null && source.checkedRevision == visibilityRevision
                && source.checkedEye.distanceToSqr(eye) < .0625) return source.visible;
        source.checkedEye = eye;
        source.checkedRevision = visibilityRevision;
        for (var member : source.group.members()) {
            if (clearRay(eye, member.position(), member.block())) return source.visible = true;
        }
        if (source.probes == null || source.probeRevision != visibilityRevision) {
            source.probes = influenceProbes(source.group);
            source.probeRevision = visibilityRevision;
        }
        for (Vec3 probe : source.probes) if (clearRay(eye, probe, null)) return source.visible = true;
        return source.visible = false;
    }

    private static List<Vec3> influenceProbes(AtmosphereLightIndex.Group group) {
        var mc = Minecraft.getInstance();
        List<Vec3> points = new ArrayList<>();
        // Endpoints preserve a partly exposed row; rays terminate before the first world obstacle.
        var ends = group.members().size() == 1 ? List.of(group.members().get(0))
                : List.of(group.members().get(0), group.members().get(group.members().size() - 1));
        for (var member : ends) for (Direction face : Direction.values()) {
            Vec3 direction = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
            Vec3 start = member.position().add(direction.scale(.55));
            if (!clearRay(member.position(), start, member.block())) continue;
            Vec3 end = member.position().add(direction.scale(group.radius() * .65));
            var hit = mc.level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
            points.add(hit.getType() == HitResult.Type.MISS ? end : hit.getLocation().subtract(direction.scale(.06)));
            points.add(start);
        }
        return points;
    }

    private static int band(double distanceSquared) { return Math.min(3, (int) (Math.sqrt(distanceSquared) / 7.5)); }

    /** Only select once per tick. A small retention bias prevents equally useful lamps swapping slots. */
    private static void select(Vec3 camera) {
        List<List<Source>> buckets = new ArrayList<>();
        for (int i = 0; i < 32; i++) buckets.add(new ArrayList<>());
        for (Source source : sources.values()) {
            Vec3 delta = source.group.position().subtract(camera);
            if (!AtmosphereRules.lightIntersectsView(delta.length(), source.group.reach()) || source.strength.dark()) continue;
            int direction = Math.min(7, (int) ((Math.atan2(delta.z, delta.x) + Math.PI) * 8 / (Math.PI * 2)));
            buckets.get(band(delta.lengthSqr()) * 8 + direction).add(source);
        }
        for (var bucket : buckets) bucket.sort(Comparator.comparingDouble(source ->
                source.group.position().distanceToSqr(camera) * (previousSelection.contains(source.group.key()) ? .8 : 1)));
        selected.clear();
        for (int round = 0; selected.size() < MAX_SOURCES; round++) {
            boolean available = false;
            for (var bucket : buckets) if (round < bucket.size()) {
                available = true;
                Source source = bucket.get(round);
                if (influenceVisible(source, camera)) selected.add(source);
                if (selected.size() == MAX_SOURCES) break;
            }
            if (!available) break;
        }
        previousSelection.clear();
        for (Source source : selected) previousSelection.add(source.group.key());
        linkTouchingEmitters();
    }

    /** A luminous block is not a screen-space occluder for its own connected light fixture. */
    private static void linkTouchingEmitters() {
        Map<BlockPos, Source> owners = new HashMap<>();
        for (Source source : selected) for (var member : source.group.members()) owners.put(member.block(), source);
        Map<Source, Set<Source>> connections = new IdentityHashMap<>();
        for (Source source : selected) {
            Set<Source> neighbors = new LinkedHashSet<>();
            for (var member : source.group.members()) for (Direction face : Direction.values()) {
                Source neighbor = owners.get(member.block().relative(face));
                if (neighbor != null && neighbor != source) neighbors.add(neighbor);
            }
            connections.put(source, neighbors);
        }
        // A 2x2 tile straddling grid boundaries can occupy four groups. Include its diagonal via connectivity.
        Set<Source> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Source root : selected) {
            if (!visited.add(root)) continue;
            List<Source> fixture = new ArrayList<>();
            fixture.add(root);
            for (int next = 0; next < fixture.size(); next++) for (Source neighbor : connections.get(fixture.get(next))) {
                if (visited.add(neighbor)) fixture.add(neighbor);
            }
            for (Source source : fixture) source.touchingEmitters = fixture.stream().filter(neighbor -> neighbor != source)
                    .sorted(Comparator.comparingDouble(neighbor -> neighbor.group.position().distanceToSqr(source.group.position())))
                    .limit(3).toList();
        }
    }

    static void upload(PostPipeline pipeline, GraphicsMode mode, Vec3 lamp, boolean anchored, float partialTick) {
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        int count = 0;
        if (mode.enabled() && lamp != null && AtmosphereRules.lightIntersectsView(lamp.distanceTo(camera), 10)
                && (!anchored || clearRay(camera, lamp, null))) {
            put(count++, lamp.subtract(camera), 10, 1.35F * handStrength.value(partialTick), 0xFFA147, Vec3.ZERO, 1);
        }
        pipeline.setInt("HandLightCount", count); uploadedHandCount = count;
        Map<Source, Integer> slots = new IdentityHashMap<>();
        if (mode.enabled()) for (Source source : selected) {
            if (count == MAX_SOURCES) break;
            slots.put(source, count);
            var group = source.group;
            put(count++, group.position().subtract(camera), group.radius(), group.emission() / 15F
                    * source.strength.value(partialTick) * group.members().size(), group.color(), group.extent(), group.members().size());
        }
        for (var entry : slots.entrySet()) {
            Source source = entry.getKey();
            // 3 count bits + three 7-bit (slot + 1) references fit exactly in a float's 24-bit integer range.
            int metadata = source.group.members().size(), shift = 3;
            for (Source neighbor : source.touchingEmitters) {
                Integer slot = slots.get(neighbor);
                if (slot == null) continue;
                metadata |= (slot + 1) << shift;
                shift += 7;
            }
            extents[entry.getValue()].w = metadata;
        }
        for (int i = count; i < MAX_SOURCES; i++) { positions[i].zero(); colors[i].zero(); extents[i].zero(); }
        pipeline.setInt("LightCount", count);
        pipeline.setInt("LightSamples", mode.lightSamples());
        pipeline.setVectors("LightPositionRadius", positions); pipeline.setVectors("LightColorPower", colors);
        pipeline.setVectors("LightExtent", extents);
        pipeline.setFloat("SurfaceLight", mode.enabled() ? 1 : 0);
        pipeline.setFloat("ContactShadows", mode.dynamicLights() ? 1 : 0);
        uploadedCount = count;
    }

    private static void put(int i, Vec3 relative, float radius, float power, int rgb, Vec3 extent, int members) {
        positions[i].set((float) relative.x, (float) relative.y, (float) relative.z, radius);
        colors[i].set((rgb >> 16 & 255) / 255F, (rgb >> 8 & 255) / 255F, (rgb & 255) / 255F, power);
        extents[i].set((float) extent.x, (float) extent.y, (float) extent.z, members);
    }

    static int sourceCount() { return uploadedCount; }
    static int handLightCount() { return uploadedHandCount; }
    static float handStrength() { return handStrength.value(1); }
    static int cachedSections() { return index.size(); }
    static long sectionScans() { return index.scans(); }
    static int groupCount() { return sources.size(); }
    static int rowSizeAt(BlockPos block) {
        for (Source source : sources.values()) if (source.group.contains(block)) return source.group.members().size();
        return 0;
    }
    static int fixtureNeighborsAt(BlockPos block) {
        for (Source source : selected) if (source.group.contains(block)) return source.touchingEmitters.size();
        return 0;
    }
    static float strengthAt(BlockPos block) {
        for (Source source : sources.values()) if (source.group.contains(block)) return source.strength.value(1);
        return 0;
    }
    static boolean uploaded(BlockPos block) {
        return selected.stream().limit(MAX_SOURCES - uploadedHandCount).anyMatch(source -> source.group.contains(block));
    }
}
