package org.example.maniacrevolution.pinkorchid;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Один серверный снимок маршрута, записываемый каждый игровой тик. */
public record PinkOrchidRouteFrame(
        Vec3 position,
        float bodyYaw,
        float headYaw,
        float pitch,
        Pose pose,
        boolean sprinting,
        boolean swinging,
        InteractionHand swingingHand,
        boolean usingItem,
        InteractionHand usedHand,
        ItemStack mainHand,
        ItemStack offHand
) {
    public PinkOrchidRouteFrame copyFrame() {
        return new PinkOrchidRouteFrame(
                position,
                bodyYaw,
                headYaw,
                pitch,
                pose,
                sprinting,
                swinging,
                swingingHand,
                usingItem,
                usedHand,
                mainHand.copy(),
                offHand.copy()
        );
    }
}
