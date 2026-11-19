package org.example.maniacrevolution.atmosphere.client;

import foundry.veil.api.client.render.post.PostPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.example.maniacrevolution.atmosphere.LightTransition;
import org.example.maniacrevolution.nightmare.ClientKeeperVisionData;
import org.example.maniacrevolution.nightmare.KeeperSoulState;
import org.example.maniacrevolution.nightmare.NightmareConfig;
import org.joml.Vector4f;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Camera-relative soul volumes, composed after fog but tested against world depth. */
final class KeeperVisionClient {
    private static final Map<UUID, SoulVisual> visuals = new LinkedHashMap<>();
    private static final Vector4f[] centers = new Vector4f[KeeperSoulState.MAX_SOULS];
    private static final Vector4f[] shapes = new Vector4f[KeeperSoulState.MAX_SOULS];
    private static Vec3 previewPosition;
    private static float previewSanity = 1;
    private static boolean previewFeeding;
    private static final LightTransition testSanity = new LightTransition(1);
    private static final LightTransition testFeeding = new LightTransition();

    static {
        for (int i = 0; i < centers.length; i++) {
            centers[i] = new Vector4f();
            shapes[i] = new Vector4f();
        }
    }

    private KeeperVisionClient() {}

    private static final class SoulVisual {
        final LightTransition sanity;
        final LightTransition feeding = new LightTransition();
        boolean visible;

        SoulVisual(float initial) { sanity = new LightTransition(initial); }
    }

    static void tick(boolean active, boolean preview) {
        ClientKeeperVisionData.tick();
        if (!active || preview) {
            visuals.clear();
            testSanity.tick(previewSanity);
            testFeeding.tick(previewFeeding ? 1 : 0);
            return;
        }
        var mc = Minecraft.getInstance();
        var souls = ClientKeeperVisionData.souls();
        visuals.keySet().removeIf(id -> souls.stream().noneMatch(soul -> soul.playerId().equals(id)));
        for (var soul : souls) {
            var visual = visuals.computeIfAbsent(soul.playerId(), id -> new SoulVisual(soul.sanity()));
            visual.sanity.tick(soul.sanity());
            visual.feeding.tick(soul.feeding() ? 1 : 0);
            var target = mc.level.getPlayerByUUID(soul.playerId());
            // Recheck locally between snapshots. The shader additionally rejects every occluded pixel.
            visual.visible = target != null && target.isAlive() && !target.isCreative() && !target.isSpectator()
                    && !target.isInvisibleTo(mc.player) && mc.player.hasLineOfSight(target)
                    && mc.player.distanceToSqr(target) <= NightmareConfig.GAZE_RANGE * NightmareConfig.GAZE_RANGE;
        }
    }

    static void upload(PostPipeline pipeline, boolean active, boolean preview, float partialTick) {
        int count = 0;
        var mc = Minecraft.getInstance();
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        if (active && preview && previewPosition != null && camera.distanceToSqr(previewPosition) <= 40 * 40) {
            put(count++, previewPosition.subtract(camera), .25F, .56F,
                    testSanity.value(partialTick), testFeeding.value(partialTick), .37F);
        } else if (active && !preview) {
            for (var entry : visuals.entrySet()) {
                if (count == centers.length) break;
                var target = mc.level.getPlayerByUUID(entry.getKey());
                var visual = entry.getValue();
                if (!visual.visible || target == null || !target.isAlive() || target.isInvisibleTo(mc.player)
                        || mc.player.distanceToSqr(target) > NightmareConfig.GAZE_RANGE * NightmareConfig.GAZE_RANGE) continue;
                Vec3 center = target.getPosition(partialTick).add(0, target.getBbHeight() * .52, 0).subtract(camera);
                put(count++, center, target.getBbWidth() * .43F, target.getBbHeight() * .31F,
                        visual.sanity.value(partialTick), visual.feeding.value(partialTick),
                        (entry.getKey().hashCode() & 1023) / 1024F);
            }
        }
        pipeline.setFloat("Keeper", active ? 1 : 0);
        pipeline.setInt("SoulCount", count);
        pipeline.setVectors("SoulCenterSanity", centers);
        pipeline.setVectors("SoulShape", shapes);
    }

    private static void put(int slot, Vec3 center, float width, float height, float sanity, float feeding, float seed) {
        centers[slot].set((float) center.x, (float) center.y, (float) center.z, sanity);
        shapes[slot].set(width, height, feeding, seed);
    }

    static void previewSoul(float sanity) {
        if (previewPosition == null) {
            var player = Minecraft.getInstance().player;
            previewPosition = player.getEyePosition().add(player.getLookAngle().scale(6)).add(0, -.45, 0);
            testSanity.snap(sanity);
        }
        previewSanity = sanity;
    }

    static void previewFeeding(boolean enabled) { previewFeeding = enabled; }

    static void clearPreview() {
        previewPosition = null;
        previewSanity = 1;
        previewFeeding = false;
        testSanity.snap(1);
        testFeeding.snap(0);
    }

    static void reset() {
        visuals.clear();
        ClientKeeperVisionData.clear();
        clearPreview();
    }
}
