package org.example.maniacrevolution.forester;

import com.mojang.blaze3d.vertex.*;
import com.google.gson.*;
import java.io.InputStreamReader;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.loading.json.raw.Model;
import software.bernie.geckolib.loading.object.*;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.util.JsonUtil;

/** Exercises the installed GeckoLib parser/baker and validates runtime item texture references. */
public final class ForesterAssetsTest {
    private static InputStreamReader resource(String name) {
        var input = ForesterAssetsTest.class.getResourceAsStream("/assets/maniacrev/" + name);
        if (input == null) throw new AssertionError("Missing runtime resource " + name);
        return new InputStreamReader(input, StandardCharsets.UTF_8);
    }
    @SuppressWarnings("unchecked") public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion(); net.minecraft.server.Bootstrap.bootStrap();
        Model raw;
        try (var reader = resource("geo/forester_trap.geo.json")) { raw = JsonUtil.GEO_GSON.fromJson(reader, Model.class); }
        var baked = BakedModelFactory.DEFAULT_FACTORY.constructGeoModel(GeometryTree.fromModel(raw));
        var renderer = (GeoRenderer<GeoAnimatable>) Proxy.newProxyInstance(ForesterAssetsTest.class.getClassLoader(),
                new Class<?>[]{GeoRenderer.class}, (proxy, method, values) -> method.isDefault() ? InvocationHandler.invokeDefault(proxy, method, values) : null);
        int[] vertices = {0}; double[] bounds = {Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        var consumer = (VertexConsumer) Proxy.newProxyInstance(ForesterAssetsTest.class.getClassLoader(), new Class<?>[]{VertexConsumer.class}, (proxy, method, values) -> {
            if (method.getName().equals("vertex") && values.length == 3) {
                for (var value : values) if (!Double.isFinite(((Number) value).doubleValue())) throw new AssertionError("Invalid vertex");
                double y = ((Number) values[1]).doubleValue(); bounds[0] = Math.min(bounds[0], y); bounds[1] = Math.max(bounds[1], y);
            }
            if (method.getName().equals("endVertex")) vertices[0]++;
            if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, values);
            return method.getReturnType() == VertexConsumer.class ? proxy : null;
        });
        var pose = new PoseStack();
        for (var bone : baked.topLevelBones()) renderer.renderRecursively(pose, null, bone, null, null, consumer, true, 0, 0, 0, 1, 1, 1, 1);
        if (vertices[0] < 100 || bounds[1] - bounds[0] < .05 || bounds[1] - bounds[0] > .25) throw new AssertionError("Trap must bake as a low-profile mesh");
        try (var reader = resource("animations/forester_trap.animation.json")) {
            var clips = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("animations");
            for (String clip : new String[]{"open", "caught", "spent"}) if (!clips.has(clip)) throw new AssertionError("Missing trap pose " + clip);
        }
        for (String name : new String[]{"forester_trap", "pepper_canister", "forester_flashlight"}) {
            try (var reader = resource("models/item/" + name + ".json")) {
                var json = JsonParser.parseReader(reader).getAsJsonObject();
                if (name.equals("forester_trap")) {
                    double minY = 100, maxY = -100;
                    for (var element : json.getAsJsonArray("elements")) {
                        minY = Math.min(minY, element.getAsJsonObject().getAsJsonArray("from").get(1).getAsDouble());
                        maxY = Math.max(maxY, element.getAsJsonObject().getAsJsonArray("to").get(1).getAsDouble());
                    }
                    if (minY < 7 || maxY > 12) throw new AssertionError("Trap item must be centered at hand height, independently of floor geometry");
                }
                if (name.equals("forester_flashlight")) {
                    for (String side : new String[]{"righthand", "lefthand"}) {
                        var rotation = json.getAsJsonObject("display").getAsJsonObject("firstperson_" + side).getAsJsonArray("rotation");
                        if (rotation.get(0).getAsFloat() != 0 || rotation.get(1).getAsFloat() != 0 || rotation.get(2).getAsFloat() != 0)
                            throw new AssertionError("Flashlight lens must point down the -Z camera axis");
                    }
                }
                String texture = json.getAsJsonObject("textures").get("0").getAsString();
                String[] parts = texture.split(":", 2);
                if (ForesterAssetsTest.class.getResource("/assets/" + parts[0] + "/textures/" + parts[1] + ".png") == null)
                    throw new AssertionError("Missing item texture " + texture);
                for (var element : json.getAsJsonArray("elements")) for (var face : element.getAsJsonObject().getAsJsonObject("faces").entrySet()) {
                    for (var uv : face.getValue().getAsJsonObject().getAsJsonArray("uv")) if (uv.getAsDouble() < 0 || uv.getAsDouble() > 16) throw new AssertionError("UV outside texture");
                }
            }
        }
        try (var reader = resource("animations/forester_work.animation.json")) {
            var clips = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("animations");
            for (String name : new String[]{"install", "collect"}) {
                var bones = clips.getAsJsonObject(name).getAsJsonObject("bones");
                for (String bone : new String[]{"left_arm", "right_arm"}) {
                    var keys = bones.getAsJsonObject(bone).getAsJsonObject("rotation");
                    if (keys.size() < 4) throw new AssertionError("Missing work gesture keyframes");
                    for (var key : keys.entrySet()) if (Float.parseFloat(key.getKey()) < 0 || Float.parseFloat(key.getKey()) > 1) throw new AssertionError("Invalid normalized work time");
                }
            }
        }
        System.out.println("Forester assets: GeckoLib baked " + vertices[0] + " finite vertices, three trap poses and all item textures resolved.");
    }
}
