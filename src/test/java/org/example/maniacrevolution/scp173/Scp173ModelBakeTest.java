package org.example.maniacrevolution.scp173;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.loading.json.raw.Model;
import software.bernie.geckolib.loading.object.BakedModelFactory;
import software.bernie.geckolib.loading.object.GeometryTree;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.util.JsonUtil;

/** Actual GeckoLib baking and vertex emission, without a GL window or game world. */
public final class Scp173ModelBakeTest {
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        // Forge 6.0.5 names generated subscribers by package + SIMPLE class + method + event.
        // Two nested Layers.add methods in scp173.client collided and initialized only the arms.
        net.minecraftforge.eventbus.IEventListenerFactory factory = (method, target) -> null;
        var body = org.example.maniacrevolution.scp173.client.Scp173PlayerForm.Layers.class.getMethod(
                "initializeScp173Body", net.minecraftforge.client.event.EntityRenderersEvent.AddLayers.class);
        var arms = org.example.maniacrevolution.scp173.client.Scp173FirstPersonArms.Layers.class.getMethod(
                "initializeScp173Arms", net.minecraftforge.client.event.EntityRenderersEvent.AddLayers.class);
        var warden = org.example.maniacrevolution.warden.client.WardenPlayerForm.Layers.class.getMethod(
                "add", net.minecraftforge.client.event.EntityRenderersEvent.AddLayers.class);
        if (java.util.Set.of(factory.getUniqueName(body), factory.getUniqueName(arms), factory.getUniqueName(warden)).size() != 3)
            throw new AssertionError("Renderer event wrapper collision");
        var input = Scp173ModelBakeTest.class.getResourceAsStream("/assets/maniacrev/geo/scp173.geo.json");
        if (input == null) throw new AssertionError("Missing model resource");
        Model raw;
        try (var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            raw = JsonUtil.GEO_GSON.fromJson(reader, Model.class);
        }
        var baked = BakedModelFactory.DEFAULT_FACTORY.constructGeoModel(GeometryTree.fromModel(raw));
        var renderer = (GeoRenderer<GeoAnimatable>) Proxy.newProxyInstance(Scp173ModelBakeTest.class.getClassLoader(),
                new Class<?>[]{GeoRenderer.class}, (proxy, method, values) ->
                        method.isDefault() ? InvocationHandler.invokeDefault(proxy, method, values) : null);
        int[] vertices = {0};
        double[] bounds = {Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        var consumer = (VertexConsumer) Proxy.newProxyInstance(Scp173ModelBakeTest.class.getClassLoader(),
                new Class<?>[]{VertexConsumer.class}, (proxy, method, values) -> {
                    if (method.getName().equals("vertex") && values.length == 3) {
                        for (var value : values) if (!Double.isFinite(((Number) value).doubleValue())) throw new AssertionError("Invalid vertex");
                        double y = ((Number) values[1]).doubleValue();
                        bounds[0] = Math.min(bounds[0], y); bounds[1] = Math.max(bounds[1], y);
                    }
                    if (method.getName().equals("endVertex")) vertices[0]++;
                    if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, values);
                    return method.getReturnType() == VertexConsumer.class ? proxy : null;
                });
        var pose = new PoseStack(); pose.scale(Scp173Geometry.SCALE, Scp173Geometry.SCALE, Scp173Geometry.SCALE);
        for (var root : baked.topLevelBones())
            renderer.renderRecursively(pose, null, root, null, null, consumer, true, 0, 0, 0, 1, 1, 1, 1);
        if (vertices[0] != 22 * 6 * 4) throw new AssertionError("Expected 528 statue vertices, got " + vertices[0]);
        double height = bounds[1] - bounds[0];
        if (height < 1.9 || height > 2.2) throw new AssertionError("Unexpected statue height " + height);
        System.out.println("SCP-173 GeckoLib mesh: " + vertices[0] + " finite vertices; height " + height);
    }
}
