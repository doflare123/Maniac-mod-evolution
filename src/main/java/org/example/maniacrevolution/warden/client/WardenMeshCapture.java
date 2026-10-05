package org.example.maniacrevolution.warden.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Records transformed renderer vertices without sending textures or vertex buffers to the GPU. */
final class WardenMeshCapture implements MultiBufferSource {
    private final List<WardenSurfaceCache.Face> faces = new ArrayList<>();
    private final Map<RenderType, Recorder> buffers = new HashMap<>();
    private final boolean yellow;
    private final boolean water;
    private final Vec3 origin;
    private final double spacing;
    private final int maxFaces, maxPoints;
    private int pointCount;
    private boolean limited;

    WardenMeshCapture(boolean yellow, double spacing, int maxFaces, int maxPoints) {
        this(yellow, spacing, maxFaces, maxPoints, false, Vec3.ZERO);
    }
    private WardenMeshCapture(boolean yellow, double spacing, int maxFaces, int maxPoints, boolean water, Vec3 origin) {
        this.yellow = yellow;
        this.water = water; this.origin = origin;
        this.spacing = spacing;
        this.maxFaces = maxFaces;
        this.maxPoints = maxPoints;
    }
    static WardenMeshCapture water(net.minecraft.core.BlockPos pos, int maxFaces, int maxPoints) {
        // LiquidBlockRenderer emits section-local positions on all three axes, including negatives.
        return new WardenMeshCapture(false, 0.25, maxFaces, maxPoints, true,
                new Vec3(pos.getX() & ~15, pos.getY() & ~15, pos.getZ() & ~15));
    }

    @Override public VertexConsumer getBuffer(RenderType type) {
        return buffers.computeIfAbsent(type, t -> new Recorder(t.mode() == VertexFormat.Mode.QUADS));
    }

    List<WardenSurfaceCache.Face> faces() { return List.copyOf(faces); }
    boolean limited() { return limited; }
    int pointCount() { return pointCount; }
    VertexConsumer quadBuffer() { return new Recorder(true); }

    private final class Recorder implements VertexConsumer {
        private final boolean quads;
        private final Vec3[] vertices = new Vec3[4];
        private Vec3 position = Vec3.ZERO, normal = Vec3.ZERO, firstNormal = Vec3.ZERO;
        private int cursor;

        Recorder(boolean quads) { this.quads = quads; }

        @Override public VertexConsumer vertex(double x, double y, double z) {
            position = new Vec3(x + origin.x, y + origin.y, z + origin.z);
            return this;
        }
        @Override public VertexConsumer normal(float x, float y, float z) { normal = new Vec3(x, y, z); return this; }
        @Override public VertexConsumer color(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer uv(float u, float v) { return this; }
        @Override public VertexConsumer overlayCoords(int u, int v) { return this; }
        @Override public VertexConsumer uv2(int u, int v) { return this; }
        @Override public void defaultColor(int r, int g, int b, int a) {}
        @Override public void unsetDefaultColor() {}

        @Override public void endVertex() {
            if (!quads || limited) return;
            if (cursor == 0) firstNormal = normal;
            vertices[cursor++] = position;
            if (cursor < 4) return;
            cursor = 0;
            for (Vec3 vertex : vertices) {
                if (!Double.isFinite(vertex.x) || !Double.isFinite(vertex.y) || !Double.isFinite(vertex.z)) return;
            }
            // Vanilla fluids supply an upward lighting normal even for side faces.
            Vec3 outward = !water && firstNormal.lengthSqr() > 0.000001 ? firstNormal.normalize()
                    : vertices[1].subtract(vertices[0]).cross(vertices[3].subtract(vertices[0])).normalize();
            if (outward.lengthSqr() < 0.000001) return;
            var face = new WardenSurfaceCache.Face(vertices[0], vertices[1], vertices[2], vertices[3],
                    outward.scale(0.003), yellow, spacing, water);
            if (faces.size() >= maxFaces || pointCount + face.points().size() > maxPoints) { limited = true; return; }
            faces.add(face);
            pointCount += face.points().size();
        }
    }
}
