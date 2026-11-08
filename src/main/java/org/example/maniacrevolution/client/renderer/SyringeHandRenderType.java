package org.example.maniacrevolution.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterShadersEvent;
import org.example.maniacrevolution.Maniacrev;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** First-person hands use world light without the entity shader's two directional lamps. */
final class SyringeHandRenderType extends RenderType {
    private static ShaderInstance shader;
    private record Key(ResourceLocation texture, boolean opaque) {}
    private static final Map<Key, RenderType> TYPES = new HashMap<>();

    private SyringeHandRenderType() {
        super("unused", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true, () -> {}, () -> {});
    }

    static void register(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(), Maniacrev.loc("syringe_hand"),
                DefaultVertexFormat.NEW_ENTITY), loaded -> shader = loaded);
    }

    static RenderType texture(ResourceLocation texture) {
        return texture(texture, false);
    }

    static RenderType skin(ResourceLocation texture) {
        return texture(texture, true);
    }

    private static RenderType texture(ResourceLocation texture, boolean opaque) {
        return TYPES.computeIfAbsent(new Key(texture, opaque), key -> create("maniacrev_syringe_hand" + (opaque ? "_skin" : ""),
                DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, !opaque,
                CompositeState.builder().setShaderState(new ShaderStateShard(() -> shader))
                        .setTextureState(new TextureStateShard(key.texture(), false, false))
                        .setTransparencyState(opaque ? NO_TRANSPARENCY : TRANSLUCENT_TRANSPARENCY)
                        .setCullState(opaque ? CULL : NO_CULL)
                        .setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).createCompositeState(false)));
    }
}
