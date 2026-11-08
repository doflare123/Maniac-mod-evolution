package org.example.maniacrevolution.client.renderer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import org.example.maniacrevolution.Maniacrev;
import software.bernie.geckolib.cache.object.GeoBone;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/** The supplied exports contain linear, numeric keys. Sample both rigs from the same game clock. */
final class SyringeAnimationClips {
    private static final Map<String, Map<String, Map<String, NavigableMap<Float, float[]>>>> FILES = new HashMap<>();

    private SyringeAnimationClips() {}

    static void clear() { FILES.clear(); }

    static float[] sample(String file, String clip, String bone, String channel, float time) {
        return sample(FILES.computeIfAbsent(file, SyringeAnimationClips::load), clip, bone, channel, time);
    }

    static float[] sample(Map<String, Map<String, NavigableMap<Float, float[]>>> clips,
                          String clip, String bone, String channel, float time) {
        var keys = clips.getOrDefault(clip, Map.of()).get(bone + "/" + channel);
        if (keys == null || keys.isEmpty()) return channel.equals("scale") ? new float[]{1, 1, 1} : new float[3];
        var before = keys.floorEntry(time);
        var after = keys.ceilingEntry(time);
        if (before == null) before = keys.firstEntry();
        if (after == null) after = keys.lastEntry();
        float factor = before.getKey().equals(after.getKey()) ? 0 :
                (time - before.getKey()) / (after.getKey() - before.getKey());
        float[] result = new float[3];
        for (int axis = 0; axis < 3; axis++) {
            result[axis] = before.getValue()[axis] + (after.getValue()[axis] - before.getValue()[axis]) * factor;
        }
        return result;
    }

    private static Map<String, Map<String, NavigableMap<Float, float[]>>> load(String file) {
        try (var reader = Minecraft.getInstance().getResourceManager().openAsReader(
                Maniacrev.loc("animations/" + file + ".animation.json"))) {
            return read(reader);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot load syringe animation " + file, exception);
        }
    }

    static Map<String, Map<String, NavigableMap<Float, float[]>>> read(Reader reader) {
        Map<String, Map<String, NavigableMap<Float, float[]>>> result = new HashMap<>();
        JsonObject clips = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("animations");
        for (var clip : clips.entrySet()) {
            Map<String, NavigableMap<Float, float[]>> tracks = new HashMap<>();
            for (var bone : clip.getValue().getAsJsonObject().getAsJsonObject("bones").entrySet()) {
                for (var channel : bone.getValue().getAsJsonObject().entrySet()) {
                    NavigableMap<Float, float[]> keys = new TreeMap<>();
                    for (var key : channel.getValue().getAsJsonObject().entrySet()) {
                        JsonElement value = key.getValue();
                        var vector = value.isJsonObject() ? value.getAsJsonObject().getAsJsonArray("vector") : value.getAsJsonArray();
                        keys.put(Float.parseFloat(key.getKey()), new float[]{
                                vector.get(0).getAsFloat(), vector.get(1).getAsFloat(), vector.get(2).getAsFloat()});
                    }
                    tracks.put(bone.getKey() + "/" + channel.getKey(), keys);
                }
            }
            result.put(clip.getKey(), tracks);
        }
        return result;
    }

    static void pose(GeoBone bone, float[] rotation, float[] position, float[] scale) {
        // The supplied exports negate authoring X/Y rotations and X positions.
        // Convert to the baked mesh's reflected-X coordinates; the hand mount reflects Y.
        bone.setRotX((float) Math.toRadians(-rotation[0]));
        bone.setRotY((float) Math.toRadians(rotation[1]));
        bone.setRotZ((float) Math.toRadians(-rotation[2]));
        bone.setPosX(-position[0]); bone.setPosY(position[1]); bone.setPosZ(position[2]);
        bone.setScaleX(scale[0]); bone.setScaleY(scale[1]); bone.setScaleZ(scale[2]);
    }
}
