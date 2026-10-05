package org.example.maniacrevolution.scp173;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;

/** Same eye ray on client and server; nearest body and solid blocks occlude the strike. */
public final class Scp173CombatGeometry {
    public static final double REACH = 1.5;
    private Scp173CombatGeometry() {}
    public static LivingEntity target(Player player) {
        var start = player.getEyePosition();
        var end = player.level().clip(new ClipContext(start, start.add(player.getLookAngle().scale(REACH)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();
        LivingEntity nearest = null; double distance = start.distanceToSqr(end);
        for (var entity : player.level().getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(0.2),
                e -> e != player && e.isAlive() && e.isPickable())) {
            var contact = entity.getBoundingBox().contains(start) ? java.util.Optional.of(start)
                    : entity.getBoundingBox().clip(start, end);
            if (contact.isPresent() && start.distanceToSqr(contact.get()) <= distance) {
                distance = start.distanceToSqr(contact.get()); nearest = entity;
            }
        }
        if (nearest instanceof Player target && (target.isCreative() || target.isSpectator()
                || target.getTeam() == null || !"survivors".equals(target.getTeam().getName()))) return null;
        return nearest;
    }
}
