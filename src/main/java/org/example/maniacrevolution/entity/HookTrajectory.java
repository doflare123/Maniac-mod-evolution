package org.example.maniacrevolution.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Identical wall/target selection for the real hook and its local aiming preview. */
public final class HookTrajectory {
    private HookTrajectory() {}

    public static Vec3 launchPosition(Player player, float partialTick) {
        return launchPosition(player, partialTick, InteractionHand.MAIN_HAND);
    }

    public static Vec3 handOffset(float yaw, float pitch, boolean rightHand) {
        // Camera-local +X is LEFT and +Z is forward in Minecraft's world camera basis.
        var rotation = new Quaternionf().rotationYXZ(-yaw * Mth.DEG_TO_RAD, pitch * Mth.DEG_TO_RAD, 0);
        var offset = new Vector3f(rightHand ? -0.28f : 0.28f, -0.20f, 0.55f).rotate(rotation);
        return new Vec3(offset);
    }

    public static Vec3 launchPosition(Player player, float partialTick, InteractionHand hand) {
        boolean right = player.getMainArm() == HumanoidArm.RIGHT;
        if (hand == InteractionHand.OFF_HAND) right = !right;
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 desired = eye.add(handOffset(player.getViewYRot(partialTick), player.getViewXRot(partialTick), right));
        var obstruction = player.level().clip(new ClipContext(eye, desired, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        return obstruction.getType() == HitResult.Type.MISS ? desired
                : obstruction.getLocation().subtract(desired.subtract(eye).normalize().scale(0.03));
    }

    public static Vec3 aimDirection(Player player, float partialTick, InteractionHand hand) {
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 look = player.getViewVector(partialTick);
        HitResult target = trace(player, player, eye, eye.add(look.scale(HookEntity.MAX_DISTANCE + 0.5)));
        Vec3 direction = target.getLocation().subtract(launchPosition(player, partialTick, hand));
        return direction.lengthSqr() < 1.0E-6 ? look : direction.normalize();
    }

    public static HitResult groundProjection(Entity source, Vec3 point) {
        return source.level().clip(new ClipContext(point,
                new Vec3(point.x, source.level().getMinBuildHeight(), point.z),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, source));
    }

    public static HitResult trace(Entity source, Player owner, Vec3 start, Vec3 end) {
        var block = source.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, source));
        Vec3 limit = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        var hit = ProjectileUtil.getEntityHitResult(source.level(), source, start, limit,
                new AABB(start, limit).inflate(1.125),
                entity -> entity instanceof LivingEntity && entity != owner && entity.isAlive() && !entity.isSpectator());
        if (hit == null) return block;
        Entity target = hit.getEntity();
        Vec3 contact = target.getBoundingBox().inflate(0.3).clip(start, limit).orElse(hit.getLocation());
        return new EntityHitResult(target, contact);
    }
}
