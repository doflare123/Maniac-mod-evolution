package org.example.maniacrevolution.strange.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.strange.StrangeEffectEntity;

public final class StrangeEffectRenderer extends EntityRenderer<StrangeEffectEntity> {
    public StrangeEffectRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
    }
    @Override public ResourceLocation getTextureLocation(StrangeEffectEntity entity) { return Maniacrev.loc("textures/entity/strange_magic.png"); }
    @Override public void render(StrangeEffectEntity entity, float yaw, float partial, PoseStack poses, MultiBufferSource buffers, int light) {
        // Arm effects are drawn by the player layer, under the actual hand transform.
        if (entity.kind() != StrangeEffectEntity.PORTAL) return;
        PortalVisual.render(entity, partial, poses, buffers);
    }
}
