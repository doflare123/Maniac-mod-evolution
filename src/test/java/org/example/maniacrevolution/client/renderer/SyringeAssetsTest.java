package org.example.maniacrevolution.client.renderer;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.loading.json.raw.Model;
import software.bernie.geckolib.loading.object.BakedModelFactory;
import software.bernie.geckolib.loading.object.GeometryTree;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.util.JsonUtil;
import software.bernie.geckolib.util.RenderUtils;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;

/** Exercises the actual mesh baker, key sampler and bone transforms without starting a client. */
public final class SyringeAssetsTest {
    private static InputStreamReader resource(String path) {
        var stream = SyringeAssetsTest.class.getResourceAsStream("/assets/maniacrev/" + path);
        if (stream == null) throw new AssertionError("Missing syringe resource " + path);
        return new InputStreamReader(stream, StandardCharsets.UTF_8);
    }

    private static Matrix4f shoulder(float x, float[] r) {
        return new Matrix4f().translation(x / 16, 2F / 16, 0).rotateZYX(
                (float) Math.toRadians(r[2]), (float) Math.toRadians(r[1]), (float) Math.toRadians(r[0]));
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        var mesh = BakedModelFactory.DEFAULT_FACTORY.constructGeoModel(GeometryTree.fromModel(
                JsonUtil.GEO_GSON.fromJson(resource("geo/syringe.geo.json"), Model.class)));
        for (String file : new String[]{"syringe", "syringe_held", "syringe_player"}) {
            try (var reader = resource("animations/" + file + ".animation.json")) {
                var animations = JsonUtil.GEO_GSON.fromJson(JsonParser.parseReader(reader).getAsJsonObject().get("animations"),
                        software.bernie.geckolib.loading.object.BakedAnimations.class);
                for (String clip : new String[]{"idle", "run", "inject"})
                    if (animations.getAnimation(clip) == null) throw new AssertionError("GeckoLib cannot load " + file + "/" + clip);
            }
        }
        var held = SyringeAnimationClips.read(resource("animations/syringe_held.animation.json"));
        var player = SyringeAnimationClips.read(resource("animations/syringe_player.animation.json"));
        var standalone = SyringeAnimationClips.read(resource("animations/syringe.animation.json"));
        for (String clip : new String[]{"idle", "run"}) for (boolean mirror : new boolean[]{false, true}) {
            int frames = clip.equals("run") ? 32 : 128;
            float minBarrelY = Float.POSITIVE_INFINITY, maxBarrelY = Float.NEGATIVE_INFINITY;
            for (int index = 0; index <= frames; index++) {
                float seconds = index / 40F;
                float[] motion = SyringeAnimationClips.sample(standalone, clip, "bone", "rotation", seconds);
                PoseStack camera = new PoseStack();
                SyringeHeldPose.firstPersonCamera(camera, mirror, false, seconds, clip.equals("run"));
                float[] arm = SyringeHeldPose.firstPersonArm(true, mirror, clip.equals("run"), seconds);
                camera.last().pose().mul(shoulder(mirror ? 5 : -5, arm));
                camera.translate((mirror ? 1 : -1) / 16F, 8.5F / 16F, SyringeHeldPose.gripZ(false, seconds) / 16F);
                camera.scale(mirror ? -.5F : .5F, -.5F, .5F);
                camera.mulPose(SyringeHeldPose.itemRotation(motion, false, seconds, true));
                for (float y : new float[]{3, 6, 11}) {
                    Vector3f point = camera.last().pose().transformPosition(new Vector3f(0, y / 16, 0));
                    float tan = (float) Math.tan(Math.toRadians(70) / 2);
                    float screenX = point.x / -point.z / tan / (16F / 9);
                    float screenY = point.y / -point.z / tan;
                    if (y == 3) {
                        minBarrelY = Math.min(minBarrelY, screenY);
                        maxBarrelY = Math.max(maxBarrelY, screenY);
                    }
                    if (point.z >= -.05F || Math.abs(screenX) < .15F || Math.abs(screenX) > .9F || screenY < -.55F || screenY > .8F
                            || (screenX < 0) != mirror)
                        throw new AssertionError("Held syringe is off-screen, under HUD or in the wrong hand: " + clip + " " + point);
                }
                Matrix4f wrist = new Matrix4f().translation((mirror ? 1 : -1) / 16F, 8.5F / 16F,
                        SyringeHeldPose.gripZ(false, seconds) / 16F).scale(mirror ? -.5F : .5F, -.5F, .5F)
                        .rotate(SyringeHeldPose.itemRotation(motion, false, seconds, false));
                for (float y : new float[]{0, 3, 6, 11})
                    if (wrist.transformPosition(new Vector3f(0, y / 16, 0)).z >= -2F / 16)
                        throw new AssertionError("Held syringe penetrates the forearm");
            }
            if (clip.equals("run") && maxBarrelY - minBarrelY < .06F)
                throw new AssertionError("Running must move the syringe, not just rotate its lighting normals");
        }
        int[] vertices = {0};
        var renderer = (GeoRenderer<GeoAnimatable>) Proxy.newProxyInstance(SyringeAssetsTest.class.getClassLoader(),
                new Class<?>[]{GeoRenderer.class}, (proxy, method, values) ->
                        method.isDefault() ? InvocationHandler.invokeDefault(proxy, method, values) : null);
        var consumer = (VertexConsumer) Proxy.newProxyInstance(SyringeAssetsTest.class.getClassLoader(),
                new Class<?>[]{VertexConsumer.class}, (proxy, method, values) -> {
                    if (method.getName().equals("vertex") && values.length == 3) {
                        for (Object value : values) if (!Double.isFinite(((Number) value).doubleValue()))
                            throw new AssertionError("Non-finite syringe vertex");
                    }
                    if (method.getName().equals("endVertex")) vertices[0]++;
                    if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, values);
                    return method.getReturnType() == VertexConsumer.class ? proxy : null;
                });
        for (String clip : new String[]{"idle", "run", "inject"}) {
            float length = clip.equals("run") ? .8F : 3.2F;
            for (int frame = 0; frame <= Math.round(length * 40); frame++) {
                float time = frame / 40F;
                for (String name : new String[]{"bone", "barrel", "plunger", "liquid", "bubble_01", "bubble_02", "bubble_03", "needle"}) {
                    SyringeAnimationClips.pose(mesh.getBone(name).orElseThrow(),
                            SyringeAnimationClips.sample(held, clip, name, "rotation", time),
                            SyringeAnimationClips.sample(held, clip, name, "position", time),
                            SyringeAnimationClips.sample(held, clip, name, "scale", time));
                }
                PoseStack pose = new PoseStack();
                for (var bone : mesh.topLevelBones()) renderer.renderRecursively(pose, null, bone, null, null,
                        consumer, true, 0, 0, 0, 1, 1, 1, 1);
                if (clip.equals("inject") && time >= .8F && time <= 2.1F) {
                    float[] right = SyringeAnimationClips.sample(player, clip, "right_arm", "rotation", time);
                    float[] left = SyringeAnimationClips.sample(player, clip, "left_arm", "rotation", time);
                    Matrix4f rightHand = shoulder(-5, right).translate(-1F / 16, 8.5F / 16, 2.3F / 16).scale(.5F, -.5F, .5F);
                    pose = new PoseStack();
                    pose.last().pose().set(rightHand);
                    RenderUtils.prepMatrixForBone(pose, mesh.getBone("bone").orElseThrow());
                    Matrix4f toLeftArm = shoulder(5, left).invert().mul(pose.last().pose());
                    Vector3f tip = toLeftArm.transformPosition(new Vector3f(0, 11F / 16, 0)).mul(16);
                    Vector3f base = toLeftArm.transformPosition(new Vector3f(0, 6F / 16, 0)).mul(16);
                    if (tip.x < -1 || tip.x > 3 || tip.y < -2 || tip.y > 10 || tip.z < -2 || tip.z > 2)
                        throw new AssertionError("Needle misses left arm at " + time + ": " + tip);
                    if (base.x >= -1 && base.x <= 3 && base.y >= -2 && base.y <= 10 && base.z >= -2 && base.z <= 2)
                        throw new AssertionError("Needle base enters the arm at " + time);
                }
            }
        }
        if (vertices[0] < 1000) throw new AssertionError("Syringe mesh did not render");
        float plunger = SyringeAnimationClips.sample(held, "inject", "plunger", "position", 2.05F)[1];
        float liquid = SyringeAnimationClips.sample(held, "inject", "liquid", "scale", 2.05F)[1];
        if (Math.abs(plunger - 5.55F) > .01F || Math.abs(liquid - .05F) > .01F)
            throw new AssertionError("Injection must press the plunger and empty the liquid");
        var item = JsonParser.parseReader(resource("models/item/syringe.json")).getAsJsonObject();
        if (!item.get("parent").getAsString().equals("builtin/entity")) throw new AssertionError("Missing custom item renderer");
        // Check the actual baked vertices under Minecraft's inventory display transform.
        var gui = item.getAsJsonObject("display").getAsJsonObject("gui");
        var r = gui.getAsJsonArray("rotation");
        var t = gui.getAsJsonArray("translation");
        float scale = gui.getAsJsonArray("scale").get(0).getAsFloat();
        PoseStack guiPose = new PoseStack();
        guiPose.translate(t.get(0).getAsFloat() / 16, t.get(1).getAsFloat() / 16, t.get(2).getAsFloat() / 16);
        guiPose.mulPose(new org.joml.Quaternionf().rotationXYZ((float) Math.toRadians(r.get(0).getAsFloat()),
                (float) Math.toRadians(r.get(1).getAsFloat()), (float) Math.toRadians(r.get(2).getAsFloat())));
        guiPose.scale(scale, scale, scale);
        var inventory = SyringeAnimationClips.read(resource("animations/syringe.animation.json"));
        for (String name : new String[]{"bone", "barrel", "plunger", "liquid", "bubble_01", "bubble_02", "bubble_03", "needle"})
            SyringeAnimationClips.pose(mesh.getBone(name).orElseThrow(),
                    SyringeAnimationClips.sample(inventory, "idle", name, "rotation", 0),
                    SyringeAnimationClips.sample(inventory, "idle", name, "position", 0),
                    SyringeAnimationClips.sample(inventory, "idle", name, "scale", 0));
        var guiConsumer = (VertexConsumer) Proxy.newProxyInstance(SyringeAssetsTest.class.getClassLoader(),
                new Class<?>[]{VertexConsumer.class}, (proxy, method, values) -> {
                    if (method.getName().equals("vertex") && values.length == 3
                            && (Math.abs(((Number) values[0]).doubleValue()) > .45 || Math.abs(((Number) values[1]).doubleValue()) > .45))
                        throw new AssertionError("Inventory syringe clips outside its slot");
                    if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, values);
                    return method.getReturnType() == VertexConsumer.class ? proxy : null;
                });
        for (var bone : mesh.topLevelBones()) renderer.renderRecursively(guiPose, null, bone, null, null,
                guiConsumer, true, 0, 0, 0, 1, 1, 1, 1);
        for (String side : new String[]{"firstperson_righthand", "firstperson_lefthand", "thirdperson_righthand", "thirdperson_lefthand"})
            if (!item.getAsJsonObject("display").getAsJsonObject(side).entrySet().isEmpty()) throw new AssertionError("Hand mount must not repeat display transforms");
        if (SyringeAssetsTest.class.getResource("/assets/maniacrev/textures/item/syringe.png") == null)
            throw new AssertionError("Missing syringe texture");
        System.out.println("Syringe assets: " + vertices[0] + " finite vertices; idle/run/inject parsed and sampled; needle enters opposite arm; plunger/liquid, hand displays and inventory bounds checked.");
    }
}
