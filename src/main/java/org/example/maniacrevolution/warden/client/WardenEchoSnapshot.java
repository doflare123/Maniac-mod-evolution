package org.example.maniacrevolution.warden.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** Immutable world-space points: no retained entity, UUID lookup, or animation after capture. */
record WardenEchoSnapshot(List<Vec3> points, double born, double duration, Vec3 areaOrigin, double radius) {
    WardenEchoSnapshot(List<Vec3> points, double born) {
        this(points, born, WardenPulseTiming.DURATION, null, WardenSurfaceCache.RADIUS);
    }
    WardenEchoSnapshot {
        points = List.copyOf(points);
    }
    float brightness(Vec3 point, double now) {
        if (areaOrigin == null) {
            double fade = Math.max(0, 1 - (now - born) / duration); return (float) (fade * fade);
        }
        return WardenPulseTiming.brightness(now - born, point.distanceTo(areaOrigin), radius, duration);
    }
    static WardenEchoSnapshot capture(AbstractClientPlayer player, double now) {
        boolean slim = "slim".equals(player.getModelName());
        PlayerModel<AbstractClientPlayer> model = new PlayerModel<>(Minecraft.getInstance().getEntityModels()
                .bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim);
        model.crouching = player.isCrouching();
        model.riding = player.isPassenger();
        model.attackTime = player.getAttackAnim(1);
        model.swimAmount = player.getSwimAmount(1);
        model.prepareMobModel(player, player.walkAnimation.position(), player.walkAnimation.speed(), 1);
        model.setupAnim(player, player.walkAnimation.position(), player.walkAnimation.speed(), player.tickCount,
                player.yHeadRot - player.yBodyRot, player.getXRot());
        PoseStack pose = new PoseStack();
        pose.translate(player.getX(), player.getY(), player.getZ());
        pose.mulPose(Axis.YP.rotationDegrees(180 - player.yBodyRot));
        if (player.isVisuallySwimming() || player.isFallFlying()) pose.mulPose(Axis.XP.rotationDegrees(-90 - player.getXRot()));
        pose.scale(-0.9375f, -0.9375f, 0.9375f);
        pose.translate(0, -1.501, 0);
        return new WardenEchoSnapshot(sample(model, pose), now);
    }
    static WardenEchoSnapshot captureServer(org.example.maniacrevolution.warden.WardenEchoPose data,
                                           Vec3 origin, double now, double duration) {
        // An isolated animation input object, never registered in the world or associated with the moving target.
        var player = new net.minecraft.client.player.RemotePlayer(Minecraft.getInstance().level,
                new com.mojang.authlib.GameProfile(new java.util.UUID(0, 0), "WardenEcho"));
        player.setPose(data.pose()); player.setXRot(data.pitch()); player.tickCount = data.age();
        player.yHeadRot = data.headYaw(); player.yBodyRot = data.bodyYaw();
        if (data.flying()) player.startFallFlying();
        PlayerModel<AbstractClientPlayer> model = new PlayerModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
        model.crouching = data.pose() == net.minecraft.world.entity.Pose.CROUCHING;
        model.riding = data.riding(); model.attackTime = data.attack(); model.swimAmount = data.swim();
        model.setupAnim(player, data.limb(), data.speed(), data.age(), data.headYaw() - data.bodyYaw(), data.pitch());
        PoseStack pose = new PoseStack();
        pose.translate(data.position().x, data.position().y, data.position().z);
        pose.mulPose(Axis.YP.rotationDegrees(180 - data.bodyYaw()));
        if (data.pose() == net.minecraft.world.entity.Pose.SWIMMING || data.flying()) pose.mulPose(Axis.XP.rotationDegrees(-90 - data.pitch()));
        if (data.pose() == net.minecraft.world.entity.Pose.SLEEPING) pose.mulPose(Axis.XP.rotationDegrees(-90));
        pose.scale(-0.9375f, -0.9375f, 0.9375f); pose.translate(0, -1.501, 0);
        return new WardenEchoSnapshot(sample(model, pose), now, duration, origin,
                org.example.maniacrevolution.warden.WardenShriekerRules.RADIUS);
    }
    static List<Vec3> sample(PlayerModel<AbstractClientPlayer> model, PoseStack pose) {
        List<Vec3> points = new ArrayList<>();
        for (ModelPart part : List.of(model.head, model.body, model.leftArm, model.rightArm, model.leftLeg, model.rightLeg)) {
            part.visit(pose, (transform, path, index, cube) -> {
                float[] min = {cube.minX / 16, cube.minY / 16, cube.minZ / 16};
                float[] max = {cube.maxX / 16, cube.maxY / 16, cube.maxZ / 16};
                for (int axis = 0; axis < 3; axis++) {
                    int u = (axis + 1) % 3, v = (axis + 2) % 3;
                    for (int side = 0; side < 2; side++) {
                        int nu = Math.max(1, (int) Math.ceil((max[u] - min[u]) / 0.07));
                        int nv = Math.max(1, (int) Math.ceil((max[v] - min[v]) / 0.07));
                        for (int i = 0; i <= nu; i++) for (int j = 0; j <= nv; j++) {
                            float[] p = min.clone();
                            p[axis] = side == 0 ? min[axis] : max[axis];
                            p[u] += (max[u] - min[u]) * i / nu;
                            p[v] += (max[v] - min[v]) * j / nv;
                            Vector3f world = transform.pose().transformPosition(new Vector3f(p[0], p[1], p[2]));
                            points.add(new Vec3(world.x, world.y, world.z));
                        }
                    }
                }
            });
        }
        return points;
    }
}
