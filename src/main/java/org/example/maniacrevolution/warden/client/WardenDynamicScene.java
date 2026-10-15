package org.example.maniacrevolution.warden.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Captures the actual animated renderer geometry once per world frame. */
final class WardenDynamicScene {
    private final double range;
    WardenDynamicScene() { this(org.example.maniacrevolution.warden.WardenMatchRules.VISION_RANGE); }
    WardenDynamicScene(double range) { this.range = range; }
    private List<WardenSurfaceCache.Face> blocks = List.of(), creatures = List.of();
    record CreatureMesh(List<WardenSurfaceCache.Face> faces, WardenVisionClassification.CreatureTint tint) {}
    private List<CreatureMesh> creatureMeshes = List.of();
    private boolean capturing, limited;
    private boolean reportedFailure;
    private long lastCapture = Long.MIN_VALUE, revision;

    boolean capturing() { return capturing; }
    boolean limited() { return limited; }
    List<WardenSurfaceCache.Face> blocks() { return blocks; }
    List<WardenSurfaceCache.Face> creatures() { return creatures; }
    List<CreatureMesh> creatureMeshes() { return creatureMeshes; }
    long revision() { return revision; }
    void clear() { blocks = creatures = List.of(); creatureMeshes = List.of(); limited = false; lastCapture = Long.MIN_VALUE; revision++; }

    void capture(Minecraft mc, WardenSurfaceCache surfaces, float partialTick) {
        long now = System.nanoTime();
        if (!WardenRenderBudget.captureDue(now, lastCapture)) return;
        clear();
        lastCapture = now;
        var blockFaces = new ArrayList<WardenSurfaceCache.Face>();
        var creatureFaces = new ArrayList<WardenSurfaceCache.Face>();
        var colouredCreatures = new ArrayList<CreatureMesh>();
        capturing = true;
        try {
            int count = 0;
            int pointCount = 0;
            var center = mc.player.position();
            var positions = java.util.stream.StreamSupport.stream(surfaces.specialPositions().spliterator(), false)
                    .sorted(java.util.Comparator.comparingDouble(p -> Vec3.atCenterOf(p).distanceToSqr(center))).toList();
            for (var pos : positions) {
                var entity = mc.level.getBlockEntity(pos);
                if (entity == null) continue;
                if (++count > 128) { limited = true; break; }
                var mesh = new WardenMeshCapture(WardenVisionClassification.isInteractive(
                        entity.getBlockState(), mc.level, pos), 0.25, 512, 8192);
                var pose = new PoseStack();
                pose.translate(pos.getX(), pos.getY(), pos.getZ());
                try { mc.getBlockEntityRenderDispatcher().render(entity, partialTick, pose, mesh); }
                catch (RuntimeException failure) { reportFailure(failure); continue; }
                blockFaces.addAll(mesh.faces());
                pointCount += mesh.pointCount();
                if (mesh.limited()) limited = true;
                if (blockFaces.size() > 6000 || pointCount > 60000) { limited = true; break; }
            }
            count = 0;
            pointCount = 0;
            var candidates = new ArrayList<LivingEntity>();
            for (var entity : mc.level.entitiesForRendering()) {
                if (entity instanceof LivingEntity living && living.isAlive() && !living.isInvisible()
                        && !living.isSpectator() && !(living == mc.player && mc.options.getCameraType().isFirstPerson())
                        && living.position().distanceToSqr(center) <= range * range) candidates.add(living);
            }
            // Decorative mobs must not exhaust capture before the actual players are considered.
            candidates.sort(java.util.Comparator.comparingInt((LivingEntity e) -> e instanceof Player ? 0 : 1)
                    .thenComparingDouble(e -> e.position().distanceToSqr(center)));
            for (var living : candidates) {
                var tint = WardenVisionClassification.creatureTint(living instanceof Player,
                        living.getTeam() == null ? null : living.getTeam().getName(),
                        WardenVisionClassification.moving(living.getX() - living.xo,
                                living.getY() - living.yo, living.getZ() - living.zo), living.isShiftKeyDown(),
                        living.position().distanceToSqr(center));
                if (++count > 32) { limited = true; break; }
                var mesh = new WardenMeshCapture(false, 0.12, 1024, 12000);
                EntityRenderer<? super LivingEntity> renderer = mc.getEntityRenderDispatcher().getRenderer(living);
                var offset = renderer.getRenderOffset(living, partialTick);
                var pose = new PoseStack();
                pose.translate(Mth.lerp(partialTick, living.xOld, living.getX()) + offset.x,
                        Mth.lerp(partialTick, living.yOld, living.getY()) + offset.y,
                        Mth.lerp(partialTick, living.zOld, living.getZ()) + offset.z);
                try { renderer.render(living, Mth.rotLerp(partialTick, living.yRotO, living.getYRot()),
                        partialTick, pose, mesh, 0xF000F0); }
                catch (RuntimeException failure) { reportFailure(failure); continue; }
                var faces = mesh.faces();
                creatureFaces.addAll(faces);
                colouredCreatures.add(new CreatureMesh(faces, tint));
                pointCount += mesh.pointCount();
                if (mesh.limited()) limited = true;
                if (creatureFaces.size() > 6000 || pointCount > 60000) { limited = true; break; }
            }
            blocks = List.copyOf(blockFaces);
            creatures = List.copyOf(creatureFaces);
            creatureMeshes = List.copyOf(colouredCreatures);
        } catch (RuntimeException failure) {
            // Native depth still occludes all points, even if a custom renderer cannot be captured.
            limited = true;
            if (!reportedFailure) {
                org.example.maniacrevolution.Maniacrev.LOGGER.warn("Warden geometry capture failed; native world occlusion retained", failure);
                reportedFailure = true;
            }
        } finally {
            capturing = false;
            WardenVisionPerformance.record(WardenVisionPerformance.Stage.CAPTURE, now);
        }
    }

    private void reportFailure(RuntimeException failure) {
        limited = true;
        if (!reportedFailure) {
            org.example.maniacrevolution.Maniacrev.LOGGER.warn("Warden skipped an unsupported geometry renderer", failure);
            reportedFailure = true;
        }
    }

}
