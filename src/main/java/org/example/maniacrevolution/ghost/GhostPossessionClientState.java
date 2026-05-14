package org.example.maniacrevolution.ghost;

import net.minecraft.client.Minecraft;

public final class GhostPossessionClientState {
    private static boolean active = false;
    private static boolean controller = false;

    private GhostPossessionClientState() {
    }

    public static void apply(boolean isActive, boolean isController, int entityId) {
        active = isActive;
        controller = isController;
    }

    public static void clear() {
        active = false;
        controller = false;
    }

    public static boolean isVictimControlled() {
        return active && !controller;
    }

    public static boolean isControllerActive() {
        return active && controller;
    }

    public static void tick(Minecraft mc) {
        // Камеру на жертву в тестовом прототипе больше не переносим:
        // это ломает валидацию interact/attack пакетов и может кикать игрока.
    }
}
