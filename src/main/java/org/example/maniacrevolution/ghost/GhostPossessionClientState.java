package org.example.maniacrevolution.ghost;

import net.minecraft.client.Minecraft;

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
    }

    public static boolean isVictimControlled() {
        return active && !controller;
    }

    public static boolean isControllerActive() {
        return active && controller;
    }

    public static int getTargetEntityId() {
        return targetEntityId;
    }

    public static void tick(Minecraft mc) {
        // Камеру на жертву не переносим:
        // это ломает валидацию атак/взаимодействий и может полностью сбить управление.
    }
}
