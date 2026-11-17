package org.example.maniacrevolution.atmosphere.client;

import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.api.client.render.VeilRenderSystem;
import net.minecraft.client.Minecraft;

/** Diagnostic renderer bypass. Veil remains loaded, including its resources and other mixins. */
public final class VeilRenderControl {
    private static volatile boolean bypassed;
    private static long skippedCalls;
    private static int observedGates;

    private VeilRenderControl() {}

    public static boolean isBypassed() { return bypassed; }

    /** Called only by the render-thread gates; also proves that the installed gates are executing. */
    public static boolean skipRenderCall(int gate) {
        if (!bypassed) return false;
        skippedCalls++;
        observedGates |= gate;
        return true;
    }

    public static long skippedCalls() { return skippedCalls; }
    public static int observedGates() { return observedGates; }

    public static String unsupportedReason() {
        var renderer = VeilRenderSystem.renderer();
        if (renderer.getDeferredRenderer().isEnabled()) return "Включён отдельный Veil Deferred: отключите его ресурс-пак для этой проверки.";
        for (var entry : renderer.getPostProcessingManager().getActivePipelines()) {
            if (!entry.getPipeline().equals(AtmosphereClient.PIPELINE) && !entry.getPipeline().equals(AtmosphereClient.LIGHT_PIPELINE))
                return "У Veil активен сторонний эффект: " + entry.getPipeline();
        }
        return null;
    }

    public static void setBypassed(boolean value) {
        RenderSystem.assertOnRenderThread();
        if (value == bypassed) return;
        var renderer = VeilRenderSystem.renderer();
        if (value) {
            String reason = unsupportedReason();
            if (reason != null) throw new IllegalStateException(reason);
            renderer.getPostProcessingManager().remove(AtmosphereClient.PIPELINE);
            renderer.getPostProcessingManager().remove(AtmosphereClient.LIGHT_PIPELINE);
            renderer.getCameraMatrices().unbind();
            renderer.getGuiInfo().unbind();
            VeilRenderSystem.endFrame();
        }
        bypassed = value;
        if (value) observedGates = 0;
        // Discard transient bindings before drawing the first frame after re-enabling.
        if (!value) VeilRenderSystem.endFrame();
        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
    }
}
