package org.example.maniacrevolution.warden.client;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

import java.util.function.Consumer;

/** Retained world-space buffers: the HUD pass reuses the frame instead of rebuilding it. */
final class WardenSceneBuffers {
    enum Layer { BLOCK_DEPTH, DYNAMIC_DEPTH, POINTS, WAVE, ECHOES, SCENT_DEPTH, SCENT }
    private final VertexBuffer[] buffers = new VertexBuffer[Layer.values().length];
    private final boolean[] populated = new boolean[buffers.length];
    private final BufferBuilder builder = new BufferBuilder(262144);
    private WardenSurfaceCache owner;
    private long blockRevision = -1, dynamicRevision = -1;

    boolean blocksChanged(WardenSurfaceCache cache) {
        if (owner == cache && blockRevision == cache.revision()) return false;
        owner = cache;
        blockRevision = cache.revision();
        return true;
    }

    boolean dynamicChanged(long revision) {
        if (dynamicRevision == revision) return false;
        dynamicRevision = revision;
        return true;
    }

    void upload(Layer layer, Consumer<BufferBuilder> writer) {
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        writer.accept(builder);
        var result = builder.end();
        int index = layer.ordinal();
        populated[index] = result.drawState().vertexCount() > 0;
        if (!populated[index]) { result.release(); return; }
        if (buffers[index] == null) buffers[index] = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        try {
            buffers[index].bind();
            buffers[index].upload(result);
        } finally {
            VertexBuffer.unbind();
        }
    }

    void draw(Layer layer, Matrix4f view, Matrix4f projection) {
        int index = layer.ordinal();
        if (!populated[index]) return;
        try {
            buffers[index].bind();
            buffers[index].drawWithShader(view, projection, GameRenderer.getPositionColorShader());
        } finally {
            VertexBuffer.unbind();
        }
    }

    void clear() {
        for (int i = 0; i < buffers.length; i++) {
            if (buffers[i] != null) buffers[i].close();
            buffers[i] = null;
            populated[i] = false;
        }
        owner = null;
        blockRevision = dynamicRevision = -1;
    }
    void clear(Layer layer) {
        int index = layer.ordinal();
        if (buffers[index] != null) buffers[index].close();
        buffers[index] = null; populated[index] = false;
    }
}
