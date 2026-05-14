package org.example.maniacrevolution.ghost;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public final class GhostPossessionClientState {
    private static boolean active = false;
    private static boolean controller = false;
    private static int targetEntityId = -1;

    private GhostPossessionClientState() {
    }

    public static void apply(boolean isActive, boolean isController, int entityId) {
        active = isActive;
        controller = isController;
        targetEntityId = entityId;
    }

    public static void clear() {
        active = false;
        controller = false;
        targetEntityId = -1;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.getCameraEntity() != mc.player) {
            mc.setCameraEntity(mc.player);
        }
    }

    public static boolean isVictimControlled() {
        return active && !controller;
    }

    public static void tick(Minecraft mc) {
        if (!active || mc.player == null) {
            return;
        }

        if (controller && mc.level != null) {
            Entity target = mc.level.getEntity(targetEntityId);
            if (target != null && mc.getCameraEntity() != target) {
                mc.setCameraEntity(target);
            }
        }
    }
}
