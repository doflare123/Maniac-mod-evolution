package org.example.maniacrevolution.atmosphere.client;

import org.example.maniacrevolution.atmosphere.AtmosphereRules;
import org.example.maniacrevolution.settings.ClientGameSettings;
import org.example.maniacrevolution.settings.GraphicsMode;

/** Test-only linkage shim for a runtime with Veil physically absent. Never packaged in the mod. */
public final class AtmosphereClient {
    public static GraphicsMode graphicsMode() { return ClientGameSettings.getGraphicsMode(); }
    public static AtmosphereRules.Profile profile() { return AtmosphereRules.Profile.OFF; }
    public static boolean hasFogLimit() { return false; }
    public static float cameraCorruption() { return 0; }
    public static void chase(float ignored) {}
}
