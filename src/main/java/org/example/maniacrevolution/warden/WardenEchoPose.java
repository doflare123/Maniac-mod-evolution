package org.example.maniacrevolution.warden;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;

/** Frozen server pose inputs. No later entity lookup is needed to draw an echo. */
public record WardenEchoPose(Vec3 position, float bodyYaw, float headYaw, float pitch,
                             float limb, float speed, float attack, float swim, int age,
                             Pose pose, boolean riding, boolean flying) {
    public static WardenEchoPose capture(ServerPlayer player) {
        double dx = player.getX() - player.xo, dz = player.getZ() - player.zo;
        return new WardenEchoPose(player.position(), player.yBodyRot, player.yHeadRot, player.getXRot(),
                player.walkDist * 6, (float) Math.min(1, Math.sqrt(dx * dx + dz * dz) * 4),
                player.getAttackAnim(1), player.getSwimAmount(1), player.tickCount,
                player.getPose(), player.isPassenger(), player.isFallFlying());
    }
    public boolean valid() {
        return position != null && Double.isFinite(position.x) && Double.isFinite(position.y) && Double.isFinite(position.z)
                && Float.isFinite(bodyYaw) && Float.isFinite(headYaw) && Float.isFinite(pitch)
                && Float.isFinite(limb) && Float.isFinite(speed) && speed >= 0 && speed <= 1
                && Float.isFinite(attack) && attack >= 0 && attack <= 1
                && Float.isFinite(swim) && swim >= 0 && swim <= 1 && age >= 0 && pose != null;
    }
}
