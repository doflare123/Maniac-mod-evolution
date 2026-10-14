package org.example.maniacrevolution.helsing;

import com.google.gson.*;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;

/** Blockbench player-rig rotations applied to vanilla skin arms, without a new animation dependency. */
public final class HelsingGesture {
    private static JsonObject bones = new JsonObject();
    private HelsingGesture() {}
    public static float[] rotation(String bone, float seconds) {
        if (!bones.has(bone)) return new float[3];
        JsonObject keys = bones.getAsJsonObject(bone).getAsJsonObject("rotation");
        float before = 0, after = Float.MAX_VALUE; JsonArray start = null, end = null;
        for (var key : keys.entrySet()) {
            float time = Float.parseFloat(key.getKey());
            if (time <= seconds && (start == null || time >= before)) { before = time; start = key.getValue().getAsJsonArray(); }
            if (time >= seconds && time < after) { after = time; end = key.getValue().getAsJsonArray(); }
        }
        if (start == null) start = end;
        if (end == null) end = start;
        if (start == null) return new float[3];
        float factor = after == before || after == Float.MAX_VALUE ? 0 : (seconds - before) / (after - before);
        float[] result = new float[3];
        for (int i = 0; i < 3; i++) result[i] = start.get(i).getAsFloat() + (end.get(i).getAsFloat() - start.get(i).getAsFloat()) * factor;
        return result;
    }
    @Mod.EventBusSubscriber(modid = Maniacrev.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {
        @SubscribeEvent public static void reload(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(new SimplePreparableReloadListener<JsonObject>() {
                @Override protected JsonObject prepare(ResourceManager manager, ProfilerFiller profiler) {
                    try (var reader = manager.getResource(Maniacrev.loc("animations/helsing_gesture.animation.json")).orElseThrow().openAsReader()) {
                        return JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("animations").getAsJsonObject("cast").getAsJsonObject("bones");
                    } catch (Exception exception) { Maniacrev.LOGGER.error("Cannot load Helsing gesture", exception); return new JsonObject(); }
                }
                @Override protected void apply(JsonObject data, ResourceManager manager, ProfilerFiller profiler) { bones = data; }
            });
        }
    }
}
