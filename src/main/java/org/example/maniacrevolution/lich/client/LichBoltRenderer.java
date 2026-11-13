package org.example.maniacrevolution.lich.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.lich.LichBolt;

/** Small emissive jade diamond; procedural geometry needs no separate model asset. */
public final class LichBoltRenderer extends EntityRenderer<LichBolt> {
    public LichBoltRenderer(EntityRendererProvider.Context context) {super(context);}
    @Override public ResourceLocation getTextureLocation(LichBolt entity) {return Maniacrev.loc("textures/entity/lich_necromancy_fx.png");}
    @Override public void render(LichBolt entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
        pose.pushPose();
        try {
            pose.mulPose(entityRenderDispatcher.cameraOrientation());
            pose.mulPose(Axis.ZP.rotation((entity.tickCount+partial)*.08f));
            var vertices=buffers.getBuffer(RenderType.lightning());var matrix=pose.last().pose();
            float[] radii={.19f,.10f,.035f};int[][] colors={{55,171,133,65},{103,237,183,220},{224,255,239,255}};
            for(int i=0;i<radii.length;i++) {
                float r=radii[i];int[] c=colors[i];
                vertices.vertex(matrix,-r,0,0).color(c[0],c[1],c[2],c[3]).endVertex();
                vertices.vertex(matrix,0,-r,0).color(c[0],c[1],c[2],c[3]).endVertex();
                vertices.vertex(matrix,r,0,0).color(c[0],c[1],c[2],c[3]).endVertex();
                vertices.vertex(matrix,0,r,0).color(c[0],c[1],c[2],c[3]).endVertex();
            }
        } finally {pose.popPose();}
        super.render(entity,yaw,partial,pose,buffers,light);
    }
}
