package org.example.maniacrevolution.atmosphere.client;

import foundry.veil.api.client.render.VeilRenderSystem;
import net.minecraft.client.Minecraft;

/** Veil 1.0.0.296 resolves its world snapshot after vanilla outlines, erasing them even with no active pipeline. */
public final class VeilPostOrder {
    private static boolean earlyPass, skipLatePass;
    private static long frames;

    private VeilPostOrder() {}

    public static void beforeOutlines() {
        if (VeilRenderControl.isBypassed()) return;
        earlyPass = true;
        try {
            VeilRenderSystem.renderPost();
            frames++;
        } finally {
            // Veil clears its framebuffer binding after an active pipeline. Vanilla outlines
            // must draw back into the main target, with its full-resolution viewport restored.
            Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
            earlyPass = false;
            skipLatePass = true;
        }
    }

    public static boolean skipLatePass() {
        if (earlyPass || !skipLatePass) return false;
        skipLatePass = false;
        return true;
    }

    public static long frames() { return frames; }
}
