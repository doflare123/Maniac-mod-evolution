package org.example.maniacrevolution.warden.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import net.minecraft.client.Minecraft;

/** Keep the actual world's occlusion, even when a bounded geometry capture is incomplete. */
final class WardenWorldDepth {
    private RenderTarget snapshot;
    private boolean ready;
    boolean ready() { return ready; }
    void capture(RenderTarget world) {
        if (snapshot == null || snapshot.width != world.width || snapshot.height != world.height
                || snapshot.isStencilEnabled() != world.isStencilEnabled()) {
            clear();
            snapshot = new TextureTarget(world.width, world.height, true, Minecraft.ON_OSX);
            if (world.isStencilEnabled()) snapshot.enableStencil();
        }
        snapshot.copyDepthFrom(world);
        world.bindWrite(false);
        ready = true;
    }
    boolean restore(RenderTarget world) {
        if (!ready || snapshot == null || snapshot.width != world.width || snapshot.height != world.height) return false;
        world.copyDepthFrom(snapshot);
        world.bindWrite(false);
        return true;
    }
    void clear() {
        ready = false;
        if (snapshot != null) snapshot.destroyBuffers();
        snapshot = null;
    }
}
