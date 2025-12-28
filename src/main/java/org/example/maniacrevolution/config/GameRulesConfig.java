package org.example.maniacrevolution.config;

public class GameRulesConfig {
    private static boolean allowItemDrop = true;
    private static boolean allowHitboxDebug = false;

    public static boolean isItemDropAllowed() {
        return allowItemDrop;
    }

    public static void setItemDropAllowed(boolean allowed) {
        allowItemDrop = allowed;
        System.out.println("[Game Rules] Item dropping " + (allowed ? "enabled" : "disabled"));
    }

    public static boolean isHitboxDebugAllowed() {
        return allowHitboxDebug;
    }

    public static void setHitboxDebugAllowed(boolean allowed) {
        allowHitboxDebug = allowed;
        System.out.println("[Game Rules] Hitbox debug " + (allowed ? "enabled" : "disabled"));
    }
}