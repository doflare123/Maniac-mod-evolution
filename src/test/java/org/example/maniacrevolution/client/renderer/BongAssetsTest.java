package org.example.maniacrevolution.client.renderer;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.loading.json.raw.Model;
import software.bernie.geckolib.loading.object.BakedAnimations;
import software.bernie.geckolib.loading.object.BakedModelFactory;
import software.bernie.geckolib.loading.object.GeometryTree;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.util.JsonUtil;

import java.io.InputStreamReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;

/** Uses the installed GeckoLib parser and mesh baker rather than a JSON-only check. */
public final class BongAssetsTest {
    private static InputStreamReader resource(String name) {
        var stream = BongAssetsTest.class.getResourceAsStream("/assets/maniacrev/" + name);
        if (stream == null) throw new AssertionError("Missing bong resource: " + name);
        return new InputStreamReader(stream, StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        var mesh = BakedModelFactory.DEFAULT_FACTORY.constructGeoModel(GeometryTree.fromModel(
                JsonUtil.GEO_GSON.fromJson(resource("geo/bong.geo.json"), Model.class)));
        if (mesh.getBone("bone").isEmpty() || mesh.getBone("liquid").isEmpty())
            throw new AssertionError("Missing bong or liquid bone");
        var json = JsonParser.parseReader(resource("animations/bong.animation.json")).getAsJsonObject().getAsJsonObject("animations");
        var clips = JsonUtil.GEO_GSON.fromJson(json, BakedAnimations.class);
        for (String name : new String[]{"idle", "run", "using"}) {
            if (clips.getAnimation(name) == null) throw new AssertionError("GeckoLib cannot load " + name);
            var clip = json.getAsJsonObject(name);
            for (var bone : clip.getAsJsonObject("bones").entrySet()) {
                if (mesh.getBone(bone.getKey()).isEmpty()) throw new AssertionError("Unknown animated bone " + bone.getKey());
                for (var channel : bone.getValue().getAsJsonObject().entrySet()) {
                    for (var key : channel.getValue().getAsJsonObject().entrySet()) {
                        double time = Double.parseDouble(key.getKey());
                        if (!Double.isFinite(time) || time < 0 || time > clip.get("animation_length").getAsDouble())
                            throw new AssertionError("Invalid key time");
                        for (var value : key.getValue().getAsJsonArray())
                            if (!Double.isFinite(value.getAsDouble())) throw new AssertionError("Invalid animation value");
                    }
                }
            }
        }
        int[] vertices = {0};
        var renderer = (GeoRenderer<GeoAnimatable>) Proxy.newProxyInstance(BongAssetsTest.class.getClassLoader(),
                new Class<?>[]{GeoRenderer.class}, (proxy, method, values) ->
                        method.isDefault() ? InvocationHandler.invokeDefault(proxy, method, values) : null);
        var consumer = (VertexConsumer) Proxy.newProxyInstance(BongAssetsTest.class.getClassLoader(),
                new Class<?>[]{VertexConsumer.class}, (proxy, method, values) -> {
                    if (method.getName().equals("vertex") && values.length == 3)
                        for (var value : values) if (!Double.isFinite(((Number) value).doubleValue()))
                            throw new AssertionError("Invalid bong vertex");
                    if (method.getName().equals("endVertex")) vertices[0]++;
                    if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, values);
                    return method.getReturnType() == VertexConsumer.class ? proxy : null;
                });
        for (var bone : mesh.topLevelBones()) renderer.renderRecursively(new PoseStack(), null, bone, null, null,
                consumer, true, 0, 0, 0, 1, 1, 1, 1);
        if (vertices[0] != 14 * 6 * 4) throw new AssertionError("Expected exactly 14 complete cubes: " + vertices[0]);
        var item = JsonParser.parseReader(resource("models/item/bong.json")).getAsJsonObject();
        if (!item.get("parent").getAsString().equals("builtin/entity")) throw new AssertionError("Missing custom item renderer");
        for (String perspective : new String[]{"firstperson_righthand", "firstperson_lefthand", "thirdperson_righthand", "thirdperson_lefthand", "gui", "ground", "fixed"})
            if (!item.getAsJsonObject("display").has(perspective)) throw new AssertionError("Missing " + perspective);
        if (BongAssetsTest.class.getResource("/assets/maniacrev/textures/item/bong.png") == null)
            throw new AssertionError("Missing bong texture");
        System.out.println("Bong: GeckoLib baked " + vertices[0] + " finite vertices; idle/run/using parsed; bones, item display and texture resolved.");
        BongThirdPersonPoseTest.verify();
    }
}
