package org.example.maniacrevolution.scp173;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4d;
import org.joml.Vector3d;

/** Server-safe surface samples of the shipped static geometry, in GeckoLib model coordinates. */
public final class Scp173Geometry {
    public static final float SCALE = 0.9F;
    private static final List<Vec3> SURFACE = load();
    public static final double CENTER_Y = (SURFACE.stream().mapToDouble(v -> v.y).min().orElseThrow()
            + SURFACE.stream().mapToDouble(v -> v.y).max().orElseThrow()) / 2;
    private Scp173Geometry() {}

    public static List<Vec3> surface() { return SURFACE; }
    public static float swimAngle(float amount, float pitch, boolean inWater) {
        return Math.max(0, Math.min(1, amount)) * (-90 - (inWater ? pitch : 0));
    }
    public static double swimCenter(float amount) {
        return CENTER_Y + Math.max(0, Math.min(1, amount)) * (0.3 - CENTER_Y);
    }
    public static Vec3 worldPoint(Vec3 local, Vec3 feet, float yaw, float swim, float pitch, boolean inWater) {
        double angle = Math.toRadians(swimAngle(swim, pitch, inWater));
        double y = local.y - CENTER_Y;
        return worldPoint(new Vec3(local.x, y * Math.cos(angle) - local.z * Math.sin(angle) + swimCenter(swim),
                y * Math.sin(angle) + local.z * Math.cos(angle)), feet, yaw);
    }

    public static Vec3 worldPoint(Vec3 local, Vec3 feet, float yaw) {
        double angle = Math.toRadians(180.0 - yaw);
        double cos = Math.cos(angle), sin = Math.sin(angle);
        return feet.add(local.x * cos + local.z * sin, local.y, -local.x * sin + local.z * cos);
    }

    private static List<Vec3> load() {
        try (var stream = Scp173Geometry.class.getResourceAsStream("/assets/maniacrev/geo/scp173.geo.json")) {
            if (stream == null) throw new IllegalStateException("Missing SCP-173 geometry");
            var geometry = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            Map<String, JsonObject> bones = new LinkedHashMap<>();
            for (var element : geometry.getAsJsonArray("bones")) {
                var bone = element.getAsJsonObject();
                bones.put(bone.get("name").getAsString(), bone);
            }
            List<Vec3> points = new ArrayList<>();
            for (var bone : bones.values()) {
                if (!bone.has("cubes") || bone.has("neverRender") && bone.get("neverRender").getAsBoolean()) continue;
                var boneTransform = boneTransform(bone, bones);
                for (var element : bone.getAsJsonArray("cubes")) {
                    var cube = element.getAsJsonObject();
                    Vec3 origin = vector(cube, "origin"), size = vector(cube, "size");
                    double inflate = cube.has("inflate") ? cube.get("inflate").getAsDouble()
                            : bone.has("inflate") ? bone.get("inflate").getAsDouble() : 0;
                    var transform = new Matrix4d(boneTransform).mul(transform(cube));
                    // Corners, edge midpoints and face centres of every cube, including small fingers.
                    for (int x = 0; x <= 2; x++) for (int y = 0; y <= 2; y++) for (int z = 0; z <= 2; z++) {
                        if (x == 1 && y == 1 && z == 1) continue;
                        var point = new Vector3d(-(origin.x - inflate + (size.x + 2 * inflate) * x / 2),
                                origin.y - inflate + (size.y + 2 * inflate) * y / 2,
                                origin.z - inflate + (size.z + 2 * inflate) * z / 2);
                        transform.transformPosition(point);
                        points.add(new Vec3(point.x, point.y, point.z).scale(SCALE / 16.0));
                    }
                }
            }
            if (points.isEmpty()) throw new IllegalStateException("Empty SCP-173 geometry");
            return List.copyOf(points);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Cannot read SCP-173 geometry", e);
        }
    }

    private static Matrix4d boneTransform(JsonObject bone, Map<String, JsonObject> bones) {
        Matrix4d parent = bone.has("parent") ? boneTransform(bones.get(bone.get("parent").getAsString()), bones)
                : new Matrix4d();
        return parent.mul(transform(bone));
    }

    private static Matrix4d transform(JsonObject object) {
        Vec3 pivot = vector(object, "pivot"), rotation = vector(object, "rotation");
        // GeckoLib reflects model X; rotations are Z, Y, X, with X/Y angle signs inverted.
        return new Matrix4d().translation(-pivot.x, pivot.y, pivot.z)
                .rotateZ(Math.toRadians(rotation.z)).rotateY(Math.toRadians(-rotation.y))
                .rotateX(Math.toRadians(-rotation.x)).translate(pivot.x, -pivot.y, -pivot.z);
    }

    private static Vec3 vector(JsonObject object, String key) {
        if (!object.has(key)) return Vec3.ZERO;
        var values = object.getAsJsonArray(key);
        return new Vec3(values.get(0).getAsDouble(), values.get(1).getAsDouble(), values.get(2).getAsDouble());
    }
}
