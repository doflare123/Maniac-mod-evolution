package org.example.maniacrevolution.warden.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/** Geometry checks without an OpenGL context or a running client. */
final class WardenWaterPatternTest {
    static void run() {
        var flat = new WardenSurfaceCache.Face(new Vec3(0, 0.89, 0), new Vec3(0, 0.89, 1),
                new Vec3(1, 0.89, 1), new Vec3(1, 0.89, 0), new Vec3(0, 0.003, 0), false, 0.25, true);
        var slope = new WardenSurfaceCache.Face(new Vec3(0, 0.89, 0), new Vec3(0, 0.49, 1),
                new Vec3(1, 0.49, 1), new Vec3(1, 0.89, 0), new Vec3(0, 0.003, 0), false, 0.25, true);
        var fall = new WardenSurfaceCache.Face(new Vec3(0, 1, 0), new Vec3(1, 1, 0),
                new Vec3(1, 0, 0), new Vec3(0, 0, 0), new Vec3(0, 0, -0.003), false, 0.25, true);
        check(flat.water() && !flat.yellow() && flat.points().size() == WardenWaterPattern.COUNT, "water uses bounded ripple sampling instead of ordinary dots");
        check(!WardenWaterPattern.ribbon(flat, 0, 0).equals(WardenWaterPattern.ribbon(flat, 0, 20)), "ripple moves over time without rescanning the world");
        check(WardenWaterPattern.RED > WardenWaterPattern.GREEN && WardenWaterPattern.GREEN > WardenWaterPattern.BLUE,
                "copper water differs from blue survivors and yellow interactions");
        for (var face : new WardenSurfaceCache.Face[]{flat, slope, fall}) for (int time = 0; time < 80; time++) {
            for (int i = 0; i < WardenWaterPattern.COUNT; i++) {
                var ribbon = WardenWaterPattern.ribbon(face, i, time);
                for (var vertex : new Vec3[]{ribbon.a(), ribbon.b(), ribbon.c(), ribbon.d()}) {
                    check(Double.isFinite(vertex.x) && Double.isFinite(vertex.y) && Double.isFinite(vertex.z)
                            && face.bounds().contains(vertex), "ripples stay inside the cached fluid bounds");
                    if (face == flat) check(Math.abs(vertex.y - 0.893) < 1.0e-9, "flat water keeps its actual surface height");
                    if (face == slope) check(Math.abs(vertex.y - (0.893 - 0.4 * vertex.z)) < 1.0e-9, "flowing-water ripple follows the slope");
                    if (face == fall) check(Math.abs(vertex.z + 0.003) < 1.0e-9, "waterfall ripple remains on the vertical face");
                }
                check(ribbon.b().subtract(ribbon.a()).cross(ribbon.d().subtract(ribbon.a())).lengthSqr() > 0,
                        "water lines have visible nondegenerate area");
            }
        }
        var capture = WardenMeshCapture.water(new BlockPos(-1, 33, -17), 1, WardenWaterPattern.COUNT);
        var buffer = capture.quadBuffer();
        emit(buffer, 15, 1.89, 15); emit(buffer, 15, 1.89, 16); emit(buffer, 16, 1.89, 16); emit(buffer, 16, 1.89, 15);
        var worldFace = capture.faces().get(0);
        check(worldFace.water() && worldFace.a().distanceToSqr(new Vec3(-1, 33.89, -17)) < 1.0e-10,
                "native section-local water vertices become correct world positions, including negatives");
        emit(buffer, 15, 1.89, 15); emit(buffer, 15, 1.89, 16); emit(buffer, 16, 1.89, 16); emit(buffer, 16, 1.89, 15);
        check(capture.limited() && capture.faces().size() == 1 && capture.pointCount() == WardenWaterPattern.COUNT,
                "water shares the existing mesh budgets");
        var side = WardenMeshCapture.water(BlockPos.ZERO, 1, 16); var sideBuffer = side.quadBuffer();
        emit(sideBuffer, 0, 1, 0); emit(sideBuffer, 1, 1, 0); emit(sideBuffer, 1, 0, 0); emit(sideBuffer, 0, 0, 0);
        check(side.faces().get(0).normal().z < 0 && side.faces().get(0).normal().y == 0,
                "fluid side uses its geometric normal instead of vanilla's upward lighting normal");
        System.out.println("Warden water: copper ripple geometry, animation, level/slopes/waterfalls, section translation and bounded capture passed.");
    }
    private static void emit(com.mojang.blaze3d.vertex.VertexConsumer buffer, double x, double y, double z) {
        buffer.vertex(x, y, z).normal(0, 1, 0).endVertex();
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
