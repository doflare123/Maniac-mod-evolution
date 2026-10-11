package org.example.maniacrevolution.techies.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

final class TechiesRenderTypes extends RenderType {
    private TechiesRenderTypes() { super("unused",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,256,false,false,()->{},()->{}); }
    static RenderType throughWall(ResourceLocation texture) {
        return create("techies_target",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,256,false,true,
                CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                        .setTextureState(new TextureStateShard(texture,false,false)).setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(NO_DEPTH_TEST).setCullState(NO_CULL).setLightmapState(LIGHTMAP)
                        .setOverlayState(OVERLAY).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    }
}
