package org.example.maniacrevolution.config;

public class HudConfig {
    private static boolean customHudEnabled = true;

    public static boolean isCustomHudEnabled() {
        return customHudEnabled;
    }

    public static void setCustomHudEnabled(boolean enabled) {
        customHudEnabled = enabled;
        System.out.println("[HUD Config] Custom HUD " + (enabled ? "enabled" : "disabled"));
    }

    public static void toggleCustomHud() {
        customHudEnabled = !customHudEnabled;
    }
}